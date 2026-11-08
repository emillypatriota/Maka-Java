package com.maka.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maka.api.model.EstoqueMovimentacao;
import com.maka.api.estoque.dto.SaldoEstoqueProjection;

import java.math.BigDecimal;
import java.util.List;

public interface EstoqueMovimentacaoRepository extends JpaRepository<EstoqueMovimentacao, Long> {

    Page<EstoqueMovimentacao> findByIngredienteId(Long ingredienteId, Pageable pageable);

    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN tipo = 'SAIDA' THEN -quantidade ELSE quantidade END), 0)
            FROM estoque_movimentacao
            WHERE ingrediente_id = :ingredienteId
            """, nativeQuery = true)
    BigDecimal calcularSaldo(@Param("ingredienteId") Long ingredienteId);

    @Query(value = """
            SELECT i.id AS id,
                   i.sku AS sku,
                   i.nome AS nome,
                   i.unidade_medida AS unidadeMedida,
                   i.estoque_minimo AS estoqueMinimo,
                   COALESCE(SUM(CASE WHEN m.tipo = 'SAIDA' THEN -m.quantidade ELSE m.quantidade END), 0) AS saldo
            FROM ingrediente i
            LEFT JOIN estoque_movimentacao m ON m.ingrediente_id = i.id
            WHERE i.status = 'ATIVO'
            GROUP BY i.id, i.sku, i.nome, i.unidade_medida, i.estoque_minimo
            ORDER BY i.nome
            """, nativeQuery = true)
    List<SaldoEstoqueProjection> listarSaldosAtivos();
}