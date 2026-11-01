package com.maka.api.fichatecnica;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

// Converte a quantidade do item da ficha para a unidade em que o ingrediente tem custo.
// Ex.: ficha em g e ingrediente com custo por kg -> divide por 1000.
public final class Unidades {

    private record Unidade(String grupo, BigDecimal fatorBase) {
    }

    private static final Map<String, Unidade> UNIDADES = Map.of(
            "g", new Unidade("massa", BigDecimal.ONE),
            "kg", new Unidade("massa", new BigDecimal("1000")),
            "ml", new Unidade("volume", BigDecimal.ONE),
            "l", new Unidade("volume", new BigDecimal("1000")),
            "un", new Unidade("unidade", BigDecimal.ONE));

    private Unidades() {
    }

    // Como a unidade fica gravada: minúscula, menos o litro (L)
    public static String normalizar(String unidade) {
        String u = chave(unidade);
        if (!UNIDADES.containsKey(u)) {
            throw new IllegalArgumentException("Unidade inválida: " + unidade + " (use g, kg, ml, L ou un)");
        }
        return "l".equals(u) ? "L" : u;
    }

    public static BigDecimal converter(BigDecimal quantidade, String de, String para) {
        Unidade origem = UNIDADES.get(chave(de));
        Unidade destino = UNIDADES.get(chave(para));
        if (origem == null || destino == null || !origem.grupo().equals(destino.grupo())) {
            throw new IllegalArgumentException("A unidade " + de + " não combina com a unidade do ingrediente (" + para + ")");
        }
        return quantidade.multiply(origem.fatorBase()).divide(destino.fatorBase(), MathContext.DECIMAL64);
    }

    private static String chave(String unidade) {
        return unidade == null ? "" : unidade.trim().toLowerCase();
    }
}
