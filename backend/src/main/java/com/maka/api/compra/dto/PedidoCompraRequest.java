package com.maka.api.compra.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// POST /api/admin/compras (cria RASCUNHO) e PUT /api/admin/compras/{id} (só enquanto RASCUNHO).
// Ex.: { "fornecedorId": 1, "itens": [ { "ingredienteId": 1, "quantidade": 5000 } ] }
public record PedidoCompraRequest(
        @NotNull(message = "Informe o fornecedor")
        Long fornecedorId,

        @NotEmpty(message = "Adicione pelo menos um item")
        @Size(max = 100, message = "No máximo 100 itens por pedido")
        List<@Valid @NotNull PedidoCompraItemRequest> itens) {
}
