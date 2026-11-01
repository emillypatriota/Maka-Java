package com.maka.api.fichatecnica;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.fichatecnica.CustoPratoService.CustoPrato;
import com.maka.api.fichatecnica.CustoPratoService.ItemFicha;
import com.maka.api.fichatecnica.dto.FaixaJson;
import com.maka.api.fichatecnica.dto.FichaTecnicaRequest;
import com.maka.api.fichatecnica.dto.FichaTecnicaResponse;
import com.maka.api.model.FichaTecnica;
import com.maka.api.model.FichaTecnicaItem;
import com.maka.api.model.Ingrediente;
import com.maka.api.model.Produto;
import com.maka.api.model.StatusPrato;
import com.maka.api.produto.dto.StatusPratoJson;
import com.maka.api.repository.FichaTecnicaRepository;
import com.maka.api.repository.IngredienteRepository;
import com.maka.api.repository.ProdutoRepository;

// O custo nunca fica gravado: é calculado na hora com o custo atual de cada ingrediente.
// Assim, quando o custo de um ingrediente muda, o food cost de todos os pratos que o usam
// já aparece recalculado (fluxo 8.3 do SRS).
@Service
public class FichaTecnicaService {

    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final ProdutoRepository produtoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final CustoPratoService custoPratoService;

    public FichaTecnicaService(
            FichaTecnicaRepository fichaTecnicaRepository,
            ProdutoRepository produtoRepository,
            IngredienteRepository ingredienteRepository,
            CustoPratoService custoPratoService) {
        this.fichaTecnicaRepository = fichaTecnicaRepository;
        this.produtoRepository = produtoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.custoPratoService = custoPratoService;
    }

    // Prato sem ficha devolve uma ficha vazia (rendimento 1), pronta para ser montada
    @Transactional(readOnly = true)
    public FichaTecnicaResponse buscar(Long produtoId) {
        Produto produto = buscarProduto(produtoId);
        FichaTecnica ficha = fichaTecnicaRepository.findByProdutoId(produtoId).orElseGet(() -> nova(produto));
        return montarResposta(produto, ficha);
    }

    @Transactional
    public FichaTecnicaResponse salvar(Long produtoId, FichaTecnicaRequest request) {
        Produto produto = buscarProduto(produtoId);
        Map<Long, Ingrediente> ingredientes = carregarIngredientes(request.items());

        // Valida tudo (unidade, RN08, quantidade) antes de mexer na ficha
        List<FichaTecnicaItem> novos = request.items().stream().map(item -> {
            FichaTecnicaItem novo = new FichaTecnicaItem();
            novo.setIngrediente(ingredientes.get(item.ingredientId()));
            novo.setQuantidade(item.quantity());
            novo.setUnidade(Unidades.normalizar(item.unit()));
            novo.setFatorCorrecao(item.correctionFactor());
            custoPratoService.custoItem(paraItemFicha(novo));
            return novo;
        }).toList();

        FichaTecnica ficha = fichaTecnicaRepository.findByProdutoId(produtoId).orElseGet(() -> nova(produto));
        ficha.setRendimento(request.servings() == null ? 1 : request.servings());
        ficha.setModoPreparo(request.preparationMethod() == null || request.preparationMethod().isBlank()
                ? null : request.preparationMethod().trim());
        ficha.getItens().clear();
        novos.forEach(item -> {
            item.setFichaTecnica(ficha);
            ficha.getItens().add(item);
        });
        fichaTecnicaRepository.save(ficha);

        // RN01: prato sem ingredientes na ficha não pode continuar ativo
        if (novos.isEmpty() && produto.getStatus() == StatusPrato.ATIVO) {
            produto.setStatus(StatusPrato.PAUSADO);
        }
        return montarResposta(produto, ficha);
    }

    // Custo de vários pratos com uma consulta só. Prato sem ingredientes na ficha não entra no mapa.
    @Transactional(readOnly = true)
    public Map<Long, CustoPrato> custosPorProduto(Collection<Produto> produtos) {
        if (produtos.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = produtos.stream().map(Produto::getId).collect(Collectors.toSet());
        Map<Long, CustoPrato> custos = new HashMap<>();
        for (FichaTecnica ficha : fichaTecnicaRepository.findByProdutoIdIn(ids)) {
            if (!ficha.getItens().isEmpty()) {
                custos.put(ficha.getProduto().getId(), calcular(ficha));
            }
        }
        return custos;
    }

    // Usado pelo ProdutoService para a RN01
    public boolean temFicha(Long produtoId) {
        return fichaTecnicaRepository.existsByProdutoIdAndItensIsNotEmpty(produtoId);
    }

    private CustoPrato calcular(FichaTecnica ficha) {
        List<ItemFicha> itens = ficha.getItens().stream().map(FichaTecnicaService::paraItemFicha).toList();
        return custoPratoService.calcular(itens, ficha.getProduto().getPreco(), ficha.getRendimento());
    }

    private FichaTecnicaResponse montarResposta(Produto produto, FichaTecnica ficha) {
        List<FichaTecnicaResponse.Item> linhas = ficha.getItens().stream().map(item -> new FichaTecnicaResponse.Item(
                item.getIngrediente().getId(),
                item.getIngrediente().getNome(),
                item.getQuantidade(),
                item.getUnidade(),
                item.getFatorCorrecao(),
                item.getIngrediente().getUnidadeMedida(),
                item.getIngrediente().getCustoUnitario(),
                custoPratoService.custoItem(paraItemFicha(item)).setScale(2, RoundingMode.HALF_UP))).toList();

        CustoPrato custo = ficha.getItens().isEmpty() ? null : calcular(ficha);
        BigDecimal custoReceita = linhas.stream()
                .map(FichaTecnicaResponse.Item::cost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FichaTecnicaResponse(
                String.valueOf(produto.getId()),
                produto.getNome(),
                produto.getPreco(),
                StatusPratoJson.toJson(produto.getStatus()),
                ficha.getRendimento(),
                ficha.getModoPreparo(),
                linhas,
                custo == null ? null : custoReceita,
                custo == null ? null : custo.custo(),
                custo == null ? null : custo.foodCost(),
                custo == null ? null : FaixaJson.toJson(custo.faixa()),
                custo != null && custo.alerta());
    }

    private Map<Long, Ingrediente> carregarIngredientes(List<FichaTecnicaRequest.Item> itens) {
        Set<Long> ids = new HashSet<>();
        for (FichaTecnicaRequest.Item item : itens) {
            if (!ids.add(item.ingredientId())) {
                throw new IllegalArgumentException("Ingrediente repetido na ficha técnica: " + item.ingredientId());
            }
        }
        Map<Long, Ingrediente> encontrados = ingredienteRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));
        ids.stream()
                .filter(id -> !encontrados.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new NoSuchElementException("Ingrediente não encontrado: " + id);
                });
        return encontrados;
    }

    private Produto buscarProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Prato não encontrado: " + id));
    }

    private static FichaTecnica nova(Produto produto) {
        FichaTecnica ficha = new FichaTecnica();
        ficha.setProduto(produto);
        return ficha;
    }

    // Converte a quantidade para a unidade do ingrediente antes de multiplicar pelo custo
    private static ItemFicha paraItemFicha(FichaTecnicaItem item) {
        BigDecimal quantidade = Unidades.converter(
                item.getQuantidade(), item.getUnidade(), item.getIngrediente().getUnidadeMedida());
        return new ItemFicha(quantidade, item.getIngrediente().getCustoUnitario(), item.getFatorCorrecao());
    }
}
