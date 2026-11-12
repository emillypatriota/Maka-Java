package com.maka.api.compra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maka.api.compra.dto.PedidoCompraItemRequest;
import com.maka.api.compra.dto.PedidoCompraRequest;
import com.maka.api.compra.dto.PedidoCompraResponse;
import com.maka.api.compra.dto.RecebimentoRequest;
import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
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

@ExtendWith(MockitoExtension.class)
class PedidoCompraServiceTest {

    private static final String EMAIL = "ger@maka.com";

    @Mock private PedidoCompraRepository pedidoCompraRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private FornecedorProdutoRepository catalogoRepository;
    @Mock private IngredienteRepository ingredienteRepository;
    @Mock private EstoqueMovimentacaoRepository movimentacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private PedidoCompraService service;

    // ── Dados de teste ────────────────────────────────────────────────

    private Usuario gerente() {
        Usuario u = new Usuario();
        u.setId(3L);
        u.setEmail(EMAIL);
        u.setNome("Gerente");
        return u;
    }

    private Fornecedor fornecedor(StatusFornecedor status) {
        Fornecedor f = new Fornecedor();
        f.setId(1L);
        f.setRazaoSocial("Casa de Carnes Prime");
        f.setCnpj("11.222.333/0001-81");
        f.setStatus(status);
        return f;
    }

    private Ingrediente picanha(StatusIngrediente status) {
        Ingrediente i = new Ingrediente();
        i.setId(10L);
        i.setNome("Picanha");
        i.setUnidadeMedida("g");
        i.setCustoUnitario(new BigDecimal("0.0700"));
        i.setStatus(status);
        return i;
    }

    // R$ 78,00 o kg
    private FornecedorProduto oferta(Fornecedor f, Ingrediente i) {
        FornecedorProduto o = new FornecedorProduto();
        o.setId(100L);
        o.setFornecedor(f);
        o.setIngrediente(i);
        o.setPreco(new BigDecimal("78.00"));
        o.setUnidadeVenda("kg");
        return o;
    }

    private PedidoCompra pedido(StatusPedidoCompra status, Ingrediente ingrediente) {
        PedidoCompra p = new PedidoCompra();
        p.setId(50L);
        p.setFornecedor(fornecedor(StatusFornecedor.ATIVO));
        p.setUsuario(gerente());
        p.setStatus(status);
        PedidoCompraItem item = new PedidoCompraItem();
        item.setId(500L);
        item.setPedidoCompra(p);
        item.setIngrediente(ingrediente);
        item.setQuantidade(new BigDecimal("5000.000"));
        item.setPrecoUnitario(new BigDecimal("0.0780"));
        item.setSubtotal(new BigDecimal("390.00"));
        p.getItens().add(item);
        p.setValorTotal(new BigDecimal("390.00"));
        return p;
    }

