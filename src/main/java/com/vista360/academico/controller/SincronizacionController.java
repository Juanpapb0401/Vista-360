package com.vista360.academico.controller;

import com.vista360.academico.dto.sync.EvaluacionSyncRequest;
import com.vista360.academico.dto.sync.EvaluacionSyncResponse;
import com.vista360.academico.service.EvaluacionSincronizacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
        EvaluacionSyncResponse respuesta = evaluacionSincronizacionService.sincronizar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
