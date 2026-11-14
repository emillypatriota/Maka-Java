package com.maka.api.historicopreco;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.historicopreco.dto.HistoricoPrecoResponse;
import com.maka.api.repository.HistoricoPrecoRepository;
import com.maka.api.repository.IngredienteRepository;

@Service
public class HistoricoPrecoService {

    private final HistoricoPrecoRepository historicoPrecoRepository;
    private final IngredienteRepository ingredienteRepository;

    public HistoricoPrecoService(HistoricoPrecoRepository historicoPrecoRepository,
            IngredienteRepository ingredienteRepository) {
        this.historicoPrecoRepository = historicoPrecoRepository;
        this.ingredienteRepository = ingredienteRepository;
    }

    @Transactional(readOnly = true)
    public List<HistoricoPrecoResponse> listar(Long ingredienteId, Long fornecedorId) {
        if (!ingredienteRepository.existsById(ingredienteId)) {
            throw new NoSuchElementException("Ingrediente não encontrado: " + ingredienteId);
        }
        return historicoPrecoRepository.findByIngredienteIdOrderByRegistradoEmAsc(ingredienteId).stream()
                .filter(h -> fornecedorId == null || h.getFornecedor().getId().equals(fornecedorId))
                .map(h -> new HistoricoPrecoResponse(
                        h.getFornecedor().getId(),
                        h.getFornecedor().getRazaoSocial(),
                        h.getPreco(),
                        h.getRegistradoEm()))
                .toList();
    }
}
