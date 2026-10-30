package com.maka.api.ingrediente;

import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.ingrediente.dto.IngredienteRequest;
import com.maka.api.ingrediente.dto.IngredienteResponse;
import com.maka.api.model.Ingrediente;
import com.maka.api.repository.IngredienteRepository;

@Service
public class IngredienteService {

    private final IngredienteRepository ingredienteRepository;

    public IngredienteService(IngredienteRepository ingredienteRepository) {
        this.ingredienteRepository = ingredienteRepository;
    }

    @Transactional(readOnly = true)
    public Page<IngredienteResponse> listar(Pageable pageable) {
        return ingredienteRepository.findAll(pageable).map(IngredienteResponse::from);
    }

    public IngredienteResponse criar(IngredienteRequest request) {
        Ingrediente ingrediente = new Ingrediente();
        aplicar(ingrediente, request);
        return IngredienteResponse.from(ingredienteRepository.save(ingrediente));
    }

    public IngredienteResponse atualizar(Long id, IngredienteRequest request) {
        Ingrediente ingrediente = buscar(id);
        aplicar(ingrediente, request);
        return IngredienteResponse.from(ingredienteRepository.save(ingrediente));
    }

    public void excluir(Long id) {
        Ingrediente ingrediente = buscar(id);
        ingredienteRepository.delete(ingrediente);
    }

    private Ingrediente buscar(Long id) {
        return ingredienteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ingrediente não encontrado: " + id));
    }

    private void aplicar(Ingrediente ingrediente, IngredienteRequest request) {
        ingrediente.setNome(request.nome().trim());
        ingrediente.setUnidadeMedida(request.unidadeMedida().trim());
        ingrediente.setCustoUnitario(request.custoUnitario());
    }
}
