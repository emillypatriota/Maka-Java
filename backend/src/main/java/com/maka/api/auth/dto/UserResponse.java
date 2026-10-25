package com.maka.api.auth.dto;

public class UserResponse {
    private String email;
    private String role;

    public UserResponse(String email, String role) {
        this.email = email;
        this.role = role.replace("ROLE_", "").toLowerCase();
    }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
