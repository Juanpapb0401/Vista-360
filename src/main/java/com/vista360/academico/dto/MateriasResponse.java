package com.vista360.academico.dto;

/** Respuesta completa del Endpoint 1 (resumen de materias). */
public record MateriasResponse(
        String codigoEstudiante,
        String periodoAcademico,
        PaginatedResult<MateriaResumenDTO> materias
) {
}
