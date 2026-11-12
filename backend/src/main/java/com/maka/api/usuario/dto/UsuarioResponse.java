package com.maka.api.usuario.dto;

import java.time.LocalDateTime;

import com.maka.api.auth.PerfilRoles;
import com.maka.api.model.Usuario;

// Nunca devolve senha_hash. id como string e role em minúsculo, iguais ao login.
public record UsuarioResponse(
        String id,
        String username,
        String email,
        String role,
        String status,
        LocalDateTime createdAt) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                String.valueOf(usuario.getId()),
                usuario.getNome(),
                usuario.getEmail(),
                PerfilRoles.frontRole(usuario.getPerfil()),
                usuario.getStatus() == null ? "ATIVO" : usuario.getStatus().name(),
                usuario.getCriadoEm());
    }
}
