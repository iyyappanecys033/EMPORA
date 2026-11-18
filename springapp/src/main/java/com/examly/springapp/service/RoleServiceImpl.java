package com.examly.springapp.service;

import com.examly.springapp.model.Role;
import com.examly.springapp.repository.RoleRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RoleServiceImpl implements RoleService {
    
    @Autowired
    private RoleRepo roleRepo;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Override
    public Role saveRole(Role role) {
        Role saved = roleRepo.save(role);
        auditLogService.recordEvent("ROLE_CREATED", null, "Role created: " + saved.getRoleName());
        return saved;
    }
    
    @Override
    public List<Role> getAllRoles() {
        return roleRepo.findAll();
    }
    
    @Override
    public Optional<Role> getRoleById(Long id) {
        return roleRepo.findById(id);
    }
    
    @Override
    public Role updateRole(Long id, Role role) {
        role.setId(id);
        Role saved = roleRepo.save(role);
        auditLogService.recordEvent("ROLE_UPDATED", null, "Role updated: " + saved.getRoleName());
        return saved;
    }
    
    @Override
    public void deleteRole(Long id) {
        Optional<Role> existing = roleRepo.findById(id);
        if (existing.isPresent()) {
            String roleName = existing.get().getRoleName();
            roleRepo.deleteById(id);
            auditLogService.recordEvent("ROLE_DELETED", null, "Role deleted: " + roleName);
        } else {
            roleRepo.deleteById(id);
        }
    }
    
    @Override
    public Page<Role> getRolesWithPagination(Pageable pageable) {
        return roleRepo.findAll(pageable);
    }
}