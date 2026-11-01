package com.maka.api.produto;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.categoria.CategoriaService;
import com.maka.api.fichatecnica.CustoPratoService.CustoPrato;
import com.maka.api.fichatecnica.FichaTecnicaService;
import com.maka.api.model.Produto;
import com.maka.api.model.StatusCategoria;
import com.maka.api.model.StatusPrato;
import com.maka.api.produto.dto.AdminProdutoResponse;
import com.maka.api.produto.dto.ProdutoRequest;
import com.maka.api.produto.dto.ProdutoResponse;
import com.maka.api.produto.dto.ResumoCardapioResponse;
import com.maka.api.produto.dto.StatusPratoJson;
import com.maka.api.repository.ProdutoRepository;

// Os métodos que devolvem DTO precisam de transação: categoria e tags são LAZY
@Service
public class ProdutoService {

    private static final Sort ORDEM_CARDAPIO = Sort.by("categoria.ordem", "id");

    private final ProdutoRepository produtoRepository;
    private final CategoriaService categoriaService;
    private final FichaTecnicaService fichaTecnicaService;

    public ProdutoService(
            ProdutoRepository produtoRepository,
            CategoriaService categoriaService,
            FichaTecnicaService fichaTecnicaService) {
        this.produtoRepository = produtoRepository;
        this.categoriaService = categoriaService;
        this.fichaTecnicaService = fichaTecnicaService;
    }

    // Cardápio público (RN09): só prato ATIVO de categoria ATIVA, com filtro opcional por categoria
    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarAtivos(String categoria) {
        List<Produto> produtos = temTexto(categoria)
                ? produtoRepository.findByStatusAndCategoriaStatusAndCategoriaNome(
                        StatusPrato.ATIVO, StatusCategoria.ATIVO, categoria.trim(), ORDEM_CARDAPIO)
                : produtoRepository.findByStatusAndCategoriaStatus(StatusPrato.ATIVO, StatusCategoria.ATIVO, ORDEM_CARDAPIO);
        return produtos.stream().map(ProdutoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarAtivo(Long id) {
        return produtoRepository.findByIdAndStatusAndCategoriaStatus(id, StatusPrato.ATIVO, StatusCategoria.ATIVO)
                .map(ProdutoResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Prato não encontrado: " + id));
    }

    // Gestão: todos os status, paginado, com custo e food cost de cada prato
    @Transactional(readOnly = true)
    public Page<AdminProdutoResponse> listar(String categoria, String status, String busca, Pageable pageable) {
        Page<Produto> produtos = produtoRepository.buscarAdmin(
                temTexto(categoria) ? categoria.trim() : null,
                StatusPratoJson.fromJson(status),
                temTexto(busca) ? busca.trim() : null,
                pageable);
        Map<Long, CustoPrato> custos = fichaTecnicaService.custosPorProduto(produtos.getContent());
        return produtos.map(produto -> AdminProdutoResponse.from(produto, custos.get(produto.getId())));
    }

    @Transactional(readOnly = true)
    public ResumoCardapioResponse resumo() {
        long ativos = produtoRepository.countByStatus(StatusPrato.ATIVO);
        long pausados = produtoRepository.countByStatus(StatusPrato.PAUSADO);
        long inativos = produtoRepository.countByStatus(StatusPrato.INATIVO);
        return new ResumoCardapioResponse(ativos + pausados + inativos, ativos, pausados, inativos);
    }

    @Transactional(readOnly = true)
    public AdminProdutoResponse buscar(Long id) {
        return paraAdmin(buscarEntidade(id));
    }

    // Prato novo nasce PAUSADO: só fica ATIVO depois de ter ficha técnica (RN01)
    @Transactional
    public AdminProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto();
        aplicar(produto, request);
        produto.setStatus(StatusPrato.PAUSADO);
        return paraAdmin(produtoRepository.save(produto));
    }

    @Transactional
    public AdminProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidade(id);
        aplicar(produto, request);
        return paraAdmin(produtoRepository.save(produto));
    }

    @Transactional
    public AdminProdutoResponse alterarStatus(Long id, String statusJson) {
        Produto produto = buscarEntidade(id);
        StatusPrato status = StatusPratoJson.fromJson(statusJson);
        if (status == StatusPrato.ATIVO) {
            if (!fichaTecnicaService.temFicha(id)) {
                throw new IllegalArgumentException("Monte a ficha técnica antes de ativar o prato (RN01)");
            }
            if (produto.getCategoria().getStatus() != StatusCategoria.ATIVO) {
                throw new IllegalArgumentException(
                        "A categoria " + produto.getCategoria().getNome() + " está inativa: reative-a antes do prato");
            }
        }
        produto.setStatus(status);
        return paraAdmin(produtoRepository.save(produto));
    }

    // Exclusão lógica (RN06): o prato pode estar em pedidos antigos, então não sai do banco
    @Transactional
    public void excluir(Long id) {
        Produto produto = buscarEntidade(id);
        produto.setStatus(StatusPrato.INATIVO);
        produtoRepository.save(produto);
    }

    private AdminProdutoResponse paraAdmin(Produto produto) {
        Map<Long, CustoPrato> custos = fichaTecnicaService.custosPorProduto(List.of(produto));
        return AdminProdutoResponse.from(produto, custos.get(produto.getId()));
    }

    private Produto buscarEntidade(Long id) {
        return produtoRepository.findWithCategoriaById(id)
                .orElseThrow(() -> new NoSuchElementException("Prato não encontrado: " + id));
    }

    private void aplicar(Produto produto, ProdutoRequest request) {
        produto.setNome(request.name().trim());
        produto.setDescricao(request.description() == null ? null : request.description().trim());
        produto.setPreco(request.price());
        // Só exige categoria ativa quando ela muda: assim dá para editar prato de categoria desativada
        boolean mesmaCategoria = produto.getCategoria() != null
                && produto.getCategoria().getNome().equalsIgnoreCase(request.category().trim());
        if (!mesmaCategoria) {
            produto.setCategoria(categoriaService.buscarAtivaPorNome(request.category()));
        }
        produto.setImagem(temTexto(request.image()) ? request.image().trim() : null);
        produto.setDestaque(Boolean.TRUE.equals(request.isSpecial()));
        produto.setTempoPreparo(request.prepTime());
        // A tela de gestão não edita calorias nem tags: se não vierem, mantém o que já existe
        if (request.calories() != null) {
            produto.setCalorias(request.calories());
        }
        if (request.tags() != null) {
            produto.getTags().clear();
            request.tags().forEach(tag -> produto.getTags().add(tag.trim()));
        }
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
