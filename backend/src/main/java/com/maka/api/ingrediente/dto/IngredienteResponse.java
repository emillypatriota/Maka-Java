package com.maka.api.ingrediente.dto;

import java.math.BigDecimal;

import com.maka.api.model.Ingrediente;
import com.maka.api.model.StatusIngrediente;

public record IngredienteResponse(
        Long id,
        String nome,
        String unidadeMedida,
        BigDecimal custoUnitario,
        String sku,
        BigDecimal estoqueMinimo,
        StatusIngrediente status) {

    public static IngredienteResponse from(Ingrediente ingrediente) {
        return new IngredienteResponse(
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getUnidadeMedida(),
                ingrediente.getCustoUnitario(),
                ingrediente.getSku(),
                ingrediente.getEstoqueMinimo(),
                ingrediente.getStatus());
    }
}
