package com.examly.springapp.dto;

import com.examly.springapp.model.UserRoleMapping;

public class UserRoleMappingDto {

    private Long id;
    private Long userId;
    private String username;
    private Long roleId;
    private String roleName;

    public static UserRoleMappingDto fromEntity(UserRoleMapping mapping) {
        UserRoleMappingDto dto = new UserRoleMappingDto();
        dto.setId(mapping.getId());
        if (mapping.getUser() != null) {
            dto.setUserId(mapping.getUser().getId());
            dto.setUsername(mapping.getUser().getUsername());
        }
        if (mapping.getRole() != null) {
            dto.setRoleId(mapping.getRole().getId());
            dto.setRoleName(mapping.getRole().getRoleName());
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}