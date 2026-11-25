package com.maka.api.categoria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.maka.api.categoria.dto.CategoriaResponse;

// Público (GET /api/cardapio/** é liberado no SecurityConfig)
@RestController
@RequestMapping("/api/cardapio/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaService.listarAtivas();
    }
}
