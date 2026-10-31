package com.maka.api.funcionario.dto;

import jakarta.validation.constraints.NotNull;

import com.maka.api.model.StatusFuncionario;

public record StatusFuncionarioRequest(@NotNull StatusFuncionario status) {
}
