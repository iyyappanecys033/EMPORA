package com.examly.springapp.service;

import com.examly.springapp.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface RoleService {
    Role saveRole(Role role);
    List<Role> getAllRoles();
    Optional<Role> getRoleById(Long id);
    Role updateRole(Long id, Role role);
    void deleteRole(Long id);
    Page<Role> getRolesWithPagination(Pageable pageable);
}