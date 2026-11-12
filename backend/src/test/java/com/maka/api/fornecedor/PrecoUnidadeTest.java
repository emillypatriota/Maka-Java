package com.maka.api.fornecedor;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.maka.api.exception.RegraNegocioException;

class PrecoUnidadeTest {

    @Test
    void converteKgParaG() {
        BigDecimal porGrama = PrecoUnidade.precoNaUnidadeDoIngrediente(new BigDecimal("78.00"), "kg", "g");
        assertEquals(0, porGrama.compareTo(new BigDecimal("0.0780")));
    }

    @Test
    void converteLitroParaMl() {
        BigDecimal porMl = PrecoUnidade.precoNaUnidadeDoIngrediente(new BigDecimal("45.00"), "L", "ml");
        assertEquals(0, porMl.compareTo(new BigDecimal("0.0450")));
    }

    @Test
    void mesmaUnidadeNaoMudaOPreco() {
        BigDecimal preco = PrecoUnidade.precoNaUnidadeDoIngrediente(new BigDecimal("0.95"), "un", "un");
        assertEquals(0, preco.compareTo(new BigDecimal("0.95")));
    }

    @Test
    void unidadesQueNaoCombinamDao422() {
        assertThrows(RegraNegocioException.class,
                () -> PrecoUnidade.precoNaUnidadeDoIngrediente(BigDecimal.TEN, "kg", "ml"));
    }

    @Test
    void normalizaUnidadeDeVenda() {
        assertEquals("kg", PrecoUnidade.normalizarUnidadeVenda("KG", "g"));
        assertEquals("L", PrecoUnidade.normalizarUnidadeVenda("l", "ml"));
        // ingrediente com unidade livre: aceita exatamente a mesma unidade
        assertEquals("pacote", PrecoUnidade.normalizarUnidadeVenda("Pacote", "pacote"));
    }

    @Test
    void unidadeDeVendaDesconhecidaOuIncompativelDa422() {
        assertThrows(RegraNegocioException.class, () -> PrecoUnidade.normalizarUnidadeVenda("caixa", "g"));
        assertThrows(RegraNegocioException.class, () -> PrecoUnidade.normalizarUnidadeVenda("ml", "g"));
    }
}
