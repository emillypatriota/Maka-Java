package com.maka.api.compra.dto;

import java.math.BigDecimal;

// Uma linha da cotação: quanto um fornecedor cobra pelo ingrediente.
// diferencaPercentual: quanto essa oferta é mais cara que a melhor (0 na melhor).
public record OfertaResponse(
        Long catalogoItemId,
        Long fornecedorId,
        String fornecedorNome,
        BigDecimal preco,
        String unidadeVenda,
        BigDecimal precoPorUnidadeIngrediente,
        boolean melhorPreco,
        BigDecimal diferencaPercentual) {
}
