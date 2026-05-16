package com.examly.springapp.service;

import com.examly.springapp.model.UserRoleMapping;
import java.util.List;
import java.util.Optional;

public interface UserRoleMappingService {
    UserRoleMapping saveUserRoleMapping(UserRoleMapping userRoleMapping);
    List<UserRoleMapping> getAllUserRoleMappings();
    Optional<UserRoleMapping> getUserRoleMappingById(Long id);
    UserRoleMapping updateUserRoleMapping(Long id, UserRoleMapping userRoleMapping);
    void deleteUserRoleMapping(Long id);
    List<UserRoleMapping> getUserRoleMappingsByUserId(Long userId);
    Optional<UserRoleMapping> getUserRoleMappingByUserIdAndRoleId(Long userId, Long roleId);
}