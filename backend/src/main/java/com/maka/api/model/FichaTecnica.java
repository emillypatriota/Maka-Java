package com.maka.api.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Cabeçalho da ficha técnica (tabela do V3): uma por prato.
// Nunca vai direto para o JSON: o controller devolve FichaTecnicaResponse.
@Entity
@Table(name = "ficha_tecnica")
@Getter
@Setter
public class FichaTecnica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", unique = true)
    private Produto produto;

    // Quantas porções a receita rende: o custo da porção é o custo total dividido por ele
    @Column(nullable = false)
    private Integer rendimento = 1;

    @Column(name = "modo_preparo", columnDefinition = "TEXT")
    private String modoPreparo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "fichaTecnica", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<FichaTecnicaItem> itens = new ArrayList<>();
}
