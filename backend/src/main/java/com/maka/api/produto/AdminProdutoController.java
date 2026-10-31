package com.maka.api.produto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import com.maka.api.produto.dto.AdminProdutoResponse;
import com.maka.api.produto.dto.ProdutoRequest;
import com.maka.api.produto.dto.ResumoCardapioResponse;
import com.maka.api.produto.dto.StatusRequest;

// Gestão do cardápio (MANAGER, pela regra /api/admin/** do SecurityConfig)
@RestController
@Validated
@RequestMapping("/api/admin/pratos")
public class AdminProdutoController {

    private final ProdutoService produtoService;

    public AdminProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    // status: active, paused ou inactive; busca: parte do nome
    @GetMapping
    public Page<AdminProdutoResponse> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return produtoService.listar(categoria, status, busca, PageRequest.of(page, size, Sort.by("categoria.id", "id")));
    }

    @GetMapping("/resumo")
    public ResumoCardapioResponse resumo() {
        return produtoService.resumo();
    }

    @GetMapping("/{id}")
    public AdminProdutoResponse buscar(@PathVariable Long id) {
        return produtoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<AdminProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        AdminProdutoResponse criado = produtoService.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.getId())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    public AdminProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest request) {
        return produtoService.atualizar(id, request);
    }

    // RN01: só aceita active se o prato tiver ficha técnica
    @PatchMapping("/{id}/status")
    public AdminProdutoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return produtoService.alterarStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        produtoService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
