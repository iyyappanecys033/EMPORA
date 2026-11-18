package com.examly.springapp.service;

import com.examly.springapp.dto.ServiceTicketDTO;
import com.examly.springapp.dto.TicketCommentDTO;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.exception.UnauthorizedScopeException;
import com.examly.springapp.model.ServiceTicket;
import com.examly.springapp.model.TicketComment;
import com.examly.springapp.model.TicketStatus;
import com.examly.springapp.model.UserAccount;
import com.examly.springapp.repository.ServiceTicketRepository;
import com.examly.springapp.repository.TicketCommentRepository;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.security.TicketSecurityEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only query service with organizational scope enforcement and pagination.
 */
@Service
@Transactional(readOnly = true)
public class TicketQueryService {

    private final ServiceTicketRepository serviceTicketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserAccountRepo userAccountRepo;
    private final TicketSecurityEvaluator ticketSecurity;
    private final TicketCommandService commandService;

    @Autowired
    public TicketQueryService(ServiceTicketRepository serviceTicketRepository,
                              TicketCommentRepository ticketCommentRepository,
                              UserAccountRepo userAccountRepo,
                              TicketSecurityEvaluator ticketSecurity,
                              TicketCommandService commandService) {
        this.serviceTicketRepository = serviceTicketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.userAccountRepo = userAccountRepo;
        this.ticketSecurity = ticketSecurity;
        this.commandService = commandService;
    }

    public ServiceTicketDTO getTicketById(Long ticketId, String viewerUsername) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        if (!ticketSecurity.canViewTicket(ticket, viewerUsername)) {
            throw new UnauthorizedScopeException("User is not authorized to view this ticket.");
        }

        return commandService.toDto(ticket);
    }

    public Page<ServiceTicketDTO> getMyTickets(String requesterUsername, Pageable pageable) {
        UserAccount requester = userAccountRepo.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterUsername));

        return serviceTicketRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId(), pageable)
                .map(commandService::toDto);
    }

    public Page<ServiceTicketDTO> getDepartmentQueue(String department, TicketStatus status, Pageable pageable, String viewerUsername) {
        if (!ticketSecurity.canAccessDepartmentQueue(department, viewerUsername)) {
            throw new UnauthorizedScopeException("User is not authorized to access queue for department: " + department);
        }

        if (status != null) {
            return serviceTicketRepository.findByDepartmentAndStatusOrderByCreatedAtDesc(department, status, pageable)
                    .map(commandService::toDto);
        } else {
            return serviceTicketRepository.findByDepartmentOrderByCreatedAtDesc(department, pageable)
                    .map(commandService::toDto);
        }
    }

    public List<TicketCommentDTO> getCommentsForTicket(Long ticketId, String viewerUsername) {
        ServiceTicket ticket = serviceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        if (!ticketSecurity.canViewTicket(ticket, viewerUsername)) {
            throw new UnauthorizedScopeException("User is not authorized to view comments on this ticket.");
        }

        boolean isSpecialistOrAdmin = ticketSecurity.isAdmin(viewerUsername) || 
                                     ticketSecurity.isDepartmentResolver(ticket, viewerUsername);

        List<TicketComment> comments;
        if (isSpecialistOrAdmin) {
            comments = ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        } else {
            comments = ticketCommentRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(ticketId);
        }

        return comments.stream().map(commandService::toDto).collect(Collectors.toList());
    }
}

