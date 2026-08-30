package com.vista360.academico.dto;

/** Respuesta completa del Endpoint 2 (detalle de evaluaciones de una materia). */
public record EvaluacionesResponse(
        String codigoEstudiante,
        String codigoMateria,
        String nombreMateria,
        String periodoAcademico,
        PaginatedResult<EvaluacionDTO> evaluaciones
) {
}
