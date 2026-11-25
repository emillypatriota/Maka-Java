package com.maka.api.estoque.dto;

import java.util.List;

public record SaldoEstoqueResumoResponse(List<SaldoEstoqueResponse> itens, long alertas) {
}
