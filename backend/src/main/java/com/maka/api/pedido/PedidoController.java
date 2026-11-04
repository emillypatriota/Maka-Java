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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PedidoController {

    private final PedidoService pedidoService;

    // Injeção de dependência explícita (evitando Lombok para não quebrar no VS Code)
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // (P1) POST /api/pedidos (cliente logado cria o pedido)
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoResponseDTO> criarPedido(@RequestBody @Valid PedidoRequestDTO request) {
        PedidoResponseDTO response = pedidoService.criarPedido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // (P1) GET /api/pedidos/meus (histórico de pedidos do cliente logado)
    @GetMapping("/pedidos/meus")
    public ResponseEntity<List<PedidoResponseDTO>> listarMeusPedidos() {
        // Temporário: Como a Auth ainda mantém a rota com permitAll(), mockamos o e-mail
        // para não quebrar o teste do Front-end até a segurança estar 100% ativada.
        String emailMock = "cliente@teste.com"; 
        return ResponseEntity.ok(pedidoService.listarMeusPedidos(emailMock));
    }

    // (P1) GET /api/pedidos/{id}/status (cliente acompanha status)
    @GetMapping("/pedidos/{id}/status")
    public ResponseEntity> buscarStatusPedido(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarStatusPedido(id));
    }

    // (P1) GET /api/admin/pedidos (painel da cozinha/gerência, com filtros e paginação)
    @GetMapping("/admin/pedidos")
    public ResponseEntity<Page<PedidoResponseDTO>> listarPedidosAdmin(
            @RequestParam(required = false) StatusPedido status,
            Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPedidosAdmin(status, pageable));
    }

    // (P1) PATCH /api/admin/pedidos/{id}/status (funcionários atualizam o status)
    @PatchMapping("/admin/pedidos/{id}/status")
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        
        String novoStatus = body.get("status");
        PedidoResponseDTO response = pedidoService.atualizarStatus(id, novoStatus);
        return ResponseEntity.ok(response);
    }
    
    // (Extra) GET /api/pedidos (endpoint antigo para os painéis, mantendo compatibilidade)
    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarPedidosAntigo() {
        return ResponseEntity.ok(pedidoService.listarMeusPedidos(null));
    }
}