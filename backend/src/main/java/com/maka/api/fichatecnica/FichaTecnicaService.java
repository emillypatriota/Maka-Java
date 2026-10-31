package com.maka.api.fichatecnica;

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

    @Transactional(readOnly = true)
    public FichaTecnicaResponse buscar(Long produtoId) {
        Produto produto = buscarProduto(produtoId);
        return montarResposta(produto, fichaTecnicaRepository.findByProdutoIdOrderById(produtoId));
    }

    @Transactional
    public FichaTecnicaResponse salvar(Long produtoId, FichaTecnicaRequest request) {
        Produto produto = buscarProduto(produtoId);
        Map<Long, Ingrediente> ingredientes = carregarIngredientes(request.items());

        // Valida tudo (RN08, quantidade) antes de apagar a ficha antiga
        request.items().forEach(item -> custoPratoService.custoItem(new ItemFicha(
                item.quantity(), ingredientes.get(item.ingredientId()).getCustoUnitario(), item.correctionFactor())));

        fichaTecnicaRepository.apagarDoProduto(produtoId);
        List<FichaTecnicaItem> itens = request.items().stream().map(item -> {
            FichaTecnicaItem novo = new FichaTecnicaItem();
            novo.setProduto(produto);
            novo.setIngrediente(ingredientes.get(item.ingredientId()));
            novo.setQuantidade(item.quantity());
            novo.setFatorCorrecao(item.correctionFactor());
            return novo;
        }).toList();
        fichaTecnicaRepository.saveAll(itens);

        // RN01: prato sem ficha não pode continuar ativo
        if (itens.isEmpty() && produto.getStatus() == StatusPrato.ATIVO) {
            produto.setStatus(StatusPrato.PAUSADO);
        }
        return montarResposta(produto, itens);
    }

    // Custo de vários pratos com uma consulta só. Prato sem ficha não entra no mapa.
    @Transactional(readOnly = true)
    public Map<Long, CustoPrato> custosPorProduto(Collection<Produto> produtos) {
        if (produtos.isEmpty()) {
            return Map.of();
        }
        Map<Long, Produto> porId = produtos.stream().collect(Collectors.toMap(Produto::getId, Function.identity()));
        Map<Long, List<ItemFicha>> itensPorProduto = fichaTecnicaRepository.findByProdutoIdIn(porId.keySet()).stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduto().getId(),
                        Collectors.mapping(FichaTecnicaService::paraItemFicha, Collectors.toList())));

        Map<Long, CustoPrato> custos = new HashMap<>();
        itensPorProduto.forEach((id, itens) ->
                custos.put(id, custoPratoService.calcular(itens, porId.get(id).getPreco())));
        return custos;
    }

    private FichaTecnicaResponse montarResposta(Produto produto, List<FichaTecnicaItem> itens) {
        List<FichaTecnicaResponse.Item> linhas = itens.stream().map(item -> new FichaTecnicaResponse.Item(
                item.getIngrediente().getId(),
                item.getIngrediente().getNome(),
                item.getIngrediente().getUnidadeMedida(),
                item.getIngrediente().getCustoUnitario(),
                item.getQuantidade(),
                item.getFatorCorrecao(),
                custoPratoService.custoItem(paraItemFicha(item)).setScale(2, RoundingMode.HALF_UP))).toList();

        CustoPrato custo = itens.isEmpty()
                ? null
                : custoPratoService.calcular(itens.stream().map(FichaTecnicaService::paraItemFicha).toList(), produto.getPreco());
        return new FichaTecnicaResponse(
                String.valueOf(produto.getId()),
                produto.getNome(),
                produto.getPreco(),
                StatusPratoJson.toJson(produto.getStatus()),
                linhas,
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

    private static ItemFicha paraItemFicha(FichaTecnicaItem item) {
        return new ItemFicha(item.getQuantidade(), item.getIngrediente().getCustoUnitario(), item.getFatorCorrecao());
    }

    // Usado pelo ProdutoService para a RN01
    public boolean temFicha(Long produtoId) {
        return fichaTecnicaRepository.existsByProdutoId(produtoId);
    }
}
