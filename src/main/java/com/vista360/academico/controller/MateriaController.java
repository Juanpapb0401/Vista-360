package com.vista360.academico.controller;

import com.vista360.academico.dto.MateriasResponse;
import com.vista360.academico.service.AutorizacionHelper;
import com.vista360.academico.service.MateriaConsultaService;
import com.vista360.academico.service.PaginacionValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint 1 del contrato: resumen paginado de materias matriculadas de un
 * estudiante, con su nota consolidada. Ver docs/02-especificacion-servicio.md.
 */
@RestController
public class MateriaController {

    private final MateriaConsultaService materiaConsultaService;
    private final AutorizacionHelper autorizacionHelper;
    private final PaginacionValidator paginacionValidator;

    public MateriaController(MateriaConsultaService materiaConsultaService,
                              AutorizacionHelper autorizacionHelper,
                              PaginacionValidator paginacionValidator) {
        this.materiaConsultaService = materiaConsultaService;
        this.autorizacionHelper = autorizacionHelper;
        this.paginacionValidator = paginacionValidator;
    }

    @GetMapping("/api/v1/estudiantes/{codigoEstudiante}/materias")
    public MateriasResponse obtenerMaterias(
            @PathVariable String codigoEstudiante,
            @RequestParam(required = false) String periodo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt token
    ) {
        autorizacionHelper.verificarAccesoAEstudiante(token, codigoEstudiante);
        Pageable pageable = paginacionValidator.resolver(page, size);
        return materiaConsultaService.obtenerResumen(codigoEstudiante, periodo, pageable);
    }
}
