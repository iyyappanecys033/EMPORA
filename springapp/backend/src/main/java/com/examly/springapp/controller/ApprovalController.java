package com.examly.springapp.controller;

import com.examly.springapp.dto.ApprovalDecisionRequestDTO;
import com.examly.springapp.dto.ApprovalTaskDTO;
import com.examly.springapp.service.ApprovalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    @Autowired
    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalTaskDTO>> getPendingApprovals(Principal principal) {
        String username = principal != null ? principal.getName() : "anonymousUser";
        List<ApprovalTaskDTO> pending = approvalService.getPendingApprovalsForUser(username);
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<List<ApprovalTaskDTO>> getApprovalHistory(
            @PathVariable("ticketId") Long ticketId,
            Principal principal) {
        String username = principal != null ? principal.getName() : "anonymousUser";
        List<ApprovalTaskDTO> history = approvalService.getApprovalHistoryForTicket(ticketId, username);
        return ResponseEntity.ok(history);
    }

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
