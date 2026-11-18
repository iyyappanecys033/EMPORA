package com.examly.springapp.service;

import com.examly.springapp.dto.EmployeeRequestDTO;
import com.examly.springapp.dto.EmployeeResponseDTO;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.ProfileRepo;
import com.examly.springapp.repository.RoleRepo;
import com.examly.springapp.repository.UserAccountRepo;
import com.examly.springapp.repository.UserRoleMappingRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements ProfileService {
    
    private final ProfileRepo profileRepo;
    private final UserAccountRepo userAccountRepo;
    private final UserRoleMappingRepo userRoleMappingRepo;
    private final RoleRepo roleRepo;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ProfileServiceImpl(ProfileRepo profileRepo,
                              UserAccountRepo userAccountRepo,
                              UserRoleMappingRepo userRoleMappingRepo,
                              RoleRepo roleRepo,
                              AuditLogService auditLogService,
                              PasswordEncoder passwordEncoder) {
        this.profileRepo = profileRepo;
        this.userAccountRepo = userAccountRepo;
        this.userRoleMappingRepo = userRoleMappingRepo;
        this.roleRepo = roleRepo;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public Profile saveProfile(Profile profile) {
        return profileRepo.save(profile);
    }
    
    @Override
    public List<Profile> getAllProfiles() {
        return profileRepo.findAll();
    }

    @Override
    public Page<Profile> getAllProfiles(Pageable pageable) {
        return profileRepo.findAll(pageable);
    }
    
    @Override
    public Optional<Profile> getProfileById(Long id) {
        return profileRepo.findById(id);
    }

    @Override
    public Optional<Profile> getProfileByEmployeeId(String employeeId) {
        return profileRepo.findByEmployeeId(employeeId);
    }

    @Override
    public Optional<Profile> getProfileByUserId(Long userId) {
        return profileRepo.findByUserId(userId);
    }
    
    @Override
    public Profile updateProfile(Long id, Profile profile) {
        profile.setId(id);
        Profile saved = profileRepo.save(profile);
        String target = saved.getUser() != null ? saved.getUser().getUsername() : saved.getEmployeeId();
        auditLogService.recordEvent("PROFILE_UPDATED", target, "Updated profile ID: " + id);
        return saved;
    }
    
    @Override
    public void deleteProfile(Long id) {
        Optional<Profile> existing = profileRepo.findById(id);
        existing.ifPresent(p -> {
            String target = p.getUser() != null ? p.getUser().getUsername() : p.getEmployeeId();
            auditLogService.recordEvent("PROFILE_DELETED", target, "Deleted profile for employee: " + p.getEmployeeId());
        });
        profileRepo.deleteById(id);
    }
    
    @Override
    public List<Profile> getProfilesByName(String firstName) {
        return profileRepo.findByName(firstName);
    }
    
    @Override
    public List<Profile> getProfilesByNameAndAddress(String firstName, String address) {
        return profileRepo.findByNameAndAddress(firstName, address);
    }

    private List<String> getUserRoles(UserAccount user) {
        if (user == null || user.getId() == null) {
            return new ArrayList<>();
        }
        return userRoleMappingRepo.findByUserId(user.getId())
                .stream()
                .map(m -> m.getRole().getRoleName().toUpperCase())
                .collect(Collectors.toList());
    }

    @Override
    public Page<EmployeeResponseDTO> searchEmployees(String query, String department, Pageable pageable) {
        Page<Profile> profiles = profileRepo.searchProfiles(query, department, pageable);
        return profiles.map(p -> EmployeeResponseDTO.fromEntity(p, getUserRoles(p.getUser())));
    }

    @Override
    public EmployeeResponseDTO getEmployeeDetailsById(Long id) {
        Profile profile = profileRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee profile not found with id: " + id));
        return EmployeeResponseDTO.fromEntity(profile, getUserRoles(profile.getUser()));
    }

    @Override
    public EmployeeResponseDTO getEmployeeDetailsByUsername(String username) {
        UserAccount user = userAccountRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Profile profile = profileRepo.findByUserId(user.getId())
                .orElseGet(() -> {
                    Profile p = new Profile();
                    p.setUser(user);
                    p.setFirstName(user.getUsername());
                    p.setEmployeeId("EMP-" + user.getId());
                    return profileRepo.save(p);
                });
        return EmployeeResponseDTO.fromEntity(profile, getUserRoles(user));
    }

    @Override
    @Transactional
    public EmployeeResponseDTO saveOrUpdateEmployee(EmployeeRequestDTO request, String actorUsername) {
        UserAccount user = null;
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            Optional<UserAccount> existingUser = userAccountRepo.findByUsername(request.getUsername());
            if (existingUser.isPresent()) {
                user = existingUser.get();
                if (request.getEmail() != null) user.setEmail(request.getEmail());
                if (request.getPhone() != null) user.setPhone(request.getPhone());
                if (request.getStatus() != null) {
                    try {
                        user.setStatus(AccountStatus.valueOf(request.getStatus().toUpperCase()));
                    } catch (Exception ignored) {}
                }
                userAccountRepo.save(user);
            } else {
                user = new UserAccount();
                user.setUsername(request.getUsername());
                String rawPass = (request.getPassword() != null && !request.getPassword().isEmpty()) 
                        ? request.getPassword() : "Welcome@123";
                user.setPasswordHash(passwordEncoder.encode(rawPass));
                user.setEmail(request.getEmail());
                user.setPhone(request.getPhone());
                user.setStatus(AccountStatus.ACTIVE);
                user = userAccountRepo.save(user);

                // Assign role
                String targetRole = (request.getRole() != null && !request.getRole().isEmpty()) 
                        ? request.getRole().toUpperCase().replace("ROLE_", "") : "EMPLOYEE";
                Role role = roleRepo.findByRoleName(targetRole).orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName(targetRole);
                    return roleRepo.save(r);
                });
                UserRoleMapping mapping = new UserRoleMapping();
                mapping.setUser(user);
                mapping.setRole(role);
                userRoleMappingRepo.save(mapping);

                auditLogService.recordEvent("USER_CREATED", user.getUsername(), "Created employee user account with role " + targetRole);
            }
        }

        Profile profile = null;
        if (request.getEmployeeId() != null && !request.getEmployeeId().isEmpty()) {
            profile = profileRepo.findByEmployeeId(request.getEmployeeId()).orElse(null);
        }
        if (profile == null && user != null) {
            profile = profileRepo.findByUserId(user.getId()).orElse(null);
        }
        if (profile == null) {
            profile = new Profile();
            if (request.getEmployeeId() != null && !request.getEmployeeId().isEmpty()) {
                profile.setEmployeeId(request.getEmployeeId());
            } else {
                profile.setEmployeeId("EMP-" + System.currentTimeMillis() % 100000);
            }
        }

        if (user != null) {
            profile.setUser(user);
        }
        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getDepartment() != null) profile.setDepartment(request.getDepartment());
        if (request.getDesignation() != null) profile.setDesignation(request.getDesignation());
        if (request.getSalary() != null) profile.setSalary(request.getSalary());
        if (request.getDateOfJoining() != null) profile.setDateOfJoining(request.getDateOfJoining());
        if (request.getReportingManager() != null) profile.setReportingManager(request.getReportingManager());
        if (request.getEmergencyContact() != null) profile.setEmergencyContact(request.getEmergencyContact());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getDob() != null) profile.setDob(request.getDob());
        if (request.getGender() != null) profile.setGender(request.getGender());

        Profile saved = profileRepo.save(profile);
        String target = user != null ? user.getUsername() : saved.getEmployeeId();
        auditLogService.recordEvent("EMPLOYEE_SAVED", target, "Saved employee record: " + saved.getEmployeeId() + " (" + saved.getDepartment() + ")");

        return EmployeeResponseDTO.fromEntity(saved, getUserRoles(user));
    }
}
