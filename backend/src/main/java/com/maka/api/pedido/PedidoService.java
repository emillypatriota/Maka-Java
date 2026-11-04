package com.maka.api.pedido;

import com.maka.api.model.*;
import com.maka.api.pedido.dto.*;
import com.maka.api.repository.ItemPedidoRepository;
import com.maka.api.repository.PedidoRepository;
import com.maka.api.repository.ProdutoRepository;
import com.maka.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    @Transactional
    public PedidoResponseDTO criarPedido(PedidoRequestDTO request) {
        // 1. Validar e buscar o cliente
        if (request.getCliente() == null || request.getCliente().getUid() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do cliente logado são obrigatórios.");
        }
        
        // Converte UID String para Long
        Long usuarioId = Long.parseLong(request.getCliente().getUid());
        Usuario cliente = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        // 2. Instanciar o Pedido
        Pedido pedido = new Pedido();
        pedido.setClienteNome(cliente.getNome());
        pedido.setClienteTelefone(request.getCliente().getTelefone() != null ? request.getCliente().getTelefone() : "N/A");
        pedido.setClienteEndereco(request.getCliente().getEndereco());
        pedido.setClienteEmail(cliente.getEmail());
        
        pedido.setFormaPagamento(request.getFormaPagamento());
        pedido.setTipo(request.getTipo() != null ? request.getTipo() : TipoPedido.delivery);
        pedido.setStatus(StatusPedido.pending);
        pedido.setTaxaEntrega(request.getTaxaEntrega() != null ? request.getTaxaEntrega() : BigDecimal.ZERO);
        pedido.setObservacoes(request.getObservacoes());
        pedido.setNumeroMesa(request.getNumeroMesa());
        // O tempo estimado será calculado posteriormente conforme a RN03
        
        BigDecimal totalPedido = pedido.getTaxaEntrega();

        // 3. Processar Itens e Calcular Total no Servidor
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O pedido deve ter pelo menos um item.");
        }

        for (ItemPedidoRequestDTO itemRequest : request.getItens()) {
            Long produtoId = Long.parseLong(itemRequest.getProdutoId());
            Produto produto = produtoRepository.findById(produtoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado: " + produtoId));

            // RN09 (Gestão): Não permitir compra de produto pausado/inativo
            if (produto.getStatus() != StatusPrato.ATIVO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O produto '" + produto.getNome() + "' não está ativo para venda.");
            }

            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setQuantidade(itemRequest.getQuantidade());
            item.setPrecoUnitario(produto.getPreco()); // Congela o preço do banco
            item.setObservacoes(itemRequest.getObservacoes());
            
            pedido.adicionarItem(item);

            // Calcula o subtotal do item e soma ao total do pedido
            BigDecimal subtotal = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
            totalPedido = totalPedido.add(subtotal);
        }

        pedido.setTotal(totalPedido);

        // 4. Salvar
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // 5. Converter para DTO
        return converterParaResponseDTO(pedidoSalvo);
    }

    private PedidoResponseDTO converterParaResponseDTO(Pedido pedido) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(String.valueOf(pedido.getId()));
        dto.setOrderNumber(String.format("%04d", pedido.getId()));
        
        ClienteDTO cliente = new ClienteDTO();
        cliente.setNome(pedido.getClienteNome());
        cliente.setTelefone(pedido.getClienteTelefone());
        cliente.setEndereco(pedido.getClienteEndereco());
        cliente.setEmail(pedido.getClienteEmail());
        dto.setCliente(cliente);

        dto.setFormaPagamento(pedido.getFormaPagamento());
        dto.setTotal(pedido.getTotal());
        dto.setTaxaEntrega(pedido.getTaxaEntrega());
        dto.setStatus(pedido.getStatus());
        dto.setTipo(pedido.getTipo());
        dto.setCriadoEm(pedido.getCriadoEm());
        dto.setAtualizadoEm(pedido.getAtualizadoEm());
        
        if (pedido.getEntregador() != null) {
            dto.setEntregadorId(String.valueOf(pedido.getEntregador().getId()));
        }
        
        dto.setObservacoes(pedido.getObservacoes());
        dto.setTempoEstimado(pedido.getTempoEstimado());
        dto.setNumeroMesa(pedido.getNumeroMesa());
        dto.setMotivoCancelamento(pedido.getMotivoCancelamento());

        List<ItemPedidoResponseDTO> itensDto = pedido.getItens().stream().map(item -> {
            ItemPedidoResponseDTO itemDto = new ItemPedidoResponseDTO();
            itemDto.setProdutoId(String.valueOf(item.getProduto().getId()));
            itemDto.setNomeProduto(item.getProduto().getNome());
            itemDto.setImagemProduto(item.getProduto().getImagem());
            itemDto.setQuantidade(item.getQuantidade());
            itemDto.setPrecoUnitario(item.getPrecoUnitario());
            itemDto.setObservacoes(item.getObservacoes());
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItens(itensDto);
        return dto;
    }
    // (P1) GET /api/pedidos/meus
    public List<PedidoResponseDTO> listarMeusPedidos(String emailCliente) {
        // Como o repositório atual não tem busca por e-mail, usamos a listagem geral 
        // filtrada via stream (o ideal é adicionar um findByClienteEmail no PedidoRepository depois)
        return pedidoRepository.findAllByOrderByCriadoEmDesc().stream()
                .filter(p -> p.getClienteEmail() != null && p.getClienteEmail().equals(emailCliente))
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
    }

    // (P1) GET /api/pedidos/{id}/status
    public Map<String, String> buscarStatusPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
        return Map.of("status", pedido.getStatus().name());
    }

    // (P1) GET /api/admin/pedidos
    public Page<PedidoResponseDTO> listarPedidosAdmin(StatusPedido status, Pageable pageable) {
        // Utiliza os métodos base do JpaRepository. 
        // Se status for nulo, busca todos paginados.
        Page<Pedido> paginaPedidos;
        // Nota: O ideal é que a Dupla Base adicione métodos com @EntityGraph e Pageable no PedidoRepository
        paginaPedidos = pedidoRepository.findAll(pageable); 
        
        List<PedidoResponseDTO> dtos = paginaPedidos.getContent().stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
                
        return new PageImpl<>(dtos, pageable, paginaPedidos.getTotalElements());
    }

    // (P1) PATCH /api/admin/pedidos/{id}/status
    @Transactional
    public PedidoResponseDTO atualizarStatus(Long id, String novoStatusStr) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));

        StatusPedido novoStatus;
        try {
            novoStatus = StatusPedido.valueOf(novoStatusStr.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido.");
        }

        if (!isTransicaoValida(pedido.getStatus(), novoStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Transição de status inválida: de " + pedido.getStatus() + " para " + novoStatus);
        }

        pedido.setStatus(novoStatus);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        return converterParaResponseDTO(pedidoSalvo);
    }

    // Regra da Máquina de Status (Seção 6.4)
    private boolean isTransicaoValida(StatusPedido atual, StatusPedido novo) {
        // Cancelamento é permitido apenas antes de 'delivering'
        if (novo == StatusPedido.cancelled) {
            return atual == StatusPedido.pending || atual == StatusPedido.confirmed || 
                   atual == StatusPedido.preparing || atual == StatusPedido.ready;
        }

        return switch (atual) {
            case pending -> novo == StatusPedido.confirmed;
            case confirmed -> novo == StatusPedido.preparing;
            case preparing -> novo == StatusPedido.ready;
            case ready -> novo == StatusPedido.delivering;
            case delivering -> novo == StatusPedido.delivered;
            default -> false;
        };
    }
}
