package com.maka.api.fornecedor;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * RF-021: CRUD de fornecedores com CNPJ válido e único (RN07) e exclusão lógica (RN06).
 * RF-022: catálogo de cada fornecedor (ingrediente, preço e unidade de venda).
 * Toda vez que um preço entra ou muda no catálogo, grava uma linha em historico_preco (RF-026).
 */
@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final FornecedorProdutoRepository catalogoRepository;
    private final HistoricoPrecoRepository historicoPrecoRepository;
    private final IngredienteRepository ingredienteRepository;

    public FornecedorService(
            FornecedorRepository fornecedorRepository,
            FornecedorProdutoRepository catalogoRepository,
            HistoricoPrecoRepository historicoPrecoRepository,
            IngredienteRepository ingredienteRepository) {
        this.fornecedorRepository = fornecedorRepository;
        this.catalogoRepository = catalogoRepository;
        this.historicoPrecoRepository = historicoPrecoRepository;
        this.ingredienteRepository = ingredienteRepository;
    }

    // ── Fornecedores (RF-021) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listar(String busca, StatusFornecedor status, Pageable pageable) {
        String termo = busca == null || busca.isBlank() ? null : busca.trim();
        return fornecedorRepository.buscar(termo, status, pageable).map(FornecedorResponse::from);
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponse> listarAtivos() {
        return fornecedorRepository.findByStatusOrderByRazaoSocialAsc(StatusFornecedor.ATIVO).stream()
                .map(FornecedorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscar(Long id) {
        return FornecedorResponse.from(buscarFornecedor(id));
    }

    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        String cnpj = Cnpj.normalizar(request.cnpj());
        if (fornecedorRepository.existsByCnpj(cnpj)) {
            throw new ConflitoException("Já existe um fornecedor com este CNPJ");
        }
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setCnpj(cnpj);
        aplicar(fornecedor, request);
        fornecedor.setStatus(request.status() == null ? StatusFornecedor.ATIVO : request.status());
        return FornecedorResponse.from(fornecedorRepository.save(fornecedor));
    }

    @Transactional
    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = buscarFornecedor(id);
        String cnpj = Cnpj.normalizar(request.cnpj());
        if (fornecedorRepository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new ConflitoException("Já existe um fornecedor com este CNPJ");
        }
        fornecedor.setCnpj(cnpj);
        aplicar(fornecedor, request);
        if (request.status() != null) {
            fornecedor.setStatus(request.status());
        }
        return FornecedorResponse.from(fornecedorRepository.save(fornecedor));
    }

    // RN06: excluir é marcar INATIVO. Inativo some da cotação e não recebe pedido de compra novo.
    @Transactional
    public FornecedorResponse alterarStatus(Long id, StatusFornecedor status) {
        Fornecedor fornecedor = buscarFornecedor(id);
        fornecedor.setStatus(status);
        return FornecedorResponse.from(fornecedorRepository.save(fornecedor));
    }

    // ── Catálogo (RF-022) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CatalogoItemResponse> catalogo(Long fornecedorId) {
        buscarFornecedor(fornecedorId);
        return catalogoRepository.findByFornecedorIdOrderByIngredienteNomeAsc(fornecedorId).stream()
                .map(CatalogoItemResponse::from)
                .toList();
    }

    @Transactional
    public CatalogoItemResponse adicionarAoCatalogo(Long fornecedorId, CatalogoItemRequest request) {
        Fornecedor fornecedor = buscarFornecedor(fornecedorId);
        Ingrediente ingrediente = ingredienteRepository.findById(request.ingredienteId())
                .orElseThrow(() -> new NoSuchElementException("Ingrediente não encontrado: " + request.ingredienteId()));
        if (ingrediente.getStatus() != StatusIngrediente.ATIVO) {
            throw new RegraNegocioException("O ingrediente " + ingrediente.getNome() + " está inativo");
        }
        if (catalogoRepository.findByFornecedorIdAndIngredienteId(fornecedorId, ingrediente.getId()).isPresent()) {
            throw new ConflitoException("Este ingrediente já está no catálogo do fornecedor. Edite o preço em vez de cadastrar de novo");
        }

        FornecedorProduto item = new FornecedorProduto();
        item.setFornecedor(fornecedor);
        item.setIngrediente(ingrediente);
        item.setUnidadeVenda(PrecoUnidade.normalizarUnidadeVenda(request.unidadeVenda(), ingrediente.getUnidadeMedida()));
        item.setPreco(request.preco());
        item = catalogoRepository.save(item);

        registrarHistorico(item);
        return CatalogoItemResponse.from(item);
    }

    @Transactional
    public CatalogoItemResponse atualizarItemCatalogo(Long fornecedorId, Long itemId, CatalogoItemRequest request) {
        FornecedorProduto item = buscarItem(fornecedorId, itemId);
        Ingrediente ingrediente = item.getIngrediente();

        String unidadeVenda = PrecoUnidade.normalizarUnidadeVenda(request.unidadeVenda(), ingrediente.getUnidadeMedida());
        BigDecimal precoAntes = precoConvertidoOuNull(item);

        item.setUnidadeVenda(unidadeVenda);
        item.setPreco(request.preco());
        item = catalogoRepository.save(item);

        // Só entra no histórico se o preço (na unidade do ingrediente) mudou de verdade
        BigDecimal precoDepois = PrecoUnidade.precoNaUnidadeDoIngrediente(
                item.getPreco(), item.getUnidadeVenda(), ingrediente.getUnidadeMedida());
        if (precoAntes == null || precoAntes.compareTo(precoDepois) != 0) {
            registrarHistorico(item);
        }
        return CatalogoItemResponse.from(item);
    }

    // Tirar um item do catálogo apaga só a oferta: o histórico de preços continua
    @Transactional
    public void removerDoCatalogo(Long fornecedorId, Long itemId) {
        catalogoRepository.delete(buscarItem(fornecedorId, itemId));
    }

    // ── Apoio ─────────────────────────────────────────────────────────

    private Fornecedor buscarFornecedor(Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fornecedor não encontrado: " + id));
    }

    private FornecedorProduto buscarItem(Long fornecedorId, Long itemId) {
        FornecedorProduto item = catalogoRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item do catálogo não encontrado: " + itemId));
        if (!item.getFornecedor().getId().equals(fornecedorId)) {
            throw new NoSuchElementException("Item do catálogo não encontrado: " + itemId);
        }
        return item;
    }

    private void aplicar(Fornecedor fornecedor, FornecedorRequest request) {
        fornecedor.setRazaoSocial(request.razaoSocial().trim());
        fornecedor.setTelefone(vazioParaNull(request.telefone()));
        fornecedor.setEmail(request.email() == null || request.email().isBlank()
                ? null : request.email().trim().toLowerCase());
        fornecedor.setCategorias(vazioParaNull(request.categorias()));
    }

    // O histórico guarda o preço na unidade do ingrediente, para o gráfico do RF-026
    // comparar fornecedores que vendem em unidades diferentes (kg x g)
    private void registrarHistorico(FornecedorProduto item) {
        HistoricoPreco historico = new HistoricoPreco();
        historico.setFornecedor(item.getFornecedor());
        historico.setIngrediente(item.getIngrediente());
        historico.setPreco(PrecoUnidade.precoNaUnidadeDoIngrediente(
                item.getPreco(), item.getUnidadeVenda(), item.getIngrediente().getUnidadeMedida()));
        historicoPrecoRepository.save(historico);
    }

    // Preço atual convertido, ou null se a unidade antiga não combina mais com o ingrediente
    private static BigDecimal precoConvertidoOuNull(FornecedorProduto item) {
        try {
            return PrecoUnidade.precoNaUnidadeDoIngrediente(
                    item.getPreco(), item.getUnidadeVenda(), item.getIngrediente().getUnidadeMedida());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String vazioParaNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
