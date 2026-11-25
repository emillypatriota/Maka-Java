package com.maka.api.insights.dto;

import java.math.BigDecimal;
import java.util.List;

import com.maka.api.estoque.dto.SaldoEstoqueResponse;

public record DashboardResumoResponseDTO(
        BigDecimal faturamentoHoje,
        long pedidosHoje,
        BigDecimal ticketMedioHoje,
        BigDecimal foodCostMedio,
        long alertasEstoque,
        List<SaldoEstoqueResponse> estoqueCritico,
        List<FaturamentoDiario> vendasPorDia) {

    public record FaturamentoDiario(String data, BigDecimal faturamento) {
    }
}
