package com.maka.api.funcionario;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.funcionario.dto.FuncionarioRequest;
import com.maka.api.funcionario.dto.FuncionarioResponse;
import com.maka.api.funcionario.dto.ResumoFuncionariosResponse;
import com.maka.api.model.Cargo;
import com.maka.api.model.Funcionario;
import com.maka.api.model.StatusFuncionario;
import com.maka.api.repository.FuncionarioRepository;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    // cargo: manager, kitchen, delivery, cashier ou waiter; busca: nome, e-mail ou telefone
    @Transactional(readOnly = true)
    public Page<FuncionarioResponse> listar(String cargo, String busca, Pageable pageable) {
        return funcionarioRepository.buscarAdmin(
                cargo(cargo),
                busca == null || busca.isBlank() ? null : busca.trim(),
                pageable).map(FuncionarioResponse::from);
    }

    @Transactional(readOnly = true)
    public ResumoFuncionariosResponse resumo() {
        Map<String, Long> porCargo = new LinkedHashMap<>();
        for (Cargo cargo : Cargo.values()) {
            porCargo.put(cargo.name(), funcionarioRepository.countByCargo(cargo));
        }
        return new ResumoFuncionariosResponse(
                funcionarioRepository.count(),
                funcionarioRepository.countByStatus(StatusFuncionario.active),
                porCargo);
    }

    @Transactional
    public FuncionarioResponse criar(FuncionarioRequest request) {
        Funcionario funcionario = new Funcionario();
        aplicar(funcionario, request);
        return FuncionarioResponse.from(funcionarioRepository.save(funcionario));
    }

    @Transactional
    public FuncionarioResponse atualizar(Long id, FuncionarioRequest request) {
        Funcionario funcionario = buscar(id);
        aplicar(funcionario, request);
        return FuncionarioResponse.from(funcionarioRepository.save(funcionario));
    }

    @Transactional
    public FuncionarioResponse alterarStatus(Long id, StatusFuncionario status) {
        Funcionario funcionario = buscar(id);
        funcionario.setStatus(status);
        return FuncionarioResponse.from(funcionarioRepository.save(funcionario));
    }

    // Exclusão lógica (RN06): o funcionário fica inativo, não sai do banco
    @Transactional
    public void excluir(Long id) {
        Funcionario funcionario = buscar(id);
        funcionario.setStatus(StatusFuncionario.inactive);
        funcionarioRepository.save(funcionario);
    }

    private Funcionario buscar(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Funcionário não encontrado: " + id));
    }

    private void aplicar(Funcionario funcionario, FuncionarioRequest request) {
        funcionario.setNome(request.name().trim());
        funcionario.setCargo(request.role());
        funcionario.setTelefone(semVazio(request.phone()));
        funcionario.setEmail(semVazio(request.email()));
        if (request.status() != null) {
            funcionario.setStatus(request.status());
        }
        funcionario.setEscala(semVazio(request.schedule()));
        funcionario.setDataAdmissao(request.hireDate());
        funcionario.setObservacoes(semVazio(request.notes()));
    }

    private static Cargo cargo(String cargo) {
        if (cargo == null || cargo.isBlank()) {
            return null;
        }
        try {
            return Cargo.valueOf(cargo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cargo inválido: " + cargo);
        }
    }

    private static String semVazio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
