package com.maka.api.fornecedor;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.exception.ConflitoException;
import com.maka.api.exception.RegraNegocioException;
import com.maka.api.fornecedor.dto.FornecedorRequest;
import com.maka.api.fornecedor.dto.FornecedorResponse;
import com.maka.api.model.Fornecedor;
import com.maka.api.model.StatusFornecedor;
import com.maka.api.repository.FornecedorRepository;

import jakarta.persistence.criteria.Predicate;

/** RF-021: CRUD de fornecedores. RN07: CNPJ válido e único. RN06: exclusão lógica (INATIVO). */
@Service
@Transactional
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listar(String busca, StatusFornecedor status, Pageable pageable) {
        Specification<Fornecedor> spec = (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (status != null) {
                filtros.add(cb.equal(root.get("status"), status));
            }
            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                filtros.add(cb.or(
                        cb.like(cb.lower(root.get("razaoSocial")), termo),
                        cb.like(root.get("cnpj"), "%" + busca.trim() + "%")));
            }
            return cb.and(filtros.toArray(new Predicate[0]));
        };
        return fornecedorRepository.findAll(spec, pageable).map(FornecedorResponse::from);
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorId(Long id) {
        return FornecedorResponse.from(buscar(id));
    }

    public FornecedorResponse criar(FornecedorRequest request) {
        String cnpj = cnpjValidado(request.cnpj());
        if (fornecedorRepository.existsByCnpj(cnpj)) {
            throw new ConflitoException("Já existe um fornecedor com este CNPJ");
        }
        Fornecedor fornecedor = new Fornecedor();
        aplicar(fornecedor, request, cnpj);
        return FornecedorResponse.from(fornecedorRepository.save(fornecedor));
    }

    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = buscar(id);
        String cnpj = cnpjValidado(request.cnpj());
        if (fornecedorRepository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new ConflitoException("Já existe um fornecedor com este CNPJ");
        }
        aplicar(fornecedor, request, cnpj);
        return FornecedorResponse.from(fornecedorRepository.save(fornecedor));
    }

    /** RN06: nunca apaga do banco; só marca ATIVO/INATIVO (histórico de compras continua íntegro). */
    public FornecedorResponse alterarStatus(Long id, StatusFornecedor status) {
        Fornecedor fornecedor = buscar(id);
        fornecedor.setStatus(status);
        return FornecedorResponse.from(fornecedorRepository.save(fornecedor));
    }

    private Fornecedor buscar(Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fornecedor não encontrado: " + id));
    }

    private String cnpjValidado(String cnpj) {
        if (!Cnpj.valido(cnpj)) {
            throw new RegraNegocioException("CNPJ inválido");
        }
        return Cnpj.formatar(cnpj);
    }

    private void aplicar(Fornecedor f, FornecedorRequest r, String cnpj) {
        f.setRazaoSocial(r.razaoSocial().trim());
        f.setCnpj(cnpj);
        f.setTelefone(vazioParaNulo(r.telefone()));
        f.setEmail(vazioParaNulo(r.email()));
        f.setCategorias(vazioParaNulo(r.categorias()));
    }

    private String vazioParaNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
