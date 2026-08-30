package com.vista360.academico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Copia local mínima del estudiante, sincronizada desde el ERP institucional.
 * Este servicio no es dueño de los datos personales del estudiante (ver SUP-01
 * y SUP-16 en docs/00-supuestos.md): solo guarda lo indispensable para
 * correlacionar matrículas y presentarlas.
 */
@Entity
@Table(name = "estudiante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estudiante {

    /** Código institucional del ERP, identificador canónico (ver SUP-13). */
    @Id
    @Column(name = "codigo_estudiante", length = 20)
    private String codigoEstudiante;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "sincronizado_en", nullable = false)
    private Instant sincronizadoEn;
}
