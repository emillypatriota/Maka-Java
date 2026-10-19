package com.maka.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maka.api.model.Funcionario;
import com.maka.api.model.StatusFuncionario;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    List<Funcionario> findByStatus(StatusFuncionario status);
}