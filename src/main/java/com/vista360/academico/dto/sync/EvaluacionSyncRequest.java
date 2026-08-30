package com.vista360.academico.dto.sync;

import com.vista360.academico.domain.TipoEvaluacion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Simula el payload de un evento "evaluación sincronizada" que en producción
 * llegaría desde el ERP a través de la plataforma de integración (ver
 * docs/01-arquitectura.md). Este DTO alimenta el endpoint de demo descrito
 * en {@link com.vista360.academico.controller.SincronizacionController},
 * que NO forma parte del contrato público del servicio.
 */
public record EvaluacionSyncRequest(

        /*
         * Identificador de la evaluación en el sistema de origen. Es obligatorio
         * porque sin él no hay forma de distinguir un reintento del mismo evento
         * de una evaluación nueva, y la entrega es "al menos una vez" (SUP-09).
         */
        @NotBlank(message = "idEvaluacionOrigen es obligatorio")
        String idEvaluacionOrigen,

        @NotBlank(message = "codigoEstudiante es obligatorio")
        String codigoEstudiante,

        @NotBlank(message = "codigoMateria es obligatorio")
        String codigoMateria,

        @NotBlank(message = "periodoAcademico es obligatorio")
        @Pattern(regexp = "\\d{4}-\\d", message = "periodoAcademico debe tener el formato AAAA-N, ej. 2026-2")
        String periodoAcademico,

        @NotNull(message = "tipo es obligatorio")
        TipoEvaluacion tipo,

        @NotBlank(message = "nombre es obligatorio")
        String nombre,

        @NotNull(message = "valor es obligatorio")
        @DecimalMin(value = "0.00", message = "valor no puede ser negativo")
        @DecimalMax(value = "5.00", message = "valor no puede superar 5.00")
        BigDecimal valor,

        @NotNull(message = "porcentaje es obligatorio")
        @DecimalMin(value = "0.01", message = "porcentaje debe ser mayor a 0")
        @DecimalMax(value = "100.00", message = "porcentaje no puede superar 100")
        BigDecimal porcentaje,

        @NotNull(message = "fecha es obligatoria")
        LocalDate fecha
) {
}
