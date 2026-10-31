package com.maka.api.fichatecnica;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

// Regras de custo da ficha técnica (RF-012, RF-013, RN02, RN08).
// Não acessa o banco: recebe os itens já montados, para poder ser usado
// pela ficha técnica e pelo recálculo quando o custo de um ingrediente muda.
@Service
public class CustoPratoService {

    // RN08: fator de correção nunca menor que 1,0
    static final BigDecimal FATOR_MINIMO = BigDecimal.ONE;

    // RN02: acima disso o gerente recebe aviso. Faixas de cor do RF-013:
    // verde até LIMITE_VERDE, amarelo até LIMITE_ALERTA, vermelho acima
    static final BigDecimal LIMITE_VERDE = new BigDecimal("30.00");
    static final BigDecimal LIMITE_ALERTA = new BigDecimal("35.00");

    private static final BigDecimal CEM = new BigDecimal("100");

    public enum Faixa { VERDE, AMARELO, VERMELHO }

    // quantidade e custoUnitario na mesma unidade de medida do ingrediente
    public record ItemFicha(BigDecimal quantidade, BigDecimal custoUnitario, BigDecimal fatorCorrecao) {
    }

    public record CustoPrato(BigDecimal custo, BigDecimal foodCost, Faixa faixa, boolean alerta) {
    }

    public void validarFator(BigDecimal fatorCorrecao) {
        if (fatorCorrecao == null || fatorCorrecao.compareTo(FATOR_MINIMO) < 0) {
            throw new IllegalArgumentException("O fator de correção deve ser maior ou igual a 1,0");
        }
    }

    // custo do item = quantidade × fator de correção × custo unitário
    public BigDecimal custoItem(ItemFicha item) {
        if (item.quantidade() == null || item.quantidade().signum() <= 0) {
            throw new IllegalArgumentException("A quantidade do ingrediente deve ser maior que zero");
        }
        validarFator(item.fatorCorrecao());
        return item.quantidade()
                .multiply(item.fatorCorrecao())
                .multiply(item.custoUnitario());
    }

    public CustoPrato calcular(List<ItemFicha> itens, BigDecimal precoVenda) {
        if (precoVenda == null || precoVenda.signum() <= 0) {
            throw new IllegalArgumentException("O preço de venda deve ser maior que zero");
        }
        BigDecimal custo = itens.stream()
                .map(this::custoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal foodCost = custo.multiply(CEM).divide(precoVenda, 2, RoundingMode.HALF_UP);
        return new CustoPrato(custo, foodCost, faixa(foodCost), foodCost.compareTo(LIMITE_ALERTA) > 0);
    }

    private static Faixa faixa(BigDecimal foodCost) {
        if (foodCost.compareTo(LIMITE_VERDE) <= 0) {
            return Faixa.VERDE;
        }
        return foodCost.compareTo(LIMITE_ALERTA) <= 0 ? Faixa.AMARELO : Faixa.VERMELHO;
    }
}
