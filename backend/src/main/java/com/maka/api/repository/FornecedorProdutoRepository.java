package com.maka.api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.FornecedorProduto;

public interface FornecedorProdutoRepository extends JpaRepository<FornecedorProduto, Long> {

    List<FornecedorProduto> findByFornecedorId(Long fornecedorId);

    Optional<FornecedorProduto> findByFornecedorIdAndIngredienteId(Long fornecedorId, Long ingredienteId);

    // Catálogo de um fornecedor (RF-022), já com o ingrediente carregado, em ordem alfabética
    @EntityGraph(attributePaths = "ingrediente")
    List<FornecedorProduto> findByFornecedorIdOrderByIngredienteNomeAsc(Long fornecedorId);

    // Cotação comparativa (RF-023): todas as ofertas dos ingredientes pedidos
    @EntityGraph(attributePaths = {"fornecedor", "ingrediente"})
    List<FornecedorProduto> findByIngredienteIdIn(Collection<Long> ingredienteIds);

    // Cotação sem filtro: todas as ofertas de todos os fornecedores
    @EntityGraph(attributePaths = {"fornecedor", "ingrediente"})
    List<FornecedorProduto> findAllByOrderByIngredienteNomeAsc();
}
