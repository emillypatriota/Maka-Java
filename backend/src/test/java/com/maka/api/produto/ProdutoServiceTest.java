package com.maka.api.produto;

import com.maka.api.categoria.CategoriaService;
import com.maka.api.fichatecnica.FichaTecnicaService;
import com.maka.api.model.Categoria;
import com.maka.api.model.Produto;
import com.maka.api.model.StatusCategoria;
import com.maka.api.model.StatusPrato;
import com.maka.api.produto.dto.AdminProdutoResponse;
import com.maka.api.produto.dto.ProdutoRequest;
import com.maka.api.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private FichaTecnicaService fichaTecnicaService;

    private ProdutoService service;

    @BeforeEach
    void setUp() {
        service = new ProdutoService(produtoRepository, categoriaService, fichaTecnicaService);
    }

    @Test
    void naoDeveAtivarPratoSemFichaTecnica() {
        Produto produto = produto(StatusPrato.PAUSADO, StatusCategoria.ATIVO);
        when(produtoRepository.findWithCategoriaById(1L)).thenReturn(Optional.of(produto));
        when(fichaTecnicaService.temFicha(1L)).thenReturn(false);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.alterarStatus(1L, "active"));

        assertEquals("Monte a ficha técnica antes de ativar o prato (RN01)", erro.getMessage());
        assertEquals(StatusPrato.PAUSADO, produto.getStatus());
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void naoDeveAtivarPratoDeCategoriaInativa() {
        Produto produto = produto(StatusPrato.PAUSADO, StatusCategoria.INATIVO);
        when(produtoRepository.findWithCategoriaById(1L)).thenReturn(Optional.of(produto));
        when(fichaTecnicaService.temFicha(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.alterarStatus(1L, "active"));
        assertEquals(StatusPrato.PAUSADO, produto.getStatus());
    }

    @Test
    void deveAtivarPratoComFichaTecnica() {
        Produto produto = produto(StatusPrato.PAUSADO, StatusCategoria.ATIVO);
        when(produtoRepository.findWithCategoriaById(1L)).thenReturn(Optional.of(produto));
        when(fichaTecnicaService.temFicha(1L)).thenReturn(true);
        when(produtoRepository.save(produto)).thenReturn(produto);
        when(fichaTecnicaService.custosPorProduto(anyCollection())).thenReturn(Map.of());

        AdminProdutoResponse resposta = service.alterarStatus(1L, "active");

        assertEquals("active", resposta.getStatus());
        assertEquals(StatusPrato.ATIVO, produto.getStatus());
    }

    @Test
    void pausarNaoExigeFichaTecnica() {
        Produto produto = produto(StatusPrato.ATIVO, StatusCategoria.ATIVO);
        when(produtoRepository.findWithCategoriaById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(produto)).thenReturn(produto);
        when(fichaTecnicaService.custosPorProduto(anyCollection())).thenReturn(Map.of());

        service.alterarStatus(1L, "paused");

        assertEquals(StatusPrato.PAUSADO, produto.getStatus());
        verify(fichaTecnicaService, never()).temFicha(any());
    }

    @Test
    void pratoNovoNascePausado() {
        Categoria categoria = categoria(StatusCategoria.ATIVO);
        when(categoriaService.buscarAtivaPorNome("Entradas")).thenReturn(categoria);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> {
            Produto salvo = inv.getArgument(0);
            salvo.setId(10L);
            return salvo;
        });
        when(fichaTecnicaService.custosPorProduto(anyCollection())).thenReturn(Map.of());

        AdminProdutoResponse resposta = service.criar(new ProdutoRequest(
                "Bruschetta", "Pão e tomate", new BigDecimal("29.90"), "Entradas",
                null, false, 10, null, "Tostar o pão", null));

        assertEquals("paused", resposta.getStatus());
        assertEquals(false, resposta.getHasTechnicalSheet());
        assertEquals("Tostar o pão", resposta.getPreparationMethod());
    }

    @Test
    void excluirDeixaOPratoInativo() {
        Produto produto = produto(StatusPrato.ATIVO, StatusCategoria.ATIVO);
        when(produtoRepository.findWithCategoriaById(1L)).thenReturn(Optional.of(produto));

        service.excluir(1L);

        assertEquals(StatusPrato.INATIVO, produto.getStatus());
        verify(produtoRepository).save(produto);
    }

    private static Produto produto(StatusPrato status, StatusCategoria statusCategoria) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Picanha");
        produto.setPreco(new BigDecimal("148.00"));
        produto.setCategoria(categoria(statusCategoria));
        produto.setStatus(status);
        return produto;
    }

    private static Categoria categoria(StatusCategoria status) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Entradas");
        categoria.setStatus(status);
        return categoria;
    }
}
