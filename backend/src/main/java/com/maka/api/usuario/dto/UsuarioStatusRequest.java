package com.maka.api.usuario.dto;

import jakarta.validation.constraints.NotNull;

import com.maka.api.model.StatusUsuario;

public record UsuarioStatusRequest(@NotNull StatusUsuario status) {
}
