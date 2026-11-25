package com.maka.api.estoque.dto;

import java.math.BigDecimal;

public interface SaldoEstoqueProjection {
    Long getId();
    String getSku();
    String getNome();
    String getUnidadeMedida();
    BigDecimal getEstoqueMinimo();
    BigDecimal getSaldo();
}
