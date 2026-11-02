package com.maka.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.EstoqueMovimentacao;

public interface EstoqueMovimentacaoRepository extends JpaRepository<EstoqueMovimentacao, Long> {

    Page<EstoqueMovimentacao> findByIngredienteId(Long ingredienteId, Pageable pageable);
}