package com.maka.api.usuario.dto;

import com.maka.api.model.StatusUsuario;

import jakarta.validation.constraints.NotNull;

// PATCH /api/admin/usuarios/{id}/status  ->  { "status": "ATIVO" } ou { "status": "INATIVO" }
public record UsuarioStatusRequest(
        @NotNull(message = "Informe o status (ATIVO ou INATIVO)")
        StatusUsuario status) {
}
