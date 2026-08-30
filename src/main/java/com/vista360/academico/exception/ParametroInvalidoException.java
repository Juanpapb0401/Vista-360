package com.vista360.academico.exception;

/** Se lanza cuando un parámetro de entrada no cumple el formato esperado. */
public class ParametroInvalidoException extends RuntimeException {
    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
