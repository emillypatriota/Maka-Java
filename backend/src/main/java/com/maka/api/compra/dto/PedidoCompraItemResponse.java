package com.maka.api.compra.dto;

import java.math.BigDecimal;

import com.maka.api.model.Ingrediente;
import com.maka.api.model.PedidoCompraItem;

public record PedidoCompraItemResponse(
        Long id,
        Long ingredienteId,
        String ingredienteNome,
        String unidadeMedida,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal) {

    public static PedidoCompraItemResponse from(PedidoCompraItem item) {
        Ingrediente ingrediente = item.getIngrediente();
        return new PedidoCompraItemResponse(
                item.getId(),
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getUnidadeMedida(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal());
    }
}
