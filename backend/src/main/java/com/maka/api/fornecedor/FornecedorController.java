package com.maka.api.fornecedor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.maka.api.fornecedor.dto.CatalogoItemRequest;
import com.maka.api.fornecedor.dto.CatalogoItemResponse;
import com.maka.api.fornecedor.dto.FornecedorRequest;
import com.maka.api.fornecedor.dto.FornecedorResponse;
import com.maka.api.fornecedor.dto.FornecedorStatusRequest;
import com.maka.api.model.StatusFornecedor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// RF-021 (fornecedores) e RF-022 (catálogo). Gerência: o cozinheiro não entra aqui (RF-043).
@RestController
@Validated
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequestMapping("/api/admin/fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    // ?busca=carnes&status=ATIVO&page=0&size=10 (busca procura na razão social e no CNPJ)
    @GetMapping
    public Page<FornecedorResponse> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusFornecedor status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return fornecedorService.listar(busca, status,
                PageRequest.of(page, size, Sort.by("razaoSocial").ascending()));
    }

    // Lista curta (sem paginação) para os selects do pedido de compra
    @GetMapping("/ativos")
    public List<FornecedorResponse> ativos() {
        return fornecedorService.listarAtivos();
    }

    @GetMapping("/{id}")
    public FornecedorResponse buscar(@PathVariable Long id) {
        return fornecedorService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<FornecedorResponse> criar(@Valid @RequestBody FornecedorRequest request) {
        FornecedorResponse criado = fornecedorService.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    public FornecedorResponse atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorRequest request) {
        return fornecedorService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public FornecedorResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody FornecedorStatusRequest request) {
        return fornecedorService.alterarStatus(id, request.status());
    }

    // Exclusão lógica (RN06): marca INATIVO e devolve 204
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        fornecedorService.alterarStatus(id, StatusFornecedor.INATIVO);
        return ResponseEntity.noContent().build();
    }

    // ── Catálogo do fornecedor (RF-022) ───────────────────────────────

    @GetMapping("/{id}/catalogo")
    public List<CatalogoItemResponse> catalogo(@PathVariable Long id) {
        return fornecedorService.catalogo(id);
    }

    @PostMapping("/{id}/catalogo")
    public ResponseEntity<CatalogoItemResponse> adicionarAoCatalogo(
            @PathVariable Long id,
            @Valid @RequestBody CatalogoItemRequest request) {
        CatalogoItemResponse criado = fornecedorService.adicionarAoCatalogo(id, request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{itemId}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}/catalogo/{itemId}")
    public CatalogoItemResponse atualizarItemCatalogo(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody CatalogoItemRequest request) {
        return fornecedorService.atualizarItemCatalogo(id, itemId, request);
    }

    @DeleteMapping("/{id}/catalogo/{itemId}")
    public ResponseEntity<Void> removerDoCatalogo(@PathVariable Long id, @PathVariable Long itemId) {
        fornecedorService.removerDoCatalogo(id, itemId);
        return ResponseEntity.noContent().build();
    }
}
