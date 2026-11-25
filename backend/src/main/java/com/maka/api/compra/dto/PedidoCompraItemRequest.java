package com.maka.api.compra.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

// Item do pedido de compra. A quantidade é na unidade do ingrediente (g, ml, un...), a mesma do estoque.
// precoUnitario é opcional: sem ele, vale o preço do catálogo do fornecedor convertido para essa unidade.
public record PedidoCompraItemRequest(
        @NotNull(message = "Informe o ingrediente")
        Long ingredienteId,

        @NotNull(message = "Informe a quantidade")
        @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero")
        @Digits(integer = 9, fraction = 3, message = "Quantidade com no máximo 9 dígitos inteiros e 3 decimais")
        BigDecimal quantidade,

        @DecimalMin(value = "0.0001", message = "O preço deve ser maior que zero")
        @Digits(integer = 6, fraction = 4, message = "Preço com no máximo 6 dígitos inteiros e 4 decimais")
        BigDecimal precoUnitario) {
}
