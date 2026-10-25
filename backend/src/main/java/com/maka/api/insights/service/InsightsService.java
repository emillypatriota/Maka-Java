package com.maka.api.insights.service;

import com.maka.api.insights.dto.InsightsResumoResponseDTO;
import com.maka.api.model.Pedido;
import com.maka.api.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InsightsService {

    private final PedidoRepository pedidoRepository;

    // Injeção de dependência via construtor (evitando armadilhas do Lombok)
    public InsightsService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

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
        resumo.setPratosMaisVendidos(List.of()); // Pode ser populado integrando com o repositório de itens se necessário

        return resumo;
    }
}
