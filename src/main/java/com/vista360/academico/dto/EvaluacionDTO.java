package com.vista360.academico.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una fila del Endpoint 2. Ver docs/02-especificacion-servicio.md. */
public record EvaluacionDTO(
        String tipo,
        String nombre,
        BigDecimal valor,
        BigDecimal porcentaje,
        LocalDate fecha
) {
}
