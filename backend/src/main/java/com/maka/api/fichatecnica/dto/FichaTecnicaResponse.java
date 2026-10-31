package com.maka.api.fichatecnica.dto;

import java.math.BigDecimal;
import java.util.List;

// Ficha técnica com o custo calculado no Service (RF-011, RF-012, RF-013, RN02).
// cost, foodCost e foodCostLevel ficam null quando a ficha está vazia.
public record FichaTecnicaResponse(
        String dishId,
        String dishName,
        BigDecimal price,
        String status,
        List<Item> items,
        BigDecimal cost,
        BigDecimal foodCost,
        String foodCostLevel,
        boolean foodCostWarning) {

    public record Item(
            Long ingredientId,
            String ingredientName,
            String unit,
            BigDecimal unitCost,
            BigDecimal quantity,
            BigDecimal correctionFactor,
            BigDecimal cost) {
    }
}
