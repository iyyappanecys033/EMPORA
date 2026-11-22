package com.examly.springapp.controller;

import com.examly.springapp.dto.ServiceTicketDTO;
import com.examly.springapp.model.TicketStatus;
import com.examly.springapp.service.TicketQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * [A01: Broken Access Control & IDOR Prevention]
 * Method-level secured Department Resolver Queue REST Controller.
 */
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentQueueController {

    private final TicketQueryService queryService;

    @Autowired
    public DepartmentQueueController(TicketQueryService queryService) {
        this.queryService = queryService;
    }

    @PreAuthorize("hasAnyRole('RESOLVER', 'IT_SUPPORT', 'HR_SPECIALIST', 'FACILITIES_CREW', 'DEPT_HEAD', 'ADMIN', 'AUDITOR')")
    @GetMapping("/{department}/queue")
    public ResponseEntity<Page<ServiceTicketDTO>> getDepartmentQueue(
            @PathVariable("department") String department,
            @RequestParam(name = "status", required = false) TicketStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Principal principal) {
        String username = principal != null ? principal.getName() : "anonymousUser";
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);
        Page<ServiceTicketDTO> queue = queryService.getDepartmentQueue(department, status, pageable, username);
        return ResponseEntity.ok(queue);
    }
}
