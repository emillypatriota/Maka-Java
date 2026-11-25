package com.maka.api.fichatecnica.dto;

import com.maka.api.fichatecnica.CustoPratoService.Faixa;

// Cor do food cost no JSON (RF-013)
public final class FaixaJson {

    private FaixaJson() {
    }

    public static String toJson(Faixa faixa) {
        return switch (faixa) {
            case VERDE -> "green";
            case AMARELO -> "yellow";
            case VERMELHO -> "red";
        };
    }
}
