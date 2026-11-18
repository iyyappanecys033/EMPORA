package com.examly.springapp;

import com.examly.springapp.dto.AuditVerificationReport;
import com.examly.springapp.dto.ServiceTicketDTO;
import com.examly.springapp.dto.TicketStatusUpdateRequestDTO;
import com.examly.springapp.exception.OptimisticLockingConflictException;
import com.examly.springapp.model.*;
import com.examly.springapp.notification.TicketNotificationEvent;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.AuditChainService;
import com.examly.springapp.service.TicketCommandService;
import com.examly.springapp.workflow.SlaScheduledProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P9.6 Stages D & M — Transaction boundary integrity (AFTER_COMMIT rollback safety),
 * notification deduplication, optimistic locking concurrency, concurrent audit chain
 * serialization, and deterministic SlaScheduledProcessor idempotency.
 */
@SpringBootTest
@ActiveProfiles("test")
class P96TransactionReliabilityTests {

    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private InAppAlertRepository alertRepository;
    @Autowired private AuditChainService auditChainService;
    @Autowired private ServiceTicketRepository ticketRepository;
    @Autowired private ServiceCatalogRepository catalogRepository;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserRoleMappingRepo userRoleMappingRepo;
    @Autowired private TicketCommandService ticketCommandService;
    @Autowired private SlaScheduledProcessor slaScheduledProcessor;
    @Autowired private PasswordEncoder passwordEncoder;

    private UserAccount requester;
    private UserAccount resolver;
    private ServiceCatalogItem catalogItem;

    @BeforeEach
    void setUp() {
        requester = ensureUser("p96_tx_req", "EMPLOYEE");
        resolver = ensureUser("p96_tx_res", "IT_SUPPORT");
        catalogItem = catalogRepository.findByItemCode("P96-TX-01").orElseGet(() -> {
            ServiceCatalogItem item = new ServiceCatalogItem();
            item.setItemCode("P96-TX-01");
            item.setTitle("Reliability Test Service");
            item.setCategory("Infrastructure");
            item.setDepartment("IT");
            item.setDescription("Service used for transaction and concurrency tests.");
            item.setEstimatedCost(BigDecimal.ZERO);
            item.setSlaTargetHours(12);
            item.setRequiresManagerApproval(false);
            item.setRequiresSecondaryApproval(false);
            item.setActive(true);
            return catalogRepository.save(item);
        });
    }

    private UserAccount ensureUser(String username, String roleName) {
        return userAccountRepo.findByUsername(username).orElseGet(() -> {
            UserAccount ua = new UserAccount();
            ua.setUsername(username);
            ua.setPasswordHash(passwordEncoder.encode("TxPass!2026"));
            ua.setEmail(username + "@epams.test");
            ua.setPhone("9222233333");
            ua.setStatus(AccountStatus.ACTIVE);
            ua = userAccountRepo.save(ua);

            Role role = roleRepo.findByRoleName(roleName).orElseGet(() -> {
                Role r = new Role();
                r.setRoleName(roleName);
                return roleRepo.save(r);
            });
            UserRoleMapping m = new UserRoleMapping();
            m.setUser(ua);
            m.setRole(role);
            userRoleMappingRepo.save(m);
            return ua;
        });
    }

