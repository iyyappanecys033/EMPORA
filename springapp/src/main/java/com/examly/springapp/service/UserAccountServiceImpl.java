package com.examly.springapp.service;

import com.examly.springapp.model.UserAccount;
import com.examly.springapp.model.AccountStatus;
import com.examly.springapp.repository.UserAccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserAccountServiceImpl implements UserAccountService {
    
    @Autowired
    private UserAccountRepo userAccountRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Override
    public UserAccount saveUserAccount(UserAccount userAccount) {
        if (userAccount.getPasswordHash() != null && !userAccount.getPasswordHash().isEmpty()) {
            userAccount.setPasswordHash(passwordEncoder.encode(userAccount.getPasswordHash()));
        }
        UserAccount saved = userAccountRepo.save(userAccount);
        auditLogService.recordEvent("USER_CREATED", saved.getUsername(), "User account created");
        return saved;
    }
    
    @Override
    public List<UserAccount> getAllUserAccounts() {
        return userAccountRepo.findAll();
    }
    
    @Override
    public Optional<UserAccount> getUserAccountById(Long id) {
        return userAccountRepo.findById(id);
    }
    
    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return userAccountRepo.findByUsername(username);
    }
    
    @Override
    public UserAccount updateUserAccount(Long id, UserAccount userAccount) {
        Optional<UserAccount> existing = userAccountRepo.findById(id);
        if (existing.isPresent()) {
            userAccount.setId(id);
            if (userAccount.getPasswordHash() != null && !userAccount.getPasswordHash().isEmpty()) {
                userAccount.setPasswordHash(passwordEncoder.encode(userAccount.getPasswordHash()));
            } else {
                userAccount.setPasswordHash(existing.get().getPasswordHash());
            }
            // Protect lifecycle fields from generic update
            userAccount.setStatus(existing.get().getStatus());
            userAccount.setFailedLoginAttempts(existing.get().getFailedLoginAttempts());
            userAccount.setLockedUntil(existing.get().getLockedUntil());
            
            UserAccount saved = userAccountRepo.save(userAccount);
            auditLogService.recordEvent("USER_UPDATED", saved.getUsername(), "User account updated");
            return saved;
        }
        return null;
    }
    
    @Override
    public void deleteUserAccount(Long id) {
        Optional<UserAccount> existing = userAccountRepo.findById(id);
        if (existing.isPresent()) {
            String targetUsername = existing.get().getUsername();
            userAccountRepo.deleteById(id);
            auditLogService.recordEvent("USER_DELETED", targetUsername, "User account deleted");
        } else {
            userAccountRepo.deleteById(id);
        }
    }

    @Override
    public UserAccount suspendUser(Long id) {
        UserAccount user = userAccountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot suspend a user that is not ACTIVE");
        }
        user.setStatus(AccountStatus.SUSPENDED);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        UserAccount saved = userAccountRepo.save(user);
        auditLogService.recordEvent("ACCOUNT_SUSPENDED", saved.getUsername(), "Account suspended by administrator");
        return saved;
    }

    @Override
    public UserAccount disableUser(Long id) {
        UserAccount user = userAccountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot disable a user that is not ACTIVE");
        }
        user.setStatus(AccountStatus.DISABLED);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        UserAccount saved = userAccountRepo.save(user);
        auditLogService.recordEvent("ACCOUNT_DISABLED", saved.getUsername(), "Account disabled by administrator");
        return saved;
    }

    @Override
    public UserAccount reactivateUser(Long id) {
        UserAccount user = userAccountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("User is already ACTIVE");
        }
        user.setStatus(AccountStatus.ACTIVE);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        UserAccount saved = userAccountRepo.save(user);
        auditLogService.recordEvent("ACCOUNT_REACTIVATED", saved.getUsername(), "Account reactivated by administrator");
        return saved;
    }

    @Override
    public UserAccount unlockUser(Long id) {
        UserAccount user = userAccountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        UserAccount saved = userAccountRepo.save(user);
        auditLogService.recordEvent("ACCOUNT_UNLOCKED", saved.getUsername(), "Temporary security lock removed by administrator");
        return saved;
    }
}