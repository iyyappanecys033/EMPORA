package com.examly.springapp.controller;

import com.examly.springapp.model.UserAccount;
import com.examly.springapp.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {
    
    @Autowired
    private UserAccountService userAccountService;
    
    @PostMapping
    public ResponseEntity<UserAccount> createUserAccount(@RequestBody UserAccount userAccount) {
        UserAccount savedUserAccount = userAccountService.saveUserAccount(userAccount);
        return ResponseEntity.ok(savedUserAccount);
    }
    
    @GetMapping
    public ResponseEntity<List<UserAccount>> getAllUserAccounts() {
        List<UserAccount> userAccounts = userAccountService.getAllUserAccounts();
        return ResponseEntity.ok(userAccounts);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getUserAccountById(@PathVariable Long id) {
        Optional<UserAccount> userAccount = userAccountService.getUserAccountById(id);
        return userAccount.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserAccount> updateUserAccount(@PathVariable Long id, @RequestBody UserAccount userAccount) {
        UserAccount updatedUserAccount = userAccountService.updateUserAccount(id, userAccount);
        return ResponseEntity.ok(updatedUserAccount);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserAccount(@PathVariable Long id) {
        userAccountService.deleteUserAccount(id);
        return ResponseEntity.noContent().build();
    }
}