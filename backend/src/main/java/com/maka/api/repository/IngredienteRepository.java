package com.maka.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maka.api.model.Ingrediente;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ingrediente i where i.id = :id")
    Optional<Ingrediente> findByIdForUpdate(@Param("id") Long id);
}
