package com.examly.springapp.controller;

import com.examly.springapp.model.UserAccount;
import com.examly.springapp.service.UserAccountService;
import com.examly.springapp.service.RiskAssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.examly.springapp.dto.UserResponseDTO;
import com.examly.springapp.dto.RiskAssessmentDTO;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {
    
    private final UserAccountService userAccountService;
    private final RiskAssessmentService riskAssessmentService;
    private final com.examly.springapp.repository.UserRoleMappingRepo userRoleMappingRepo;
    private final com.examly.springapp.repository.ProfileRepo profileRepo;

    @Autowired
    public UserAccountController(UserAccountService userAccountService, 
                                 RiskAssessmentService riskAssessmentService,
                                 com.examly.springapp.repository.UserRoleMappingRepo userRoleMappingRepo,
                                 com.examly.springapp.repository.ProfileRepo profileRepo) {
        this.userAccountService = userAccountService;
        this.riskAssessmentService = riskAssessmentService;
        this.userRoleMappingRepo = userRoleMappingRepo;
        this.profileRepo = profileRepo;
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserAccount> createUserAccount(@RequestBody UserAccount userAccount) {
        UserAccount savedUserAccount = userAccountService.saveUserAccount(userAccount);
        return ResponseEntity.ok(savedUserAccount);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<UserAccount> userAccountOpt = userAccountService.findByUsername(username);
        if (userAccountOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        UserAccount userAccount = userAccountOpt.get();
        List<String> roles = userRoleMappingRepo.findByUserId(userAccount.getId())
                .stream()
                .map(m -> m.getRole().getRoleName().toUpperCase())
                .toList();
        UserResponseDTO dto = new UserResponseDTO(userAccount, roles);
        profileRepo.findByUserId(userAccount.getId()).ifPresent(p -> {
            dto.setEmployeeId(p.getEmployeeId());
            dto.setDepartment(p.getDepartment());
            dto.setDesignation(p.getDesignation());
            String first = p.getFirstName() != null ? p.getFirstName() : "";
            String last = p.getLastName() != null ? p.getLastName() : "";
            dto.setFullName((first + " " + last).trim());
        });
        return ResponseEntity.ok(dto);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'AUDITOR')")
    @GetMapping
    public ResponseEntity<List<UserAccount>> getAllUserAccounts() {
        List<UserAccount> userAccounts = userAccountService.getAllUserAccounts();
        return ResponseEntity.ok(userAccounts);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'AUDITOR')")
    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getUserAccountById(@PathVariable Long id) {
        Optional<UserAccount> userAccount = userAccountService.getUserAccountById(id);
        return userAccount.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserAccount> updateUserAccount(@PathVariable Long id, @RequestBody UserAccount userAccount) {
        UserAccount updatedUserAccount = userAccountService.updateUserAccount(id, userAccount);
        return ResponseEntity.ok(updatedUserAccount);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserAccount(@PathVariable Long id) {
        userAccountService.deleteUserAccount(id);
        return ResponseEntity.noContent().build();
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/suspend")
    public ResponseEntity<UserAccount> suspendUserAccount(@PathVariable Long id) {
        try {
            UserAccount account = userAccountService.suspendUser(id);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/disable")
    public ResponseEntity<UserAccount> disableUserAccount(@PathVariable Long id) {
        try {
            UserAccount account = userAccountService.disableUser(id);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<UserAccount> reactivateUserAccount(@PathVariable Long id) {
        try {
            UserAccount account = userAccountService.reactivateUser(id);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/unlock")
    public ResponseEntity<UserAccount> unlockUserAccount(@PathVariable Long id) {
        try {
            UserAccount account = userAccountService.unlockUser(id);
            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/risk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RiskAssessmentDTO> getRiskAssessment(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(riskAssessmentService.assessRisk(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}