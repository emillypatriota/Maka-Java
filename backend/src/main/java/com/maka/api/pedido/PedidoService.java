package com.maka.api.pedido;

import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.model.*;
import com.maka.api.pedido.dto.*;
import com.maka.api.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueMovimentacaoRepository estoqueRepository;
    private final FichaTecnicaRepository fichaTecnicaRepository;

    // CORREÇÃO (Erro 4): Removido o FuncionarioRepository do construtor, pois o Entregador é um Usuario
    public PedidoService(PedidoRepository pedidoRepository, 
                         ProdutoRepository produtoRepository, 
                         UsuarioRepository usuarioRepository,
                         EstoqueMovimentacaoRepository estoqueRepository,
                         FichaTecnicaRepository fichaTecnicaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoqueRepository = estoqueRepository;
        this.fichaTecnicaRepository = fichaTecnicaRepository;
    }

    @Transactional
    public PedidoResponseDTO criarPedido(PedidoRequestDTO request, String emailLogado) {
        Usuario cliente = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RegraNegocioException("Cliente logado não encontrado."));

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
            totalPedido = totalPedido.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }

        pedido.setTotal(totalPedido);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        return converterParaResponseDTO(pedidoSalvo);
    }

    @Transactional
    public PedidoResponseDTO atualizarStatus(Long id, String novoStatusStr) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));

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

        if (novoStatus == StatusPedido.confirmed && pedido.getStatus() == StatusPedido.pending) {
            processarBaixaDeEstoque(pedido);
        }

        pedido.setStatus(novoStatus);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        return converterParaResponseDTO(pedidoSalvo);
    }

    @Transactional
    public PedidoResponseDTO atribuirEntregador(Long id, Long entregadorId) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));
        if (entregadorId == null) throw new RegraNegocioException("ID do entregador é obrigatório.");

        // CORREÇÃO (Erro 4): Busca em UsuarioRepository e valida o perfil DELIVERY
        Usuario entregador = usuarioRepository.findById(entregadorId)
                .orElseThrow(() -> new NoSuchElementException("Entregador não encontrado."));
        
        if (entregador.getPerfil() != Perfil.DELIVERY) {
            throw new RegraNegocioException("O usuário selecionado não tem perfil de entregador.");
        }

        pedido.setEntregador(entregador);
        return converterParaResponseDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponseDTO cancelarPedido(Long id, String motivo, String emailLogado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));

        Usuario usuarioLogado = usuarioRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new NoSuchElementException("Usuário logado não encontrado."));

        // CORREÇÃO (Erro 6): Impede que o cliente cancele o pedido de outra pessoa
        if (usuarioLogado.getPerfil() == Perfil.CLIENTE && !usuarioLogado.getEmail().equals(pedido.getClienteEmail())) {
            throw new AccessDeniedException("Você não tem permissão para cancelar o pedido de outro cliente.");
        }

        if ((pedido.getStatus() == StatusPedido.preparing || pedido.getStatus() == StatusPedido.ready) 
            && (usuarioLogado.getPerfil() == Perfil.CLIENTE || usuarioLogado.getPerfil() == Perfil.KITCHEN || usuarioLogado.getPerfil() == Perfil.DELIVERY)) {
            throw new AccessDeniedException("Apenas o Gerente ou Admin pode cancelar um pedido em preparo ou pronto.");
        }

        if (pedido.getStatus() == StatusPedido.delivering || pedido.getStatus() == StatusPedido.delivered) {
            throw new ConflitoException("Não é possível cancelar um pedido que já saiu para entrega ou foi entregue.");
        }

        if (pedido.getStatus() == StatusPedido.confirmed || pedido.getStatus() == StatusPedido.preparing || pedido.getStatus() == StatusPedido.ready) {
            processarEstornoDeEstoque(pedido, motivo);
        }

        pedido.setStatus(StatusPedido.cancelled);
        pedido.setMotivoCancelamento(motivo != null ? motivo : "Cancelado sem motivo especificado");
        return converterParaResponseDTO(pedidoRepository.save(pedido));
    }

    // --- REGRAS DE ESTOQUE (BLOCO 6) ---

    private void processarBaixaDeEstoque(Pedido pedido) {
        // CORREÇÃO (Erro 8): Trava para dois cliques simultâneos
        List<EstoqueMovimentacao> verificacao = estoqueRepository.findByPedido(pedido);
        if (verificacao != null && !verificacao.isEmpty()) {
            throw new ConflitoException("O estoque deste pedido já foi baixado.");
        }

        List<String> faltantes = new ArrayList<>();
        List<EstoqueMovimentacao> movimentacoesPendentes = new ArrayList<>();

        for (ItemPedido item : pedido.getItens()) {
            FichaTecnica ficha = fichaTecnicaRepository.findByProdutoId(item.getProduto().getId()).orElse(null);
            if (ficha == null) continue;

            for (FichaTecnicaItem fichaItem : ficha.getItens()) {
                // CORREÇÃO (Erro 7): Fator de Correção
                BigDecimal fator = fichaItem.getFatorCorrecao() != null ? fichaItem.getFatorCorrecao() : BigDecimal.ONE;
                BigDecimal quantidadeNecessaria = fichaItem.getQuantidade()
                        .multiply(BigDecimal.valueOf(item.getQuantidade()))
                        .multiply(fator);

                // CORREÇÃO (Erro 7): Conversão de Unidade (KG para G / L para ML)
                String unFicha = fichaItem.getUnidadeMedida();
                String unIng = fichaItem.getIngrediente().getUnidadeMedida();
                if (unFicha != null && unIng != null) {
                    if ((unFicha.equalsIgnoreCase("KG") && unIng.equalsIgnoreCase("G")) ||
                        (unFicha.equalsIgnoreCase("L") && unIng.equalsIgnoreCase("ML"))) {
                        quantidadeNecessaria = quantidadeNecessaria.multiply(BigDecimal.valueOf(1000));
                    }
                }

                BigDecimal saldoAtual = estoqueRepository.calcularSaldo(fichaItem.getIngrediente().getId());

                if (saldoAtual.compareTo(quantidadeNecessaria) < 0) {
                    faltantes.add(fichaItem.getIngrediente().getNome() + " (Falta: " + quantidadeNecessaria.subtract(saldoAtual) + ")");
                } else {
                    EstoqueMovimentacao mov = new EstoqueMovimentacao();
                    mov.setIngrediente(fichaItem.getIngrediente());
                    mov.setQuantidade(quantidadeNecessaria);
                    // CORREÇÃO (Erro 2): Uso dos Enums
                    mov.setTipo(TipoMovimentacao.SAIDA);
                    mov.setMotivo(MotivoMovimentacao.VENDA);
                    // CORREÇÃO (Erro 7): Ligação com o Pedido
                    mov.setPedido(pedido);
                    // CORREÇÃO (Erro 3): mov.setDataMovimentacao removido (O BD preenche created_at)
                    movimentacoesPendentes.add(mov);
                }
            }
        }

        if (!faltantes.isEmpty()) {
            throw new RegraNegocioException("Estoque insuficiente. Faltam: " + String.join(", ", faltantes));
        }

        estoqueRepository.saveAll(movimentacoesPendentes);
    }

    private void processarEstornoDeEstoque(Pedido pedido, String motivo) {
        // CORREÇÃO (Erro 8): Checa o que REALMENTE saiu em vez de simular
        List<EstoqueMovimentacao> saidas = estoqueRepository.findByPedido(pedido);
        if (saidas == null || saidas.isEmpty()) return;

        List<EstoqueMovimentacao> estornos = new ArrayList<>();
        for (EstoqueMovimentacao saida : saidas) {
            // Se foi uma SAIDA, nós estornamos como ENTRADA
            if (saida.getTipo() == TipoMovimentacao.SAIDA) {
                EstoqueMovimentacao estorno = new EstoqueMovimentacao();
                estorno.setIngrediente(saida.getIngrediente());
                estorno.setQuantidade(saida.getQuantidade());
                // CORREÇÃO (Erro 5): Enums corretos (ENTRADA com motivo ESTORNO)
                estorno.setTipo(TipoMovimentacao.ENTRADA);
                estorno.setMotivo(MotivoMovimentacao.ESTORNO);
                // CORREÇÃO (Erro 7): Ligação mantida com o Pedido
                estorno.setPedido(pedido); 
                estornos.add(estorno);
            }
        }
        estoqueRepository.saveAll(estornos);
    }

    // --- MÉTODOS AUXILIARES ---

    public List<PedidoResponseDTO> listarMeusPedidos(String emailCliente) {
        return pedidoRepository.findByClienteEmailOrderByCriadoEmDesc(emailCliente).stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
    }

    public Map<String, String> buscarStatusPedido(Long id, String emailLogado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido não encontrado."));
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
        Page<Pedido> paginaPedidos = (status != null) ? 
            pedidoRepository.findByStatus(status, pageable) : pedidoRepository.findAll(pageable);
        List<PedidoResponseDTO> dtos = paginaPedidos.getContent().stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, paginaPedidos.getTotalElements());
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