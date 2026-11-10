package com.maka.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.Pedido;
import com.maka.api.model.StatusPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = { "itens", "itens.produto" })
    List<Pedido> findAllByOrderByCriadoEmDesc();

    @EntityGraph(attributePaths = { "itens", "itens.produto" })
    List<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status);

    // CORREÇÕES DA PR #39
    @EntityGraph(attributePaths = { "itens", "itens.produto" })
    Page<Pedido> findAll(Pageable pageable);

    @EntityGraph(attributePaths = { "itens", "itens.produto" })
    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);

    @EntityGraph(attributePaths = { "itens", "itens.produto" })
    List<Pedido> findByClienteEmailOrderByCriadoEmDesc(String clienteEmail);
}