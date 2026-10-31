package com.maka.api.produto;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.model.Produto;
import com.maka.api.produto.dto.ProdutoRequest;
import com.maka.api.produto.dto.ProdutoResponse;
import com.maka.api.repository.ProdutoRepository;

// Os métodos que devolvem ProdutoResponse precisam de transação: as tags são LAZY
@Service
public class ProdutoService {

    private static final Sort ORDEM_CARDAPIO = Sort.by("categoria", "id");

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    // Cardápio público: só pratos ativos (RN09), com filtro opcional por categoria
    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarAtivos(String categoria) {
        List<Produto> produtos = temTexto(categoria)
                ? produtoRepository.findByAtivoTrueAndCategoria(categoria.trim(), ORDEM_CARDAPIO)
                : produtoRepository.findByAtivoTrue(ORDEM_CARDAPIO);
        return produtos.stream().map(ProdutoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarAtivo(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id)
                .map(ProdutoResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Prato não encontrado: " + id));
    }

    // Gestão: ativos e inativos, paginado
    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(String categoria, Pageable pageable) {
        Page<Produto> produtos = temTexto(categoria)
                ? produtoRepository.findByCategoria(categoria.trim(), pageable)
                : produtoRepository.findAll(pageable);
        return produtos.map(ProdutoResponse::from);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) {
        return ProdutoResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto();
        aplicar(produto, request);
        produto.setAtivo(request.active() == null || request.active());
        return ProdutoResponse.from(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidade(id);
        aplicar(produto, request);
        if (request.active() != null) {
            produto.setAtivo(request.active());
        }
        return ProdutoResponse.from(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse alterarAtivo(Long id, boolean ativo) {
        Produto produto = buscarEntidade(id);
        produto.setAtivo(ativo);
        return ProdutoResponse.from(produtoRepository.save(produto));
    }

    // Exclusão lógica (RN06): o prato pode estar em pedidos antigos, então não sai do banco
    @Transactional
    public void excluir(Long id) {
        Produto produto = buscarEntidade(id);
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    private Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Prato não encontrado: " + id));
    }

    private void aplicar(Produto produto, ProdutoRequest request) {
        produto.setNome(request.name().trim());
        produto.setDescricao(request.description() == null ? null : request.description().trim());
        produto.setPreco(request.price());
        produto.setCategoria(request.category().trim());
        produto.setImagem(temTexto(request.image()) ? request.image().trim() : null);
        produto.setDestaque(Boolean.TRUE.equals(request.isSpecial()));
        produto.setTempoPreparo(request.prepTime());
        // A tela de gestão não edita calorias nem tags: se não vierem, mantém o que já existe
        if (request.calories() != null) {
            produto.setCalorias(request.calories());
        }
        if (request.tags() != null) {
            produto.getTags().clear();
            request.tags().forEach(tag -> produto.getTags().add(tag.trim()));
        }
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
