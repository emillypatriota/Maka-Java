package com.maka.api.exception;

/**
 * Violação de regra de negócio (HTTP 422).
 * Ex.: ficha técnica com fator de correção menor que 1, estoque insuficiente,
 * prato sem ficha técnica sendo ativado (RN01).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}