package com.maka.api.produto;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.produto.dto.ProdutoResponse;
import com.maka.api.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    // Devolve todos (ativos e inativos): o front filtra pelo campo active.
    // readOnly mantém a sessão aberta para carregar as tags LAZY na conversão.
    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return produtoRepository.findAll(Sort.by("categoria", "id")).stream()
                .map(ProdutoResponse::from)
                .toList();
    }
}
