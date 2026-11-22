package com.examly.springapp.controller;

import com.examly.springapp.dto.*;
import com.examly.springapp.service.TicketCommandService;
import com.examly.springapp.service.TicketQueryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketCommandService commandService;
    private final TicketQueryService queryService;

    @Autowired
    public TicketController(TicketCommandService commandService, TicketQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<ServiceTicketDTO> submitTicket(@Valid @RequestBody CreateTicketRequestDTO request, 
                                                         Principal principal) {
        String username = getUsername(principal);
        ServiceTicketDTO created = commandService.createTicket(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceTicketDTO> getTicketById(@PathVariable("id") Long id, 
                                                          Principal principal) {
        String username = getUsername(principal);
        ServiceTicketDTO ticket = queryService.getTicketById(id, username);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ServiceTicketDTO>> getMyTickets(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Principal principal) {
        String username = getUsername(principal);
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);
        Page<ServiceTicketDTO> tickets = queryService.getMyTickets(username, pageable);
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ServiceTicketDTO> assignTicket(@PathVariable("id") Long id,
                                                         @Valid @RequestBody(required = false) TicketAssignRequestDTO request,
                                                         Principal principal) {
        String username = getUsername(principal);
        Long assigneeId = request != null ? request.getAssigneeId() : null;
        ServiceTicketDTO updated = commandService.assignTicket(id, assigneeId, username);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<ServiceTicketDTO> updateStatus(@PathVariable("id") Long id,
                                                         @Valid @RequestBody TicketStatusUpdateRequestDTO request,
                                                         Principal principal) {
        String username = getUsername(principal);
        ServiceTicketDTO updated = commandService.updateStatus(id, request, username);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ServiceTicketDTO> cancelTicket(@PathVariable("id") Long id,
                                                         @Valid @RequestBody(required = false) TicketActionRequestDTO request,
                                                         Principal principal) {
        String username = getUsername(principal);
        String reason = request != null ? request.getReason() : null;
        Long version = request != null ? request.getVersion() : null;
        ServiceTicketDTO updated = commandService.cancelTicket(id, reason, version, username);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<ServiceTicketDTO> reopenTicket(@PathVariable("id") Long id,
                                                         @Valid @RequestBody(required = false) TicketActionRequestDTO request,
                                                         Principal principal) {
        String username = getUsername(principal);
        String reason = request != null ? request.getReason() : null;
        Long version = request != null ? request.getVersion() : null;
        ServiceTicketDTO updated = commandService.reopenTicket(id, reason, version, username);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<TicketCommentDTO>> getComments(@PathVariable("id") Long id,
                                                              Principal principal) {
        String username = getUsername(principal);
        List<TicketCommentDTO> comments = queryService.getCommentsForTicket(id, username);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketCommentDTO> addComment(@PathVariable("id") Long id,
                                                       @Valid @RequestBody TicketCommentRequestDTO request,
                                                       Principal principal) {
        String username = getUsername(principal);
        TicketCommentDTO created = commandService.addComment(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private String getUsername(Principal principal) {
        if (principal == null) {
            return "anonymousUser";
        }
        return principal.getName();
    }
}
