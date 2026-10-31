package com.maka.api.fichatecnica;

import com.maka.api.fichatecnica.CustoPratoService.CustoPrato;
import com.maka.api.fichatecnica.CustoPratoService.Faixa;
import com.maka.api.fichatecnica.CustoPratoService.ItemFicha;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustoPratoServiceTest {

    private final CustoPratoService service = new CustoPratoService();

    @Test
    void deveSomarOCustoDosItensAplicandoOFatorDeCorrecao() {
        // 0,2 kg × 1,25 × R$ 80,00 = R$ 20,00 ; 0,1 kg × 1,0 × R$ 10,00 = R$ 1,00
        List<ItemFicha> itens = List.of(
                item("0.2", "80.00", "1.25"),
                item("0.1", "10.00", "1.0"));

        CustoPrato resultado = service.calcular(itens, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("21.00"), resultado.custo());
        assertEquals(new BigDecimal("21.00"), resultado.foodCost());
        assertEquals(Faixa.VERDE, resultado.faixa());
        assertFalse(resultado.alerta());
    }

    @Test
    void deveClassificarAsFaixasDeFoodCost() {
        assertEquals(Faixa.VERDE, faixaPara("30.00"));
        assertEquals(Faixa.AMARELO, faixaPara("30.01"));
        assertEquals(Faixa.AMARELO, faixaPara("35.00"));
        assertEquals(Faixa.VERMELHO, faixaPara("35.01"));
    }

    @Test
    void deveAlertarQuandoFoodCostPassaDe35PorCento() {
        CustoPrato noLimite = service.calcular(List.of(item("1", "35.00", "1")), new BigDecimal("100"));
        CustoPrato acima = service.calcular(List.of(item("1", "36.00", "1")), new BigDecimal("100"));

        assertFalse(noLimite.alerta());
        assertTrue(acima.alerta());
    }

    @Test
    void deveRecusarFatorDeCorrecaoMenorQueUm() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.calcular(List.of(item("1", "10.00", "0.9")), new BigDecimal("50")));

        assertEquals("O fator de correção deve ser maior ou igual a 1,0", erro.getMessage());
    }

    @Test
    void deveRecusarQuantidadeZeroOuPrecoZero() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calcular(List.of(item("0", "10.00", "1")), new BigDecimal("50")));
        assertThrows(IllegalArgumentException.class,
                () -> service.calcular(List.of(item("1", "10.00", "1")), BigDecimal.ZERO));
    }

    @Test
    void fichaVaziaTemCustoZero() {
        CustoPrato resultado = service.calcular(List.of(), new BigDecimal("40.00"));

        assertEquals(new BigDecimal("0.00"), resultado.custo());
        assertEquals(Faixa.VERDE, resultado.faixa());
    }

    private Faixa faixaPara(String custo) {
        return service.calcular(List.of(item("1", custo, "1")), new BigDecimal("100")).faixa();
    }

    private static ItemFicha item(String quantidade, String custoUnitario, String fator) {
        return new ItemFicha(new BigDecimal(quantidade), new BigDecimal(custoUnitario), new BigDecimal(fator));
    }
}
