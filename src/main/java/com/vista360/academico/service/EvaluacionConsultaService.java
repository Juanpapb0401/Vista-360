package com.vista360.academico.service;

import com.vista360.academico.domain.Evaluacion;
import com.vista360.academico.domain.Matricula;
import com.vista360.academico.dto.EvaluacionDTO;
import com.vista360.academico.dto.EvaluacionesResponse;
import com.vista360.academico.dto.PaginatedResult;
import com.vista360.academico.exception.RecursoNoEncontradoException;
import com.vista360.academico.repository.EvaluacionRepository;
import com.vista360.academico.repository.MatriculaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el Endpoint 2 del contrato: el desglose de evaluaciones de una
 * materia puntual de un estudiante. Ver docs/02-especificacion-servicio.md.
 */
@Service
@Transactional(readOnly = true)
public class EvaluacionConsultaService {

    private final MatriculaRepository matriculaRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final PeriodoResolver periodoResolver;

    public EvaluacionConsultaService(MatriculaRepository matriculaRepository,
                                      EvaluacionRepository evaluacionRepository,
                                      PeriodoResolver periodoResolver) {
        this.matriculaRepository = matriculaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.periodoResolver = periodoResolver;
    }

    public EvaluacionesResponse obtenerEvaluaciones(String codigoEstudiante, String codigoMateria,
                                                      String periodoSolicitado, Pageable pageable) {
        String periodo = periodoResolver.resolver(periodoSolicitado);

        // Se valida que la materia de verdad pertenezca a este estudiante en
        // este periodo antes de listar nada. Así no se revela, ni por error,
        // si una materia existe para otro estudiante distinto al que pregunta.
        Matricula matricula = matriculaRepository
                .findByEstudiante_CodigoEstudianteAndMateria_CodigoMateriaAndPeriodoAcademico_Codigo(
                        codigoEstudiante, codigoMateria, periodo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El estudiante " + codigoEstudiante + " no tiene la materia " + codigoMateria
                                + " matriculada en el periodo " + periodo));

        Page<Evaluacion> pagina = evaluacionRepository.findByMatricula_Id(matricula.getId(), pageable);

        return new EvaluacionesResponse(
                codigoEstudiante,
                codigoMateria,
                matricula.getMateria().getNombre(),
                periodo,
                PaginatedResult.from(pagina, this::aDTO)
        );
    }

    private EvaluacionDTO aDTO(Evaluacion evaluacion) {
        return new EvaluacionDTO(
                evaluacion.getTipo().name(),
                evaluacion.getNombre(),
                evaluacion.getValor(),
                evaluacion.getPorcentaje(),
                evaluacion.getFecha()
        );
    }
}
