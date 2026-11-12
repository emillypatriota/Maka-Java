package com.maka.api.fornecedor.dto;

import java.math.BigDecimal;

import com.maka.api.fornecedor.PrecoUnidade;
import com.maka.api.model.FornecedorProduto;
import com.maka.api.model.Ingrediente;

// precoPorUnidadeIngrediente: o preço convertido para a unidade do ingrediente (ex.: R$/g),
// que é a base do custo da ficha técnica e da cotação.
public record CatalogoItemResponse(
        Long id,
        Long fornecedorId,
        Long ingredienteId,
        String ingredienteNome,
        String unidadeMedida,
        BigDecimal preco,
        String unidadeVenda,
        BigDecimal precoPorUnidadeIngrediente) {

    public static CatalogoItemResponse from(FornecedorProduto item) {
        Ingrediente ingrediente = item.getIngrediente();
        return new CatalogoItemResponse(
                item.getId(),
                item.getFornecedor().getId(),
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getUnidadeMedida(),
                item.getPreco(),
                item.getUnidadeVenda(),
                precoConvertido(item, ingrediente));
    }

    // Se a unidade do ingrediente mudou depois e não combina mais, devolve null em vez de quebrar a lista
    private static BigDecimal precoConvertido(FornecedorProduto item, Ingrediente ingrediente) {
        try {
            return PrecoUnidade.precoNaUnidadeDoIngrediente(
                    item.getPreco(), item.getUnidadeVenda(), ingrediente.getUnidadeMedida());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
