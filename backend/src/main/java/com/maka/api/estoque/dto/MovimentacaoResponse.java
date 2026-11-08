package com.maka.api.estoque.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.maka.api.model.EstoqueMovimentacao;
import com.maka.api.model.MotivoMovimentacao;
import com.maka.api.model.TipoMovimentacao;

public record MovimentacaoResponse(
        Long id,
        Long ingredienteId,
        String ingredienteNome,
        String unidadeMedida,
        TipoMovimentacao tipo,
        MotivoMovimentacao motivo,
        BigDecimal quantidade,
        LocalDateTime createdAt) {

    public static MovimentacaoResponse from(EstoqueMovimentacao movimentacao) {
        return new MovimentacaoResponse(
                movimentacao.getId(),
                movimentacao.getIngrediente().getId(),
                movimentacao.getIngrediente().getNome(),
                movimentacao.getIngrediente().getUnidadeMedida(),
                movimentacao.getTipo(),
                movimentacao.getMotivo(),
                movimentacao.getQuantidade(),
                movimentacao.getCreatedAt());
    }
}
