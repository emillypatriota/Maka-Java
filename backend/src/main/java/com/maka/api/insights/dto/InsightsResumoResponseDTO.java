package com.maka.api.insights.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class InsightsResumoResponseDTO {

    private BigDecimal faturamentoTotal;
    private BigDecimal ticketMedio;
    private long totalPedidos;
    private List<FaturamentoDiarioDTO> faturamentoPorDia;
    private List<PratoMaisVendidoDTO> pratosMaisVendidos;

    @Getter
    @Setter
    public static class FaturamentoDiarioDTO {
        private String data;
        private BigDecimal faturamento;
    }

    @Getter
    @Setter
    public static class PratoMaisVendidoDTO {
        private String nomeProduto;
        private long quantidadeTotal;
    }
}
