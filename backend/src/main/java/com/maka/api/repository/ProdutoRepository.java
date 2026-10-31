package com.maka.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrue();

    List<Produto> findByAtivoTrue(Sort sort);

    List<Produto> findByAtivoTrueAndCategoria(String categoria, Sort sort);

    Optional<Produto> findByIdAndAtivoTrue(Long id);

    List<Produto> findByCategoria(String categoria);

    Page<Produto> findByCategoria(String categoria, Pageable pageable);
}
