package com.maka.api.fornecedor.dto;

import java.time.LocalDateTime;

import com.maka.api.model.Fornecedor;
import com.maka.api.model.StatusFornecedor;

public record FornecedorResponse(
        Long id,
        String razaoSocial,
        String cnpj,
        String telefone,
        String email,
        String categorias,
        StatusFornecedor status,
        LocalDateTime createdAt) {

    public static FornecedorResponse from(Fornecedor fornecedor) {
        return new FornecedorResponse(
                fornecedor.getId(),
                fornecedor.getRazaoSocial(),
                fornecedor.getCnpj(),
                fornecedor.getTelefone(),
                fornecedor.getEmail(),
                fornecedor.getCategorias(),
                fornecedor.getStatus(),
                fornecedor.getCreatedAt());
    }
}
