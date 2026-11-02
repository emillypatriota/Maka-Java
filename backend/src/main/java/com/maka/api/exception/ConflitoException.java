package com.maka.api.exception;

/**
 * Conflito com o estado atual do sistema (HTTP 409).
 * Ex.: e-mail já cadastrado (RN10), CNPJ duplicado, SKU repetido.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String message) {
        super(message);
    }
}