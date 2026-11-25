package com.maka.api.pedido.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maka.api.model.FormaPagamento;
import com.maka.api.model.StatusPedido;
import com.maka.api.model.TipoPedido;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PedidoResponseDTO {

    @JsonProperty("id")
    private String id; // Exigência do front: ID como string

    @JsonProperty("orderNumber")
    private String orderNumber;

    @JsonProperty("items")
    private List<ItemPedidoResponseDTO> itens;

    @JsonProperty("customer")
    private ClienteDTO cliente;

    @JsonProperty("paymentMethod")
    private FormaPagamento formaPagamento;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("deliveryFee")
    private BigDecimal taxaEntrega;

    @JsonProperty("status")
    private StatusPedido status;

    @JsonProperty("orderType")
    private TipoPedido tipo;

    @JsonProperty("createdAt")
    private LocalDateTime criadoEm;

    @JsonProperty("updatedAt")
    private LocalDateTime atualizadoEm;

    @JsonProperty("deliveryPersonId")
    private String entregadorId;

    @JsonProperty("observations")
    private String observacoes;

    @JsonProperty("estimatedTime")
    private Integer tempoEstimado;

    @JsonProperty("tableNumber")
    private Integer numeroMesa;

    @JsonProperty("cancelReason")
    private String motivoCancelamento;
}