    private void prepararCriacao(Fornecedor f, Ingrediente i) {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(gerente()));
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(f));
        when(ingredienteRepository.findById(10L)).thenReturn(Optional.of(i));
        when(catalogoRepository.findByFornecedorIdAndIngredienteId(1L, 10L)).thenReturn(Optional.of(oferta(f, i)));
    }

    private static PedidoCompraRequest pedir(BigDecimal quantidade, BigDecimal preco) {
        return new PedidoCompraRequest(1L, List.of(new PedidoCompraItemRequest(10L, quantidade, preco)));
    }

    private static void igual(String esperado, BigDecimal valor) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor), "esperado " + esperado + ", veio " + valor);
    }

    // ── Criação (RF-024) ──────────────────────────────────────────────

    @Test
    void criaRascunhoComPrecoDoCatalogoETotalCalculadoNoServidor() {
        prepararCriacao(fornecedor(StatusFornecedor.ATIVO), picanha(StatusIngrediente.ATIVO));
        when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoCompraResponse resp = service.criar(pedir(new BigDecimal("5000"), null), EMAIL);

        assertEquals(StatusPedidoCompra.RASCUNHO, resp.status());
        igual("0.0780", resp.itens().get(0).precoUnitario());   // 78,00/kg -> 0,0780/g
        igual("390.00", resp.itens().get(0).subtotal());        // 5000 g x 0,0780
        igual("390.00", resp.valorTotal());
        assertEquals("Gerente", resp.criadoPor());
    }

    @Test
    void precoInformadoSubstituiOPrecoDoCatalogo() {
        prepararCriacao(fornecedor(StatusFornecedor.ATIVO), picanha(StatusIngrediente.ATIVO));
        when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoCompraResponse resp = service.criar(pedir(new BigDecimal("5000"), new BigDecimal("0.07")), EMAIL);

        igual("350.00", resp.valorTotal());
    }

    @Test
    void ingredienteForaDoCatalogoDa422() {
        Fornecedor f = fornecedor(StatusFornecedor.ATIVO);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(gerente()));
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(f));
        when(ingredienteRepository.findById(10L)).thenReturn(Optional.of(picanha(StatusIngrediente.ATIVO)));
        when(catalogoRepository.findByFornecedorIdAndIngredienteId(1L, 10L)).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class, () -> service.criar(pedir(BigDecimal.TEN, null), EMAIL));
        verify(pedidoCompraRepository, never()).save(any());
    }

    @Test
    void ingredienteRepetidoNoPedidoDa422() {
        prepararCriacao(fornecedor(StatusFornecedor.ATIVO), picanha(StatusIngrediente.ATIVO));
        PedidoCompraRequest req = new PedidoCompraRequest(1L, List.of(
                new PedidoCompraItemRequest(10L, BigDecimal.ONE, null),
                new PedidoCompraItemRequest(10L, BigDecimal.TEN, null)));

        assertThrows(RegraNegocioException.class, () -> service.criar(req, EMAIL));
        verify(pedidoCompraRepository, never()).save(any());
    }

    @Test
    void fornecedorInativoNaoRecebePedido() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(gerente()));
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor(StatusFornecedor.INATIVO)));

        assertThrows(RegraNegocioException.class, () -> service.criar(pedir(BigDecimal.TEN, null), EMAIL));
    }

    @Test
    void soEditaRascunho() {
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.ENVIADO, picanha(StatusIngrediente.ATIVO))));

        assertThrows(ConflitoException.class, () -> service.atualizar(50L, pedir(BigDecimal.TEN, null)));
    }

    // ── Status (RF-024) ───────────────────────────────────────────────

    @Test
    void enviaRascunho() {
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.RASCUNHO, picanha(StatusIngrediente.ATIVO))));
        when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(StatusPedidoCompra.ENVIADO, service.enviar(50L).status());
    }

    @Test
    void naoEnviaPedidoQueJaFoiEnviado() {
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.ENVIADO, picanha(StatusIngrediente.ATIVO))));

        assertThrows(ConflitoException.class, () -> service.enviar(50L));
    }

    @Test
    void cancelaPedidoEnviado() {
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.ENVIADO, picanha(StatusIngrediente.ATIVO))));
        when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(StatusPedidoCompra.CANCELADO, service.cancelar(50L).status());
    }

    @Test
    void naoCancelaPedidoRecebido() {
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.RECEBIDO, picanha(StatusIngrediente.ATIVO))));

        assertThrows(ConflitoException.class, () -> service.cancelar(50L));
    }

    // ── Recebimento (RF-025, RN05) ────────────────────────────────────

    @Test
    void receberCriaEntradaNoEstoqueEAtualizaCustoDoIngrediente() {
        Ingrediente picanha = picanha(StatusIngrediente.ATIVO);
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.ENVIADO, picanha)));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(gerente()));
        when(ingredienteRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(picanha));
        when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoCompraResponse resp = service.receber(50L, null, EMAIL);

        assertEquals(StatusPedidoCompra.RECEBIDO, resp.status());
        ArgumentCaptor<EstoqueMovimentacao> mov = ArgumentCaptor.forClass(EstoqueMovimentacao.class);
        verify(movimentacaoRepository).save(mov.capture());
        assertEquals(TipoMovimentacao.ENTRADA, mov.getValue().getTipo());
        assertEquals(MotivoMovimentacao.COMPRA, mov.getValue().getMotivo());
        igual("5000", mov.getValue().getQuantidade());
        igual("0.0780", mov.getValue().getCustoUnitario());
        assertEquals(50L, mov.getValue().getPedidoCompraId());
        assertEquals(3L, mov.getValue().getUsuarioId());
        assertEquals("PC-50", mov.getValue().getLote());
        // RN05: custo do ingrediente passa a ser o preço pago (era 0,0700)
        igual("0.0780", picanha.getCustoUnitario());
        verify(ingredienteRepository).save(picanha);
    }

    @Test
    void receberUsaLoteEValidadeInformados() {
        Ingrediente picanha = picanha(StatusIngrediente.ATIVO);
        LocalDate validade = LocalDate.now().plusDays(10);
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.ENVIADO, picanha)));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(gerente()));
        when(ingredienteRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(picanha));
        when(pedidoCompraRepository.save(any(PedidoCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        service.receber(50L, new RecebimentoRequest(" L-123 ", validade), EMAIL);

        ArgumentCaptor<EstoqueMovimentacao> mov = ArgumentCaptor.forClass(EstoqueMovimentacao.class);
        verify(movimentacaoRepository).save(mov.capture());
        assertEquals("L-123", mov.getValue().getLote());
        assertEquals(validade, mov.getValue().getValidade());
    }

    @Test
    void naoRecebePedidoQueNaoFoiEnviado() {
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.RASCUNHO, picanha(StatusIngrediente.ATIVO))));

        assertThrows(ConflitoException.class, () -> service.receber(50L, null, EMAIL));
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    void naoRecebeComValidadeVencida() {
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.ENVIADO, picanha(StatusIngrediente.ATIVO))));

        assertThrows(RegraNegocioException.class,
                () -> service.receber(50L, new RecebimentoRequest(null, LocalDate.now().minusDays(1)), EMAIL));
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    void ingredienteInativoBloqueiaORecebimentoInteiro() {
        Ingrediente inativo = picanha(StatusIngrediente.INATIVO);
        when(pedidoCompraRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(pedido(StatusPedidoCompra.ENVIADO, inativo)));
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(gerente()));
        when(ingredienteRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(inativo));

        assertThrows(RegraNegocioException.class, () -> service.receber(50L, null, EMAIL));
        verify(movimentacaoRepository, never()).save(any());
        verify(pedidoCompraRepository, never()).save(any());
    }
}
