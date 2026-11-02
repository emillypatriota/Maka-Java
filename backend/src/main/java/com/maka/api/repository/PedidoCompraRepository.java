package com.maka.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.PedidoCompra;
import com.maka.api.model.StatusPedidoCompra;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {

    Page<PedidoCompra> findByStatus(StatusPedidoCompra status, Pageable pageable);
}