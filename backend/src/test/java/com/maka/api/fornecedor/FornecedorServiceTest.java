package com.maka.api.fornecedor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.fornecedor.dto.CatalogoItemRequest;
import com.maka.api.fornecedor.dto.CatalogoItemResponse;
import com.maka.api.fornecedor.dto.FornecedorRequest;
import com.maka.api.fornecedor.dto.FornecedorResponse;
import com.maka.api.model.Fornecedor;
import com.maka.api.model.FornecedorProduto;
import com.maka.api.model.HistoricoPreco;
import com.maka.api.model.Ingrediente;
import com.maka.api.model.StatusFornecedor;
import com.maka.api.model.StatusIngrediente;
import com.maka.api.repository.FornecedorProdutoRepository;
import com.maka.api.repository.FornecedorRepository;
import com.maka.api.repository.HistoricoPrecoRepository;
import com.maka.api.repository.IngredienteRepository;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    private static final String CNPJ = "11.222.333/0001-81";

    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private FornecedorProdutoRepository catalogoRepository;
    @Mock private HistoricoPrecoRepository historicoPrecoRepository;
    @Mock private IngredienteRepository ingredienteRepository;
    @InjectMocks private FornecedorService fornecedorService;

    private FornecedorRequest request(String cnpj) {
        return new FornecedorRequest("Casa de Carnes Prime", cnpj, "(11) 3000-1111", " Vendas@Prime.com ", "Carnes", null);
    }

    private Fornecedor fornecedor(Long id) {
        Fornecedor f = new Fornecedor();
        f.setId(id);
        f.setRazaoSocial("Casa de Carnes Prime");
        f.setCnpj(CNPJ);
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

    @Test
    void criaFornecedorComCnpjFormatadoEAtivo() {
        when(fornecedorRepository.existsByCnpj(CNPJ)).thenReturn(false);
        when(fornecedorRepository.save(any(Fornecedor.class))).thenAnswer(inv -> inv.getArgument(0));

        FornecedorResponse resp = fornecedorService.criar(request("11222333000181"));

        assertEquals(CNPJ, resp.cnpj());
        assertEquals(StatusFornecedor.ATIVO, resp.status());
        assertEquals("vendas@prime.com", resp.email());
    }

    @Test
    void cnpjInvalidoNaoSalva() {
        assertThrows(IllegalArgumentException.class, () -> fornecedorService.criar(request("11.222.333/0001-00")));
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void cnpjRepetidoDevolveConflito() {
        when(fornecedorRepository.existsByCnpj(CNPJ)).thenReturn(true);

        assertThrows(ConflitoException.class, () -> fornecedorService.criar(request(CNPJ)));
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void atualizarComCnpjDeOutroFornecedorDevolveConflito() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor(1L)));
        when(fornecedorRepository.existsByCnpjAndIdNot(CNPJ, 1L)).thenReturn(true);

        assertThrows(ConflitoException.class, () -> fornecedorService.atualizar(1L, request(CNPJ)));
    }

    @Test
    void itemNovoNoCatalogoGravaHistoricoNaUnidadeDoIngrediente() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor(1L)));
        when(ingredienteRepository.findById(10L)).thenReturn(Optional.of(picanha(StatusIngrediente.ATIVO)));
        when(catalogoRepository.findByFornecedorIdAndIngredienteId(1L, 10L)).thenReturn(Optional.empty());
        when(catalogoRepository.save(any(FornecedorProduto.class))).thenAnswer(inv -> {
            FornecedorProduto item = inv.getArgument(0);
            item.setId(100L);
            return item;
        });

        CatalogoItemResponse resp = fornecedorService.adicionarAoCatalogo(1L,
                new CatalogoItemRequest(10L, new BigDecimal("78.00"), "KG"));

        assertEquals("kg", resp.unidadeVenda());
        assertEquals(0, resp.precoPorUnidadeIngrediente().compareTo(new BigDecimal("0.0780")));
        ArgumentCaptor<HistoricoPreco> historico = ArgumentCaptor.forClass(HistoricoPreco.class);
        verify(historicoPrecoRepository).save(historico.capture());
        assertEquals(0, historico.getValue().getPreco().compareTo(new BigDecimal("0.0780")));
    }

    @Test
    void ingredienteRepetidoNoCatalogoDevolveConflito() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor(1L)));
        when(ingredienteRepository.findById(10L)).thenReturn(Optional.of(picanha(StatusIngrediente.ATIVO)));
        when(catalogoRepository.findByFornecedorIdAndIngredienteId(1L, 10L)).thenReturn(Optional.of(new FornecedorProduto()));

        assertThrows(ConflitoException.class, () -> fornecedorService.adicionarAoCatalogo(1L,
                new CatalogoItemRequest(10L, new BigDecimal("78.00"), "kg")));
    }

    @Test
    void ingredienteInativoNaoEntraNoCatalogo() {
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedor(1L)));
        when(ingredienteRepository.findById(10L)).thenReturn(Optional.of(picanha(StatusIngrediente.INATIVO)));

        assertThrows(RegraNegocioException.class, () -> fornecedorService.adicionarAoCatalogo(1L,
                new CatalogoItemRequest(10L, new BigDecimal("78.00"), "kg")));
        verify(catalogoRepository, never()).save(any());
    }

    @Test
    void mesmoPrecoEmOutraUnidadeNaoDuplicaHistorico() {
        FornecedorProduto item = new FornecedorProduto();
        item.setId(100L);
        item.setFornecedor(fornecedor(1L));
        item.setIngrediente(picanha(StatusIngrediente.ATIVO));
        item.setPreco(new BigDecimal("0.0780"));
        item.setUnidadeVenda("g");
        when(catalogoRepository.findById(100L)).thenReturn(Optional.of(item));
        when(catalogoRepository.save(any(FornecedorProduto.class))).thenAnswer(inv -> inv.getArgument(0));

        // R$ 78,00/kg é o mesmo que R$ 0,0780/g
        fornecedorService.atualizarItemCatalogo(1L, 100L, new CatalogoItemRequest(10L, new BigDecimal("78.00"), "kg"));

        verify(historicoPrecoRepository, never()).save(any());
    }

    @Test
    void itemDeOutroFornecedorDa404() {
        FornecedorProduto item = new FornecedorProduto();
        item.setId(100L);
        item.setFornecedor(fornecedor(2L));
        when(catalogoRepository.findById(100L)).thenReturn(Optional.of(item));

        assertThrows(NoSuchElementException.class, () -> fornecedorService.removerDoCatalogo(1L, 100L));
        verify(catalogoRepository, never()).delete(any());
    }
}
