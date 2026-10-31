package com.maka.api.fichatecnica.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

// Substitui a ficha inteira do prato. Lista vazia apaga a ficha (e pausa o prato, RN01)
public record FichaTecnicaRequest(@NotNull List<@Valid Item> items) {

    public record Item(
            @NotNull Long ingredientId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @NotNull BigDecimal correctionFactor) {
    }
}
