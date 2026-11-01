package com.maka.api.fichatecnica.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Substitui a ficha inteira do prato. Lista vazia tira os ingredientes (e pausa o prato, RN01).
// servings = rendimento (porções), preparationMethod = modo de preparo (RF-014)
public record FichaTecnicaRequest(
        @Min(1) Integer servings,
        String preparationMethod,
        @NotNull List<@Valid Item> items) {

    public record Item(
            @NotNull Long ingredientId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 9, fraction = 3) BigDecimal quantity,
            @NotBlank String unit,
            @NotNull @Digits(integer = 3, fraction = 2) BigDecimal correctionFactor) {
    }
}
