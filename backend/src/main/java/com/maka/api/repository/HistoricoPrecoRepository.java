package com.maka.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.HistoricoPreco;

public interface HistoricoPrecoRepository extends JpaRepository<HistoricoPreco, Long> {

    // Evolução do preço de um ingrediente (RF-026, gráfico)
    List<HistoricoPreco> findByIngredienteIdOrderByRegistradoEmAsc(Long ingredienteId);
}