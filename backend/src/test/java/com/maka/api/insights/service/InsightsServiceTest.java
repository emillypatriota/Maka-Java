package com.maka.api.insights.service;

import com.maka.api.insights.dto.InsightsResumoResponseDTO;
import com.maka.api.estoque.EstoqueService;
import com.maka.api.estoque.dto.SaldoEstoqueResponse;
import com.maka.api.estoque.dto.SaldoEstoqueResumoResponse;
import com.maka.api.fichatecnica.CustoPratoService.CustoPrato;
import com.maka.api.fichatecnica.CustoPratoService.Faixa;
import com.maka.api.fichatecnica.FichaTecnicaService;
import com.maka.api.model.ItemPedido;
import com.maka.api.model.Pedido;
import com.maka.api.model.Produto;
import com.maka.api.model.StatusPedido;
import com.maka.api.repository.ItemPedidoRepository;
import com.maka.api.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ItemPedidoRepository itemPedidoRepository;

    @Mock
    private FichaTecnicaService fichaTecnicaService;

    @Mock
    private EstoqueService estoqueService;

    @Test
    void deveRetornarOsCincoPratosMaisVendidos() {
        List<ItemPedidoRepository.PratoMaisVendidoProjection> ranking = List.of(
                prato("Lasanha", 12L),
                prato("Risoto", 9L)
        );
        when(pedidoRepository.findAll()).thenReturn(List.of());
        when(itemPedidoRepository.findPratosMaisVendidos(PageRequest.of(0, 5))).thenReturn(ranking);

        InsightsResumoResponseDTO resumo = service().obterResumo();

        assertEquals(2, resumo.getPratosMaisVendidos().size());
        assertEquals("Lasanha", resumo.getPratosMaisVendidos().get(0).getNomeProduto());
        assertEquals(12L, resumo.getPratosMaisVendidos().get(0).getQuantidadeTotal());
        assertEquals("Risoto", resumo.getPratosMaisVendidos().get(1).getNomeProduto());
        assertEquals(9L, resumo.getPratosMaisVendidos().get(1).getQuantidadeTotal());
        verify(itemPedidoRepository).findPratosMaisVendidos(PageRequest.of(0, 5));
    }

    @Test
    void deveRetornarTopPratosLimitadoAoPeriodoInformado() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 7);
        List<ItemPedidoRepository.PratoMaisVendidoProjection> ranking = List.of(prato("Lasanha", 12L));
        when(itemPedidoRepository.findPratosMaisVendidosNoPeriodo(
                inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay(), PageRequest.of(0, 5)))
                .thenReturn(ranking);

        var topPratos = service().obterTopPratos(inicio, fim);

        assertEquals(1, topPratos.size());
        assertEquals("Lasanha", topPratos.get(0).nomeProduto());
        assertEquals(12L, topPratos.get(0).quantidadeTotal());
    }

    @Test
    void devePreencherDiasSemVendasComZeroNoResumo() {
        LocalDate inicio = LocalDate.now().minusDays(1);
        LocalDate fim = LocalDate.now();
        when(pedidoRepository.findByCriadoEmGreaterThanEqualAndCriadoEmLessThan(
                inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay())).thenReturn(List.of());
        when(pedidoRepository.findByCriadoEmGreaterThanEqualAndCriadoEmLessThan(
                fim.atStartOfDay(), fim.plusDays(1).atStartOfDay())).thenReturn(List.of());
        when(estoqueService.saldos()).thenReturn(new SaldoEstoqueResumoResponse(List.of(), 0));
        when(fichaTecnicaService.custosPorProduto(anyList())).thenReturn(Map.of());

        var resumo = service().obterDashboardResumo(inicio, fim);

        assertEquals(0, resumo.pedidosHoje());
        assertEquals(0, resumo.alertasEstoque());
        assertEquals(2, resumo.vendasPorDia().size());
        assertEquals("0", resumo.vendasPorDia().get(0).faturamento().toString());
    }

    @Test
    void deveCalcularKpisDeHojeEAlertasDeEstoque() {
        LocalDate hoje = LocalDate.now();
        Produto produto = new Produto();
        produto.setId(5L);

        ItemPedido item = new ItemPedido();
        item.setProduto(produto);
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("50.00"));

        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.confirmed);
        pedido.setTotal(new BigDecimal("100.00"));
        pedido.setCriadoEm(LocalDateTime.now());
        pedido.getItens().add(item);

        when(pedidoRepository.findByCriadoEmGreaterThanEqualAndCriadoEmLessThan(
                hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay())).thenReturn(List.of(pedido));
        when(fichaTecnicaService.custosPorProduto(anyList())).thenReturn(Map.of(
                produto.getId(),
                new CustoPrato(new BigDecimal("20.00"), new BigDecimal("40.00"),
                        Faixa.VERMELHO, true)));
        when(estoqueService.saldos()).thenReturn(new SaldoEstoqueResumoResponse(List.of(
                new SaldoEstoqueResponse(2L, "ING-2", "Tomate", "kg",
                        new BigDecimal("10.000"), new BigDecimal("3.000"), true)), 1));

        var resumo = service().obterDashboardResumo(hoje, hoje);

        assertEquals(new BigDecimal("100.00"), resumo.faturamentoHoje());
        assertEquals(1, resumo.pedidosHoje());
        assertEquals(new BigDecimal("100.00"), resumo.ticketMedioHoje());
        assertEquals(new BigDecimal("40.00"), resumo.foodCostMedio());
        assertEquals(1, resumo.alertasEstoque());
        assertEquals("Tomate", resumo.estoqueCritico().get(0).nome());
        assertEquals(new BigDecimal("100.00"), resumo.vendasPorDia().get(0).faturamento());
    }

    private InsightsService service() {
        return new InsightsService(pedidoRepository, itemPedidoRepository, fichaTecnicaService, estoqueService);
    }

    private ItemPedidoRepository.PratoMaisVendidoProjection prato(String nome, Long quantidade) {
        return new ItemPedidoRepository.PratoMaisVendidoProjection() {
            @Override
            public String getNomeProduto() {
                return nome;
            }

            @Override
            public Long getQuantidadeTotal() {
                return quantidade;
            }
        };
    }
}
