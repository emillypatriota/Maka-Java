package com.maka.api.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maka.api.model.FichaTecnicaItem;

public interface FichaTecnicaRepository extends JpaRepository<FichaTecnicaItem, Long> {

    @EntityGraph(attributePaths = "ingrediente")
    List<FichaTecnicaItem> findByProdutoIdOrderById(Long produtoId);

    // Uma consulta só para calcular o custo de uma página inteira de pratos
    @EntityGraph(attributePaths = "ingrediente")
    List<FichaTecnicaItem> findByProdutoIdIn(Collection<Long> produtoIds);

    boolean existsByProdutoId(Long produtoId);

    @Modifying
    @Query("delete from FichaTecnicaItem f where f.produto.id = :produtoId")
    void apagarDoProduto(@Param("produtoId") Long produtoId);
}
