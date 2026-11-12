package com.maka.api.compra.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

// PATCH /api/admin/compras/{id}/receber. Corpo opcional: lote e validade valem para todas as entradas.
public record RecebimentoRequest(
        @Size(max = 40, message = "O lote pode ter no máximo 40 caracteres")
        String lote,

        LocalDate validade) {
}
