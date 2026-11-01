package com.maka.api.categoria;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.categoria.dto.CategoriaRequest;
import com.maka.api.categoria.dto.CategoriaResponse;
import com.maka.api.model.Categoria;
import com.maka.api.model.StatusCategoria;
import com.maka.api.repository.CategoriaRepository;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    // Cardápio público: só categorias ativas, na ordem das abas
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarAtivas() {
        return categoriaRepository.findByStatus(StatusCategoria.ATIVO, Sort.by("ordem", "id")).stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listar(Pageable pageable) {
        return categoriaRepository.findAll(pageable).map(CategoriaResponse::from);
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        String nome = request.name().trim();
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new DataIntegrityViolationException("Já existe uma categoria chamada " + nome);
        }
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setDescricao(descricao(request));
        categoria.setOrdem(categoriaRepository.proximaOrdem());
        if (request.status() != null) {
            categoria.setStatus(status(request.status()));
        }
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nome = request.name().trim();
        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new DataIntegrityViolationException("Já existe uma categoria chamada " + nome);
        }
        categoria.setNome(nome);
        categoria.setDescricao(descricao(request));
        if (request.status() != null) {
            categoria.setStatus(status(request.status()));
        }
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    // Exclusão lógica (RN06): os pratos continuam no banco, mas saem do cardápio público
    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscar(id);
        categoria.setStatus(StatusCategoria.INATIVO);
        categoriaRepository.save(categoria);
    }

    // Usado pelo ProdutoService ao salvar um prato: o front manda o nome da categoria
    @Transactional(readOnly = true)
    public Categoria buscarAtivaPorNome(String nome) {
        Categoria categoria = categoriaRepository.findByNomeIgnoreCase(nome.trim())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + nome));
        if (categoria.getStatus() != StatusCategoria.ATIVO) {
            throw new IllegalArgumentException("A categoria " + categoria.getNome() + " está inativa");
        }
        return categoria;
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Categoria não encontrada: " + id));
    }

    private static String descricao(CategoriaRequest request) {
        return request.description() == null || request.description().isBlank() ? null : request.description().trim();
    }

    private static StatusCategoria status(String status) {
        return "active".equals(status) ? StatusCategoria.ATIVO : StatusCategoria.INATIVO;
    }
}
