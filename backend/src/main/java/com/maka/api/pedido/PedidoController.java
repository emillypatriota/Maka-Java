package com.maka.api.pedido;

import org.springframework.security.access.prepost.PreAuthorize;
import com.maka.api.model.StatusPedido;
import com.maka.api.pedido.dto.PedidoRequestDTO;
import com.maka.api.pedido.dto.PedidoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PedidoController {

    private final PedidoService pedidoService;

    // Construtor explícito (sem Lombok) para injeção de dependência
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // (P1) POST /api/pedidos (cliente logado cria o pedido)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CLIENTE')")
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoResponseDTO> criarPedido(
            @RequestBody @Valid PedidoRequestDTO request, 
            Principal principal) {
        
        // Extrai o e-mail do token JWT via Principal
        String emailLogado = principal != null ? principal.getName() : null;
        PedidoResponseDTO response = pedidoService.criarPedido(request, emailLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // (P1) GET /api/pedidos/meus (histórico de pedidos do cliente logado)
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/pedidos/meus")
    public ResponseEntity<List<PedidoResponseDTO>> listarMeusPedidos(Principal principal) {
        String emailLogado = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(pedidoService.listarMeusPedidos(emailLogado));
    }

    // (P1) GET /api/pedidos/{id}/status (cliente acompanha status)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/pedidos/{id}/status")
    public ResponseEntity> buscarStatusPedido(@PathVariable Long id, Principal principal) {
        String emailLogado = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(pedidoService.buscarStatusPedido(id, emailLogado));
    }

    // (P1) GET /api/admin/pedidos (painel da cozinha/gerência, com filtros e paginação)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','KITCHEN','DELIVERY')")
    @GetMapping("/admin/pedidos")
    public ResponseEntity<Page<PedidoResponseDTO>> listarPedidosAdmin(
            @RequestParam(required = false) StatusPedido status,
            Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPedidosAdmin(status, pageable));
    }

    // (P1) PATCH /api/admin/pedidos/{id}/status (funcionários atualizam o status)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','KITCHEN','DELIVERY')")
    @PatchMapping("/admin/pedidos/{id}/status")
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        
        String novoStatus = body.get("status");
        PedidoResponseDTO response = pedidoService.atualizarStatus(id, novoStatus);
        return ResponseEntity.ok(response);
    }
    
    // (Extra) GET /api/pedidos (endpoint antigo para os painéis, mantendo compatibilidade)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','KITCHEN','DELIVERY')")
    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarPedidosAntigo() {
        return ResponseEntity.ok(pedidoService.listarPedidosAdmin(null, Pageable.unpaged()).getContent());
    }
}