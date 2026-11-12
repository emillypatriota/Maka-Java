package com.maka.api.fornecedor.dto;

import com.maka.api.model.StatusFornecedor;

import jakarta.validation.constraints.NotNull;

// PATCH /api/admin/fornecedores/{id}/status  ->  { "status": "ATIVO" } ou { "status": "INATIVO" }
public record FornecedorStatusRequest(
        @NotNull(message = "Informe o status (ATIVO ou INATIVO)")
        StatusFornecedor status) {
}
