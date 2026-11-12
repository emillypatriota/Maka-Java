package com.maka.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maka.api.model.PedidoCompra;
import com.maka.api.model.StatusPedidoCompra;

import jakarta.persistence.LockModeType;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {

    Page<PedidoCompra> findByStatus(StatusPedidoCompra status, Pageable pageable);

    // Lista de pedidos de compra (RF-024) com filtros opcionais de status e fornecedor
    @Query("""
            select p from PedidoCompra p
            where (:status is null or p.status = :status)
              and (:fornecedorId is null or p.fornecedor.id = :fornecedorId)
            """)
    Page<PedidoCompra> buscar(@Param("status") StatusPedidoCompra status,
                              @Param("fornecedorId") Long fornecedorId,
                              Pageable pageable);

    // Trava a linha do pedido: dois cliques em "Receber" não geram entrada de estoque em dobro
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoCompra p where p.id = :id")
    Optional<PedidoCompra> findByIdForUpdate(@Param("id") Long id);
}
