package com.maka.api.pedido.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemPedidoRequestDTO {

    @JsonProperty("id")
    private String produtoId; // Vem como string do front, faremos o parse para Long no Service

    @JsonProperty("quantity")
    private Integer quantidade;

    @JsonProperty("observations")
    private String observacoes;
}