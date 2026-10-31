package com.maka.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maka.api.model.Cargo;
import com.maka.api.model.Funcionario;
import com.maka.api.model.StatusFuncionario;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    List<Funcionario> findByStatus(StatusFuncionario status);

    // busca: parte do nome, do e-mail ou do telefone
    @Query("""
            select f from Funcionario f
            where (:cargo is null or f.cargo = :cargo)
              and (:busca is null
                   or lower(f.nome) like lower(concat('%', :busca, '%'))
                   or lower(f.email) like lower(concat('%', :busca, '%'))
                   or f.telefone like concat('%', :busca, '%'))
            """)
    Page<Funcionario> buscarAdmin(@Param("cargo") Cargo cargo, @Param("busca") String busca, Pageable pageable);

    long countByStatus(StatusFuncionario status);

    long countByCargo(Cargo cargo);
}
