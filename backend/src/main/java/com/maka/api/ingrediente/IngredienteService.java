package com.maka.api.ingrediente;

import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.exception.ConflitoException;
import com.maka.api.ingrediente.dto.IngredienteRequest;
import com.maka.api.ingrediente.dto.IngredienteResponse;
import com.maka.api.model.Ingrediente;
import com.maka.api.repository.IngredienteRepository;
import com.maka.api.model.StatusIngrediente;

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
        validarSku(request.sku(), null);
        aplicar(ingrediente, request, true);
        return IngredienteResponse.from(ingredienteRepository.save(ingrediente));
    }

    public IngredienteResponse atualizar(Long id, IngredienteRequest request) {
        Ingrediente ingrediente = buscar(id);
        validarSku(request.sku(), id);
        aplicar(ingrediente, request, false);
        return IngredienteResponse.from(ingredienteRepository.save(ingrediente));
    }

    public void excluir(Long id) {
        Ingrediente ingrediente = buscar(id);
        ingrediente.setStatus(StatusIngrediente.INATIVO);
        ingredienteRepository.save(ingrediente);
    }

    private Ingrediente buscar(Long id) {
        return ingredienteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ingrediente não encontrado: " + id));
    }

    private void aplicar(Ingrediente ingrediente, IngredienteRequest request, boolean novo) {
        ingrediente.setNome(request.nome().trim());
        ingrediente.setUnidadeMedida(request.unidadeMedida().trim());
        ingrediente.setCustoUnitario(request.custoUnitario());
        if (novo || request.sku() != null) {
            ingrediente.setSku(request.sku() == null || request.sku().isBlank() ? null : request.sku().trim());
        }
        if (novo || request.estoqueMinimo() != null) {
            ingrediente.setEstoqueMinimo(request.estoqueMinimo() == null
                    ? java.math.BigDecimal.ZERO : request.estoqueMinimo());
        }
        if (request.status() != null) {
            ingrediente.setStatus(request.status());
        } else if (novo) {
            ingrediente.setStatus(StatusIngrediente.ATIVO);
        }
    }

    private void validarSku(String sku, Long id) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        boolean existe = id == null
                ? ingredienteRepository.existsBySkuIgnoreCase(sku.trim())
                : ingredienteRepository.existsBySkuIgnoreCaseAndIdNot(sku.trim(), id);
        if (existe) {
            throw new ConflitoException("Já existe um ingrediente com este SKU.");
        }
    }
}
