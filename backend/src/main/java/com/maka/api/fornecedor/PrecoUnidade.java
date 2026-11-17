package com.maka.api.fornecedor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.maka.api.exception.RegraNegocioException;
import com.maka.api.fichatecnica.Unidades;

/**
 * O fornecedor vende numa unidade (unidade_venda: kg, L, un...) e o ingrediente tem custo
 * e estoque na unidade dele (unidade_medida: g, ml, un...). Esta classe faz a ponte.
 * Ex.: picanha a R$ 78,00 o kg e ingrediente em g -> R$ 0,0780 por g.
 */
public final class PrecoUnidade {

    // Mesma escala das colunas de preço/custo do banco (DECIMAL(10,4) e DECIMAL(12,4))
    public static final int ESCALA_PRECO = 4;

    private PrecoUnidade() {
    }

    /**
     * Unidade de venda como fica gravada. Aceita g, kg, ml, L, un ou exatamente a unidade
     * do ingrediente (para ingredientes cadastrados com unidade livre). Senão: 422.
     */
    public static String normalizarUnidadeVenda(String unidadeVenda, String unidadeIngrediente) {
        if (unidadeVenda == null || unidadeVenda.isBlank()) {
            throw new IllegalArgumentException("Informe a unidade de venda");
        }
        String venda = unidadeVenda.trim();
        if (unidadeIngrediente != null && venda.equalsIgnoreCase(unidadeIngrediente.trim())) {
            return unidadeIngrediente.trim();
        }
        String normalizada;
        try {
            normalizada = Unidades.normalizar(venda);
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Unidade de venda inválida: " + venda + " (use g, kg, ml, L, un ou "
                    + unidadeIngrediente + ")");
        }
        // Confere já aqui se combina com o ingrediente (kg com g sim, kg com ml não)
        quantasUnidadesDoIngrediente(normalizada, unidadeIngrediente);
        return normalizada;
    }

    /** Quantas unidades do ingrediente cabem em 1 unidade de venda (kg -> g = 1000). */
    public static BigDecimal quantasUnidadesDoIngrediente(String unidadeVenda, String unidadeIngrediente) {
        if (unidadeVenda != null && unidadeIngrediente != null
                && unidadeVenda.trim().equalsIgnoreCase(unidadeIngrediente.trim())) {
            return BigDecimal.ONE;
        }
        return Unidades.converter(BigDecimal.ONE, unidadeVenda, unidadeIngrediente);
    }

    /** Preço da unidade de venda convertido para a unidade do ingrediente. */
    public static BigDecimal precoNaUnidadeDoIngrediente(BigDecimal preco, String unidadeVenda, String unidadeIngrediente) {
        BigDecimal fator = quantasUnidadesDoIngrediente(unidadeVenda, unidadeIngrediente);
        return preco.divide(fator, ESCALA_PRECO, RoundingMode.HALF_UP);
    }
}
