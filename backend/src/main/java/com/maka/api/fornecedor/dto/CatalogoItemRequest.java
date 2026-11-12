package com.maka.api.fornecedor.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Item do catálogo do fornecedor (RF-022): quanto ele cobra por unidade de venda.
// Ex.: { "ingredienteId": 1, "preco": 78.00, "unidadeVenda": "kg" }
// No PUT o ingredienteId é ignorado (para trocar o ingrediente, remova e cadastre de novo).
public record CatalogoItemRequest(
        @NotNull(message = "Informe o ingrediente")
        Long ingredienteId,

        @NotNull(message = "Informe o preço")
        @DecimalMin(value = "0.0001", message = "O preço deve ser maior que zero")
        @Digits(integer = 6, fraction = 4, message = "Preço com no máximo 6 dígitos inteiros e 4 decimais")
        BigDecimal preco,

        @NotBlank(message = "Informe a unidade de venda")
        @Size(max = 10, message = "A unidade de venda pode ter no máximo 10 caracteres")
        String unidadeVenda) {
}
