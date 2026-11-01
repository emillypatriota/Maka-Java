package com.maka.api.fichatecnica;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnidadesTest {

    @Test
    void converteEntreUnidadesDoMesmoTipo() {
        assertEquals(0, new BigDecimal("0.3").compareTo(Unidades.converter(new BigDecimal("300"), "g", "kg")));
        assertEquals(0, new BigDecimal("1500").compareTo(Unidades.converter(new BigDecimal("1.5"), "kg", "g")));
        assertEquals(0, new BigDecimal("250").compareTo(Unidades.converter(new BigDecimal("0.25"), "L", "ml")));
        assertEquals(0, new BigDecimal("2").compareTo(Unidades.converter(new BigDecimal("2"), "un", "un")));
    }

    @Test
    void naoConverteEntreTiposDiferentes() {
        assertThrows(IllegalArgumentException.class, () -> Unidades.converter(BigDecimal.ONE, "g", "ml"));
        assertThrows(IllegalArgumentException.class, () -> Unidades.converter(BigDecimal.ONE, "un", "kg"));
    }

    @Test
    void normalizaAUnidadeGravada() {
        assertEquals("L", Unidades.normalizar(" l "));
        assertEquals("kg", Unidades.normalizar("KG"));
        assertThrows(IllegalArgumentException.class, () -> Unidades.normalizar("xícara"));
    }
}
