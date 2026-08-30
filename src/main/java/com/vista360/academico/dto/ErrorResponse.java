package com.vista360.academico.dto;

/**
 * Cuerpo de error uniforme para las respuestas 400/401/403/404 descritas
 * en docs/02-especificacion-servicio.md.
 */
public record ErrorResponse(String error, String mensaje) {
}
