package com.maka.api.produto;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maka.api.produto.dto.ProdutoResponse;

// Cardápio público: só pratos ativos. A escrita fica em /api/admin/pratos
@RestController
@RequestMapping("/api/cardapio")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public List<ProdutoResponse> listar(@RequestParam(required = false) String categoria) {
        return produtoService.listarAtivos(categoria);
    }

    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return produtoService.buscarAtivo(id);
    }
}
