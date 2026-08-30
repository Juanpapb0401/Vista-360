package com.vista360.academico.dto;

import java.math.BigDecimal;

/** Una fila del Endpoint 1. Ver docs/02-especificacion-servicio.md. */
public record MateriaResumenDTO(
        String codigoMateria,
        String nombreMateria,
        String grupo,
        Short creditos,
        BigDecimal notaActual,
        String estado
) {
}
