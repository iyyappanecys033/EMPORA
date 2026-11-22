package com.examly.springapp;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TicketPersistenceTests {

    @Autowired
    private ServiceCatalogRepository serviceCatalogRepository;

    @Autowired
    private ServiceTicketRepository serviceTicketRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private UserAccount testRequester;
    private UserAccount testAssignee;
    private ServiceCatalogItem testCatalogItem;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        // Clean up tickets and comments from prior test runs in isolation
        transactionTemplate.execute(status -> {
            ticketCommentRepository.deleteAll();
            serviceTicketRepository.deleteAll();
            return null;
        });

        // Initialize reusable test identities
        testRequester = userAccountRepo.findByUsername("ticket_requester_user")
                .orElseGet(() -> {
                    UserAccount user = new UserAccount();
                    user.setUsername("ticket_requester_user");
                    user.setPasswordHash("$2a$10$wO7vI13Lg1J13O0t7Vz7euZ8yG3bU0wUvO7vI13Lg1J13O0t7Vz7e");
                    user.setEmail("requester@epams.com");
                    user.setStatus(AccountStatus.ACTIVE);
                    return userAccountRepo.save(user);
                });

        testAssignee = userAccountRepo.findByUsername("ticket_assignee_user")
                .orElseGet(() -> {
                    UserAccount user = new UserAccount();
                    user.setUsername("ticket_assignee_user");
                    user.setPasswordHash("$2a$10$wO7vI13Lg1J13O0t7Vz7euZ8yG3bU0wUvO7vI13Lg1J13O0t7Vz7e");
                    user.setEmail("assignee@epams.com");
                    user.setStatus(AccountStatus.ACTIVE);
                    return userAccountRepo.save(user);
                });

        testCatalogItem = serviceCatalogRepository.findByItemCode("IT-DEV-LAPTOP")
                .orElseGet(() -> {
                    ServiceCatalogItem item = new ServiceCatalogItem(
                            "IT-DEV-LAPTOP",
                            "Standard Developer Laptop",
                            "Hardware",
                            "IT",
                            "High performance 32GB RAM workstation laptop",
                            new BigDecimal("1850.00"),
                            48,
                            true,
                            false,
                            true
                    );
                    return serviceCatalogRepository.save(item);
                });
    }

    @Test
    @Order(1)
    @DisplayName("Stage B: ServiceCatalogItem persists, retrieves, and active filters operate correctly")
    void test1_ServiceCatalogItem_PersistAndRetrieve() {
        ServiceCatalogItem item = new ServiceCatalogItem(
                "HR-ACCESS-CARD",
                "Building Access Card Replacement",
                "Facilities",
                "HR",
                "Physical NFC RFID building smart card",
                new BigDecimal("25.00"),
                24,
                false,
                false,
                true
        );

        ServiceCatalogItem saved = serviceCatalogRepository.save(item);
        assertNotNull(saved.getId(), "Catalog item ID must be populated by database");

        Optional<ServiceCatalogItem> fetched = serviceCatalogRepository.findByItemCode("HR-ACCESS-CARD");
        assertTrue(fetched.isPresent(), "Saved catalog item must be queryable by item code");
        assertEquals("Building Access Card Replacement", fetched.get().getTitle());
        assertEquals("Facilities", fetched.get().getCategory());
        assertEquals("HR", fetched.get().getDepartment());
        assertEquals(new BigDecimal("25.00"), fetched.get().getEstimatedCost());
        assertEquals(24, fetched.get().getSlaTargetHours());
        assertFalse(fetched.get().getRequiresManagerApproval());
        assertTrue(fetched.get().isActive());

        List<ServiceCatalogItem> activeItems = serviceCatalogRepository.findByIsActiveTrue();
        assertFalse(activeItems.isEmpty(), "Active catalog list must contain items");

        List<ServiceCatalogItem> hrItems = serviceCatalogRepository.findByDepartmentAndIsActiveTrue("HR");
        assertTrue(hrItems.stream().anyMatch(i -> i.getItemCode().equals("HR-ACCESS-CARD")));
    }

    @Test
    @Order(2)
    @DisplayName("Stage B: ServiceCatalogItem enforces unique constraint on item_code")
    void test2_ServiceCatalogItem_UniqueConstraintViolation() {
        ServiceCatalogItem duplicate = new ServiceCatalogItem(
                "IT-DEV-LAPTOP", // Duplicate of existing item code
                "Duplicate Item Title",
                "Hardware",
                "IT",
                "Duplicate description",
                BigDecimal.ZERO,
                24,
                true,
                false,
                true
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                serviceCatalogRepository.save(duplicate);
                entityManager.flush();
                return null;
            });
        }, "Database must reject duplicate item_code via uk_service_catalog_item_code");
    }

    @Test
    @Order(3)
    @DisplayName("Stage C: ServiceTicket persists with valid foreign keys and default values")
    void test3_ServiceTicket_PersistAndRetrieveWithRelationships() {
        ServiceTicket ticket = new ServiceTicket(
                "TKT-2026-000101",
                testCatalogItem,
                testRequester,
                "IT",
                TicketPriority.HIGH,
                "Need replacement developer laptop due to battery failure",
                LocalDateTime.now().plusHours(48)
        );
        ticket.setAssignee(testAssignee);

        ServiceTicket saved = serviceTicketRepository.save(ticket);
        assertNotNull(saved.getId(), "Service ticket ID must be generated");
        assertEquals(0L, saved.getVersion(), "Initial version must be 0");
        assertEquals(TicketStatus.SUBMITTED, saved.getStatus(), "Default status must be SUBMITTED");
        assertEquals(0, saved.getTotalPausedMinutes(), "Initial paused minutes must be 0");

        Optional<ServiceTicket> retrieved = serviceTicketRepository.findByTicketNumber("TKT-2026-000101");
        assertTrue(retrieved.isPresent());
        assertEquals("TKT-2026-000101", retrieved.get().getTicketNumber());
        assertEquals(TicketPriority.HIGH, retrieved.get().getPriority());
        assertEquals(testRequester.getId(), retrieved.get().getRequester().getId());
        assertEquals(testAssignee.getId(), retrieved.get().getAssignee().getId());
        assertEquals(testCatalogItem.getId(), retrieved.get().getCatalogItem().getId());
        assertNotNull(retrieved.get().getCreatedAt());
    }

    @Test
    @Order(4)
    @DisplayName("Stage C: ServiceTicket optimistic locking increments version token on update")
    void test4_ServiceTicket_OptimisticLockingIncrementsVersion() {
        ServiceTicket ticket = new ServiceTicket(
                "TKT-2026-000102",
                testCatalogItem,
                testRequester,
                "IT",
                TicketPriority.MEDIUM,
                "Initial ticket description for concurrency verification",
                LocalDateTime.now().plusHours(24)
        );

        ServiceTicket saved = transactionTemplate.execute(status -> {
            ServiceTicket t = serviceTicketRepository.save(ticket);
            entityManager.flush();
            return t;
        });
        assertNotNull(saved);
        Long initialVersion = saved.getVersion();
        assertEquals(0L, initialVersion);

        // Perform an update in a separate transaction to verify JPA @Version increment
        ServiceTicket updated = transactionTemplate.execute(status -> {
            ServiceTicket t = serviceTicketRepository.findById(saved.getId()).orElseThrow();
            t.setDescription("Updated ticket description verifying optimistic version increment");
            ServiceTicket refreshed = serviceTicketRepository.save(t);
            entityManager.flush();
            return refreshed;
        });

        assertNotNull(updated);
        assertEquals(initialVersion + 1, updated.getVersion(), "JPA @Version must increment after atomic update");
    }

    @Test
    @Order(5)
    @DisplayName("Stage C: ServiceTicket rejects unpersisted foreign keys")
    void test5_ServiceTicket_ForeignKeyConstraintViolation() {
        UserAccount phantomUser = new UserAccount();
        phantomUser.setId(999999L); // Non-existent user

        ServiceTicket invalidTicket = new ServiceTicket(
                "TKT-2026-000999",
                testCatalogItem,
                phantomUser,
                "IT",
                TicketPriority.LOW,
                "Invalid foreign key ticket",
                LocalDateTime.now().plusHours(24)
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                serviceTicketRepository.save(invalidTicket);
                entityManager.flush();
                return null;
            });
        }, "Database must enforce referential integrity on requester_id");
    }

    @Test
    @Order(6)
    @DisplayName("Stage D: TicketComment persists, retrieves by ticket, and filters internal visibility")
    void test6_TicketComment_PersistAndRetrieveByTicket() {
        ServiceTicket ticket = new ServiceTicket(
                "TKT-2026-000103",
                testCatalogItem,
                testRequester,
                "IT",
                TicketPriority.MEDIUM,
                "Ticket for comment verification",
                LocalDateTime.now().plusHours(24)
        );
        ServiceTicket savedTicket = serviceTicketRepository.save(ticket);

        TicketComment publicComment = new TicketComment(
                savedTicket,
                testRequester,
                "Hello IT Team, here are the battery diagnostic logs.",
                false
        );

        TicketComment internalComment = new TicketComment(
                savedTicket,
                testAssignee,
                "Internal specialist note: Hardware stock verified in warehouse rack 4B.",
                true
        );

        ticketCommentRepository.save(publicComment);
        ticketCommentRepository.save(internalComment);

        List<TicketComment> allComments = ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(savedTicket.getId());
        assertEquals(2, allComments.size(), "Total comments count must be 2");

        List<TicketComment> externalComments = ticketCommentRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(savedTicket.getId());
        assertEquals(1, externalComments.size(), "Only 1 public comment should be returned for non-internal view");
        assertEquals("Hello IT Team, here are the battery diagnostic logs.", externalComments.get(0).getCommentText());
        assertFalse(externalComments.get(0).isInternal());

        assertEquals(2, ticketCommentRepository.countByTicketId(savedTicket.getId()));
    }

    @Test
    @Order(7)
    @DisplayName("Stage E: Deleting ServiceTicket cascades to comments but preserves IAM User and Catalog Item")
    void test7_DeleteTicket_CascadesComments_PreservesUserAndCatalog() {
        ServiceTicket ticket = new ServiceTicket(
                "TKT-2026-000104",
                testCatalogItem,
                testRequester,
                "IT",
                TicketPriority.LOW,
                "Ticket for cascade safety verification",
                LocalDateTime.now().plusHours(24)
        );
        ServiceTicket savedTicket = serviceTicketRepository.save(ticket);

        TicketComment comment = new TicketComment(
                savedTicket,
                testRequester,
                "Comment to be cascaded when ticket is deleted",
                false
        );
        TicketComment savedComment = ticketCommentRepository.save(comment);

        Long ticketId = savedTicket.getId();
        Long commentId = savedComment.getId();
        Long requesterId = testRequester.getId();
        Long catalogId = testCatalogItem.getId();

        // Delete ticket
        transactionTemplate.execute(status -> {
            serviceTicketRepository.deleteById(ticketId);
            entityManager.flush();
            return null;
        });

        // 1. Ticket must be deleted
        assertFalse(serviceTicketRepository.findById(ticketId).isPresent(), "Ticket must be deleted");

        // 2. Associated comment must be deleted via DB cascade (fk_comment_ticket ON DELETE CASCADE)
        assertFalse(ticketCommentRepository.findById(commentId).isPresent(), "Associated comment must be cascaded");

        // 3. UserAccount must remain completely intact (NO unintended cascade delete of IAM identities)
        assertTrue(userAccountRepo.findById(requesterId).isPresent(), "Requester UserAccount must NOT be deleted");

        // 4. ServiceCatalogItem must remain completely intact (NO unintended cascade delete of catalog)
        assertTrue(serviceCatalogRepository.findById(catalogId).isPresent(), "ServiceCatalogItem must NOT be deleted");
    }

    @Test
    @Order(8)
    @DisplayName("Stage C: ServiceTicket domain methods pause and resume SLA clock accurately")
    void test8_ServiceTicket_SlaPauseResumeBusinessMethods() {
        ServiceTicket ticket = new ServiceTicket(
                "TKT-2026-000105",
                testCatalogItem,
                testRequester,
                "IT",
                TicketPriority.URGENT,
                "SLA pause and resume test",
                LocalDateTime.now().plusHours(12)
        );

        assertTrue(ticket.canBeCancelledByRequester());

        // Pause SLA
        LocalDateTime pauseTime = LocalDateTime.now().minusMinutes(45);
        ticket.pauseSlaClock(pauseTime);
        assertEquals(TicketStatus.ON_HOLD, ticket.getStatus());
        assertNotNull(ticket.getLastPausedAt());

        // Resume SLA after 45 minutes
        LocalDateTime resumeTime = LocalDateTime.now();
        ticket.resumeSlaClock(resumeTime);
        assertNull(ticket.getLastPausedAt());
        assertTrue(ticket.getTotalPausedMinutes() >= 44 && ticket.getTotalPausedMinutes() <= 46);

        // When resolved, cannot be cancelled by requester
        ticket.setStatus(TicketStatus.RESOLVED);
        assertFalse(ticket.canBeCancelledByRequester());
    }

    @Test
    @Order(9)
    @DisplayName("Stage F: ServiceTicketRepository pagination and custom status/department queries")
    void test9_PaginationAndCustomQueries() {
        for (int i = 1; i <= 6; i++) {
            ServiceTicket t = new ServiceTicket(
                    "TKT-2026-BATCH-" + i,
                    testCatalogItem,
                    testRequester,
                    "IT",
                    TicketPriority.MEDIUM,
                    "Batch ticket " + i,
                    LocalDateTime.now().plusHours(24)
            );
            if (i > 4) {
                t.setStatus(TicketStatus.IN_PROGRESS);
            }
            serviceTicketRepository.save(t);
        }

        Page<ServiceTicket> page1 = serviceTicketRepository.findByRequesterIdOrderByCreatedAtDesc(
                testRequester.getId(),
                PageRequest.of(0, 4)
        );
        assertEquals(4, page1.getContent().size());
        assertEquals(6, page1.getTotalElements());
        assertEquals(2, page1.getTotalPages());

        Page<ServiceTicket> inProgressTickets = serviceTicketRepository.findByDepartmentAndStatusOrderByCreatedAtDesc(
                "IT",
                TicketStatus.IN_PROGRESS,
                PageRequest.of(0, 10)
        );
        assertEquals(2, inProgressTickets.getContent().size());

        assertEquals(4, serviceTicketRepository.countByDepartmentAndStatus("IT", TicketStatus.SUBMITTED));
        assertEquals(2, serviceTicketRepository.countByDepartmentAndStatus("IT", TicketStatus.IN_PROGRESS));
    }

    @Test
    @Order(10)
    @DisplayName("Stage E & Schema: Verify explicit @Table mappings and Hibernate ddl-auto=validate integrity")
    void test10_SchemaValidation_ExplicitTableMappingProof() {
        Table catalogTable = ServiceCatalogItem.class.getAnnotation(Table.class);
        assertNotNull(catalogTable, "ServiceCatalogItem must have @Table annotation");
        assertEquals("service_catalog", catalogTable.name(), "Table name must strictly be 'service_catalog'");

        Table ticketTable = ServiceTicket.class.getAnnotation(Table.class);
        assertNotNull(ticketTable, "ServiceTicket must have @Table annotation");
        assertEquals("service_ticket", ticketTable.name(), "Table name must strictly be 'service_ticket'");

        Table commentTable = TicketComment.class.getAnnotation(Table.class);
        assertNotNull(commentTable, "TicketComment must have @Table annotation");
        assertEquals("ticket_comment", commentTable.name(), "Table name must strictly be 'ticket_comment'");

        // Verifies that Hibernate context successfully started under spring.jpa.hibernate.ddl-auto=validate
        assertNotNull(entityManager, "EntityManager must be active and valid under ddl-auto=validate");
    }

    @AfterAll
    static void tearDownAll(@Autowired ServiceTicketRepository serviceTicketRepository,
                            @Autowired TicketCommentRepository ticketCommentRepository) {
        ticketCommentRepository.deleteAll();
        serviceTicketRepository.deleteAll();
    }
}

