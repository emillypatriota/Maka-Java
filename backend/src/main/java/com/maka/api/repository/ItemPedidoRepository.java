package com.maka.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maka.api.model.ItemPedido;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    @Query("""
            select item.produto.nome as nomeProduto, sum(item.quantidade) as quantidadeTotal
            from ItemPedido item
            where item.pedido.status is null
               or item.pedido.status <> com.maka.api.model.StatusPedido.cancelled
            group by item.produto.id, item.produto.nome
            order by sum(item.quantidade) desc, item.produto.id
            """)
    List<PratoMaisVendidoProjection> findPratosMaisVendidos(Pageable pageable);

    @Query("""
            select item.produto.nome as nomeProduto, sum(item.quantidade) as quantidadeTotal
            from ItemPedido item
            where (item.pedido.status is null
               or item.pedido.status <> com.maka.api.model.StatusPedido.cancelled)
              and item.pedido.criadoEm >= :inicio
              and item.pedido.criadoEm < :fimExclusivo
            group by item.produto.id, item.produto.nome
            order by sum(item.quantidade) desc, item.produto.id
            """)
    List<PratoMaisVendidoProjection> findPratosMaisVendidosNoPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fimExclusivo") LocalDateTime fimExclusivo,
            Pageable pageable);

    interface PratoMaisVendidoProjection {
        String getNomeProduto();

        Long getQuantidadeTotal();
    }
}