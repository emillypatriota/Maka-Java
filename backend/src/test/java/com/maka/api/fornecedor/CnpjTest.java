package com.maka.api.fornecedor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CnpjTest {

    @Test
    void aceitaCnpjValidoComOuSemMascara() {
        assertTrue(Cnpj.valido("11.222.333/0001-81"));
        assertTrue(Cnpj.valido("11222333000181"));
        assertTrue(Cnpj.valido("44.555.666/0001-81"));
    }

    @Test
    void rejeitaDigitoErradoSequenciaRepetidaETamanhoErrado() {
        assertFalse(Cnpj.valido("11.222.333/0001-82"));
        assertFalse(Cnpj.valido("00.000.000/0000-00"));
        assertFalse(Cnpj.valido("123"));
        assertFalse(Cnpj.valido(null));
    }

    @Test
    void formataNoPadraoDaColuna() {
        assertEquals("11.222.333/0001-81", Cnpj.formatar("11222333000181"));
    }
}
