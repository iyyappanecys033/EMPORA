package com.examly.springapp.controller;

import com.examly.springapp.model.AccountStatusLog;
import com.examly.springapp.service.AccountStatusLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/accountStatusLogs")
@PreAuthorize("hasRole('ADMIN')")
public class AccountStatusLogController {
    
    @Autowired
    private AccountStatusLogService accountStatusLogService;
    
    @PostMapping
    public ResponseEntity<AccountStatusLog> createAccountStatusLog(@RequestBody AccountStatusLog accountStatusLog) {
        AccountStatusLog savedAccountStatusLog = accountStatusLogService.saveAccountStatusLog(accountStatusLog);
        return ResponseEntity.ok(savedAccountStatusLog);
    }
    
    @GetMapping
    public ResponseEntity<List<AccountStatusLog>> getAllAccountStatusLogs() {
        List<AccountStatusLog> accountStatusLogs = accountStatusLogService.getAllAccountStatusLogs();
        return ResponseEntity.ok(accountStatusLogs);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AccountStatusLog> getAccountStatusLogById(@PathVariable Long id) {
        Optional<AccountStatusLog> accountStatusLog = accountStatusLogService.getAccountStatusLogById(id);
        return accountStatusLog.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AccountStatusLog> updateAccountStatusLog(@PathVariable Long id, @RequestBody AccountStatusLog accountStatusLog) {
        AccountStatusLog updatedAccountStatusLog = accountStatusLogService.updateAccountStatusLog(id, accountStatusLog);
        return ResponseEntity.ok(updatedAccountStatusLog);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccountStatusLog(@PathVariable Long id) {
        accountStatusLogService.deleteAccountStatusLog(id);
        return ResponseEntity.noContent().build();
    }
}