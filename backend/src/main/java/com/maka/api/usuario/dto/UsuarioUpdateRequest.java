package com.maka.api.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** password é opcional: só troca a senha se vier preenchida. E-mail não muda. */
public record UsuarioUpdateRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank String role,
        @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres") String password) {
}
