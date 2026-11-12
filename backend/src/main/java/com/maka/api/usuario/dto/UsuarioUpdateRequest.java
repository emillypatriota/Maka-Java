package com.maka.api.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// PUT /api/admin/usuarios/{id}. O e-mail não muda (é o login).
// password é opcional: vazio ou ausente mantém a senha atual.
public record UsuarioUpdateRequest(
        @NotBlank(message = "Informe o nome")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres")
        String username,

        @NotBlank(message = "Informe o perfil")
        String role,

        @Size(max = 72, message = "A senha pode ter no máximo 72 caracteres")
        String password) {
}
