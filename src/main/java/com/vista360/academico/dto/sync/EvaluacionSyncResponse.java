package com.vista360.academico.dto.sync;

import java.math.BigDecimal;

/** Confirmación de que la evaluación se sincronizó y la nota se recalculó. */
public record EvaluacionSyncResponse(
        String codigoEstudiante,
        String codigoMateria,
        String periodoAcademico,
        BigDecimal notaActualRecalculada,
        int totalEvaluacionesRegistradas
) {
}
