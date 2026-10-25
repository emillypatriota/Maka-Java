package com.maka.api.insights.service;

import com.maka.api.insights.dto.InsightsResumoResponseDTO;
import com.maka.api.repository.ItemPedidoRepository;
import com.maka.api.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ItemPedidoRepository itemPedidoRepository;

    @Test
    void deveRetornarOsCincoPratosMaisVendidos() {
        List<ItemPedidoRepository.PratoMaisVendidoProjection> ranking = List.of(
                prato("Lasanha", 12L),
                prato("Risoto", 9L)
        );
        when(pedidoRepository.findAll()).thenReturn(List.of());
        when(itemPedidoRepository.findPratosMaisVendidos(PageRequest.of(0, 5))).thenReturn(ranking);

        InsightsResumoResponseDTO resumo = new InsightsService(pedidoRepository, itemPedidoRepository).obterResumo();

        assertEquals(2, resumo.getPratosMaisVendidos().size());
        assertEquals("Lasanha", resumo.getPratosMaisVendidos().get(0).getNomeProduto());
        assertEquals(12L, resumo.getPratosMaisVendidos().get(0).getQuantidadeTotal());
        assertEquals("Risoto", resumo.getPratosMaisVendidos().get(1).getNomeProduto());
        assertEquals(9L, resumo.getPratosMaisVendidos().get(1).getQuantidadeTotal());
        verify(itemPedidoRepository).findPratosMaisVendidos(PageRequest.of(0, 5));
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
