package com.maka.api.usuario.dto;

import java.time.LocalDateTime;

import com.maka.api.model.Usuario;

/** Nunca devolve senha_hash. id como string, igual ao resto da API. */
public record UsuarioResponse(
        String id,
        String username,
        String email,
        String role,
        String status,
        LocalDateTime createdAt) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                String.valueOf(u.getId()),
                u.getNome(),
                u.getEmail(),
                u.getPerfil().name().toLowerCase(),
                u.getStatus().name(),
                u.getCriadoEm());
    }
}
