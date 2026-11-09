package com.maka.api.pedido;

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

    // Construtor explícito sem lombok
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/pedidos")
    public ResponseEntity<PedidoResponseDTO> criarPedido(
            @RequestBody @Valid PedidoRequestDTO request, 
            Principal principal) {
        
        // O JwtFilter injeta o e-mail no name do Principal
        String emailLogado = principal != null ? principal.getName() : null;
        PedidoResponseDTO response = pedidoService.criarPedido(request, emailLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pedidos/meus")
    public ResponseEntity<List<PedidoResponseDTO>> listarMeusPedidos(Principal principal) {
        String emailLogado = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(pedidoService.listarMeusPedidos(emailLogado));
    }

    @GetMapping("/pedidos/{id}/status")
    public ResponseEntity> buscarStatusPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarStatusPedido(id));
    }

    @GetMapping("/admin/pedidos")
    public ResponseEntity<Page<PedidoResponseDTO>> listarPedidosAdmin(
            @RequestParam(required = false) StatusPedido status,
            Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPedidosAdmin(status, pageable));
    }

    @PatchMapping("/admin/pedidos/{id}/status")
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        
        String novoStatus = body.get("status");
        PedidoResponseDTO response = pedidoService.atualizarStatus(id, novoStatus);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarPedidosAntigo() {
        // Compatibilidade com o painel antigo
        return ResponseEntity.ok(pedidoService.listarPedidosAdmin(null, Pageable.unpaged()).getContent());
    }
}