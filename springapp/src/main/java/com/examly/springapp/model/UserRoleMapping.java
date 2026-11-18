package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
@Entity
public class UserRoleMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private UserAccount user;

    @ManyToOne
    private Role role;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
