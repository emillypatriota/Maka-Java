package com.maka.api.pedido.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ItemPedidoResponseDTO {

    @JsonProperty("id")
    private String produtoId;

    @JsonProperty("name")
    private String nomeProduto;

    @JsonProperty("image")
    private String imagemProduto;

    @JsonProperty("quantity")
    private Integer quantidade;

    @JsonProperty("price")
    private BigDecimal precoUnitario; // Preço no momento da compra

    @JsonProperty("observations")
    private String observacoes;
}