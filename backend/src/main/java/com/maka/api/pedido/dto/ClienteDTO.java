package com.maka.api.pedido.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteDTO {

    @JsonProperty("name")
    private String nome;

    @JsonProperty("phone")
    private String telefone;

    @JsonProperty("address")
    private String endereco;

    @JsonProperty("email")
    private String email;
    
    @JsonProperty("uid")
    private String uid;
}