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
    
    @Override
    public UserRoleMapping saveUserRoleMapping(UserRoleMapping userRoleMapping) {
        return userRoleMappingRepo.save(userRoleMapping);
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
        userRoleMappingRepo.deleteById(id);
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