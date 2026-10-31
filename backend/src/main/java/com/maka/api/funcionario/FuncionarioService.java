package com.maka.api.funcionario;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.funcionario.dto.FuncionarioRequest;
import com.maka.api.funcionario.dto.FuncionarioResponse;
import com.maka.api.model.Funcionario;
import com.maka.api.model.StatusFuncionario;
import com.maka.api.repository.FuncionarioRepository;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listar() {
        return funcionarioRepository.findAll(Sort.by("nome")).stream()
                .map(FuncionarioResponse::from)
                .toList();
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

    private static String semVazio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
