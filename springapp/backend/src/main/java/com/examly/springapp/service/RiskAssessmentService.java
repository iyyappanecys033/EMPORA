package com.examly.springapp.service;

import com.examly.springapp.dto.RiskAssessmentDTO;
import com.examly.springapp.model.AccountStatus;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.repository.UserRoleMappingRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class RiskAssessmentService {

    private final UserAccountRepo userAccountRepo;
    private final UserRoleMappingRepo userRoleMappingRepo;

    public RiskAssessmentService(UserAccountRepo userAccountRepo, UserRoleMappingRepo userRoleMappingRepo) {
        this.userAccountRepo = userAccountRepo;
        this.userRoleMappingRepo = userRoleMappingRepo;
    }

    public RiskAssessmentDTO assessRisk(Long userId) {
        UserAccount user = userAccountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<String> roleNames = userRoleMappingRepo.findByUserId(userId).stream()
                .map(UserRoleMapping::getRole)
                .filter(Objects::nonNull)
                .map(Role::getRoleName)
                .filter(Objects::nonNull)
                .toList();

        return assessRisk(user, roleNames, LocalDateTime.now());
    }

    /**
     * Pure in-memory risk scoring reusable by bulk admin dashboard aggregation (eliminates N+1 queries).
     */
    public RiskAssessmentDTO assessRisk(UserAccount user, Collection<String> roleNames, LocalDateTime evaluationTime) {
        Collection<String> effectiveRoles = roleNames != null ? roleNames : Collections.emptyList();
        LocalDateTime now = evaluationTime != null ? evaluationTime : LocalDateTime.now();

        int score = 0;
        List<String> explanations = new ArrayList<>();

        // Rule 1: ADMIN role & Rule 2: Multiple privileged roles
        boolean hasAdmin = false;
        int privilegedRoleCount = 0;

        for (String rawRoleName : effectiveRoles) {
            if (rawRoleName != null) {
                String roleName = rawRoleName.toUpperCase();
                // Consider any role other than USER as privileged
                if (!roleName.equals("USER")) {
                    privilegedRoleCount++;
                    if (roleName.equals("ADMIN")) {
                        hasAdmin = true;
                    }
                }
            }
        }

        if (hasAdmin) {
            score += 30;
            explanations.add("Privileged ADMIN role");
        }

        if (privilegedRoleCount > 1) {
            score += 15;
            explanations.add("Multiple privileged roles");
        }

        // Rule 3: Failed login attempts
        int failedAttempts = user.getFailedLoginAttempts();
        if (failedAttempts >= 5) {
            score += 20;
            explanations.add("Repeated failed login attempts");
        } else if (failedAttempts >= 3) {
            score += 10;
            explanations.add("Multiple failed login attempts");
        }

        // Rule 4: Active security lock
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            score += 20;
            explanations.add("Active security lock");
        }

        // Rule 5 & 6: Account Status
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            score += 20;
            explanations.add("Account is administratively suspended");
        } else if (user.getStatus() == AccountStatus.DISABLED) {
            score += 30;
            explanations.add("Account is administratively disabled");
        }

        // Score Boundaries
        score = Math.min(score, 100);

        // Map to Risk Levels
        String riskLevel = determineRiskLevel(score);

        return new RiskAssessmentDTO(score, riskLevel, explanations);
    }

    private String determineRiskLevel(int score) {
        if (score <= 24) return "LOW";
        if (score <= 49) return "MEDIUM";
        if (score <= 74) return "HIGH";
        return "CRITICAL";
    }
}
