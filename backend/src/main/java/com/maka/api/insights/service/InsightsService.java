package com.maka.api.insights.service;

import com.maka.api.insights.dto.InsightsResumoResponseDTO;
import com.maka.api.insights.dto.DashboardResumoResponseDTO;
import com.maka.api.insights.dto.TopPratoResponseDTO;
import com.maka.api.estoque.EstoqueService;
import com.maka.api.estoque.dto.SaldoEstoqueResponse;
import com.maka.api.fichatecnica.CustoPratoService.CustoPrato;
import com.maka.api.fichatecnica.FichaTecnicaService;
import com.maka.api.model.ItemPedido;
import com.maka.api.model.Pedido;
import com.maka.api.model.Produto;
import com.maka.api.repository.ItemPedidoRepository;
import com.maka.api.repository.PedidoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InsightsService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final FichaTecnicaService fichaTecnicaService;
    private final EstoqueService estoqueService;

    public InsightsService(
            PedidoRepository pedidoRepository,
            ItemPedidoRepository itemPedidoRepository,
            FichaTecnicaService fichaTecnicaService,
            EstoqueService estoqueService) {
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.fichaTecnicaService = fichaTecnicaService;
        this.estoqueService = estoqueService;
    }

    @Transactional(readOnly = true)
    public InsightsResumoResponseDTO obterResumo() {
        List<Pedido> pedidos = pedidoRepository.findAll();

        // Regra: Desconsiderar pedidos cancelados (status 'cancelled')
        List<Pedido> pedidosValidos = pedidos.stream()
                .filter(p -> p.getStatus() != null && !p.getStatus().name().equalsIgnoreCase("cancelled"))
                .collect(Collectors.toList());

        // Faturamento Total
        BigDecimal faturamentoTotal = pedidosValidos.stream()
                .map(Pedido::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalPedidos = pedidosValidos.size();

        // Ticket Médio
        BigDecimal ticketMedio = totalPedidos > 0
                ? faturamentoTotal.divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Faturamento por Dia (agrupado por data de criação)
        Map<LocalDate, BigDecimal> faturamentoDiarioMap = pedidosValidos.stream()
                .filter(p -> p.getCriadoEm() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCriadoEm().toLocalDate(),
                        Collectors.mapping(Pedido::getTotal, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        List<InsightsResumoResponseDTO.FaturamentoDiarioDTO> faturamentoPorDia = faturamentoDiarioMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    InsightsResumoResponseDTO.FaturamentoDiarioDTO dto = new InsightsResumoResponseDTO.FaturamentoDiarioDTO();
                    dto.setData(entry.getKey().toString());
                    dto.setFaturamento(entry.getValue());
                    return dto;
                })
                .collect(Collectors.toList());

        // Montagem do DTO de Resumo
        InsightsResumoResponseDTO resumo = new InsightsResumoResponseDTO();
        resumo.setFaturamentoTotal(faturamentoTotal);
        resumo.setTicketMedio(ticketMedio);
        resumo.setTotalPedidos(totalPedidos);
        resumo.setFaturamentoPorDia(faturamentoPorDia);
        List<InsightsResumoResponseDTO.PratoMaisVendidoDTO> pratosMaisVendidos =
                itemPedidoRepository.findPratosMaisVendidos(PageRequest.of(0, 5)).stream()
                        .map(item -> {
                            InsightsResumoResponseDTO.PratoMaisVendidoDTO dto =
                                    new InsightsResumoResponseDTO.PratoMaisVendidoDTO();
                            dto.setNomeProduto(item.getNomeProduto());
                            dto.setQuantidadeTotal(item.getQuantidadeTotal());
                            return dto;
                        })
                        .collect(Collectors.toList());
        resumo.setPratosMaisVendidos(pratosMaisVendidos);

        return resumo;
    }

    @Transactional(readOnly = true)
    public DashboardResumoResponseDTO obterDashboardResumo(LocalDate dataInicio, LocalDate dataFim) {
        Periodo periodo = normalizarPeriodo(dataInicio, dataFim);
        LocalDate hoje = LocalDate.now();
        List<Pedido> pedidosHoje = pedidosNoPeriodo(hoje, hoje).stream()
                .filter(InsightsService::pedidoValido)
                .toList();
        List<Pedido> pedidosGrafico = pedidosNoPeriodo(periodo.inicio(), periodo.fim()).stream()
                .filter(InsightsService::pedidoValido)
                .toList();

        BigDecimal faturamentoHoje = pedidosHoje.stream()
                .map(Pedido::getTotal)
                .filter(total -> total != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalPedidosHoje = pedidosHoje.size();
        BigDecimal ticketMedioHoje = totalPedidosHoje == 0
                ? BigDecimal.ZERO
                : faturamentoHoje.divide(BigDecimal.valueOf(totalPedidosHoje), 2, RoundingMode.HALF_UP);

        List<SaldoEstoqueResponse> estoqueCritico = estoqueService.saldos().itens().stream()
                .filter(SaldoEstoqueResponse::abaixoDoMinimo)
                .toList();

        return new DashboardResumoResponseDTO(
                faturamentoHoje,
                totalPedidosHoje,
                ticketMedioHoje,
                calcularFoodCostMedio(pedidosHoje),
                estoqueCritico.size(),
                estoqueCritico,
                montarVendasPorDia(periodo.inicio(), periodo.fim(), pedidosGrafico));
    }

    @Transactional(readOnly = true)
    public List<TopPratoResponseDTO> obterTopPratos(LocalDate dataInicio, LocalDate dataFim) {
        Periodo periodo = normalizarPeriodo(dataInicio, dataFim);
        return itemPedidoRepository.findPratosMaisVendidosNoPeriodo(
                        periodo.inicio().atStartOfDay(),
                        periodo.fim().plusDays(1).atStartOfDay(),
                        PageRequest.of(0, 5))
                .stream()
                .map(item -> new TopPratoResponseDTO(item.getNomeProduto(), item.getQuantidadeTotal()))
                .toList();
    }

    private BigDecimal calcularFoodCostMedio(List<Pedido> pedidos) {
        Map<Long, Produto> produtos = pedidos.stream()
                .flatMap(pedido -> pedido.getItens().stream())
                .map(ItemPedido::getProduto)
                .collect(Collectors.toMap(Produto::getId, Function.identity(), (produto, repetido) -> produto));
        Map<Long, CustoPrato> custos = fichaTecnicaService.custosPorProduto(List.copyOf(produtos.values()));

        BigDecimal custoTotal = BigDecimal.ZERO;
        BigDecimal vendaTotal = BigDecimal.ZERO;
        for (Pedido pedido : pedidos) {
            for (ItemPedido item : pedido.getItens()) {
                CustoPrato custo = custos.get(item.getProduto().getId());
                if (custo == null || item.getPrecoUnitario() == null || item.getQuantidade() == null) {
                    continue;
                }
                BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade());
                custoTotal = custoTotal.add(custo.custo().multiply(quantidade));
                vendaTotal = vendaTotal.add(item.getPrecoUnitario().multiply(quantidade));
            }
        }
        return vendaTotal.signum() == 0
                ? BigDecimal.ZERO
                : custoTotal.multiply(BigDecimal.valueOf(100))
                        .divide(vendaTotal, 2, RoundingMode.HALF_UP);
    }

    private List<DashboardResumoResponseDTO.FaturamentoDiario> montarVendasPorDia(
            LocalDate inicio, LocalDate fim, List<Pedido> pedidos) {
        Map<LocalDate, BigDecimal> faturamentoPorData = pedidos.stream()
                .filter(pedido -> pedido.getCriadoEm() != null && pedido.getTotal() != null)
                .collect(Collectors.groupingBy(
                        pedido -> pedido.getCriadoEm().toLocalDate(),
                        Collectors.mapping(Pedido::getTotal,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        return inicio.datesUntil(fim.plusDays(1))
                .map(data -> new DashboardResumoResponseDTO.FaturamentoDiario(
                        data.toString(), faturamentoPorData.getOrDefault(data, BigDecimal.ZERO)))
                .toList();
    }

    private List<Pedido> pedidosNoPeriodo(LocalDate inicio, LocalDate fim) {
        return pedidoRepository.findByCriadoEmGreaterThanEqualAndCriadoEmLessThan(
                inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
    }

    private static boolean pedidoValido(Pedido pedido) {
        return pedido.getStatus() == null || !pedido.getStatus().name().equalsIgnoreCase("cancelled");
    }

    private static Periodo normalizarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = dataFim == null ? LocalDate.now() : dataFim;
        LocalDate inicio = dataInicio == null ? fim.minusDays(6) : dataInicio;
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual à data final.");
        }
        return new Periodo(inicio, fim);
    }

    private record Periodo(LocalDate inicio, LocalDate fim) {
    }
}
