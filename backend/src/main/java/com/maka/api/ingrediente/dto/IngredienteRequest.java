package com.maka.api.ingrediente.dto;

import java.math.BigDecimal;

import com.maka.api.model.StatusIngrediente;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngredienteRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 20) String unidadeMedida,
        @NotNull @DecimalMin("0.0") BigDecimal custoUnitario,
        @Size(max = 40) String sku,
        @DecimalMin("0.0") BigDecimal estoqueMinimo,
        StatusIngrediente status) {
}
