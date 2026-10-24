package com.maka.api.produto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.maka.api.model.Produto;

import lombok.Getter;
import lombok.Setter;

// Espelha o MenuItem do front (frontend/src/app/models/index.ts)
@Getter
@Setter
public class ProdutoResponse {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String image;
    private Boolean isSpecial;
    private Boolean active;
    private Integer prepTime;
    private Integer calories;
    private List<String> tags;
    private LocalDateTime createdAt;

    // Precisa ser chamado dentro de uma transação: as tags são LAZY
    public static ProdutoResponse from(Produto produto) {
        ProdutoResponse dto = new ProdutoResponse();
        dto.setId(String.valueOf(produto.getId()));
        dto.setName(produto.getNome());
        dto.setDescription(produto.getDescricao());
        dto.setPrice(produto.getPreco());
        dto.setCategory(produto.getCategoria());
        dto.setImage(produto.getImagem());
        dto.setIsSpecial(produto.getDestaque());
        dto.setActive(produto.getAtivo());
        dto.setPrepTime(produto.getTempoPreparo());
        dto.setCalories(produto.getCalorias());
        dto.setTags(new ArrayList<>(produto.getTags()));
        dto.setCreatedAt(produto.getCriadoEm());
        return dto;
    }
}
