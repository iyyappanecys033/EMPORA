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
    
    @Override
    public Role saveRole(Role role) {
        return roleRepo.save(role);
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
        return roleRepo.save(role);
    }
    
    @Override
    public void deleteRole(Long id) {
        roleRepo.deleteById(id);
    }
    
    @Override
    public Page<Role> getRolesWithPagination(Pageable pageable) {
        return roleRepo.findAll(pageable);
    }
}