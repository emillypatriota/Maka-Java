package com.maka.api.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** role no mesmo formato do front: manager, kitchen ou delivery. */
public record UsuarioCreateRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String password,
        @NotBlank String role) {
}
