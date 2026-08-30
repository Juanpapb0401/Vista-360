package com.vista360.academico.exception;

/** Se lanza cuando el estudiante, periodo o materia solicitados no existen o no aplican. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
