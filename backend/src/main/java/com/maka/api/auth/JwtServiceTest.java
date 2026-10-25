package com.maka.api.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyString", "minhaChaveSecretaSuperSuperSeguraParaOTrabalhoDeTI2026DevesTerPeloMenos256BitsDeComprimento!");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationInMs", 36000000L);
    }

    @Test
    void deveGerarETokenValido() {
        String token = jwtService.generateToken("teste@maka.com", "ROLE_MANAGER");
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("teste@maka.com", jwtService.extractEmail(token));
        assertEquals("ROLE_MANAGER", jwtService.extractRole(token));
    }

    @Test
    void deveRetornarInvalidoParaTokenModificado() {
        String token = jwtService.generateToken("teste@maka.com", "ROLE_MANAGER");
        String tokenModificado = token + "erro";
        assertFalse(jwtService.isTokenValid(tokenModificado));
    }
}
