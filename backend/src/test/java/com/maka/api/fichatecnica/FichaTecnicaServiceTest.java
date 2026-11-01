package com.maka.api.fichatecnica;

import com.maka.api.fichatecnica.dto.FichaTecnicaRequest;
import com.maka.api.fichatecnica.dto.FichaTecnicaResponse;
import com.maka.api.model.FichaTecnica;
import com.maka.api.model.FichaTecnicaItem;
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
    void deveCriarFichaECalcularOFoodCost() {
        Produto produto = produto(StatusPrato.PAUSADO, "100.00");
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(fichaTecnicaRepository.findByProdutoId(1L)).thenReturn(Optional.empty());
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "g", "0.05")));

        // 500 g × 1,2 × R$ 0,05/g = R$ 30,00 → 30% do preço
        FichaTecnicaResponse resposta = service.salvar(1L, request(1, "Grelhar", item(7L, "500", "g", "1.2")));

        assertEquals(new BigDecimal("30.00"), resposta.cost());
        assertEquals(new BigDecimal("30.00"), resposta.foodCost());
        assertEquals("green", resposta.foodCostLevel());
        assertFalse(resposta.foodCostWarning());
        assertEquals("Grelhar", resposta.preparationMethod());
        assertEquals("g", resposta.items().get(0).unit());
        verify(fichaTecnicaRepository).save(any(FichaTecnica.class));
    }

    @Test
    void deveConverterAUnidadeDoItemParaADoIngrediente() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.PAUSADO, "100.00")));
        when(fichaTecnicaRepository.findByProdutoId(1L)).thenReturn(Optional.empty());
        // ingrediente com custo por kg; ficha em gramas: 300 g = 0,3 kg × R$ 50,00 = R$ 15,00
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "kg", "50.00")));

        FichaTecnicaResponse resposta = service.salvar(1L, request(1, null, item(7L, "300", "g", "1")));

        assertEquals(new BigDecimal("15.00"), resposta.cost());
    }

    @Test
    void custoDaPorcaoDivideAReceitaPeloRendimento() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.PAUSADO, "50.00")));
        when(fichaTecnicaRepository.findByProdutoId(1L)).thenReturn(Optional.empty());
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "un", "40.00")));

        // receita de R$ 40,00 que rende 4 porções → R$ 10,00 a porção → 20% de R$ 50,00
        FichaTecnicaResponse resposta = service.salvar(1L, request(4, null, item(7L, "1", "un", "1")));

        assertEquals(new BigDecimal("40.00"), resposta.batchCost());
        assertEquals(new BigDecimal("10.00"), resposta.cost());
        assertEquals(new BigDecimal("20.00"), resposta.foodCost());
        assertEquals(4, resposta.servings());
    }

    @Test
    void deveAvisarQuandoFoodCostPassaDe35PorCento() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));
        when(fichaTecnicaRepository.findByProdutoId(1L)).thenReturn(Optional.empty());
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "un", "40.00")));

        FichaTecnicaResponse resposta = service.salvar(1L, request(1, null, item(7L, "1", "un", "1")));

        assertEquals("red", resposta.foodCostLevel());
        assertTrue(resposta.foodCostWarning());
    }

    @Test
    void naoDeveMexerNaFichaQuandoOFatorDeCorrecaoEInvalido() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "un", "40.00")));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.salvar(1L, request(1, null, item(7L, "1", "un", "0.8"))));

        assertEquals("O fator de correção deve ser maior ou igual a 1,0", erro.getMessage());
        verify(fichaTecnicaRepository, never()).save(any());
    }

    @Test
    void deveRecusarUnidadeQueNaoCombinaComODoIngrediente() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of(ingrediente(7L, "g", "0.05")));

        assertThrows(IllegalArgumentException.class,
                () -> service.salvar(1L, request(1, null, item(7L, "1", "ml", "1"))));
        verify(fichaTecnicaRepository, never()).save(any());
    }

    @Test
    void deveRecusarIngredienteRepetido() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));

        assertThrows(IllegalArgumentException.class,
                () -> service.salvar(1L, request(1, null, item(7L, "1", "un", "1"), item(7L, "2", "un", "1"))));
        verify(fichaTecnicaRepository, never()).save(any());
    }

    @Test
    void deveRecusarIngredienteQueNaoExiste() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(StatusPrato.ATIVO, "100.00")));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> service.salvar(1L, request(1, null, item(99L, "1", "un", "1"))));
    }

    @Test
    void fichaSemIngredientesPausaOPratoAtivo() {
        Produto produto = produto(StatusPrato.ATIVO, "100.00");
        FichaTecnica existente = new FichaTecnica();
        existente.setProduto(produto);
        FichaTecnicaItem antigo = new FichaTecnicaItem();
        antigo.setFichaTecnica(existente);
        existente.getItens().add(antigo);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(fichaTecnicaRepository.findByProdutoId(1L)).thenReturn(Optional.of(existente));
        when(ingredienteRepository.findAllById(any())).thenReturn(List.of());

        FichaTecnicaResponse resposta = service.salvar(1L, request(1, "Montar", new FichaTecnicaRequest.Item[0]));

        assertEquals(StatusPrato.PAUSADO, produto.getStatus());
        assertEquals("paused", resposta.status());
        assertTrue(existente.getItens().isEmpty());
        assertNull(resposta.cost());
    }

    private static FichaTecnicaRequest request(int rendimento, String modoPreparo, FichaTecnicaRequest.Item... itens) {
        return new FichaTecnicaRequest(rendimento, modoPreparo, List.of(itens));
    }

    private static FichaTecnicaRequest.Item item(Long ingredienteId, String quantidade, String unidade, String fator) {
        return new FichaTecnicaRequest.Item(ingredienteId, new BigDecimal(quantidade), unidade, new BigDecimal(fator));
    }

    private static Produto produto(StatusPrato status, String preco) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Prato");
        produto.setPreco(new BigDecimal(preco));
        produto.setStatus(status);
        return produto;
    }

    private static Ingrediente ingrediente(Long id, String unidade, String custo) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId(id);
        ingrediente.setNome("Ingrediente " + id);
        ingrediente.setUnidadeMedida(unidade);
        ingrediente.setCustoUnitario(new BigDecimal(custo));
        return ingrediente;
    }
}
