package com.maka.api.fichatecnica;

import com.maka.api.fichatecnica.dto.FichaTecnicaRequest;
import com.maka.api.fichatecnica.dto.FichaTecnicaResponse;
import com.maka.api.model.Ingrediente;
import com.maka.api.model.Produto;
import com.maka.api.model.StatusPrato;
import com.maka.api.repository.FichaTecnicaRepository;
import com.maka.api.repository.IngredienteRepository;
import com.maka.api.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FichaTecnicaServiceTest {

    @Mock
    private FichaTecnicaRepository fichaTecnicaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private IngredienteRepository ingredienteRepository;

    private FichaTecnicaService service;

    @BeforeEach
    void setUp() {
        service = new FichaTecnicaService(
                fichaTecnicaRepository, produtoRepository, ingredienteRepository, new CustoPratoService());
    }

    @Test
    void deveSalvarFichaECalcularOFoodCost() {
        Produto produto = produto(StatusPrato.PAUSADO, "100.00");
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "50.00")));

        // 0,5 kg × 1,2 × R$ 50,00 = R$ 30,00 → 30% do preço
        FichaTecnicaResponse resposta = service.salvar(1L, request(item(7L, "0.5", "1.2")));

        assertEquals(new BigDecimal("30.00"), resposta.cost());
        assertEquals(new BigDecimal("30.00"), resposta.foodCost());
        assertEquals("green", resposta.foodCostLevel());
        assertFalse(resposta.foodCostWarning());
        assertEquals(new BigDecimal("30.00"), resposta.items().get(0).cost());
        verify(fichaTecnicaRepository).apagarDoProduto(1L);
        verify(fichaTecnicaRepository).saveAll(any());
    }

    @Test
    void deveAvisarQuandoFoodCostPassaDe35PorCento() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "40.00")));

        FichaTecnicaResponse resposta = service.salvar(1L, request(item(7L, "1", "1")));

        assertEquals("red", resposta.foodCostLevel());
        assertTrue(resposta.foodCostWarning());
    }

    @Test
    void naoDeveApagarAFichaAntigaQuandoOFatorDeCorrecaoEInvalido() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "40.00")));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.salvar(1L, request(item(7L, "1", "0.8"))));

        assertEquals("O fator de correção deve ser maior ou igual a 1,0", erro.getMessage());
        verify(fichaTecnicaRepository, never()).apagarDoProduto(anyLong());
    }

    @Test
    void deveRecusarIngredienteRepetido() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));

        assertThrows(IllegalArgumentException.class,
                () -> service.salvar(1L, request(item(7L, "1", "1"), item(7L, "2", "1"))));
        verify(fichaTecnicaRepository, never()).apagarDoProduto(anyLong());
    }

    @Test
    void deveRecusarIngredienteQueNaoExiste() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> service.salvar(1L, request(item(99L, "1", "1"))));
    }

    @Test
    void fichaVaziaPausaOPratoAtivo() {
        Produto produto = produto(StatusPrato.ATIVO, "100.00");
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of());

        FichaTecnicaResponse resposta = service.salvar(1L, request());

        assertEquals(StatusPrato.PAUSADO, produto.getStatus());
        assertEquals("paused", resposta.status());
        assertNull(resposta.cost());
    }

    private static FichaTecnicaRequest request(FichaTecnicaRequest.Item... itens) {
        return new FichaTecnicaRequest(List.of(itens));
    }

    private static FichaTecnicaRequest.Item item(Long ingredienteId, String quantidade, String fator) {
        return new FichaTecnicaRequest.Item(ingredienteId, new BigDecimal(quantidade), new BigDecimal(fator));
    }

    private static Produto produto(StatusPrato status, String preco) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Prato");
        produto.setPreco(new BigDecimal(preco));
        produto.setStatus(status);
        return produto;
    }

    private static Ingrediente ingrediente(Long id, String custo) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId(id);
        ingrediente.setNome("Ingrediente " + id);
        ingrediente.setUnidadeMedida("kg");
        ingrediente.setCustoUnitario(new BigDecimal(custo));
        return ingrediente;
    }
}
