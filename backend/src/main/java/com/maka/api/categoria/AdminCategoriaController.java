package com.maka.api.categoria;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.maka.api.categoria.dto.CategoriaRequest;
import com.maka.api.categoria.dto.CategoriaResponse;

// Gestão de categorias (MANAGER, pela regra /api/admin/** do SecurityConfig)
@RestController
@Validated
@RequestMapping("/api/admin/categorias")
public class AdminCategoriaController {

    private final CategoriaService categoriaService;

    public AdminCategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public Page<CategoriaResponse> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return categoriaService.listar(PageRequest.of(page, size, Sort.by("ordem", "id")));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse criada = categoriaService.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    public CategoriaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        categoriaService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
