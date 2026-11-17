package com.maka.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maka.api.model.Fornecedor;
import com.maka.api.model.StatusFornecedor;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    // RN07: CNPJ único (o banco também tem UNIQUE na coluna)
    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    // Selects do front (catálogo, pedido de compra): só fornecedores ativos
    List<Fornecedor> findByStatusOrderByRazaoSocialAsc(StatusFornecedor status);

    // Lista paginada com filtro opcional por status e por texto (razão social ou CNPJ)
    @Query("""
            select f from Fornecedor f
            where (:status is null or f.status = :status)
              and (:busca is null
                   or lower(f.razaoSocial) like lower(concat('%', :busca, '%'))
                   or f.cnpj like concat('%', :busca, '%'))
            """)
    Page<Fornecedor> buscar(@Param("busca") String busca,
                            @Param("status") StatusFornecedor status,
                            Pageable pageable);
}
