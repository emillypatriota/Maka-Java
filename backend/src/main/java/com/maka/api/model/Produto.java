package com.maka.api.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import lombok.Setter;

@Entity
@Table(name = "produto")
@Getter
@Setter

public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(length = 500)
    private String imagem;

    private Boolean destaque = false;

    private Boolean ativo = true;

    @Column(name = "tempo_preparo")
    private Integer tempoPreparo;

    private Integer calorias;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "produto_tag", joinColumns = @JoinColumn(name = "produto_id"))
    @Column(name = "tag", length = 40)
    private Set<String> tags = new HashSet<>();

    @Column(name = "criado_em", insertable = false, updatable = false)
    private LocalDateTime criadoEm;
}