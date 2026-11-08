package com.maka.api.fornecedor.dto;

import jakarta.validation.constraints.NotNull;

import com.maka.api.model.StatusFornecedor;

public record FornecedorStatusRequest(@NotNull StatusFornecedor status) {
}
