package com.maka.api.fornecedor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
        @NotBlank @Size(max = 150) String razaoSocial,
        @NotBlank @Size(max = 18) String cnpj,
        @Size(max = 20) String telefone,
        @Email @Size(max = 120) String email,
        @Size(max = 255) String categorias) {
}
