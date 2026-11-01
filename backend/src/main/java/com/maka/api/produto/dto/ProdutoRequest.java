package com.maka.api.produto.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Mesmos nomes do MenuItem do front (frontend/src/app/models/index.ts)
public record ProdutoRequest(
        @NotBlank @Size(max = 120) String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank @Size(max = 60) String category,
        @Size(max = 500) String image,
        Boolean isSpecial,
        Boolean active,
        @Min(0) Integer prepTime,
        @Min(0) Integer calories,
        List<@NotBlank @Size(max = 40) String> tags) {
}
