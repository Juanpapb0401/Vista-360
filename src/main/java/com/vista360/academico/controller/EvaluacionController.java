package com.vista360.academico.controller;

import com.vista360.academico.dto.EvaluacionesResponse;
import com.vista360.academico.service.AutorizacionHelper;
import com.vista360.academico.service.EvaluacionConsultaService;
import com.vista360.academico.service.PaginacionValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint 2 del contrato: detalle paginado de evaluaciones (parciales,
 * quices, tareas) de una materia puntual. Ver docs/02-especificacion-servicio.md.
 */
@RestController
public class EvaluacionController {

    private final EvaluacionConsultaService evaluacionConsultaService;
    private final AutorizacionHelper autorizacionHelper;
    private final PaginacionValidator paginacionValidator;

    public EvaluacionController(EvaluacionConsultaService evaluacionConsultaService,
                                 AutorizacionHelper autorizacionHelper,
                                 PaginacionValidator paginacionValidator) {
        this.evaluacionConsultaService = evaluacionConsultaService;
        this.autorizacionHelper = autorizacionHelper;
        this.paginacionValidator = paginacionValidator;
    }

    @GetMapping("/api/v1/estudiantes/{codigoEstudiante}/materias/{codigoMateria}/evaluaciones")
    public EvaluacionesResponse obtenerEvaluaciones(
            @PathVariable String codigoEstudiante,
            @PathVariable String codigoMateria,
            @RequestParam(required = false) String periodo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt token
    ) {
        autorizacionHelper.verificarAccesoAEstudiante(token, codigoEstudiante);
        Pageable pageable = paginacionValidator.resolver(page, size);
        return evaluacionConsultaService.obtenerEvaluaciones(codigoEstudiante, codigoMateria, periodo, pageable);
    }
}
