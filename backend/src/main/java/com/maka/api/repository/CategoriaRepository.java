package com.maka.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.maka.api.model.Categoria;
import com.maka.api.model.StatusCategoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByStatus(StatusCategoria status, Sort sort);

    Optional<Categoria> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    boolean existsByNomeIgnoreCase(String nome);

    // Categoria nova entra no fim das abas do cardápio
    @Query("select coalesce(max(c.ordem), 0) + 1 from Categoria c")
    Integer proximaOrdem();
}
