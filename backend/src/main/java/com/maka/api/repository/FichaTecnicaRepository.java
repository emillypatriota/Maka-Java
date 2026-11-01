package com.maka.api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.FichaTecnica;

public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {

    @EntityGraph(attributePaths = {"itens", "itens.ingrediente"})
    Optional<FichaTecnica> findByProdutoId(Long produtoId);

    // Uma consulta só para calcular o custo de uma página inteira de pratos
    @EntityGraph(attributePaths = {"itens", "itens.ingrediente"})
    List<FichaTecnica> findByProdutoIdIn(Collection<Long> produtoIds);

    // RN01: ficha sem nenhum ingrediente não conta
    boolean existsByProdutoIdAndItensIsNotEmpty(Long produtoId);
}
