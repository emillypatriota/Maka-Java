package com.maka.api.estoque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maka.api.estoque.dto.MovimentacaoRequest;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.model.EstoqueMovimentacao;
import com.maka.api.model.Ingrediente;
import com.maka.api.model.MotivoMovimentacao;
import com.maka.api.model.StatusIngrediente;
import com.maka.api.model.TipoMovimentacao;
import com.maka.api.repository.EstoqueMovimentacaoRepository;
import com.maka.api.repository.IngredienteRepository;
import com.maka.api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private IngredienteRepository ingredienteRepository;
    @Mock
    private EstoqueMovimentacaoRepository movimentacaoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void deveRegistrarEntradaManualComoAjuste() {
        Ingrediente ingrediente = ingredienteAtivo();
        when(ingredienteRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(ingrediente));
        when(movimentacaoRepository.save(any(EstoqueMovimentacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service().entrada(
                new MovimentacaoRequest(4L, new BigDecimal("2.500"), null), "manager@maka.com");

        assertEquals(TipoMovimentacao.ENTRADA, resposta.tipo());
        assertEquals(MotivoMovimentacao.AJUSTE, resposta.motivo());
        assertEquals(new BigDecimal("2.500"), resposta.quantidade());
    }

    @Test
    void deveImpedirSaidaMaiorQueSaldo() {
        Ingrediente ingrediente = ingredienteAtivo();
        when(ingredienteRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(ingrediente));
        when(movimentacaoRepository.calcularSaldo(4L)).thenReturn(new BigDecimal("1.000"));

        assertThrows(RegraNegocioException.class, () -> service().saida(
                new MovimentacaoRequest(4L, new BigDecimal("1.001"), MotivoMovimentacao.QUEBRA),
                "manager@maka.com"));

        verify(movimentacaoRepository, never()).save(any(EstoqueMovimentacao.class));
    }

    @Test
    void deveRejeitarMotivoDeSaidaQueNaoSejaManualPermitido() {
        Ingrediente ingrediente = ingredienteAtivo();
        when(ingredienteRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(ingrediente));

        assertThrows(RegraNegocioException.class, () -> service().saida(
                new MovimentacaoRequest(4L, BigDecimal.ONE, MotivoMovimentacao.COMPRA),
                "manager@maka.com"));

        verify(movimentacaoRepository, never()).calcularSaldo(4L);
    }

    private EstoqueService service() {
        return new EstoqueService(ingredienteRepository, movimentacaoRepository, usuarioRepository);
    }

    private Ingrediente ingredienteAtivo() {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId(4L);
        ingrediente.setStatus(StatusIngrediente.ATIVO);
        return ingrediente;
    }
}
