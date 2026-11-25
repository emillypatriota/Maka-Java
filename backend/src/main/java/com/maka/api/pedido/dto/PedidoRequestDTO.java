package com.maka.api.pedido.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maka.api.model.FormaPagamento;
import com.maka.api.model.TipoPedido;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PedidoRequestDTO {

    @JsonProperty("items")
    private List<ItemPedidoRequestDTO> itens;

    @JsonProperty("customer")
    private ClienteDTO cliente;

    @JsonProperty("paymentMethod")
    private FormaPagamento formaPagamento;

    @JsonProperty("orderType")
    private TipoPedido tipo;

    @JsonProperty("deliveryFee")
    private BigDecimal taxaEntrega;

    @JsonProperty("total")
    private BigDecimal total; // Vamos receber, mas o cálculo real será feito no Servidor

    @JsonProperty("observations")
    private String observacoes;

    @JsonProperty("tableNumber")
    private Integer numeroMesa;
}