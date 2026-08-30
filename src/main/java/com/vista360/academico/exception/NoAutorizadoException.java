package com.vista360.academico.exception;

/**
 * Se lanza cuando el token es válido, pero quien lo presenta no tiene
 * permiso para consultar el {@code codigoEstudiante} solicitado.
 * Ver la sección de autenticación y autorización en
 * docs/02-especificacion-servicio.md.
 */
public class NoAutorizadoException extends RuntimeException {
    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
