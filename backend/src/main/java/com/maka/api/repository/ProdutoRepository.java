package com.maka.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maka.api.model.Produto;
import com.maka.api.model.StatusCategoria;
import com.maka.api.model.StatusPrato;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // Cardápio público (RN09): prato ATIVO de categoria ATIVA
    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByStatusAndCategoriaStatus(StatusPrato status, StatusCategoria categoriaStatus, Sort sort);

    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByStatusAndCategoriaStatusAndCategoriaNome(
            StatusPrato status, StatusCategoria categoriaStatus, String categoriaNome, Sort sort);

    @EntityGraph(attributePaths = "categoria")
    Optional<Produto> findByIdAndStatusAndCategoriaStatus(Long id, StatusPrato status, StatusCategoria categoriaStatus);

    @EntityGraph(attributePaths = "categoria")
    @Query("""
            select p from Produto p
            where (:categoria is null or p.categoria.nome = :categoria)
              and (:status is null or p.status = :status)
              and (:busca is null or lower(p.nome) like lower(concat('%', :busca, '%')))
            """)
    Page<Produto> buscarAdmin(
            @Param("categoria") String categoria,
            @Param("status") StatusPrato status,
            @Param("busca") String busca,
            Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Optional<Produto> findWithCategoriaById(Long id);

    long countByStatus(StatusPrato status);
}
