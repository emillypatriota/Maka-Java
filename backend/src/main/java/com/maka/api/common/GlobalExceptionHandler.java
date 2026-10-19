package com.maka.api.common;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.annotation.JsonInclude;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErroResponse(
            int status,
            String erro,
            String message,
            Map<String, String> campos,
            LocalDateTime timestamp) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> argumentoInvalido(IllegalArgumentException ex) {
        return montar(HttpStatus.BAD_REQUEST, texto(ex, "Requisição inválida"), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> credenciaisInvalidas(BadCredentialsException ex) {
        return montar(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex) {
        return montar(HttpStatus.FORBIDDEN, "Você não tem permissão para esta ação", null);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(NoSuchElementException ex) {
        return montar(HttpStatus.NOT_FOUND, texto(ex, "Recurso não encontrado"), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> conflito(DataIntegrityViolationException ex) {
        return montar(HttpStatus.CONFLICT, "Operação conflita com dados já existentes", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> generico(Exception ex) {
        // Erros padrão do Spring (rota inexistente, método não permitido etc.)
        // já trazem o status correto: devolvemos eles como são.
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return montar(status, texto(er.getBody().getDetail(), status.getReasonPhrase()), null);
        }
        log.error("Erro inesperado", ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", null);
    }

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String message, Map<String, String> campos) {
        ErroResponse corpo = new ErroResponse(
                status.value(), status.getReasonPhrase(), message, campos, LocalDateTime.now());
        return ResponseEntity.status(status).body(corpo);
    }

    private String texto(Exception ex, String padrao) {
        return texto(ex.getMessage(), padrao);
    }

    private String texto(String valor, String padrao) {
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }
}