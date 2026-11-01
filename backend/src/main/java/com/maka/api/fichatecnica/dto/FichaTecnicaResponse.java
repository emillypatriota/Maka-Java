package com.maka.api.fichatecnica.dto;

import java.math.BigDecimal;
import java.util.List;

// Ficha técnica com o custo calculado no Service (RF-011, RF-012, RF-013, RN02).
// batchCost = custo da receita inteira; cost e foodCost = da porção (batchCost / servings).
// Os campos de custo ficam null quando a ficha não tem ingredientes.
public record FichaTecnicaResponse(
        String dishId,
        String dishName,
        BigDecimal price,
        String status,
        int servings,
        String preparationMethod,
        List<Item> items,
        BigDecimal batchCost,
        BigDecimal cost,
        BigDecimal foodCost,
        String foodCostLevel,
        boolean foodCostWarning) {

    // unit = unidade usada na ficha; ingredientUnit e unitCost = como o ingrediente é cadastrado
    public record Item(
            Long ingredientId,
            String ingredientName,
            BigDecimal quantity,
            String unit,
            BigDecimal correctionFactor,
            String ingredientUnit,
            BigDecimal unitCost,
            BigDecimal cost) {
    }
}
