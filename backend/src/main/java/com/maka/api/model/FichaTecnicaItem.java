package com.maka.api.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Uma linha da ficha técnica (tabela do V3): quanto de um ingrediente vai na receita
@Entity
@Table(name = "ficha_tecnica_item")
@Getter
@Setter
public class FichaTecnicaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_tecnica_id")
    private FichaTecnica fichaTecnica;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id")
    private Ingrediente ingrediente;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    // g, kg, ml, L ou un; pode ser diferente da unidade do ingrediente (ex.: g e kg)
    @Column(nullable = false, length = 10)
    private String unidade;

    // RN08: >= 1,00 (o V3 também tem um CHECK no banco)
    @Column(name = "fator_correcao", nullable = false, precision = 5, scale = 2)
    private BigDecimal fatorCorrecao = BigDecimal.ONE;
}
