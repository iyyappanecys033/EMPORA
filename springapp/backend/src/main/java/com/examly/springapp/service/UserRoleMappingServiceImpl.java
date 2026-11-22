package com.examly.springapp.service;

import com.examly.springapp.model.UserRoleMapping;
import com.examly.springapp.repository.UserRoleMappingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserRoleMappingServiceImpl implements UserRoleMappingService {
    
    @Autowired
    private UserRoleMappingRepo userRoleMappingRepo;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Override
    public UserRoleMapping saveUserRoleMapping(UserRoleMapping userRoleMapping) {
        if (userRoleMapping.getUser() != null && userRoleMapping.getRole() != null) {
            UserRoleMapping existing = userRoleMappingRepo.findByUserIdAndRoleId(
                    userRoleMapping.getUser().getId(),
                    userRoleMapping.getRole().getId()
            );
            if (existing != null) {
                throw new IllegalArgumentException("User role mapping already exists");
            }
        }
        UserRoleMapping saved = userRoleMappingRepo.save(userRoleMapping);
        String targetUsername = saved.getUser() != null ? saved.getUser().getUsername() : null;
        String roleName = saved.getRole() != null ? saved.getRole().getRoleName() : null;
        auditLogService.recordEvent("ROLE_ASSIGNED", targetUsername, "Role assigned: " + roleName);
        return saved;
    }
    
    @Override
    public List<UserRoleMapping> getAllUserRoleMappings() {
        return userRoleMappingRepo.findAll();
    }
    
    @Override
    public Optional<UserRoleMapping> getUserRoleMappingById(Long id) {
        return userRoleMappingRepo.findById(id);
    }
    
    @Override
    public UserRoleMapping updateUserRoleMapping(Long id, UserRoleMapping userRoleMapping) {
        userRoleMapping.setId(id);
        return userRoleMappingRepo.save(userRoleMapping);
    }
    
    @Override
    public void deleteUserRoleMapping(Long id) {
        Optional<UserRoleMapping> existing = userRoleMappingRepo.findById(id);
        if (existing.isPresent()) {
            UserRoleMapping mapping = existing.get();
            String targetUsername = mapping.getUser() != null ? mapping.getUser().getUsername() : null;
            String roleName = mapping.getRole() != null ? mapping.getRole().getRoleName() : null;
            userRoleMappingRepo.deleteById(id);
            auditLogService.recordEvent("ROLE_REMOVED", targetUsername, "Role removed: " + roleName);
        } else {
            userRoleMappingRepo.deleteById(id);
        }
    }
    
    @Override
    public List<UserRoleMapping> getUserRoleMappingsByUserId(Long userId) {
        return userRoleMappingRepo.findByUserId(userId);
    }
    
    @Override
    public Optional<UserRoleMapping> getUserRoleMappingByUserIdAndRoleId(Long userId, Long roleId) {
        UserRoleMapping mapping = userRoleMappingRepo.findByUserIdAndRoleId(userId, roleId);
        return Optional.ofNullable(mapping);
    }
}