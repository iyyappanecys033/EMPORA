package com.examly.springapp.service;

import com.examly.springapp.dto.EmployeeRequestDTO;
import com.examly.springapp.dto.EmployeeResponseDTO;
import com.examly.springapp.model.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProfileService {
    Profile saveProfile(Profile profile);
    List<Profile> getAllProfiles();
    Page<Profile> getAllProfiles(Pageable pageable);
    Optional<Profile> getProfileById(Long id);
    Optional<Profile> getProfileByEmployeeId(String employeeId);
    Optional<Profile> getProfileByUserId(Long userId);
    Profile updateProfile(Long id, Profile profile);
    void deleteProfile(Long id);
    List<Profile> getProfilesByName(String firstName);
    List<Profile> getProfilesByNameAndAddress(String firstName, String address);

    Page<EmployeeResponseDTO> searchEmployees(String query, String department, Pageable pageable);
    EmployeeResponseDTO getEmployeeDetailsById(Long id);
    EmployeeResponseDTO getEmployeeDetailsByUsername(String username);
    EmployeeResponseDTO saveOrUpdateEmployee(EmployeeRequestDTO request, String actorUsername);
}
