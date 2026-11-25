package com.maka.api.auth;

import com.maka.api.model.Perfil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PerfilRolesTest {

    @Test
    void authorityTemPrefixoRoleENomeDoPerfil() {
        assertEquals("ROLE_ADMIN", PerfilRoles.authority(Perfil.ADMIN));
        assertEquals("ROLE_MANAGER", PerfilRoles.authority(Perfil.MANAGER));
        assertEquals("ROLE_KITCHEN", PerfilRoles.authority(Perfil.KITCHEN));
        assertEquals("ROLE_DELIVERY", PerfilRoles.authority(Perfil.DELIVERY));
        assertEquals("ROLE_CLIENTE", PerfilRoles.authority(Perfil.CLIENTE));
    }

    @Test
    void roleDoFrontBateComModelsIndexTs() {
        assertEquals("admin", PerfilRoles.frontRole(Perfil.ADMIN));
        assertEquals("manager", PerfilRoles.frontRole(Perfil.MANAGER));
        assertEquals("kitchen", PerfilRoles.frontRole(Perfil.KITCHEN));
        assertEquals("delivery", PerfilRoles.frontRole(Perfil.DELIVERY));
        assertEquals("client", PerfilRoles.frontRole(Perfil.CLIENTE));
    }

    @Test
    void todoPerfilTemRoleNoFront() {
        for (Perfil p : Perfil.values()) {
            assertNotNull(PerfilRoles.frontRole(p), "sem role no front para " + p);
            assertTrue(PerfilRoles.authority(p).startsWith("ROLE_"), "authority inválida para " + p);
        }
    }

    @Test
    void claimDoTokenVoltaComoAuthority() {
        assertEquals("ROLE_MANAGER", PerfilRoles.authorityDoClaim("ROLE_MANAGER"));
        assertEquals("ROLE_KITCHEN", PerfilRoles.authorityDoClaim("ROLE_KITCHEN"));
        assertEquals("ROLE_CLIENTE", PerfilRoles.authorityDoClaim("ROLE_CLIENTE"));
        assertEquals("ROLE_ADMIN", PerfilRoles.authorityDoClaim("ADMIN"));
    }

    @Test
    void claimInvalidoDevolveNull() {
        assertNull(PerfilRoles.authorityDoClaim(null));
        assertNull(PerfilRoles.authorityDoClaim(""));
        assertNull(PerfilRoles.authorityDoClaim("ROLE_ALIEN"));
        assertNull(PerfilRoles.authorityDoClaim("ROLE_GERENTE"));
    }
}
