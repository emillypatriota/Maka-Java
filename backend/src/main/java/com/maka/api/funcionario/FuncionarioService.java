package com.maka.api.funcionario;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.funcionario.dto.FuncionarioResponse;
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
}
