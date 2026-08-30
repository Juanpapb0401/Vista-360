package com.vista360.academico.domain;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una evaluación puntual (parcial, quiz, tarea...) dentro de una matrícula.
 * Cuelga de {@link Matricula}, no de Estudiante ni Materia por separado,
 * porque solo tiene sentido en el contexto de una matrícula concreta.
 * Responde al Endpoint 2 del contrato.
 */
@Entity
@Table(name = "evaluacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoEvaluacion tipo;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    /** Escala colombiana estándar: 0.00 a 5.00. */
    @Column(name = "valor", precision = 3, scale = 2, nullable = false)
    private BigDecimal valor;

    /** Peso de esta evaluación dentro de la nota final de la materia (0 a 100). */
    @Column(name = "porcentaje", precision = 5, scale = 2, nullable = false)
    private BigDecimal porcentaje;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;
}
