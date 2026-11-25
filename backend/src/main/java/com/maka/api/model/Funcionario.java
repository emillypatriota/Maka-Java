package com.maka.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import lombok.Setter;

@Entity
@Table(name = "funcionario")
@Getter
@Setter

public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cargo cargo;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    private StatusFuncionario status = StatusFuncionario.active;

    @Column(length = 60)
    private String escala;

    @Column(name = "data_admissao")
    private LocalDate dataAdmissao;

    @Column(length = 255)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private LocalDateTime criadoEm;
}