package com.maka.api.funcionario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.maka.api.funcionario.dto.FuncionarioRequest;
import com.maka.api.funcionario.dto.FuncionarioResponse;
import com.maka.api.funcionario.dto.ResumoFuncionariosResponse;
import com.maka.api.funcionario.dto.StatusFuncionarioRequest;

// Funcionários (MANAGER, pela regra /api/admin/** do SecurityConfig).
// Substitui o antigo GET /api/funcionarios, que ficava aberto sem login.
@RestController
@Validated
@RequestMapping("/api/admin/funcionarios")
public class AdminFuncionarioController {

    private final FuncionarioService funcionarioService;

    public AdminFuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public Page<FuncionarioResponse> listar(
            @RequestParam(required = false) String cargo,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return funcionarioService.listar(cargo, busca, PageRequest.of(page, size, Sort.by("nome")));
    }

    @GetMapping("/resumo")
    public ResumoFuncionariosResponse resumo() {
        return funcionarioService.resumo();
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
