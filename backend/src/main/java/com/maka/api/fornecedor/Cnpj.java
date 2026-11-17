package com.maka.api.fornecedor;

/**
 * RN07: CNPJ válido (14 dígitos com os dois dígitos verificadores corretos).
 * Aceita com ou sem pontuação e grava sempre formatado: 11.222.333/0001-81 (igual ao seed da V4).
 */
public final class Cnpj {

    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {
    }

    /** Só os números do CNPJ ("11.222.333/0001-81" -> "11222333000181"). */
    public static String digitos(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("\\D", "");
    }

    public static boolean valido(String cnpj) {
        String d = digitos(cnpj);
        if (d.length() != 14) {
            return false;
        }
        // 00.000.000/0000-00, 11.111.111/1111-11... passam na conta mas não existem
        if (d.chars().distinct().count() == 1) {
            return false;
        }
        int dv1 = digitoVerificador(d, PESOS_1);
        int dv2 = digitoVerificador(d, PESOS_2);
        return dv1 == d.charAt(12) - '0' && dv2 == d.charAt(13) - '0';
    }

    /** Valida e devolve formatado. CNPJ inválido lança IllegalArgumentException (400). */
    public static String normalizar(String cnpj) {
        if (!valido(cnpj)) {
            throw new IllegalArgumentException("CNPJ inválido: confira os 14 números (RN07)");
        }
        String d = digitos(cnpj);
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8)
                + "/" + d.substring(8, 12) + "-" + d.substring(12, 14);
    }

    private static int digitoVerificador(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
