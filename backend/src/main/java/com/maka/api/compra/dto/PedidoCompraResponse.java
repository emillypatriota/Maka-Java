package com.maka.api.compra.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.maka.api.model.PedidoCompra;
import com.maka.api.model.StatusPedidoCompra;

public record PedidoCompraResponse(
        Long id,
        Long fornecedorId,
        String fornecedorNome,
        String fornecedorCnpj,
        StatusPedidoCompra status,
        BigDecimal valorTotal,
        String criadoPor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PedidoCompraItemResponse> itens) {

    public static PedidoCompraResponse from(PedidoCompra pedido) {
        return new PedidoCompraResponse(
                pedido.getId(),
                pedido.getFornecedor().getId(),
                pedido.getFornecedor().getRazaoSocial(),
                pedido.getFornecedor().getCnpj(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                pedido.getUsuario() == null ? null : pedido.getUsuario().getNome(),
                pedido.getCreatedAt(),
                pedido.getUpdatedAt(),
                pedido.getItens().stream().map(PedidoCompraItemResponse::from).toList());
    }
}
