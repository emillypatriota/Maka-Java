package com.maka.api.auth.dto;

public class LoginResponse {
    private String token;
    private UserDto user;

    public LoginResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public static class UserDto {
        private String uid;
        private String email;
        private String role;
        private String username;

        public UserDto(String uid, String email, String role, String username) {
            this.uid = uid;
            this.email = email;
            this.role = role;
            this.username = username;
        }

        public String getUid() { return uid; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getUsername() { return username; }
    }
}
