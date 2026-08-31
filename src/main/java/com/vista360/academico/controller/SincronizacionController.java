package com.vista360.academico.controller;

import com.vista360.academico.dto.sync.EvaluacionSyncRequest;
import com.vista360.academico.dto.sync.EvaluacionSyncResponse;
import com.vista360.academico.service.EvaluacionSincronizacionService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de demostración, NO forma parte del contrato público del
 * servicio (ver docs/02-especificacion-servicio.md). Simula lo que en
 * producción haría el componente real de sincronización: recibir un evento
 * de evaluación desde el ERP (a través de la plataforma de integración,
 * ver docs/01-arquitectura.md) y aplicarlo.
 *
 * <p>Se expone como HTTP solo para poder probar de punta a punta, en esta
 * prueba técnica, que el recálculo automático de {@code nota_actual}
 * funciona. En un sistema real este flujo lo dispararía un consumidor de
 * eventos, no una llamada HTTP pública.
 */
@RestController
public class SincronizacionController {

    private final EvaluacionSincronizacionService evaluacionSincronizacionService;

    public SincronizacionController(EvaluacionSincronizacionService evaluacionSincronizacionService) {
        this.evaluacionSincronizacionService = evaluacionSincronizacionService;
    }

    @PostMapping("/api/v1/interno/sincronizacion/evaluaciones")
    public ResponseEntity<EvaluacionSyncResponse> sincronizarEvaluacion(
            @Valid @RequestBody EvaluacionSyncRequest request
    ) {
        EvaluacionSyncResponse respuesta = sincronizarConReintento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Con entrega "al menos una vez" (SUP-09), dos entregas simultáneas del
     * mismo evento pueden chocar: ambas pasan la verificación de idempotencia
     * y la segunda revienta contra la restricción {@code uq_evaluacion_origen},
     * o pierde el bloqueo optimista de la matrícula ({@code @Version} en
     * Matricula). Ninguno de los dos casos es un error del emisor: se reintenta
     * una vez en una transacción nueva, donde la verificación de idempotencia
     * ya encuentra la fila y la actualiza. Si el reintento también falla, el
     * 409 resultante (ver GlobalExceptionHandler) le indica a la plataforma de
     * integración que debe reentregar el evento.
     */
    private EvaluacionSyncResponse sincronizarConReintento(EvaluacionSyncRequest request) {
        try {
            return evaluacionSincronizacionService.sincronizar(request);
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            return evaluacionSincronizacionService.sincronizar(request);
        }
    }
}
