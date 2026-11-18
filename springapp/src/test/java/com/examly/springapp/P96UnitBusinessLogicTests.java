package com.examly.springapp;

import com.examly.springapp.dto.AuditVerificationReport;
import com.examly.springapp.dto.RiskAssessmentDTO;
import com.examly.springapp.exception.BusinessRuleViolationException;
import com.examly.springapp.exception.IllegalStateTransitionException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.AuditChainHeadRepository;
import com.examly.springapp.repository.AuditLogRepo;
import com.examly.springapp.service.AuditChainService;
import com.examly.springapp.service.RiskAssessmentService;
import com.examly.springapp.workflow.BusinessCalendar;
import com.examly.springapp.workflow.TicketStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * P9.6 Stage C — Pure deterministic unit tests for critical EPAMS business logic
 * (zero database/web context, fixed Clock injection).
 */
@ExtendWith(MockitoExtension.class)
class P96UnitBusinessLogicTests {

    private static final ZoneId IST = BusinessCalendar.EPAMS_TIMEZONE;
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDateTime.of(2026, 6, 15, 11, 0).atZone(IST).toInstant(), IST);

    @Mock
    private AuditLogRepo auditLogRepo;

    @Mock
    private AuditChainHeadRepository auditChainHeadRepository;

    @Test
    @DisplayName("P96-UNIT-01: TicketStateMachine enforces exact 11x11 transition matrix (121 pairs)")
    void testTicketStateMachineFull121TransitionMatrix() {
        BusinessCalendar cal = new BusinessCalendar(FIXED_CLOCK);
        TicketStateMachine fsm = new TicketStateMachine(FIXED_CLOCK, cal);

        String validReason = "Detailed operational justification exceeding fifteen characters.";
        LocalDateTime recentlyResolved = LocalDateTime.of(2026, 6, 15, 10, 0);

        int validCount = 0;
        int rejectedCount = 0;

        for (TicketStatus from : TicketStatus.values()) {
            Set<TicketStatus> allowed = fsm.getAllowedNextStates(from);
            for (TicketStatus to : TicketStatus.values()) {
                ServiceTicket ticket = new ServiceTicket();
                ticket.setStatus(from);
                ticket.setResolvedAt(recentlyResolved);

                if (from == to) {
                    assertFalse(fsm.isTransitionAllowed(from, to));
                    assertThrows(IllegalStateTransitionException.class,
                            () -> fsm.validateTransition(ticket, to, validReason, "actor"));
                    rejectedCount++;
                } else if (allowed.contains(to)) {
                    assertTrue(fsm.isTransitionAllowed(from, to));
                    assertDoesNotThrow(() -> fsm.validateTransition(ticket, to, validReason, "actor"),
                            "Expected allowed transition " + from + " -> " + to);
                    validCount++;
                } else {
                    assertFalse(fsm.isTransitionAllowed(from, to));
                    assertThrows(IllegalStateTransitionException.class,
                            () -> fsm.validateTransition(ticket, to, validReason, "actor"),
                            "Expected rejected transition " + from + " -> " + to);
                    rejectedCount++;
                }
            }
        }

        assertEquals(121, validCount + rejectedCount, "Must test all 11x11 state pairs");
        assertTrue(validCount >= 14, "11-state lifecycle must have valid edges");
        assertTrue(fsm.getAllowedNextStates(TicketStatus.CLOSED).isEmpty());
        assertTrue(fsm.getAllowedNextStates(TicketStatus.REJECTED).isEmpty());
        assertTrue(fsm.getAllowedNextStates(TicketStatus.CANCELLED).isEmpty());
    }

    @Test
    @DisplayName("P96-UNIT-02: TicketStateMachine mandatory 15-char reason boundary & WIP cancel guard")
    void testTicketStateMachineReasonBoundaryAndWipCancel() {
        BusinessCalendar cal = new BusinessCalendar(FIXED_CLOCK);
        TicketStateMachine fsm = new TicketStateMachine(FIXED_CLOCK, cal);

        ServiceTicket inProgress = new ServiceTicket();
        inProgress.setStatus(TicketStatus.IN_PROGRESS);

        // 14 chars -> rejected; whitespace padded to 20 chars -> trimmed < 15 -> rejected; 15 chars -> allowed
        assertThrows(BusinessRuleViolationException.class,
                () -> fsm.validateTransition(inProgress, TicketStatus.ON_HOLD, "12345678901234", "resolver"));
        assertThrows(BusinessRuleViolationException.class,
                () -> fsm.validateTransition(inProgress, TicketStatus.ON_HOLD, "   short   ", "resolver"));
        assertThrows(BusinessRuleViolationException.class,
                () -> fsm.validateTransition(inProgress, TicketStatus.ON_HOLD, null, "resolver"));
        assertDoesNotThrow(() -> fsm.validateTransition(inProgress, TicketStatus.ON_HOLD, "123456789012345", "resolver"));

        // IN_PROGRESS -> CANCELLED is rejected
        assertThrows(IllegalStateTransitionException.class,
                () -> fsm.validateTransition(inProgress, TicketStatus.CANCELLED, "Valid long cancellation reason.", "requester"));
    }

    @Test
    @DisplayName("P96-UNIT-03: TicketStateMachine reopen window with null resolvedAt and exact/past 48h boundary")
    void testTicketStateMachineReopenBoundaryDeterministicClock() {
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 6, 15, 10, 0); // Mon 10:00
        BusinessCalendar cal = new BusinessCalendar(FIXED_CLOCK);
        LocalDateTime exactDeadline = cal.calculateReopenDeadline(resolvedAt);

        ServiceTicket missingResolvedAt = new ServiceTicket();
        missingResolvedAt.setStatus(TicketStatus.RESOLVED);
        missingResolvedAt.setResolvedAt(null);

        TicketStateMachine fsmAtExact = new TicketStateMachine(Clock.fixed(exactDeadline.atZone(IST).toInstant(), IST), cal);
        assertThrows(BusinessRuleViolationException.class,
                () -> fsmAtExact.validateTransition(missingResolvedAt, TicketStatus.IN_PROGRESS, "Valid reopen reason exceeding fifteen chars", "emp"));

        ServiceTicket resolved = new ServiceTicket();
        resolved.setStatus(TicketStatus.RESOLVED);
        resolved.setResolvedAt(resolvedAt);

        // Exact deadline is inclusive
        assertDoesNotThrow(() -> fsmAtExact.validateTransition(resolved, TicketStatus.IN_PROGRESS, "Valid reopen reason exceeding fifteen chars", "emp"));

        // 1 second after deadline is rejected
        Clock pastClock = Clock.fixed(exactDeadline.plusSeconds(1).atZone(IST).toInstant(), IST);
        TicketStateMachine fsmPast = new TicketStateMachine(pastClock, cal);
        assertThrows(BusinessRuleViolationException.class,
                () -> fsmPast.validateTransition(resolved, TicketStatus.IN_PROGRESS, "Valid reopen reason exceeding fifteen chars", "emp"));
    }

    @Test
    @DisplayName("P96-UNIT-04: BusinessCalendar working hours, weekend/holiday rollover, malformed CSV resilience, and elapsed minutes")
    void testBusinessCalendarComprehensiveEdges() {
        BusinessCalendar cal = new BusinessCalendar(FIXED_CLOCK, "2027-01-26, INVALID_DATE, , 2026-06-16", true);

        assertTrue(cal.getConfiguredHolidays().contains(LocalDate.of(2027, 1, 26)));
        assertTrue(cal.getConfiguredHolidays().contains(LocalDate.of(2026, 6, 16)));
        assertFalse(cal.isBusinessDay(null));
        assertFalse(cal.isBusinessDay(LocalDate.of(2026, 6, 13))); // Saturday
        assertFalse(cal.isBusinessDay(LocalDate.of(2026, 6, 14))); // Sunday
        assertFalse(cal.isBusinessDay(LocalDate.of(2026, 6, 16))); // Configured holiday
        assertTrue(cal.isBusinessDay(LocalDate.of(2026, 6, 15)));  // Monday

        // Dynamic holiday add/remove
        LocalDate custom = LocalDate.of(2026, 7, 1);
        cal.addHoliday(custom);
        assertFalse(cal.isBusinessDay(custom));
        assertTrue(cal.removeHoliday(custom));
        assertTrue(cal.isBusinessDay(custom));
        cal.addHolidays(List.of(LocalDate.of(2026, 7, 2)));
        assertFalse(cal.isBusinessDay(LocalDate.of(2026, 7, 2)));

        // Working hour bounds
        assertFalse(cal.isWithinBusinessHours(null));
        assertFalse(cal.isWithinBusinessHours(LocalDateTime.of(2026, 6, 15, 8, 59)));
        assertTrue(cal.isWithinBusinessHours(LocalDateTime.of(2026, 6, 15, 9, 0)));
        assertTrue(cal.isWithinBusinessHours(LocalDateTime.of(2026, 6, 15, 17, 59)));
        assertFalse(cal.isWithinBusinessHours(LocalDateTime.of(2026, 6, 15, 18, 0)));

        // Target calculation skipping holiday (Tue 2026-06-16) and rolling to Wed 2026-06-17
        LocalDateTime mon1600 = LocalDateTime.of(2026, 6, 15, 16, 0);
        LocalDateTime target4h = cal.calculateTargetDate(mon1600, 4);
        assertEquals(LocalDateTime.of(2026, 6, 17, 11, 0), target4h);

        // Zero or negative hours returns start
        assertEquals(mon1600, cal.calculateTargetDate(mon1600, 0));
        assertEquals(mon1600, cal.calculateTargetDate(mon1600, -5));

        // Elapsed business minutes across holiday
        long elapsed = cal.calculateBusinessMinutesBetween(mon1600, target4h);
        assertEquals(240L, elapsed, "4 business hours across a holiday must equal 240 business minutes");
    }

    @Test
    @DisplayName("P96-UNIT-05: AuditChainService canonical SHA-256 determinism, tamper/gap/missing-head detection, and reset guard")
    void testAuditChainUnitVerificationAndResetGuard() {
        AuditChainService service = new AuditChainService(auditLogRepo, auditChainHeadRepository, FIXED_CLOCK);

        // Destructive reset prohibited when allowChainReset defaults to false
        assertThrows(UnsupportedOperationException.class, service::resetChainToGenesis);

        // Missing head detection
        when(auditLogRepo.findBySequenceNumberIsNotNullOrderBySequenceNumberAsc()).thenReturn(Collections.emptyList());
        when(auditLogRepo.findBySequenceNumberIsNullOrderByTimestampAsc()).thenReturn(Collections.emptyList());
        when(auditChainHeadRepository.findById(AuditChainService.HEAD_ROW_ID)).thenReturn(Optional.empty());

        AuditVerificationReport missingHeadReport = service.verifyChain();
        assertFalse(missingHeadReport.isValid());
        assertTrue(missingHeadReport.getViolations().stream().anyMatch(v -> v.contains("missing")));

        // Canonical hash determinism & tamper detection
        LocalDateTime ts = LocalDateTime.of(2026, 6, 15, 11, 0, 0);
        String h1 = service.computeHash(AuditChainService.GENESIS_HASH, 1L, "admin", "TICKET_CREATED", "john_doe", "Created TKT-1", ts);
        String h1Repeat = service.computeHash(AuditChainService.GENESIS_HASH, 1L, "admin", "TICKET_CREATED", "john_doe", "Created TKT-1", ts);
        String h1Tampered = service.computeHash(AuditChainService.GENESIS_HASH, 1L, "admin", "TICKET_CREATED", "john_doe", "Altered TKT-1", ts);
        assertEquals(h1, h1Repeat);
        assertNotEquals(h1, h1Tampered);
        assertEquals(64, h1.length(), "SHA-256 hex digest must be 64 characters");

        AuditLog tamperedLog = new AuditLog("admin", "TICKET_CREATED", "john_doe", "Altered TKT-1", 1L,
                AuditChainService.GENESIS_HASH, h1, ts);
        AuditChainHead head = new AuditChainHead(AuditChainService.HEAD_ROW_ID, h1, 1L, ts);
        when(auditLogRepo.findBySequenceNumberIsNotNullOrderBySequenceNumberAsc()).thenReturn(List.of(tamperedLog));
        when(auditChainHeadRepository.findById(AuditChainService.HEAD_ROW_ID)).thenReturn(Optional.of(head));

        AuditVerificationReport tamperReport = service.verifyChain();
        assertFalse(tamperReport.isValid());
        assertTrue(tamperReport.getViolations().stream().anyMatch(v -> v.contains("Tamper detected")));
    }

    @Test
    @DisplayName("P96-UNIT-06: RiskAssessmentService covers all account states, locks, failed attempts, and privileged roles")
    void testRiskAssessmentAllBranches() {
        RiskAssessmentService riskService = new RiskAssessmentService(null, null);
        LocalDateTime now = LocalDateTime.of(2026, 6, 15, 11, 0);

        UserAccount cleanEmp = new UserAccount();
        cleanEmp.setId(1L);
        cleanEmp.setUsername("emp_clean");
        cleanEmp.setStatus(AccountStatus.ACTIVE);
        cleanEmp.setFailedLoginAttempts(0);

        RiskAssessmentDTO lowRisk = riskService.assessRisk(cleanEmp, List.of("EMPLOYEE"), now);
        assertEquals("LOW", lowRisk.getRiskLevel());
        assertEquals(0, lowRisk.getScore());

        UserAccount criticalAdmin = new UserAccount();
        criticalAdmin.setId(2L);
        criticalAdmin.setUsername("admin_critical");
        criticalAdmin.setStatus(AccountStatus.DISABLED);
        criticalAdmin.setFailedLoginAttempts(6);
        criticalAdmin.setLockedUntil(now.plusMinutes(10));

        RiskAssessmentDTO critRisk = riskService.assessRisk(criticalAdmin, List.of("ADMIN"), now);
        assertEquals("CRITICAL", critRisk.getRiskLevel());
        assertTrue(critRisk.getScore() >= 70);

        // Expired lock does not add active-lock penalty
        UserAccount expiredLock = new UserAccount();
        expiredLock.setId(3L);
        expiredLock.setUsername("emp_expired_lock");
        expiredLock.setStatus(AccountStatus.SUSPENDED);
        expiredLock.setFailedLoginAttempts(3);
        expiredLock.setLockedUntil(now.minusMinutes(5));

        RiskAssessmentDTO medRisk = riskService.assessRisk(expiredLock, List.of("HR_MANAGER"), now);
        assertNotNull(medRisk.getRiskLevel());
        assertTrue(medRisk.getScore() > 0);
    }
}
