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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Representa que un estudiante cursa una materia en un periodo académico, en
 * un grupo puntual. Es la tabla central del modelo: cada fila responde al
 * Endpoint 1 del contrato (docs/02-especificacion-servicio.md).
 *
 * <p>{@code notaActual} se almacena y se recalcula de forma automática cada
 * vez que una evaluación de esta matrícula se sincroniza (ver
 * {@link com.vista360.academico.service.EvaluacionSincronizacionService}),
 * siguiendo el mismo patrón que sistemas de gestión de cursos como Moodle.
 * Nunca se recalcula en la ruta de lectura.
 */
@Entity
@Table(
        name = "matricula",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_matricula",
                columnNames = {"codigo_estudiante", "codigo_materia", "periodo_academico"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_estudiante", referencedColumnName = "codigo_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_materia", referencedColumnName = "codigo_materia", nullable = false)
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_academico", referencedColumnName = "codigo", nullable = false)
    private PeriodoAcademico periodoAcademico;

    @Column(name = "grupo", length = 5, nullable = false)
    private String grupo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoMatricula estado;

    /** Nota consolidada, recalculada automáticamente. Nula si no hay evaluaciones aún. */
    @Column(name = "nota_actual", precision = 3, scale = 2)
    private BigDecimal notaActual;
}
