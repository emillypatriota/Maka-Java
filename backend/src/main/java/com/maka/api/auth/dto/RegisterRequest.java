package com.maka.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corpo de POST /api/auth/register (JSON em inglês, igual ao front)
public class RegisterRequest {

    @NotBlank(message = "Informe o nome")
    @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres")
    private String name;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "E-mail inválido")
    @Size(max = 120, message = "O e-mail pode ter no máximo 120 caracteres")
    private String email;

    @NotBlank(message = "Informe a senha")
    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
    private String password;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
