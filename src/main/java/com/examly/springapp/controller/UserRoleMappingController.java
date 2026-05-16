package com.examly.springapp.controller;

import com.examly.springapp.dto.UserRoleMappingDto;
import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.service.UserRoleMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/userRoleMappings")
public class UserRoleMappingController {
    
    @Autowired
    private UserRoleMappingService userRoleMappingService;
    
    @PostMapping
    public ResponseEntity<UserRoleMapping> createUserRoleMapping(@RequestBody UserRoleMapping userRoleMapping) {
        UserRoleMapping savedUserRoleMapping = userRoleMappingService.saveUserRoleMapping(userRoleMapping);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUserRoleMapping);
    }
    
    @GetMapping
    public ResponseEntity<List<UserRoleMapping>> getAllUserRoleMappings() {
        List<UserRoleMapping> userRoleMappings = userRoleMappingService.getAllUserRoleMappings();
        return ResponseEntity.ok(userRoleMappings);
    }

    @GetMapping("/expanded")
    public ResponseEntity<List<UserRoleMappingDto>> getAllExpandedUserRoleMappings() {
        List<UserRoleMappingDto> dtos = userRoleMappingService.getAllUserRoleMappings().stream()
                .map(UserRoleMappingDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserRoleMapping> getUserRoleMappingById(@PathVariable Long id) {
        Optional<UserRoleMapping> userRoleMapping = userRoleMappingService.getUserRoleMappingById(id);
        return userRoleMapping.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserRoleMapping> updateUserRoleMapping(@PathVariable Long id, @RequestBody UserRoleMapping userRoleMapping) {
        UserRoleMapping updatedUserRoleMapping = userRoleMappingService.updateUserRoleMapping(id, userRoleMapping);
        return ResponseEntity.ok(updatedUserRoleMapping);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserRoleMapping(@PathVariable Long id) {
        userRoleMappingService.deleteUserRoleMapping(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserRoleMapping>> getUserRoleMappingsByUserId(@PathVariable Long userId) {
        List<UserRoleMapping> userRoleMappings = userRoleMappingService.getUserRoleMappingsByUserId(userId);
        if (userRoleMappings.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(userRoleMappings);
    }
    
    @GetMapping("/user/{userId}/role/{roleId}")
    public ResponseEntity<UserRoleMapping> getUserRoleMappingByUserIdAndRoleId(@PathVariable Long userId, @PathVariable Long roleId) {
        Optional<UserRoleMapping> userRoleMapping = userRoleMappingService.getUserRoleMappingByUserIdAndRoleId(userId, roleId);
        return userRoleMapping.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}