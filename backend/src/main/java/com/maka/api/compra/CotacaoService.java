package com.maka.api.compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maka.api.compra.dto.CotacaoResponse;
import com.maka.api.compra.dto.OfertaResponse;
import com.maka.api.fornecedor.PrecoUnidade;
import com.maka.api.model.FornecedorProduto;
import com.maka.api.model.Ingrediente;
import com.maka.api.model.StatusFornecedor;
import com.maka.api.model.StatusIngrediente;
import com.maka.api.repository.FornecedorProdutoRepository;

/**
 * RF-023: cotação comparativa. Para cada ingrediente, lista o preço de cada fornecedor ATIVO,
 * sempre convertido para a unidade do ingrediente (assim kg e g ficam comparáveis),
 * do mais barato para o mais caro, marcando a melhor oferta.
 */
@Service
public class CotacaoService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final FornecedorProdutoRepository catalogoRepository;

    public CotacaoService(FornecedorProdutoRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    // ingredienteIds vazio = cotação de todos os ingredientes que têm pelo menos uma oferta
    @Transactional(readOnly = true)
    public List<CotacaoResponse> cotar(Collection<Long> ingredienteIds) {
        List<FornecedorProduto> ofertas = ingredienteIds == null || ingredienteIds.isEmpty()
                ? catalogoRepository.findAllByOrderByIngredienteNomeAsc()
                : catalogoRepository.findByIngredienteIdIn(ingredienteIds);

        // Agrupa por ingrediente, em ordem alfabética
        Map<Long, List<FornecedorProduto>> porIngrediente = new LinkedHashMap<>();
        ofertas.stream()
                .filter(o -> o.getFornecedor().getStatus() == StatusFornecedor.ATIVO)
                .filter(o -> o.getIngrediente().getStatus() == StatusIngrediente.ATIVO)
                .sorted(Comparator.comparing((FornecedorProduto o) -> o.getIngrediente().getNome(),
                        String.CASE_INSENSITIVE_ORDER))
                .forEach(o -> porIngrediente.computeIfAbsent(o.getIngrediente().getId(), k -> new ArrayList<>()).add(o));

        List<CotacaoResponse> resultado = new ArrayList<>();
        porIngrediente.values().forEach(lista -> resultado.add(montar(lista)));
        return resultado;
    }

    private CotacaoResponse montar(List<FornecedorProduto> ofertas) {
        Ingrediente ingrediente = ofertas.get(0).getIngrediente();

        // Oferta + preço convertido (null se a unidade não combina: vai para o fim da lista)
        record Linha(FornecedorProduto oferta, BigDecimal precoConvertido) {
        }
        List<Linha> linhas = new ArrayList<>(ofertas.stream()
                .map(o -> new Linha(o, precoConvertido(o)))
                .toList());
        linhas.sort(Comparator.comparing(Linha::precoConvertido, Comparator.nullsLast(Comparator.<BigDecimal>naturalOrder())));

        BigDecimal melhor = linhas.get(0).precoConvertido();
        List<OfertaResponse> respostas = linhas.stream().map(l -> {
            BigDecimal preco = l.precoConvertido();
            boolean ehMelhor = preco != null && melhor != null && preco.compareTo(melhor) == 0;
            return new OfertaResponse(
                    l.oferta().getId(),
                    l.oferta().getFornecedor().getId(),
                    l.oferta().getFornecedor().getRazaoSocial(),
                    l.oferta().getPreco(),
                    l.oferta().getUnidadeVenda(),
                    preco,
                    ehMelhor,
                    diferencaPercentual(preco, melhor));
        }).toList();

        Long melhorFornecedorId = melhor == null ? null : linhas.get(0).oferta().getFornecedor().getId();
        return new CotacaoResponse(
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getUnidadeMedida(),
                ingrediente.getCustoUnitario(),
                melhorFornecedorId,
                respostas);
    }

    private static BigDecimal precoConvertido(FornecedorProduto oferta) {
        try {
            return PrecoUnidade.precoNaUnidadeDoIngrediente(
                    oferta.getPreco(), oferta.getUnidadeVenda(), oferta.getIngrediente().getUnidadeMedida());
        } catch (RuntimeException e) {
            return null;
        }
    }

    // (preço - melhor) / melhor * 100, com 1 casa. 0 para a melhor oferta.
    private static BigDecimal diferencaPercentual(BigDecimal preco, BigDecimal melhor) {
        if (preco == null || melhor == null || melhor.signum() == 0) {
            return null;
        }
        return preco.subtract(melhor).multiply(CEM).divide(melhor, 1, RoundingMode.HALF_UP);
    }
}
