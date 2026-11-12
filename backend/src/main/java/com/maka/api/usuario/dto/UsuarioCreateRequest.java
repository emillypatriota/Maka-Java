package com.maka.api.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /api/admin/usuarios. role no formato do front: manager, kitchen ou delivery.
public record UsuarioCreateRequest(
        @NotBlank(message = "Informe o nome")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres")
        String username,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        @Size(max = 120, message = "O e-mail pode ter no máximo 120 caracteres")
        String email,

        @NotBlank(message = "Informe a senha")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
        String password,

        @NotBlank(message = "Informe o perfil")
        String role) {
}
