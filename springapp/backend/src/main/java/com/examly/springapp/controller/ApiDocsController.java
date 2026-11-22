package com.examly.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/docs")
public class ApiDocsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getApiDocumentation() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("title", "EMPORA - Employee Services & Workflow Platform API");
        doc.put("tagline", "One platform. Connected operations.");
        doc.put("version", "2.2.0");
        doc.put("description", "A platform for managing employee services, approvals, cross-department workflows and organisational operations.");

        List<Map<String, Object>> modules = new ArrayList<>();

        // Auth
        Map<String, Object> authModule = new LinkedHashMap<>();
        authModule.put("module", "Authentication");
        authModule.put("basePath", "/api/auth");
        authModule.put("endpoints", List.of(
                Map.of("method", "POST", "path", "/login", "auth", "Public", "description", "Authenticate and obtain JWT accessToken & refreshToken cookies"),
                Map.of("method", "POST", "path", "/register", "auth", "Public", "description", "Register a new user account"),
                Map.of("method", "POST", "path", "/refresh", "auth", "Public / Cookie", "description", "Silently refresh access token using HTTP-only refresh cookie"),
                Map.of("method", "POST", "path", "/logout", "auth", "Authenticated", "description", "Revoke refresh token and wipe auth cookies")
        ));
        modules.add(authModule);

        // Service Catalog
        Map<String, Object> catalogModule = new LinkedHashMap<>();
        catalogModule.put("module", "Service Catalog");
        catalogModule.put("basePath", "/api/v1/catalog");
        catalogModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "", "auth", "Authenticated", "description", "List active catalog items with pagination and department/category filters"),
                Map.of("method", "GET", "path", "/{id}", "auth", "Authenticated", "description", "Retrieve service catalog item details by ID"),
                Map.of("method", "GET", "path", "/code/{itemCode}", "auth", "Authenticated", "description", "Retrieve service catalog item details by unique item code")
        ));
        modules.add(catalogModule);

        // Ticket Management
        Map<String, Object> ticketModule = new LinkedHashMap<>();
        ticketModule.put("module", "Service Tickets & Workflow");
        ticketModule.put("basePath", "/api/v1/tickets");
        ticketModule.put("endpoints", List.of(
                Map.of("method", "POST", "path", "", "auth", "Authenticated", "description", "Create and submit a new service ticket"),
                Map.of("method", "GET", "path", "/{id}", "auth", "Requester / Resolver / Admin", "description", "Retrieve ticket details and SLA target status"),
                Map.of("method", "GET", "path", "/my", "auth", "Authenticated", "description", "List tickets created by authenticated requester"),
                Map.of("method", "POST", "path", "/{id}/assign", "auth", "Department Resolver / Admin", "description", "Assign ticket to specialist and transition to IN_PROGRESS"),
                Map.of("method", "POST", "path", "/{id}/status", "auth", "Assignee / Department Resolver / Admin", "description", "Transition ticket status according to 11-State Machine"),
                Map.of("method", "POST", "path", "/{id}/cancel", "auth", "Requester (Pre-pickup) / Admin", "description", "Cancel ticket with mandatory reason (>= 15 chars)"),
                Map.of("method", "POST", "path", "/{id}/reopen", "auth", "Requester", "description", "Reopen resolved ticket within 48-business-hour dispute window"),
                Map.of("method", "GET", "path", "/{id}/comments", "auth", "Requester / Resolver / Admin", "description", "Get discussion comments (internal comments filtered for requesters)"),
                Map.of("method", "POST", "path", "/{id}/comments", "auth", "Requester / Resolver / Admin", "description", "Post discussion comment on ticket")
        ));
        modules.add(ticketModule);

        // Department Queue
        Map<String, Object> queueModule = new LinkedHashMap<>();
        queueModule.put("module", "Department Resolving Queues");
        queueModule.put("basePath", "/api/v1/departments");
        queueModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "/{department}/queue", "auth", "Department Resolver / Admin", "description", "Paginated queue of departmental service tickets filtered by status")
        ));
        modules.add(queueModule);

        // Approvals
        Map<String, Object> approvalModule = new LinkedHashMap<>();
        approvalModule.put("module", "Multi-Stage Approvals");
        approvalModule.put("basePath", "/api/v1/approvals");
        approvalModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "/pending", "auth", "Designated Approver", "description", "List pending approval tasks awaiting decision"),
                Map.of("method", "GET", "path", "/tickets/{ticketId}", "auth", "Requester / Approver / Admin", "description", "Get full multi-stage approval audit history for ticket"),
                Map.of("method", "POST", "path", "/{taskId}/decide", "auth", "Designated Approver", "description", "Approve or reject task with anti-self-approval enforcement")
        ));
        modules.add(approvalModule);

        // Notifications (P8)
        Map<String, Object> notificationModule = new LinkedHashMap<>();
        notificationModule.put("module", "In-App Notifications");
        notificationModule.put("basePath", "/api/v1/notifications");
        notificationModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "/my", "auth", "Authenticated Owner", "description", "Paginated user notifications with unreadOnly and eventType filters"),
                Map.of("method", "GET", "path", "/my/unread-count", "auth", "Authenticated Owner", "description", "Authoritative unread notification count"),
                Map.of("method", "PUT", "path", "/{id}/read", "auth", "Authenticated Owner", "description", "Idempotently mark an owned notification as read"),
                Map.of("method", "PUT", "path", "/my/read-all", "auth", "Authenticated Owner", "description", "Mark all unread notifications for authenticated user as read")
        ));
        modules.add(notificationModule);

        // Operational Analytics (P8)
        Map<String, Object> analyticsModule = new LinkedHashMap<>();
        analyticsModule.put("module", "Business & Operational Analytics");
        analyticsModule.put("basePath", "/api/v1/analytics");
        analyticsModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "/dashboard", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "Single-roundtrip aggregated dashboard bundle"),
                Map.of("method", "GET", "path", "/overview", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "KPI summary snapshot and period totals"),
                Map.of("method", "GET", "path", "/tickets/status-distribution", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "Ticket volume distribution by lifecycle status"),
                Map.of("method", "GET", "path", "/tickets/priority-distribution", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "Ticket distribution across all 5 priorities"),
                Map.of("method", "GET", "path", "/tickets/trends", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "Daily ticket submission vs resolution trend series"),
                Map.of("method", "GET", "path", "/departments/performance", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "Department backlog, volume, and SLA compliance metrics"),
                Map.of("method", "GET", "path", "/services/popularity", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "Most frequently requested service catalog offerings"),
                Map.of("method", "GET", "path", "/sla/compliance", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "Authoritative SLA compliance rate and average resolution time"),
                Map.of("method", "GET", "path", "/approvals/overview", "auth", "ADMIN, AUDITOR, MANAGER, SPECIALIST (dept-scoped)", "description", "Pending, overdue, approved, and rejected approval pipeline metrics")
        ));
        modules.add(analyticsModule);

        // Operational Health (P8)
        Map<String, Object> healthModule = new LinkedHashMap<>();
        healthModule.put("module", "Operational Health");
        healthModule.put("basePath", "/api/v1/health");
        healthModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "", "auth", "Public", "description", "Lightweight application and database connectivity probe")
        ));
        modules.add(healthModule);

        // Cryptographic Audit
        Map<String, Object> auditApiModule = new LinkedHashMap<>();
        auditApiModule.put("module", "Cryptographic Audit Chain");
        auditApiModule.put("basePath", "/api/v1/audit");
        auditApiModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "/logs", "auth", "ADMIN, AUDITOR", "description", "Paginated chronological cryptographic audit trail"),
                Map.of("method", "GET", "path", "/verify", "auth", "ADMIN, AUDITOR", "description", "Cryptographic verification of complete SHA-256 hash chain from genesis to anchor head")
        ));
        modules.add(auditApiModule);

        // Employee Management
        Map<String, Object> employeeModule = new LinkedHashMap<>();
        employeeModule.put("module", "Employee Directory & Profiles");
        employeeModule.put("basePath", "/api/profiles");
        employeeModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "/directory", "auth", "ADMIN, HR_MANAGER, EMPLOYEE", "description", "Search and paginate employee directory"),
                Map.of("method", "GET", "path", "/me", "auth", "Authenticated", "description", "Retrieve current logged-in employee profile")
        ));
        modules.add(employeeModule);

        // User Account Admin
        Map<String, Object> userModule = new LinkedHashMap<>();
        userModule.put("module", "User Account & Security Administration");
        userModule.put("basePath", "/api/users");
        userModule.put("endpoints", List.of(
                Map.of("method", "GET", "path", "/me", "auth", "Authenticated", "description", "Get current user session, roles, and profile summary"),
                Map.of("method", "GET", "path", "", "auth", "ADMIN", "description", "List all user accounts"),
                Map.of("method", "PUT", "path", "/{id}/suspend", "auth", "ADMIN", "description", "Suspend employee account access"),
                Map.of("method", "PUT", "path", "/{id}/disable", "auth", "ADMIN", "description", "Disable employee account access"),
                Map.of("method", "PUT", "path", "/{id}/reactivate", "auth", "ADMIN", "description", "Reactivate disabled/suspended account"),
                Map.of("method", "PUT", "path", "/{id}/unlock", "auth", "ADMIN", "description", "Manually unlock account locked by failed login attempts")
        ));
        modules.add(userModule);

        doc.put("modules", modules);
        return ResponseEntity.ok(doc);
    }
}
