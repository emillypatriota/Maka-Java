package com.maka.api.fornecedor;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.maka.api.fornecedor.dto.FornecedorRequest;
import com.maka.api.fornecedor.dto.FornecedorResponse;
import com.maka.api.fornecedor.dto.FornecedorStatusRequest;
import com.maka.api.model.StatusFornecedor;

/** Fornecedores: só ADMIN e GERENTE (cozinheiro e cliente não entram — RF-043). */
@RestController
@Validated
@RequestMapping("/api/admin/fornecedores")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    @GetMapping
    public Page<FornecedorResponse> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusFornecedor status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return fornecedorService.listar(busca, status, PageRequest.of(page, size, Sort.by("razaoSocial").ascending()));
    }

    @GetMapping("/{id}")
    public FornecedorResponse buscar(@PathVariable Long id) {
        return fornecedorService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<FornecedorResponse> criar(@Valid @RequestBody FornecedorRequest request) {
        FornecedorResponse criado = fornecedorService.criar(request);
        return ResponseEntity.created(URI.create("/api/admin/fornecedores/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public FornecedorResponse atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorRequest request) {
        return fornecedorService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public FornecedorResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody FornecedorStatusRequest request) {
        return fornecedorService.alterarStatus(id, request.status());
    }

    /** Exclusão lógica (RN06): marca INATIVO. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        fornecedorService.alterarStatus(id, StatusFornecedor.INATIVO);
        return ResponseEntity.noContent().build();
    }
}
