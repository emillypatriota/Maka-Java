package com.maka.api.ingrediente.dto;

import java.math.BigDecimal;

import com.maka.api.model.Ingrediente;

public record IngredienteResponse(
        Long id,
        String nome,
        String unidadeMedida,
        BigDecimal custoUnitario) {

    public static IngredienteResponse from(Ingrediente ingrediente) {
        return new IngredienteResponse(
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getUnidadeMedida(),
                ingrediente.getCustoUnitario());
    }
}
