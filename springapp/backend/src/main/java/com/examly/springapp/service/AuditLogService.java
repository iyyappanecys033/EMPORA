package com.examly.springapp.service;

import com.examly.springapp.model.AuditLog;
import java.util.List;

public interface AuditLogService {
    void recordEvent(String action, String targetUsername, String details);
    List<AuditLog> getAllAuditLogs();
    AuditLog getAuditLogById(Long id);
    List<AuditLog> getAuditLogsByAction(String action);
    List<AuditLog> getAuditLogsByActor(String actorUsername);
    List<AuditLog> getAuditLogsByTarget(String targetUsername);
}
