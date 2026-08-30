package com.vista360.academico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de materias, sincronizado desde el ERP institucional.
 */
@Entity
@Table(name = "materia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Materia {

    @Id
    @Column(name = "codigo_materia", length = 20)
    private String codigoMateria;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "creditos", nullable = false)
    private Short creditos;
}
