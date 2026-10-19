package com.maka.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.Pedido;
import com.maka.api.model.StatusPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = { "itens", "itens.produto" })
    List<Pedido> findAllByOrderByCriadoEmDesc();

    @EntityGraph(attributePaths = { "itens", "itens.produto" })
    List<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status);
}