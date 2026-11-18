package com.examly.springapp.controller;

import com.examly.springapp.dto.EmployeeRequestDTO;
import com.examly.springapp.dto.EmployeeResponseDTO;
import com.examly.springapp.model.Profile;
import com.examly.springapp.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    
    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @PostMapping
    public ResponseEntity<Profile> createProfile(@RequestBody Profile profile) {
        Profile savedProfile = profileService.saveProfile(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProfile);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'AUDITOR')")
    @GetMapping
    public ResponseEntity<List<Profile>> getAllProfiles() {
        List<Profile> profiles = profileService.getAllProfiles();
        return ResponseEntity.ok(profiles);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'AUDITOR')")
    @GetMapping("/{id}")
    public ResponseEntity<Profile> getProfileById(@PathVariable Long id) {
        Optional<Profile> profile = profileService.getProfileById(id);
        return profile.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<Profile> updateProfile(@PathVariable Long id, @RequestBody Profile profile) {
        Profile updatedProfile = profileService.updateProfile(id, profile);
        return ResponseEntity.ok(updatedProfile);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'AUDITOR')")
    @GetMapping("/name/{firstName}")
    public ResponseEntity<List<Profile>> getProfilesByName(@PathVariable String firstName) {
        List<Profile> profiles = profileService.getProfilesByName(firstName);
        return ResponseEntity.ok(profiles);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'AUDITOR')")
    @GetMapping("/search/{firstName}/{address}")
    public ResponseEntity<List<Profile>> getProfilesByNameAndAddress(@PathVariable String firstName, @PathVariable String address) {
        List<Profile> profiles = profileService.getProfilesByNameAndAddress(firstName, address);
        return ResponseEntity.ok(profiles);
    }

    // --- Modern EPAMS Employee Account Management Endpoints ---

    @GetMapping("/directory")
    public ResponseEntity<Page<EmployeeResponseDTO>> getEmployeeDirectory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department
    ) {
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.min(Math.max(size, 1), 100);
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sort);
        Page<EmployeeResponseDTO> results = profileService.searchEmployees(query, department, pageable);

        // Redact sensitive HR/PII fields for non-Admin/HR_MANAGER directory viewers
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean privileged = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority())
                        || "ROLE_HR_MANAGER".equalsIgnoreCase(a.getAuthority()));
        if (!privileged) {
            results.forEach(dto -> {
                dto.setSalary(null);
                dto.setDob(null);
                dto.setAddress(null);
                dto.setEmergencyContact(null);
                dto.setFailedLoginAttempts(0);
                dto.setLockedUntil(null);
            });
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponseDTO> getMyProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            EmployeeResponseDTO details = profileService.getEmployeeDetailsByUsername(username);
            return ResponseEntity.ok(details);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'AUDITOR')")
    @GetMapping("/{id}/details")
    public ResponseEntity<EmployeeResponseDTO> getEmployeeDetails(@PathVariable Long id) {
        try {
            EmployeeResponseDTO details = profileService.getEmployeeDetailsById(id);
            return ResponseEntity.ok(details);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @PostMapping("/employee")
    public ResponseEntity<EmployeeResponseDTO> saveEmployee(@RequestBody EmployeeRequestDTO request) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        EmployeeResponseDTO saved = profileService.saveOrUpdateEmployee(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    @PutMapping("/employee")
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(@RequestBody EmployeeRequestDTO request) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        EmployeeResponseDTO updated = profileService.saveOrUpdateEmployee(request, actor);
        return ResponseEntity.ok(updated);
    }
}
