package com.examly.springapp.dto;

import com.examly.springapp.model.UserAccount;
import java.util.ArrayList;
import java.util.List;

public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String status;
    private int failedLoginAttempts;
    private List<String> roles = new ArrayList<>();
    private String employeeId;
    private String fullName;
    private String department;
    private String designation;

    public UserResponseDTO() {}

    public UserResponseDTO(UserAccount userAccount) {
        if (userAccount != null) {
            this.id = userAccount.getId();
            this.username = userAccount.getUsername();
            this.email = userAccount.getEmail();
            this.phone = userAccount.getPhone();
            this.status = userAccount.getStatus() != null ? userAccount.getStatus().name() : null;
            this.failedLoginAttempts = userAccount.getFailedLoginAttempts();
        }
    }

    public UserResponseDTO(UserAccount userAccount, List<String> roles) {
        this(userAccount);
        if (roles != null) {
            this.roles = roles;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
