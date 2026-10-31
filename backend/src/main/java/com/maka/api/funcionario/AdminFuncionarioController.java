package com.maka.api.funcionario;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.maka.api.funcionario.dto.FuncionarioRequest;
import com.maka.api.funcionario.dto.FuncionarioResponse;
import com.maka.api.funcionario.dto.StatusFuncionarioRequest;

// Escrita de funcionários (MANAGER, pela regra /api/admin/** do SecurityConfig)
@RestController
@RequestMapping("/api/admin/funcionarios")
public class AdminFuncionarioController {

    private final FuncionarioService funcionarioService;

    public AdminFuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @PostMapping
    public ResponseEntity<FuncionarioResponse> criar(@Valid @RequestBody FuncionarioRequest request) {
        FuncionarioResponse criado = funcionarioService.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.getId())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    public FuncionarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioRequest request) {
        return funcionarioService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public FuncionarioResponse alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusFuncionarioRequest request) {
        return funcionarioService.alterarStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        funcionarioService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
