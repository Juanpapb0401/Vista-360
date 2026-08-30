package com.vista360.academico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Periodo académico (ej. "2026-2"). El campo {@code vigente} es explícito,
 * no inferido por fecha, porque el cierre real de un semestre no siempre
 * coincide con el rango de fechas de calendario. Ver docs/03-modelo-datos.md.
 */
@Entity
@Table(name = "periodo_academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoAcademico {

    @Id
    @Column(name = "codigo", length = 10)
    private String codigo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "vigente", nullable = false)
    private boolean vigente;
}
