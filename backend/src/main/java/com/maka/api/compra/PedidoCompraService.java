package com.maka.api.compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.compra.dto.PedidoCompraItemRequest;
import com.maka.api.compra.dto.PedidoCompraRequest;
import com.maka.api.compra.dto.PedidoCompraResponse;
import com.maka.api.compra.dto.RecebimentoRequest;
import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.fornecedor.PrecoUnidade;
import com.maka.api.model.EstoqueMovimentacao;
import com.maka.api.model.Fornecedor;
import com.maka.api.model.FornecedorProduto;
import com.maka.api.model.Ingrediente;
import com.maka.api.model.MotivoMovimentacao;
import com.maka.api.model.PedidoCompra;
import com.maka.api.model.PedidoCompraItem;
import com.maka.api.model.StatusFornecedor;
import com.maka.api.model.StatusIngrediente;
import com.maka.api.model.StatusPedidoCompra;
import com.maka.api.model.TipoMovimentacao;
import com.maka.api.model.Usuario;
import com.maka.api.repository.EstoqueMovimentacaoRepository;
import com.maka.api.repository.FornecedorProdutoRepository;
import com.maka.api.repository.FornecedorRepository;
import com.maka.api.repository.IngredienteRepository;
import com.maka.api.repository.PedidoCompraRepository;
import com.maka.api.repository.UsuarioRepository;

/**
 * RF-024: pedido de compra RASCUNHO -> ENVIADO -> RECEBIDO (ou CANCELADO antes de receber).
 * RF-025 / RN05: receber o pedido cria uma ENTRADA (motivo COMPRA) no estoque para cada item
 * e atualiza o custo unitário do ingrediente com o preço pago. Como o custo da ficha técnica
 * é calculado na hora a partir do custo do ingrediente, o food cost dos pratos já reflete a compra.
 * Tudo no mesmo @Transactional: se um item falhar, nada é gravado.
 * Transição de status inválida devolve 409 (mesma regra do pedido de venda).
 */
@Service
public class PedidoCompraService {

