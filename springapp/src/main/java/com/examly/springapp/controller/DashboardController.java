package com.examly.springapp.controller;

import com.examly.springapp.dto.DashboardMetricsDTO;
import com.examly.springapp.dto.RiskAssessmentDTO;
import com.examly.springapp.model.AccountStatus;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.AuditLogRepo;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.repository.UserRoleMappingRepo;
import com.examly.springapp.service.RiskAssessmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserAccountRepo userAccountRepo;
    private final UserRoleMappingRepo userRoleMappingRepo;
    private final AuditLogRepo auditLogRepo;
    private final RiskAssessmentService riskAssessmentService;

    public DashboardController(UserAccountRepo userAccountRepo,
                               UserRoleMappingRepo userRoleMappingRepo,
                               AuditLogRepo auditLogRepo,
                               RiskAssessmentService riskAssessmentService) {
        this.userAccountRepo = userAccountRepo;
        this.userRoleMappingRepo = userRoleMappingRepo;
        this.auditLogRepo = auditLogRepo;
        this.riskAssessmentService = riskAssessmentService;
    }

    /**
     * Optimized O(1)-query IAM administrative risk dashboard (TECH-P8-01 fix):
     * Executes exactly 3 constant queries (users + bulk role scalar projection + audit count)
     * instead of 3N+2 N+1 per-user queries while preserving identical DTO semantics.
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetrics() {
        DashboardMetricsDTO metrics = new DashboardMetricsDTO();

        List<UserAccount> users = userAccountRepo.findAll();
        Map<Long, List<String>> rolesByUserId = new HashMap<>();
        for (Object[] row : userRoleMappingRepo.findAllUserIdAndRoleNames()) {
            Long userId = (Long) row[0];
            String roleName = (String) row[1];
            if (userId != null && roleName != null) {
                rolesByUserId.computeIfAbsent(userId, k -> new ArrayList<>()).add(roleName);
            }
        }

        long totalUsers = users.size();
        long activeUsers = 0;
        long suspendedUsers = 0;
        long disabledUsers = 0;
        long lockedUsers = 0;
        long highRiskUsers = 0;
        long criticalRiskUsers = 0;

        LocalDateTime now = LocalDateTime.now();

        for (UserAccount user : users) {
            if (user.getStatus() == AccountStatus.ACTIVE) activeUsers++;
            else if (user.getStatus() == AccountStatus.SUSPENDED) suspendedUsers++;
            else if (user.getStatus() == AccountStatus.DISABLED) disabledUsers++;

            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                lockedUsers++;
            }

            List<String> userRoles = rolesByUserId.getOrDefault(user.getId(), Collections.emptyList());
            RiskAssessmentDTO risk = riskAssessmentService.assessRisk(user, userRoles, now);
            if ("CRITICAL".equals(risk.getRiskLevel())) criticalRiskUsers++;
            else if ("HIGH".equals(risk.getRiskLevel())) highRiskUsers++;
        }

        metrics.setTotalUsers(totalUsers);
        metrics.setActiveUsers(activeUsers);
        metrics.setSuspendedUsers(suspendedUsers);
        metrics.setDisabledUsers(disabledUsers);
        metrics.setLockedUsers(lockedUsers);
        metrics.setHighRiskUsers(highRiskUsers);
        metrics.setCriticalRiskUsers(criticalRiskUsers);
        metrics.setTotalAuditEvents(auditLogRepo.count());

        return ResponseEntity.ok(metrics);
    }
}
