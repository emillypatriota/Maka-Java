package com.maka.api.fornecedor;

/** Validação e formatação de CNPJ (RN07). Usa os dois dígitos verificadores. */
public final class Cnpj {

    private static final int[] PESO_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESO_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {
    }

    public static String somenteDigitos(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("\\D", "");
    }

    public static boolean valido(String cnpj) {
        String d = somenteDigitos(cnpj);
        if (d.length() != 14 || d.chars().distinct().count() == 1) {
            return false;
        }
        int dv1 = digito(d, PESO_1);
        int dv2 = digito(d.substring(0, 12) + dv1, PESO_2);
        return d.charAt(12) - '0' == dv1 && d.charAt(13) - '0' == dv2;
    }

    /** 00.000.000/0000-00 (cabe nos 18 caracteres da coluna). */
    public static String formatar(String cnpj) {
        String d = somenteDigitos(cnpj);
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8)
                + "/" + d.substring(8, 12) + "-" + d.substring(12, 14);
    }

    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
