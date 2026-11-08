package com.maka.api.auth;

import com.maka.api.model.Perfil;

/**
 * Traduz o Perfil do banco para os dois "idiomas" do sistema:
 *  - authority do Spring Security (ROLE_MANAGER, ROLE_ADMIN, ...), usada em hasRole / @PreAuthorize;
 *  - role em minúsculo que o front Angular espera (admin, manager, kitchen, delivery, client).
 *
 * Perfis (enum Perfil): ADMIN, MANAGER (gerente), KITCHEN (cozinheiro), DELIVERY (entregador), CLIENTE.
 */
public final class PerfilRoles {

    private PerfilRoles() {
    }

    /** Ex.: MANAGER -> "ROLE_MANAGER" (é o que vai dentro do JWT). */
    public static String authority(Perfil perfil) {
        return "ROLE_" + perfil.name();
    }

    /** Ex.: MANAGER -> "manager" (formato do UserRole em frontend/src/app/models/index.ts). */
    public static String frontRole(Perfil perfil) {
        switch (perfil) {
            case ADMIN:
                return "admin";
            case MANAGER:
                return "manager";
            case KITCHEN:
                return "kitchen";
            case DELIVERY:
                return "delivery";
            default:
                return "client";
        }
    }

    /**
     * Lê o claim "role" de um token e devolve a authority, ou null se o claim estiver
     * ausente ou não for um perfil conhecido (aí o JwtFilter deixa a requisição anônima).
     */
    public static String authorityDoClaim(String claim) {
        if (claim == null || claim.isBlank()) {
            return null;
        }
        String nome = claim.startsWith("ROLE_") ? claim.substring(5) : claim;
        try {
            return authority(Perfil.valueOf(nome));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
