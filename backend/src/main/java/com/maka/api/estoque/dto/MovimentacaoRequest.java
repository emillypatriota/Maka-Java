package com.maka.api.estoque.dto;

import java.math.BigDecimal;

import com.maka.api.model.MotivoMovimentacao;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record MovimentacaoRequest(
        @NotNull Long ingredienteId,
        @NotNull @DecimalMin("0.001") @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
        MotivoMovimentacao motivo) {
}
