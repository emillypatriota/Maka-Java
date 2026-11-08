package com.maka.api.fornecedor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.fornecedor.dto.FornecedorRequest;
import com.maka.api.fornecedor.dto.FornecedorResponse;
import com.maka.api.model.Fornecedor;
import com.maka.api.model.StatusFornecedor;
import com.maka.api.repository.FornecedorRepository;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock private FornecedorRepository fornecedorRepository;
    @InjectMocks private FornecedorService fornecedorService;

    private FornecedorRequest req(String cnpj) {
        return new FornecedorRequest("Casa de Carnes", cnpj, "", null, "Carnes");
    }

    @Test
    void cnpjInvalidoDevolve422() {
        assertThrows(RegraNegocioException.class, () -> fornecedorService.criar(req("11.222.333/0001-82")));
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void cnpjDuplicadoDevolve409() {
        when(fornecedorRepository.existsByCnpj("11.222.333/0001-81")).thenReturn(true);
        assertThrows(ConflitoException.class, () -> fornecedorService.criar(req("11222333000181")));
    }

    @Test
    void salvaCnpjFormatadoETelefoneVazioViraNulo() {
        when(fornecedorRepository.existsByCnpj("11.222.333/0001-81")).thenReturn(false);
        when(fornecedorRepository.save(any(Fornecedor.class))).thenAnswer(inv -> inv.getArgument(0));

        FornecedorResponse resp = fornecedorService.criar(req("11222333000181"));

        assertEquals("11.222.333/0001-81", resp.cnpj());
        assertNull(resp.telefone());
        assertEquals("ATIVO", resp.status());
    }

    @Test
    void exclusaoELogica() {
        Fornecedor f = new Fornecedor();
        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(f));
        when(fornecedorRepository.save(any(Fornecedor.class))).thenAnswer(inv -> inv.getArgument(0));

        fornecedorService.alterarStatus(1L, StatusFornecedor.INATIVO);

        assertEquals(StatusFornecedor.INATIVO, f.getStatus());
        verify(fornecedorRepository, never()).delete(any());
    }
}
