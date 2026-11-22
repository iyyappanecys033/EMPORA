package com.examly.springapp.controller;

import com.examly.springapp.dto.ApprovalDecisionRequestDTO;
import com.examly.springapp.dto.ApprovalTaskDTO;
import com.examly.springapp.service.ApprovalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * [A01: Broken Access Control & Anti-Self-Approval]
 * Method-level secured Approval REST Controller.
 */
@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    @Autowired
    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DEPT_HEAD', 'ADMIN') and !hasRole('AUDITOR')")
    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalTaskDTO>> getPendingApprovals(Principal principal) {
        String username = principal != null ? principal.getName() : "anonymousUser";
        List<ApprovalTaskDTO> pending = approvalService.getPendingApprovalsForUser(username);
        return ResponseEntity.ok(pending);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<List<ApprovalTaskDTO>> getApprovalHistory(
            @PathVariable("ticketId") Long ticketId,
            Principal principal) {
        String username = principal != null ? principal.getName() : "anonymousUser";
        List<ApprovalTaskDTO> history = approvalService.getApprovalHistoryForTicket(ticketId, username);
        return ResponseEntity.ok(history);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'DEPT_HEAD', 'ADMIN') and !hasRole('AUDITOR')")
    @PostMapping("/{taskId}/decide")
    public ResponseEntity<ApprovalTaskDTO> decideApproval(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody ApprovalDecisionRequestDTO request,
            Principal principal) {
        String username = principal != null ? principal.getName() : "anonymousUser";
        ApprovalTaskDTO decided = approvalService.decideApproval(taskId, request, username);
        return ResponseEntity.ok(decided);
    }
}
