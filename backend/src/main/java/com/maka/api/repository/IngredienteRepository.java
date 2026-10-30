package com.maka.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.Ingrediente;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {
}
