package com.maka.api.pedido;

import com.maka.api.model.StatusPedido;
import com.maka.api.pedido.dto.PedidoRequestDTO;
import com.maka.api.pedido.dto.PedidoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PedidoController {

    // (P1) POST /api/pedidos (cliente logado cria o pedido)
    @PostMapping("/pedidos")
    public ResponseEntity<PedidoResponseDTO> criarPedido(@RequestBody @Valid PedidoRequestDTO request) {
        // TODO: Implementar lógica no PedidoService (@Transactional, cálculo no servidor, baixa de estoque)
        // Por enquanto, retorna um esqueleto vazio para o front não quebrar
        PedidoResponseDTO response = new PedidoResponseDTO();
        response.setId("0"); // Retornando string como o front espera
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // (P1) GET /api/pedidos/meus (histórico de pedidos do cliente logado)
    @GetMapping("/pedidos/meus")
    public ResponseEntity<List<PedidoResponseDTO>> listarMeusPedidos() {
        // TODO: Chamar o PedidoService buscando pelo cliente logado
        return ResponseEntity.ok(new ArrayList<>());
    }

    // (P1) GET /api/pedidos/{id}/status (cliente acompanha status)
    @GetMapping("/pedidos/{id}/status")
    public ResponseEntity<Map<String, String>> buscarStatusPedido(@PathVariable Long id) {
        // TODO: Retornar status atual
        return ResponseEntity.ok(Map.of("status", StatusPedido.pending.name()));
    }

    // (P1) GET /api/admin/pedidos (painel da cozinha/gerência, com filtros e paginação)
    @GetMapping("/admin/pedidos")
    public ResponseEntity<Page<PedidoResponseDTO>> listarPedidosAdmin(
            @RequestParam(required = false) StatusPedido status,
            Pageable pageable) {
        // TODO: Implementar filtros e paginação real no PedidoService
        return ResponseEntity.ok(new PageImpl<>(new ArrayList<>()));
    }

    // (P1) PATCH /api/admin/pedidos/{id}/status (funcionários atualizam o status)
    @PatchMapping("/admin/pedidos/{id}/status")
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        
        String novoStatus = body.get("status");
        // TODO: Validar transição (se inválido, jogar exceção que o Handler pegará como 409 Conflict)
        
        PedidoResponseDTO response = new PedidoResponseDTO();
        response.setId(String.valueOf(id));
        return ResponseEntity.ok(response);
    }
    
    // (Extra) GET /api/pedidos (endpoint antigo para os painéis, mantendo compatibilidade)
    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarPedidosAntigo() {
        return ResponseEntity.ok(new ArrayList<>());
    }
}