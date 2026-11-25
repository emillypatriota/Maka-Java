package com.maka.api.estoque;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.maka.api.estoque.dto.MovimentacaoRequest;
import com.maka.api.estoque.dto.MovimentacaoResponse;
import com.maka.api.estoque.dto.SaldoEstoqueResumoResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@Validated
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequestMapping("/api/admin/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping("/saldos")
    public SaldoEstoqueResumoResponse saldos() {
        return estoqueService.saldos();
    }

    @GetMapping("/movimentacoes")
    public Page<MovimentacaoResponse> movimentacoes(
            @RequestParam(required = false) Long ingredienteId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return estoqueService.movimentacoes(ingredienteId, page, size);
    }

    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse entrada(
            @Valid @RequestBody MovimentacaoRequest request,
            Authentication authentication) {
        return estoqueService.entrada(request, authentication.getName());
    }

    @PostMapping("/saidas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse saida(
            @Valid @RequestBody MovimentacaoRequest request,
            Authentication authentication) {
        return estoqueService.saida(request, authentication.getName());
    }
}
