package com.maka.api.pedido;

import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.model.*;
import com.maka.api.pedido.dto.*;
import com.maka.api.repository.PedidoRepository;
import com.maka.api.repository.ProdutoRepository;
import com.maka.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository, UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PedidoResponseDTO criarPedido(PedidoRequestDTO request, String emailLogado) {
        Usuario cliente = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RegraNegocioException("Cliente logado não encontrado no banco de dados."));

        Pedido pedido = new Pedido();
        pedido.setClienteNome(cliente.getNome());
        pedido.setClienteEmail(cliente.getEmail());
        pedido.setClienteTelefone(request.getCliente() != null && request.getCliente().getTelefone() != null ? request.getCliente().getTelefone() : "N/A");
        pedido.setClienteEndereco(request.getCliente() != null ? request.getCliente().getEndereco() : null);
        
        pedido.setFormaPagamento(request.getFormaPagamento());
        pedido.setTipo(request.getTipo() != null ? request.getTipo() : TipoPedido.delivery);
        pedido.setStatus(StatusPedido.pending);
        pedido.setTaxaEntrega(request.getTaxaEntrega() != null ? request.getTaxaEntrega() : BigDecimal.ZERO);
        pedido.setObservacoes(request.getObservacoes());
        pedido.setNumeroMesa(request.getNumeroMesa());
        
        BigDecimal totalPedido = pedido.getTaxaEntrega();

        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new RegraNegocioException("O pedido deve ter pelo menos um item.");
        }

        for (ItemPedidoRequestDTO itemRequest : request.getItens()) {
            Long produtoId = Long.parseLong(itemRequest.getProdutoId());
            Produto produto = produtoRepository.findById(produtoId)
                    .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + produtoId));

            if (produto.getStatus() != StatusPrato.ATIVO) {
                throw new RegraNegocioException("O produto '" + produto.getNome() + "' não está ativo para venda.");
            }

            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setQuantidade(itemRequest.getQuantidade());
            item.setPrecoUnitario(produto.getPreco());
            item.setObservacoes(itemRequest.getObservacoes());
            
            pedido.adicionarItem(item);

            BigDecimal subtotal = item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
            totalPedido = totalPedido.add(subtotal);
        }

        pedido.setTotal(totalPedido);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        return converterParaResponseDTO(pedidoSalvo);
    }

    public List<PedidoResponseDTO> listarMeusPedidos(String emailCliente) {
        // Agora usa a query direta do banco com @EntityGraph em vez de filtrar em memória
        return pedidoRepository.findByClienteEmailOrderByCriadoEmDesc(emailCliente).stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
    }

    public Map<String, String> buscarStatusPedido(Long id, String emailLogado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));
        
        // Bloqueia se um CLIENTE tentar ver o pedido de outro e-mail
        if (emailLogado != null) {
            Usuario usuario = usuarioRepository.findByEmail(emailLogado).orElse(null);
            if (usuario != null && usuario.getPerfil() == Perfil.CLIENTE) {
                if (!emailLogado.equals(pedido.getClienteEmail())) {
                    throw new AccessDeniedException("Acesso negado. Apenas o dono pode consultar este pedido.");
                }
            }
        }

        return Map.of("status", pedido.getStatus().name());
    }

    public Page<PedidoResponseDTO> listarPedidosAdmin(StatusPedido status, Pageable pageable) {
        // Resolve o LazyInitializationException usando os novos métodos do repository
        Page<Pedido> paginaPedidos;
        if (status != null) {
            paginaPedidos = pedidoRepository.findByStatus(status, pageable);
        } else {
            paginaPedidos = pedidoRepository.findAll(pageable);
        }
        
        List<PedidoResponseDTO> dtos = paginaPedidos.getContent().stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, paginaPedidos.getTotalElements());
    }

    @Transactional
    public PedidoResponseDTO atualizarStatus(Long id, String novoStatusStr) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));

        // Evita NullPointerException se o body vier sem a chave status
        if (novoStatusStr == null || novoStatusStr.trim().isEmpty()) {
            throw new RegraNegocioException("O novo status é obrigatório.");
        }

        StatusPedido novoStatus;
        try {
            novoStatus = StatusPedido.valueOf(novoStatusStr.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Status inválido.");
        }

        if (!isTransicaoValida(pedido.getStatus(), novoStatus)) {
            throw new ConflitoException("Transição de status inválida: de " + pedido.getStatus() + " para " + novoStatus);
        }

        pedido.setStatus(novoStatus);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        return converterParaResponseDTO(pedidoSalvo);
    }

    private boolean isTransicaoValida(StatusPedido atual, StatusPedido novo) {
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
}