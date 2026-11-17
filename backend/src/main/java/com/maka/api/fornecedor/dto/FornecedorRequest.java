package com.maka.api.fornecedor.dto;

import com.maka.api.model.StatusFornecedor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST e PUT /api/admin/fornecedores (RF-021). CNPJ com ou sem pontuação; o Service valida (RN07).
public record FornecedorRequest(
        @NotBlank(message = "Informe a razão social")
        @Size(max = 150, message = "A razão social pode ter no máximo 150 caracteres")
        String razaoSocial,

        @NotBlank(message = "Informe o CNPJ")
        @Size(max = 18, message = "CNPJ inválido")
        String cnpj,

        @Size(max = 20, message = "O telefone pode ter no máximo 20 caracteres")
        String telefone,

        @Email(message = "E-mail inválido")
        @Size(max = 120, message = "O e-mail pode ter no máximo 120 caracteres")
        String email,

        @Size(max = 255, message = "As categorias podem ter no máximo 255 caracteres")
        String categorias,

        // Opcional: ausente mantém o status atual (novo fornecedor nasce ATIVO)
        StatusFornecedor status) {
}
