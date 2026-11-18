package com.examly.springapp.controller;

import com.examly.springapp.dto.AuditLogDTO;
import com.examly.springapp.dto.AuditVerificationReport;
import com.examly.springapp.model.AuditLog;
import com.examly.springapp.repository.AuditLogRepo;
import com.examly.springapp.service.AuditChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditApiController {

    private final AuditChainService auditChainService;
    private final AuditLogRepo auditLogRepo;

    @Autowired
    public AuditApiController(AuditChainService auditChainService, AuditLogRepo auditLogRepo) {
        this.auditChainService = auditChainService;
        this.auditLogRepo = auditLogRepo;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);
        Page<AuditLogDTO> logs = auditLogRepo.findAllByOrderByTimestampDesc(pageable)
                .map(this::toDto);
        return ResponseEntity.ok(logs);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @GetMapping("/verify")
    public ResponseEntity<AuditVerificationReport> verifyAuditChain() {
        AuditVerificationReport report = auditChainService.verifyChain();
        return ResponseEntity.ok(report);
    }

    private AuditLogDTO toDto(AuditLog log) {
        return new AuditLogDTO(
                log.getId(),
                log.getSequenceNumber(),
                log.getPreviousHash(),
                log.getCurrentHash(),
                log.getActorUsername(),
                log.getAction(),
                log.getTargetUsername(),
                log.getDetails(),
                log.getTimestamp()
        );
    }
}
