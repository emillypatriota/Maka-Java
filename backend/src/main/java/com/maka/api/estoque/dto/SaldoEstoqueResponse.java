package com.maka.api.estoque.dto;

import java.math.BigDecimal;

public record SaldoEstoqueResponse(
        Long ingredienteId,
        String sku,
        String nome,
        String unidadeMedida,
        BigDecimal estoqueMinimo,
        BigDecimal saldo,
        boolean abaixoDoMinimo) {

    public static SaldoEstoqueResponse from(SaldoEstoqueProjection saldo) {
        return new SaldoEstoqueResponse(
                saldo.getId(),
                saldo.getSku(),
                saldo.getNome(),
                saldo.getUnidadeMedida(),
                saldo.getEstoqueMinimo(),
                saldo.getSaldo(),
                saldo.getSaldo().compareTo(saldo.getEstoqueMinimo()) <= 0);
    }
}
