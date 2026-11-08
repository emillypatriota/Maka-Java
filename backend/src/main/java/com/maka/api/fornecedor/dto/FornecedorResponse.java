package com.maka.api.fornecedor.dto;

import java.time.LocalDateTime;

import com.maka.api.model.Fornecedor;

public record FornecedorResponse(
        Long id,
        String razaoSocial,
        String cnpj,
        String telefone,
        String email,
        String categorias,
        String status,
        LocalDateTime createdAt) {

    public static FornecedorResponse from(Fornecedor f) {
        return new FornecedorResponse(f.getId(), f.getRazaoSocial(), f.getCnpj(), f.getTelefone(),
                f.getEmail(), f.getCategorias(), f.getStatus().name(), f.getCreatedAt());
    }
}
