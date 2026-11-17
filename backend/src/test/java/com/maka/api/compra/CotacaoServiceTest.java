package com.maka.api.compra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maka.api.compra.dto.CotacaoResponse;
import com.maka.api.model.Fornecedor;
import com.maka.api.model.FornecedorProduto;
import com.maka.api.model.Ingrediente;
import com.maka.api.model.StatusFornecedor;
import com.maka.api.model.StatusIngrediente;
import com.maka.api.repository.FornecedorProdutoRepository;

// RF-023: cotação comparativa
@ExtendWith(MockitoExtension.class)
class CotacaoServiceTest {

    @Mock private FornecedorProdutoRepository catalogoRepository;
    @InjectMocks private CotacaoService cotacaoService;

    private Ingrediente salmao() {
        Ingrediente i = new Ingrediente();
        i.setId(3L);
        i.setNome("Salmão fresco");
        i.setUnidadeMedida("g");
        i.setCustoUnitario(new BigDecimal("0.0950"));
        i.setStatus(StatusIngrediente.ATIVO);
        return i;
    }

    private FornecedorProduto oferta(Long fornecedorId, StatusFornecedor status, Ingrediente i, String preco, String unidade) {
        Fornecedor f = new Fornecedor();
        f.setId(fornecedorId);
        f.setRazaoSocial("Fornecedor " + fornecedorId);
        f.setStatus(status);
        FornecedorProduto o = new FornecedorProduto();
        o.setId(fornecedorId * 100);
        o.setFornecedor(f);
        o.setIngrediente(i);
        o.setPreco(new BigDecimal(preco));
        o.setUnidadeVenda(unidade);
        return o;
    }

    @Test
    void ordenaDoMaisBaratoParaOMaisCaroEMarcaAMelhorOferta() {
        Ingrediente salmao = salmao();
        List<Long> ids = List.of(3L);
        when(catalogoRepository.findByIngredienteIdIn(ids)).thenReturn(List.of(
                oferta(2L, StatusFornecedor.ATIVO, salmao, "102.00", "kg"),     // 0,1020/g
                oferta(1L, StatusFornecedor.ATIVO, salmao, "0.0950", "g"),      // 0,0950/g
                oferta(9L, StatusFornecedor.INATIVO, salmao, "0.0100", "g")));  // inativo: fica de fora

        List<CotacaoResponse> cotacao = cotacaoService.cotar(ids);

        assertEquals(1, cotacao.size());
        CotacaoResponse c = cotacao.get(0);
        assertEquals(1L, c.melhorFornecedorId());
        assertEquals(2, c.ofertas().size());
        assertEquals(1L, c.ofertas().get(0).fornecedorId());
        assertTrue(c.ofertas().get(0).melhorPreco());
        assertEquals(0, c.ofertas().get(0).diferencaPercentual().compareTo(BigDecimal.ZERO));
        assertFalse(c.ofertas().get(1).melhorPreco());
        assertEquals(0, c.ofertas().get(1).diferencaPercentual().compareTo(new BigDecimal("7.4")));
    }

    @Test
    void semFiltroComparaTodosOsIngredientes() {
        when(catalogoRepository.findAllByOrderByIngredienteNomeAsc()).thenReturn(List.of());

        assertTrue(cotacaoService.cotar(null).isEmpty());
        verify(catalogoRepository, never()).findByIngredienteIdIn(any());
    }
}
