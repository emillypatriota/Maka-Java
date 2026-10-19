package com.maka.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrue();

    List<Produto> findByCategoria(String categoria);
}