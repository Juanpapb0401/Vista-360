package com.vista360.academico.service;

import com.vista360.academico.exception.ParametroInvalidoException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Valida {@code page} y {@code size} según el contrato
 * (docs/02-especificacion-servicio.md: page por defecto 0, size por
 * defecto 10, máximo 50). Se valida explícitamente en vez de dejar que el
 * resolver por defecto de Spring Data recorte el tamaño en silencio, porque
 * el contrato pide devolver 400, no un tamaño ajustado sin avisar.
 */
@Component
public class PaginacionValidator {

    private static final int TAMANO_MAXIMO = 50;

    public Pageable resolver(int page, int size) {
        if (page < 0) {
            throw new ParametroInvalidoException("El parámetro page no puede ser negativo");
        }
        if (size <= 0) {
            throw new ParametroInvalidoException("El parámetro size debe ser mayor a 0");
        }
        if (size > TAMANO_MAXIMO) {
            throw new ParametroInvalidoException("El parámetro size no puede superar " + TAMANO_MAXIMO);
        }
        return PageRequest.of(page, size);
    }
}
