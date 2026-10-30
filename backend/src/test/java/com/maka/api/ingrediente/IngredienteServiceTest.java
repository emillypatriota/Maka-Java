package com.maka.api.ingrediente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maka.api.ingrediente.dto.IngredienteRequest;
import com.maka.api.model.Ingrediente;
import com.maka.api.repository.IngredienteRepository;

@ExtendWith(MockitoExtension.class)
class IngredienteServiceTest {

    @Mock
    private IngredienteRepository ingredienteRepository;

    @Test
    void deveCriarIngredienteComCamposNormalizados() {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId(7L);
        when(ingredienteRepository.save(org.mockito.ArgumentMatchers.any(Ingrediente.class)))
                .thenAnswer(invocation -> {
                    Ingrediente salvo = invocation.getArgument(0);
                    salvo.setId(7L);
                    return salvo;
                });

        var resposta = new IngredienteService(ingredienteRepository).criar(
                new IngredienteRequest("  Farinha  ", " kg ", new BigDecimal("2.5000")));

        assertEquals(7L, resposta.id());
        assertEquals("Farinha", resposta.nome());
        assertEquals("kg", resposta.unidadeMedida());
        assertEquals(new BigDecimal("2.5000"), resposta.custoUnitario());
    }

    @Test
    void deveAtualizarIngredienteExistente() {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId(3L);
        when(ingredienteRepository.findById(3L)).thenReturn(Optional.of(ingrediente));
        when(ingredienteRepository.save(ingrediente)).thenReturn(ingrediente);

        var resposta = new IngredienteService(ingredienteRepository).atualizar(
                3L, new IngredienteRequest("Tomate", "kg", new BigDecimal("6.75")));

        assertEquals("Tomate", resposta.nome());
        assertEquals(new BigDecimal("6.75"), resposta.custoUnitario());
    }

    @Test
    void deveRetornarNotFoundAoExcluirIngredienteInexistente() {
        when(ingredienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> new IngredienteService(ingredienteRepository).excluir(99L));
        verify(ingredienteRepository).findById(99L);
    }
}
