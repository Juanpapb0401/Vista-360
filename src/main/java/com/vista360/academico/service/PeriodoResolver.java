package com.vista360.academico.service;

import com.vista360.academico.domain.PeriodoAcademico;
import com.vista360.academico.exception.ParametroInvalidoException;
import com.vista360.academico.exception.RecursoNoEncontradoException;
import com.vista360.academico.repository.PeriodoAcademicoRepository;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Resuelve qué periodo académico consultar: el que llega por parámetro,
 * validado contra el formato AAAA-N, o el vigente si no se especifica
 * ninguno. Usado por ambos endpoints de consulta (docs/02-especificacion-servicio.md).
 */
@Component
public class PeriodoResolver {

    private static final Pattern FORMATO_PERIODO = Pattern.compile("\\d{4}-\\d");

    private final PeriodoAcademicoRepository periodoAcademicoRepository;

    public PeriodoResolver(PeriodoAcademicoRepository periodoAcademicoRepository) {
        this.periodoAcademicoRepository = periodoAcademicoRepository;
    }

    public String resolver(String periodoSolicitado) {
        if (periodoSolicitado != null && !periodoSolicitado.isBlank()) {
            if (!FORMATO_PERIODO.matcher(periodoSolicitado).matches()) {
                throw new ParametroInvalidoException(
                        "El parámetro periodo debe tener el formato AAAA-N, ej. 2026-2");
            }
            return periodoSolicitado;
        }
        return periodoAcademicoRepository.findFirstByVigenteTrue()
                .map(PeriodoAcademico::getCodigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay un periodo académico vigente configurado"));
    }
}
