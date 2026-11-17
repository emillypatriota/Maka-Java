package com.maka.api.fornecedor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

// RN07: CNPJ com dígitos verificadores válidos
class CnpjTest {

    @Test
    void aceitaCnpjValidoComOuSemPontuacao() {
        assertTrue(Cnpj.valido("11.222.333/0001-81"));
        assertTrue(Cnpj.valido("11222333000181"));
        assertTrue(Cnpj.valido("44.555.666/0001-81"));   // seed da V4
    }

    @Test
    void recusaDigitoVerificadorErradoTamanhoErradoERepetidos() {
        assertFalse(Cnpj.valido("11.222.333/0001-82"));
        assertFalse(Cnpj.valido("1122233300018"));
        assertFalse(Cnpj.valido("00000000000000"));
        assertFalse(Cnpj.valido("11111111111111"));
        assertFalse(Cnpj.valido(null));
        assertFalse(Cnpj.valido(""));
    }

    @Test
    void normalizarDevolveFormatado() {
        assertEquals("11.222.333/0001-81", Cnpj.normalizar("11222333000181"));
        assertEquals("11.222.333/0001-81", Cnpj.normalizar(" 11.222.333/0001-81 "));
    }

    @Test
    void normalizarCnpjInvalidoDa400() {
        assertThrows(IllegalArgumentException.class, () -> Cnpj.normalizar("12.345.678/0001-00"));
    }
}
