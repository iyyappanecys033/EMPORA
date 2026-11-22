package com.examly.springapp.service;

import com.examly.springapp.model.AuditLog;
import com.examly.springapp.repository.AuditLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepo auditLogRepo;

    @Override
    public void recordEvent(String action, String targetUsername, String details) {
        try {
            String actorUsername = "anonymous";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
                actorUsername = authentication.getName();
            }

            AuditLog auditLog = new AuditLog(actorUsername, action, targetUsername, details);
            auditLogRepo.save(auditLog);
        } catch (Exception e) {
            // Log the failure to standard error to prevent crashing the main transaction
            System.err.println("Failed to persist audit log: " + e.getMessage());
        }
    }

    @Override
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepo.findAllByOrderByTimestampDesc();
    }

    @Override
    public AuditLog getAuditLogById(Long id) {
        return auditLogRepo.findById(id).orElse(null);
    }

    @Override
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepo.findByActionOrderByTimestampDesc(action);
    }

    @Override
    public List<AuditLog> getAuditLogsByActor(String actorUsername) {
        return auditLogRepo.findByActorUsernameOrderByTimestampDesc(actorUsername);
    }

    @Override
    public List<AuditLog> getAuditLogsByTarget(String targetUsername) {
        return auditLogRepo.findByTargetUsernameOrderByTimestampDesc(targetUsername);
    }
}
