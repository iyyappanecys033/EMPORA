package com.examly.springapp.security;

import com.examly.springapp.model.ApprovalTask;
import com.examly.springapp.model.Profile;
import com.examly.springapp.model.ServiceTicket;
import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.repository.ProfileRepo;
import com.examly.springapp.repository.UserRoleMappingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Security Evaluator enforcing resource ownership, role boundaries, and
 * organizational department scope on ticket lifecycle and approval operations.
 */
@Component("ticketSecurity")
public class TicketSecurityEvaluator {

    private final ProfileRepo profileRepo;
    private final UserRoleMappingRepo userRoleMappingRepo;

    @Autowired
    public TicketSecurityEvaluator(ProfileRepo profileRepo, UserRoleMappingRepo userRoleMappingRepo) {
        this.profileRepo = profileRepo;
        this.userRoleMappingRepo = userRoleMappingRepo;
    }

    public boolean canViewTicket(ServiceTicket ticket, String username) {
        if (ticket == null || username == null) return false;
        if (isAdmin(username) || isAuditor(username)) return true;
        if (isRequester(ticket, username)) return true;
        if (isAssignee(ticket, username)) return true;
        return isDepartmentResolver(ticket, username);
    }

    public boolean canCancelTicket(ServiceTicket ticket, String username) {
        if (ticket == null || username == null) return false;
        if (isAdmin(username)) return true;
        return isRequester(ticket, username);
    }

    public boolean canAssignTicket(ServiceTicket ticket, String username) {
        if (ticket == null || username == null) return false;
        if (isAdmin(username)) return true;
        return isDepartmentResolver(ticket, username);
    }

    public boolean canChangeStatus(ServiceTicket ticket, String username) {
        if (ticket == null || username == null) return false;
        if (isAdmin(username)) return true;
        if (isAssignee(ticket, username)) return true;
        return isDepartmentResolver(ticket, username);
    }

    public boolean canDecideApproval(ApprovalTask task, String username) {
        if (task == null || username == null) return false;
        // Anti-self-approval rule: Requester cannot decide their own approval task
        if (task.getTicket() != null && task.getTicket().getRequester() != null) {
            if (task.getTicket().getRequester().getUsername().equalsIgnoreCase(username)) {
                return false;
            }
        }
        if (isAdmin(username)) return true;
        return task.getApprover() != null && task.getApprover().getUsername().equalsIgnoreCase(username);
    }

    public boolean canAccessDepartmentQueue(String department, String username) {
        if (department == null || username == null) return false;
        if (isAdmin(username) || isAuditor(username)) return true;
        if (!isResolverRole(username)) return false;

        Optional<Profile> profileOpt = profileRepo.findByUserUsername(username);
        return profileOpt.isPresent() && department.equalsIgnoreCase(profileOpt.get().getDepartment());
    }

    public boolean isRequester(ServiceTicket ticket, String username) {
        return ticket != null && ticket.getRequester() != null &&
               ticket.getRequester().getUsername().equalsIgnoreCase(username);
    }

    public boolean isAssignee(ServiceTicket ticket, String username) {
        return ticket != null && ticket.getAssignee() != null &&
               ticket.getAssignee().getUsername().equalsIgnoreCase(username);
    }

    public boolean isDepartmentResolver(ServiceTicket ticket, String username) {
        if (ticket == null || username == null) return false;
        if (!isResolverRole(username)) return false;

        Optional<Profile> profileOpt = profileRepo.findByUserUsername(username);
        return profileOpt.isPresent() && ticket.getDepartment().equalsIgnoreCase(profileOpt.get().getDepartment());
    }

    public boolean isResolverRole(String username) {
        return hasRole(username, "ROLE_RESOLVER")
                || hasRole(username, "ROLE_IT_SUPPORT")
                || hasRole(username, "ROLE_HR_SPECIALIST")
                || hasRole(username, "ROLE_FACILITIES_CREW")
                || hasRole(username, "ROLE_DEPT_HEAD");
    }

    public boolean isAdmin(String username) {
        return hasRole(username, "ROLE_ADMIN");
    }

    public boolean isAuditor(String username) {
        return hasRole(username, "ROLE_AUDITOR");
    }

    public boolean hasRole(String username, String roleName) {
        if (username == null || roleName == null) return false;
        List<UserRoleMapping> mappings = userRoleMappingRepo.findByUserUsername(username);
        String target = roleName.toUpperCase().startsWith("ROLE_") ? roleName.toUpperCase() : "ROLE_" + roleName.toUpperCase();
        return mappings.stream().anyMatch(m -> {
            if (m.getRole() == null || m.getRole().getRoleName() == null) return false;
            String raw = m.getRole().getRoleName().toUpperCase();
            String normalized = raw.startsWith("ROLE_") ? raw : "ROLE_" + raw;
            return normalized.equals(target);
        });
    }
}
