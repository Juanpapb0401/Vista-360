package com.vista360.academico.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Sobre de paginación común a ambos endpoints del contrato
 * (docs/02-especificacion-servicio.md). Evita repetir la forma
 * {@code content/page/size/totalElements/totalPages} en cada respuesta.
 */
public record PaginatedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <E, T> PaginatedResult<T> from(Page<E> pageResult, Function<E, T> mapper) {
        return new PaginatedResult<>(
                pageResult.getContent().stream().map(mapper).toList(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
