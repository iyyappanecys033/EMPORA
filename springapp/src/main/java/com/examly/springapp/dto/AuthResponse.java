package com.examly.springapp.dto;

import java.util.ArrayList;
import java.util.List;

public class AuthResponse {
    private String username;
    private String message = "Authentication successful";
    private List<String> roles = new ArrayList<>();
    private String accessToken;

    public AuthResponse() {}

    public AuthResponse(String username) {
        this.username = username;
    }

    public AuthResponse(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public AuthResponse(String username, String message, List<String> roles) {
        this.username = username;
        this.message = message;
        this.roles = roles;
    }

    public AuthResponse(String username, String message, List<String> roles, String accessToken) {
        this.username = username;
        this.message = message;
        this.roles = roles;
        this.accessToken = accessToken;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