    private final PedidoCompraRepository pedidoCompraRepository;
    private final FornecedorRepository fornecedorRepository;
    private final FornecedorProdutoRepository catalogoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final EstoqueMovimentacaoRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoCompraService(
            PedidoCompraRepository pedidoCompraRepository,
            FornecedorRepository fornecedorRepository,
            FornecedorProdutoRepository catalogoRepository,
            IngredienteRepository ingredienteRepository,
            EstoqueMovimentacaoRepository movimentacaoRepository,
            UsuarioRepository usuarioRepository) {
        this.pedidoCompraRepository = pedidoCompraRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.catalogoRepository = catalogoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<PedidoCompraResponse> listar(StatusPedidoCompra status, Long fornecedorId, Pageable pageable) {
        return pedidoCompraRepository.buscar(status, fornecedorId, pageable).map(PedidoCompraResponse::from);
    }

    @Transactional(readOnly = true)
    public PedidoCompraResponse buscar(Long id) {
        return PedidoCompraResponse.from(buscarPedido(id));
    }

    // Novo pedido nasce RASCUNHO, com total e preços calculados no servidor
    @Transactional
    public PedidoCompraResponse criar(PedidoCompraRequest request, String emailUsuario) {
        PedidoCompra pedido = new PedidoCompra();
        pedido.setStatus(StatusPedidoCompra.RASCUNHO);
        pedido.setUsuario(usuarioLogado(emailUsuario));
        pedido.setFornecedor(fornecedorAtivo(request.fornecedorId()));
        montarItens(pedido, request.itens());

        LocalDateTime agora = LocalDateTime.now();
        pedido.setCreatedAt(agora);   // o banco grava o próprio horário; este é só para a resposta
        pedido.setUpdatedAt(agora);
        return PedidoCompraResponse.from(pedidoCompraRepository.save(pedido));
    }

    // Só o rascunho pode ser editado (troca fornecedor e itens)
    @Transactional
    public PedidoCompraResponse atualizar(Long id, PedidoCompraRequest request) {
        PedidoCompra pedido = buscarParaAlterar(id);
        exigirStatus(pedido, StatusPedidoCompra.RASCUNHO, "Só é possível editar um pedido em rascunho");

        pedido.setFornecedor(fornecedorAtivo(request.fornecedorId()));
        pedido.getItens().clear();
        montarItens(pedido, request.itens());
        pedido.setUpdatedAt(LocalDateTime.now());
        return PedidoCompraResponse.from(pedidoCompraRepository.save(pedido));
    }

    @Transactional
    public PedidoCompraResponse enviar(Long id) {
        PedidoCompra pedido = buscarParaAlterar(id);
        exigirStatus(pedido, StatusPedidoCompra.RASCUNHO, "Só um pedido em rascunho pode ser enviado");
        if (pedido.getItens().isEmpty()) {
            throw new RegraNegocioException("Adicione pelo menos um item antes de enviar o pedido");
        }
        if (pedido.getFornecedor().getStatus() != StatusFornecedor.ATIVO) {
            throw new RegraNegocioException("O fornecedor " + pedido.getFornecedor().getRazaoSocial() + " está inativo");
        }
        pedido.setStatus(StatusPedidoCompra.ENVIADO);
        pedido.setUpdatedAt(LocalDateTime.now());
        return PedidoCompraResponse.from(pedidoCompraRepository.save(pedido));
    }

    // RF-025 + RN05: entrada no estoque e custo do ingrediente atualizado, tudo ou nada
    @Transactional
    public PedidoCompraResponse receber(Long id, RecebimentoRequest request, String emailUsuario) {
        PedidoCompra pedido = buscarParaAlterar(id);
        exigirStatus(pedido, StatusPedidoCompra.ENVIADO, "Só um pedido enviado pode ser recebido");

        String lote = request == null || request.lote() == null || request.lote().isBlank()
                ? "PC-" + pedido.getId() : request.lote().trim();
        LocalDate validade = request == null ? null : request.validade();
        if (validade != null && validade.isBefore(LocalDate.now())) {
            throw new RegraNegocioException("A validade não pode ser anterior a hoje");
        }
        Long usuarioId = usuarioLogado(emailUsuario).getId();
        LocalDateTime agora = LocalDateTime.now();

        for (PedidoCompraItem item : pedido.getItens()) {
            // Trava o ingrediente (mesma trava do EstoqueService) para o saldo não se perder
            Ingrediente ingrediente = ingredienteRepository.findByIdForUpdate(item.getIngrediente().getId())
                    .orElseThrow(() -> new NoSuchElementException("Ingrediente não encontrado: " + item.getIngrediente().getId()));
            if (ingrediente.getStatus() != StatusIngrediente.ATIVO) {
                throw new RegraNegocioException("O ingrediente " + ingrediente.getNome()
                        + " está inativo. Reative o ingrediente ou cancele o pedido");
            }

            EstoqueMovimentacao entrada = new EstoqueMovimentacao();
            entrada.setIngrediente(ingrediente);
            entrada.setTipo(TipoMovimentacao.ENTRADA);
            entrada.setMotivo(MotivoMovimentacao.COMPRA);
            entrada.setQuantidade(item.getQuantidade());
            entrada.setCustoUnitario(item.getPrecoUnitario());
            entrada.setLote(lote);
            entrada.setValidade(validade);
            entrada.setPedidoCompraId(pedido.getId());
            entrada.setUsuarioId(usuarioId);
            entrada.setCreatedAt(agora);
            movimentacaoRepository.save(entrada);

            // RN05: o custo do ingrediente passa a ser o preço desta compra
            ingrediente.setCustoUnitario(item.getPrecoUnitario());
            ingredienteRepository.save(ingrediente);
        }

        pedido.setStatus(StatusPedidoCompra.RECEBIDO);
        pedido.setUpdatedAt(agora);
        return PedidoCompraResponse.from(pedidoCompraRepository.save(pedido));
    }

    // Cancelar só antes de receber (depois disso o estoque já entrou)
    @Transactional
    public PedidoCompraResponse cancelar(Long id) {
        PedidoCompra pedido = buscarParaAlterar(id);
        if (pedido.getStatus() != StatusPedidoCompra.RASCUNHO && pedido.getStatus() != StatusPedidoCompra.ENVIADO) {
            throw new ConflitoException("Não é possível cancelar um pedido " + pedido.getStatus().name().toLowerCase());
        }
        pedido.setStatus(StatusPedidoCompra.CANCELADO);
        pedido.setUpdatedAt(LocalDateTime.now());
        return PedidoCompraResponse.from(pedidoCompraRepository.save(pedido));
    }

    // ── Apoio ─────────────────────────────────────────────────────────

    private void montarItens(PedidoCompra pedido, List<PedidoCompraItemRequest> itens) {
        Fornecedor fornecedor = pedido.getFornecedor();
        Set<Long> vistos = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PedidoCompraItemRequest req : itens) {
            if (!vistos.add(req.ingredienteId())) {
                throw new RegraNegocioException("O mesmo ingrediente aparece duas vezes no pedido. Junte as quantidades em um item só");
            }
            Ingrediente ingrediente = ingredienteRepository.findById(req.ingredienteId())
                    .orElseThrow(() -> new NoSuchElementException("Ingrediente não encontrado: " + req.ingredienteId()));
            if (ingrediente.getStatus() != StatusIngrediente.ATIVO) {
                throw new RegraNegocioException("O ingrediente " + ingrediente.getNome() + " está inativo");
            }
            FornecedorProduto oferta = catalogoRepository
                    .findByFornecedorIdAndIngredienteId(fornecedor.getId(), ingrediente.getId())
                    .orElseThrow(() -> new RegraNegocioException("O ingrediente " + ingrediente.getNome()
                            + " não está no catálogo de " + fornecedor.getRazaoSocial()));

            BigDecimal preco = req.precoUnitario() != null
                    ? req.precoUnitario().setScale(PrecoUnidade.ESCALA_PRECO, RoundingMode.HALF_UP)
                    : PrecoUnidade.precoNaUnidadeDoIngrediente(
                            oferta.getPreco(), oferta.getUnidadeVenda(), ingrediente.getUnidadeMedida());
            BigDecimal quantidade = req.quantidade().setScale(3, RoundingMode.HALF_UP);
            BigDecimal subtotal = quantidade.multiply(preco).setScale(2, RoundingMode.HALF_UP);

            PedidoCompraItem item = new PedidoCompraItem();
            item.setPedidoCompra(pedido);
            item.setIngrediente(ingrediente);
            item.setQuantidade(quantidade);
            item.setPrecoUnitario(preco);
            item.setSubtotal(subtotal);
            pedido.getItens().add(item);

            total = total.add(subtotal);
        }
        pedido.setValorTotal(total.setScale(2, RoundingMode.HALF_UP));
    }

    private PedidoCompra buscarPedido(Long id) {
        return pedidoCompraRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido de compra não encontrado: " + id));
    }

    // Com trava: duas abas mudando o mesmo pedido ao mesmo tempo não passam juntas
    private PedidoCompra buscarParaAlterar(Long id) {
        return pedidoCompraRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NoSuchElementException("Pedido de compra não encontrado: " + id));
    }

    private static void exigirStatus(PedidoCompra pedido, StatusPedidoCompra esperado, String mensagem) {
        if (pedido.getStatus() != esperado) {
            throw new ConflitoException(mensagem + " (status atual: " + pedido.getStatus() + ")");
        }
    }

    private Fornecedor fornecedorAtivo(Long fornecedorId) {
        Fornecedor fornecedor = fornecedorRepository.findById(fornecedorId)
                .orElseThrow(() -> new NoSuchElementException("Fornecedor não encontrado: " + fornecedorId));
        if (fornecedor.getStatus() != StatusFornecedor.ATIVO) {
            throw new RegraNegocioException("O fornecedor " + fornecedor.getRazaoSocial() + " está inativo");
        }
        return fornecedor;
    }

    private Usuario usuarioLogado(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
    }
}
