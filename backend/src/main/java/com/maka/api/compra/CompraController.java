package com.maka.api.compra;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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

import com.maka.api.compra.dto.CotacaoResponse;
import com.maka.api.compra.dto.PedidoCompraRequest;
import com.maka.api.compra.dto.PedidoCompraResponse;
import com.maka.api.compra.dto.RecebimentoRequest;
import com.maka.api.model.StatusPedidoCompra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Bloco 7: cotação (RF-023) e pedido de compra (RF-024, RF-025). Gerência.
@RestController
@Validated
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequestMapping("/api/admin/compras")
public class CompraController {

    private final PedidoCompraService pedidoCompraService;
    private final CotacaoService cotacaoService;

    public CompraController(PedidoCompraService pedidoCompraService, CotacaoService cotacaoService) {
        this.pedidoCompraService = pedidoCompraService;
        this.cotacaoService = cotacaoService;
    }

    // ?ingredienteIds=1,2,3  (sem parâmetro: todos os ingredientes que têm oferta)
    @GetMapping("/cotacao")
    public List<CotacaoResponse> cotacao(@RequestParam(required = false) List<Long> ingredienteIds) {
        return cotacaoService.cotar(ingredienteIds);
    }

    // ?status=ENVIADO&fornecedorId=1&page=0&size=10 (mais recentes primeiro)
    @GetMapping
    public Page<PedidoCompraResponse> listar(
            @RequestParam(required = false) StatusPedidoCompra status,
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return pedidoCompraService.listar(status, fornecedorId,
                PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending())));
    }

    @GetMapping("/{id}")
    public PedidoCompraResponse buscar(@PathVariable Long id) {
        return pedidoCompraService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<PedidoCompraResponse> criar(
            @Valid @RequestBody PedidoCompraRequest request,
            Authentication authentication) {
        PedidoCompraResponse criado = pedidoCompraService.criar(request, authentication.getName());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    public PedidoCompraResponse atualizar(@PathVariable Long id, @Valid @RequestBody PedidoCompraRequest request) {
        return pedidoCompraService.atualizar(id, request);
    }

    // RASCUNHO -> ENVIADO
    @PatchMapping("/{id}/enviar")
    public PedidoCompraResponse enviar(@PathVariable Long id) {
        return pedidoCompraService.enviar(id);
    }

    // ENVIADO -> RECEBIDO: entrada no estoque + custo do ingrediente (RF-025, RN05). Corpo opcional.
    @PatchMapping("/{id}/receber")
    public PedidoCompraResponse receber(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RecebimentoRequest request,
            Authentication authentication) {
        return pedidoCompraService.receber(id, request, authentication.getName());
    }

    // RASCUNHO ou ENVIADO -> CANCELADO
    @PatchMapping("/{id}/cancelar")
    public PedidoCompraResponse cancelar(@PathVariable Long id) {
        return pedidoCompraService.cancelar(id);
    }
}
