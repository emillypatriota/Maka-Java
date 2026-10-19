package com.maka.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.ItemPedido;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {
}