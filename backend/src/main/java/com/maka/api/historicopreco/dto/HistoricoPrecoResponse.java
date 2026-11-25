package com.maka.api.historicopreco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoPrecoResponse(
        Long fornecedorId,
        String fornecedor,
        BigDecimal preco,
        LocalDateTime registradoEm) {
}
