package com.examly.springapp.dto;

import com.examly.springapp.model.Profile;
import com.examly.springapp.model.UserAccount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EmployeeResponseDTO {
    private Long id;
    private Long userId;
    private String employeeId;
    private String username;
    private String email;
    private String phone;
    private String status;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private List<String> roles = new ArrayList<>();

    private String firstName;
    private String lastName;
    private String fullName;
    private String department;
    private String designation;
    private Double salary;
    private LocalDate dateOfJoining;
    private String reportingManager;
    private String emergencyContact;
    private String address;
    private LocalDate dob;
    private String gender;

    public EmployeeResponseDTO() {}

    public static EmployeeResponseDTO fromEntity(Profile profile, List<String> roles) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        if (profile != null) {
            dto.setId(profile.getId());
            dto.setEmployeeId(profile.getEmployeeId());
            dto.setFirstName(profile.getFirstName());
            dto.setLastName(profile.getLastName());
            
            String first = profile.getFirstName() != null ? profile.getFirstName() : "";
            String last = profile.getLastName() != null ? profile.getLastName() : "";
            dto.setFullName((first + " " + last).trim());

            dto.setDepartment(profile.getDepartment());
            dto.setDesignation(profile.getDesignation());
            dto.setSalary(profile.getSalary());
            dto.setDateOfJoining(profile.getDateOfJoining());
            dto.setReportingManager(profile.getReportingManager());
            dto.setEmergencyContact(profile.getEmergencyContact());
            dto.setAddress(profile.getAddress());
            dto.setDob(profile.getDob());
            dto.setGender(profile.getGender());

            UserAccount user = profile.getUser();
            if (user != null) {
                dto.setUserId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setEmail(user.getEmail());
                dto.setPhone(user.getPhone());
                dto.setStatus(user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
                dto.setFailedLoginAttempts(user.getFailedLoginAttempts());
                dto.setLockedUntil(user.getLockedUntil());
            }
        }
        if (roles != null) {
            dto.setRoles(roles);
        }
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }
    public LocalDate getDateOfJoining() { return dateOfJoining; }
    public void setDateOfJoining(LocalDate dateOfJoining) { this.dateOfJoining = dateOfJoining; }
    public String getReportingManager() { return reportingManager; }
    public void setReportingManager(String reportingManager) { this.reportingManager = reportingManager; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}

