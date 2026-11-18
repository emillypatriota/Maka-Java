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

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CLIENTE')")
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoResponseDTO> criarPedido(
            @RequestBody @Valid PedidoRequestDTO request, 
            Principal principal) {
        String emailLogado = principal != null ? principal.getName() : null;
        PedidoResponseDTO response = pedidoService.criarPedido(request, emailLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/pedidos/meus")
    public ResponseEntity<List<PedidoResponseDTO>> listarMeusPedidos(Principal principal) {
        String emailLogado = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(pedidoService.listarMeusPedidos(emailLogado));
    }

    // CORREÇÃO (Erro 1): Sintaxe do ResponseEntity>
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/pedidos/{id}/status")
        public ResponseEntity<Map<String, String>> buscarStatusPedido(@PathVariable Long id, Principal principal) {

        String emailLogado = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(pedidoService.buscarStatusPedido(id, emailLogado));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','KITCHEN','DELIVERY')")
    @GetMapping("/admin/pedidos")
    public ResponseEntity<Page<PedidoResponseDTO>> listarPedidosAdmin(
            @RequestParam(required = false) StatusPedido status,
            Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPedidosAdmin(status, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','KITCHEN','DELIVERY')")
    @PatchMapping("/admin/pedidos/{id}/status")
    public ResponseEntity<PedidoResponseDTO> atualizarStatusAdmin(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String novoStatus = body.get("status");
        PedidoResponseDTO response = pedidoService.atualizarStatus(id, novoStatus);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('MANAGER','KITCHEN','DELIVERY')")
    @PatchMapping("/pedidos/{id}/status")
    public ResponseEntity<PedidoResponseDTO> atualizarStatusFront(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String novoStatus = body.get("status");
        PedidoResponseDTO response = pedidoService.atualizarStatus(id, novoStatus);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('MANAGER','DELIVERY')")
    @PatchMapping("/pedidos/{id}/entregador")
    public ResponseEntity<PedidoResponseDTO> atribuirEntregador(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long entregadorId = body.get("entregadorId");
        PedidoResponseDTO response = pedidoService.atribuirEntregador(id, entregadorId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CLIENTE')")
    @PatchMapping("/admin/pedidos/{id}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelarPedido(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        String motivo = body.get("motivo");
        String emailLogado = principal != null ? principal.getName() : null;
        PedidoResponseDTO response = pedidoService.cancelarPedido(id, motivo, emailLogado);
        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','KITCHEN','DELIVERY')")
    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarPedidosAntigo() {
        return ResponseEntity.ok(pedidoService.listarPedidosAdmin(null, Pageable.unpaged()).getContent());
    }
}
