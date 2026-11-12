package com.maka.api.compra.dto;

import java.math.BigDecimal;
import java.util.List;

// Cotação comparativa de um ingrediente (RF-023): ofertas do mais barato para o mais caro.
public record CotacaoResponse(
        Long ingredienteId,
        String ingredienteNome,
        String unidadeMedida,
        BigDecimal custoAtual,
        Long melhorFornecedorId,
        List<OfertaResponse> ofertas) {
}
