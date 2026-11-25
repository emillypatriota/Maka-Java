package com.maka.api.produto.dto;

import com.maka.api.model.StatusPrato;

// O banco usa os nomes do SRS (ATIVO, PAUSADO, INATIVO); o JSON usa os do front
public final class StatusPratoJson {

    public static final String PADRAO = "active|paused|inactive";

    private StatusPratoJson() {
    }

    public static String toJson(StatusPrato status) {
        return switch (status) {
            case ATIVO -> "active";
            case PAUSADO -> "paused";
            case INATIVO -> "inactive";
        };
    }

    public static StatusPrato fromJson(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status) {
            case "active" -> StatusPrato.ATIVO;
            case "paused" -> StatusPrato.PAUSADO;
            case "inactive" -> StatusPrato.INATIVO;
            default -> throw new IllegalArgumentException("Status inválido: " + status + " (use active, paused ou inactive)");
        };
    }
}
