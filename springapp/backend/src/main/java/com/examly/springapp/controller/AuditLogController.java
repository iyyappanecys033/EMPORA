package com.examly.springapp.controller;

import com.examly.springapp.model.AuditLog;
import com.examly.springapp.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/audit-logs", "/api/audit"})
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable Long id) {
        AuditLog auditLog = auditLogService.getAuditLogById(id);
        if (auditLog != null) {
            return ResponseEntity.ok(auditLog);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByAction(@PathVariable String action) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByAction(action));
    }

    @GetMapping("/actor/{actorUsername}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByActor(@PathVariable String actorUsername) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByActor(actorUsername));
    }

    @GetMapping("/target/{targetUsername}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByTarget(@PathVariable String targetUsername) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByTarget(targetUsername));
    }
}
