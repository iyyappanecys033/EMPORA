package com.examly.springapp.controller;

import com.examly.springapp.dto.analytics.*;
import com.examly.springapp.model.TicketPriority;
import com.examly.springapp.model.TicketStatus;
import com.examly.springapp.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardBundleDTO> getDashboardBundle(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "priority", required = false) TicketPriority priority,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getDashboardBundle(startDate, endDate, department, priority, getUsername(principal)));
    }

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewDTO> getOverview(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "priority", required = false) TicketPriority priority,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getOverview(startDate, endDate, department, priority, getUsername(principal)));
    }

    @GetMapping("/tickets/status-distribution")
    public ResponseEntity<List<StatusDistributionDTO>> getStatusDistribution(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "priority", required = false) TicketPriority priority,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getStatusDistribution(startDate, endDate, department, priority, getUsername(principal)));
    }

    @GetMapping("/tickets/priority-distribution")
    public ResponseEntity<List<PriorityDistributionDTO>> getPriorityDistribution(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "status", required = false) TicketStatus status,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getPriorityDistribution(startDate, endDate, department, status, getUsername(principal)));
    }

    @GetMapping("/tickets/trends")
    public ResponseEntity<List<TicketTrendPointDTO>> getTicketTrends(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "priority", required = false) TicketPriority priority,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getTicketTrends(startDate, endDate, department, priority, getUsername(principal)));
    }

    @GetMapping("/departments/performance")
    public ResponseEntity<List<DepartmentPerformanceDTO>> getDepartmentPerformance(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getDepartmentPerformance(startDate, endDate, department, getUsername(principal)));
    }

    @GetMapping("/services/popularity")
    public ResponseEntity<List<ServicePopularityDTO>> getServicePopularity(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getServicePopularity(startDate, endDate, department, category, limit, getUsername(principal)));
    }

    @GetMapping("/sla/compliance")
    public ResponseEntity<SlaComplianceDTO> getSlaCompliance(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "priority", required = false) TicketPriority priority,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getSlaCompliance(startDate, endDate, department, priority, getUsername(principal)));
    }

    @GetMapping("/approvals/overview")
    public ResponseEntity<ApprovalOverviewDTO> getApprovalsOverview(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "department", required = false) String department,
            Principal principal) {
        return ResponseEntity.ok(analyticsService.getApprovalsOverview(startDate, endDate, department, getUsername(principal)));
    }

    private String getUsername(Principal principal) {
        return principal != null ? principal.getName() : null;
    }
}
