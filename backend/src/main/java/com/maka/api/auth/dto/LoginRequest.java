package com.maka.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corpo de POST /api/auth/login. Campo vazio devolve 400 antes de chegar no banco.
public class LoginRequest {

    @NotBlank(message = "Informe o e-mail")
    @Size(max = 120, message = "O e-mail pode ter no máximo 120 caracteres")
    private String email;

    @NotBlank(message = "Informe a senha")
    @Size(max = 72, message = "A senha pode ter no máximo 72 caracteres")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