    @Test
    @DisplayName("P96-TX-01: Rolled-back business transaction leaves ZERO phantom AFTER_COMMIT notifications")
    void testRollbackLeavesNoPhantomNotification() {
        String uniqueKey = "ROLLBACK_PHANTOM_KEY_" + System.nanoTime();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThrows(RuntimeException.class, () -> txTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new TicketNotificationEvent(
                    99999L,
                    "TKT-ROLLBACK-01",
                    requester.getUsername(),
                    "TICKET_SUBMITTED",
                    "Should Never Persist",
                    "Published before forced rollback.",
                    "TICKET",
                    99999L,
                    uniqueKey
            ));
            throw new RuntimeException("Simulated database failure forcing rollback");
        }));

        assertFalse(alertRepository.existsByRecipientIdAndDedupKey(requester.getId(), uniqueKey),
                "AFTER_COMMIT listener must NEVER persist a notification for a rolled-back transaction");
    }

    @Test
    @DisplayName("P96-TX-02: Duplicate notification events with identical deduplicationKey persist strictly once")
    void testNotificationEventDeduplication() {
        String dupKey = "P96_DEDUP_KEY_" + System.nanoTime();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        long before = alertRepository.count();
        for (int i = 0; i < 3; i++) {
            txTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new TicketNotificationEvent(
                    88888L,
                    "TKT-DEDUP-01",
                    requester.getUsername(),
                    "TICKET_SUBMITTED",
                    "Deduplicated Alert",
                    "Duplicate delivery attempt.",
                    "TICKET",
                    88888L,
                    dupKey
            )));
        }

        assertTrue(alertRepository.existsByRecipientIdAndDedupKey(requester.getId(), dupKey));
        assertEquals(before + 1, alertRepository.count(), "Duplicate events must create exactly 1 persisted alert");
    }

    @Test
    @DisplayName("P96-TX-03: Optimistic locking rejects stale concurrent version update without silent overwrite")
    void testOptimisticLockingPreventsSilentOverwrite() {
        ServiceTicket t = new ServiceTicket();
        t.setTicketNumber("TKT-P96-LOCK-" + System.nanoTime() % 100000);
        t.setCatalogItem(catalogItem);
        t.setRequester(requester);
        t.setAssignee(resolver);
        t.setDepartment("IT");
        t.setPriority(TicketPriority.MEDIUM);
        t.setStatus(TicketStatus.ASSIGNED);
        t.setDescription("Concurrency conflict ticket for optimistic locking verification.");
        t.setCreatedAt(LocalDateTime.now());
        t.setSlaTargetDate(LocalDateTime.now().plusHours(12));
        ServiceTicket saved = ticketRepository.saveAndFlush(t);
        Long initialVersion = saved.getVersion();

        // First transition succeeds and increments version
        TicketStatusUpdateRequestDTO req1 = new TicketStatusUpdateRequestDTO();
        req1.setStatus(TicketStatus.IN_PROGRESS);
        req1.setVersion(initialVersion);
        ServiceTicketDTO updated = ticketCommandService.updateStatus(saved.getId(), req1, resolver.getUsername());
        assertTrue(updated.getVersion() > initialVersion);

        // Stale version update must fail with OptimisticLockingConflictException (never silently overwrite)
        TicketStatusUpdateRequestDTO staleReq = new TicketStatusUpdateRequestDTO();
        staleReq.setStatus(TicketStatus.ON_HOLD);
        staleReq.setReason("Stale concurrent update attempt exceeding fifteen chars.");
        staleReq.setVersion(initialVersion);
        Exception ex = assertThrows(Exception.class, () ->
                ticketCommandService.updateStatus(saved.getId(), staleReq, resolver.getUsername()));
        assertTrue(ex instanceof OptimisticLockingConflictException
                        || ex instanceof ObjectOptimisticLockingFailureException,
                "Expected optimistic locking conflict, got: " + ex.getClass().getName());
    }

    @Test
    @DisplayName("P96-TX-04: Concurrent audit chain appends under 8 parallel threads preserve gapless SHA-256 chain")
    void testConcurrentAuditChainAppendsPreserveIntegrity() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<AuditLog>> futures = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                startGate.await();
                return auditChainService.recordChainedEvent(
                        "p96_worker_" + idx,
                        "P96_CONCURRENT_EVENT",
                        "p96_target_" + idx,
                        "Parallel audit write #" + idx
                );
            }));
        }

        startGate.countDown();
        List<Long> sequences = new ArrayList<>();
        for (Future<AuditLog> f : futures) {
            AuditLog log = f.get(15, TimeUnit.SECONDS);
            assertNotNull(log.getSequenceNumber());
            assertNotNull(log.getCurrentHash());
            sequences.add(log.getSequenceNumber());
        }
        pool.shutdown();

        assertEquals(8, sequences.stream().distinct().count(), "All 8 concurrent appends must get distinct sequence numbers");
        AuditVerificationReport report = auditChainService.verifyChain();
        assertTrue(report.isValid(), "Audit chain must remain 100% valid after concurrent writes: " + report.getViolations());
    }

    @Test
    @DisplayName("P96-TX-05: SlaScheduledProcessor deterministic auto-closure, ON_HOLD exclusion, and SLA breach alert idempotency")
    void testSlaScheduledProcessorDeterminismAndIdempotency() {
        LocalDateTime resolvedMon = LocalDateTime.of(2026, 6, 1, 10, 0);

        // Ticket A: RESOLVED past 48 business hours -> must auto-close
        ServiceTicket expiredResolved = new ServiceTicket();
        expiredResolved.setTicketNumber("TKT-P96-AUTO-" + System.nanoTime() % 100000);
        expiredResolved.setCatalogItem(catalogItem);
        expiredResolved.setRequester(requester);
        expiredResolved.setAssignee(resolver);
        expiredResolved.setDepartment("IT");
        expiredResolved.setPriority(TicketPriority.LOW);
        expiredResolved.setStatus(TicketStatus.RESOLVED);
        expiredResolved.setDescription("Resolved ticket eligible for 48-business-hour auto-closure.");
        expiredResolved.setCreatedAt(resolvedMon.minusDays(1));
        expiredResolved.setResolvedAt(resolvedMon);
        expiredResolved.setSlaTargetDate(resolvedMon.plusDays(1));
        expiredResolved = ticketRepository.saveAndFlush(expiredResolved);

        // Ticket B: ON_HOLD past SLA -> must be EXCLUDED from SLA breach alerts
        ServiceTicket pausedTicket = new ServiceTicket();
        pausedTicket.setTicketNumber("TKT-P96-HOLD-" + System.nanoTime() % 100000);
        pausedTicket.setCatalogItem(catalogItem);
        pausedTicket.setRequester(requester);
        pausedTicket.setAssignee(resolver);
        pausedTicket.setDepartment("IT");
        pausedTicket.setPriority(TicketPriority.HIGH);
        pausedTicket.setStatus(TicketStatus.ON_HOLD);
        pausedTicket.setDescription("Paused ticket whose SLA clock must not breach.");
        pausedTicket.setCreatedAt(resolvedMon.minusDays(2));
        pausedTicket.setSlaTargetDate(resolvedMon.minusHours(5));
        pausedTicket = ticketRepository.saveAndFlush(pausedTicket);

        // Ticket C: IN_PROGRESS past SLA -> must emit SLA_BREACHED alert once (idempotent)
        ServiceTicket breachedTicket = new ServiceTicket();
        breachedTicket.setTicketNumber("TKT-P96-BREACH-" + System.nanoTime() % 100000);
        breachedTicket.setCatalogItem(catalogItem);
        breachedTicket.setRequester(requester);
        breachedTicket.setAssignee(resolver);
        breachedTicket.setDepartment("IT");
        breachedTicket.setPriority(TicketPriority.CRITICAL);
        breachedTicket.setStatus(TicketStatus.IN_PROGRESS);
        breachedTicket.setDescription("Active ticket past SLA deadline for breach scan.");
        breachedTicket.setCreatedAt(resolvedMon.minusDays(2));
        breachedTicket.setSlaTargetDate(resolvedMon.minusHours(2));
        breachedTicket = ticketRepository.saveAndFlush(breachedTicket);

        LocalDateTime evalTime = LocalDateTime.of(2026, 6, 15, 14, 0); // Well past 48 business hours

        SlaScheduledProcessor.SlaJobSummary closeRun1 = slaScheduledProcessor.processAutoClosures(evalTime);
        assertTrue(closeRun1.processed() >= 1);
        assertEquals(0, closeRun1.failed());

        ServiceTicket afterClose = ticketRepository.findById(expiredResolved.getId()).orElseThrow();
        assertEquals(TicketStatus.CLOSED, afterClose.getStatus());
        assertNotNull(afterClose.getClosedAt());

        // Second run is strictly idempotent
        SlaScheduledProcessor.SlaJobSummary closeRun2 = slaScheduledProcessor.processAutoClosures(evalTime);
        assertEquals(0, closeRun2.processed());

        // SLA breach scanner alerts breachedTicket, ignores pausedTicket, and deduplicates across reruns
        SlaScheduledProcessor.SlaJobSummary alertRun1 = slaScheduledProcessor.processSlaAlerts(evalTime);
        assertTrue(alertRun1.processed() >= 1);
        assertTrue(alertRepository.existsByRecipientIdAndDedupKey(resolver.getId(), "SLA_BREACH_" + breachedTicket.getId()));
        assertFalse(alertRepository.existsByRecipientIdAndDedupKey(resolver.getId(), "SLA_BREACH_" + pausedTicket.getId()),
                "ON_HOLD ticket must NEVER trigger SLA breach alert");

        long countBeforeSecondAlert = alertRepository.count();
        slaScheduledProcessor.processSlaAlerts(evalTime);
        assertEquals(countBeforeSecondAlert, alertRepository.count(),
                "Repeated SLA alert scans must not create duplicate alerts");
    }
}
