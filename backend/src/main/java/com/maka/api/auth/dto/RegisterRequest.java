package com.maka.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe o nome") @Size(max = 100) String username,
        @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 120) String email,
        @NotBlank(message = "Informe a senha") @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String password) {
}
