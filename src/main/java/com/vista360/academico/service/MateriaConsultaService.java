package com.vista360.academico.service;

import com.vista360.academico.domain.Matricula;
import com.vista360.academico.dto.MateriaResumenDTO;
import com.vista360.academico.dto.MateriasResponse;
import com.vista360.academico.dto.PaginatedResult;
import com.vista360.academico.exception.RecursoNoEncontradoException;
import com.vista360.academico.repository.EstudianteRepository;
import com.vista360.academico.repository.MatriculaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el Endpoint 1 del contrato: dado un estudiante, la lista paginada
 * de materias matriculadas en un periodo, con su nota consolidada.
 * Ver docs/02-especificacion-servicio.md.
 */
@Service
@Transactional(readOnly = true)
public class MateriaConsultaService {

    private final MatriculaRepository matriculaRepository;
    private final EstudianteRepository estudianteRepository;
    private final PeriodoResolver periodoResolver;

    public MateriaConsultaService(MatriculaRepository matriculaRepository,
                                  EstudianteRepository estudianteRepository,
                                  PeriodoResolver periodoResolver) {
        this.matriculaRepository = matriculaRepository;
        this.estudianteRepository = estudianteRepository;
        this.periodoResolver = periodoResolver;
    }

    public MateriasResponse obtenerResumen(String codigoEstudiante, String periodoSolicitado, Pageable pageable) {
        String periodo = periodoResolver.resolver(periodoSolicitado);

        Page<Matricula> pagina = matriculaRepository
                .findByEstudiante_CodigoEstudianteAndPeriodoAcademico_Codigo(codigoEstudiante, periodo, pageable);

        // Los dos casos que producen una página sin resultados son distintos y
        // el cliente necesita distinguirlos: un estudiante que no existe es un
        // 404 (identificador equivocado), mientras que uno que existe pero no
        // matriculó nada en el periodo (ej. semestre de receso) recibe 200 con
        // la lista vacía, igual que hace el endpoint de evaluaciones cuando una
        // matrícula aún no tiene notas.
        if (pagina.getTotalElements() == 0 && !estudianteRepository.existsById(codigoEstudiante)) {
            throw new RecursoNoEncontradoException(
                    "El estudiante " + codigoEstudiante + " no existe");
        }

        return new MateriasResponse(
                codigoEstudiante,
                periodo,
                PaginatedResult.from(pagina, this::aResumenDTO)
        );
    }

    private MateriaResumenDTO aResumenDTO(Matricula matricula) {
        return new MateriaResumenDTO(
                matricula.getMateria().getCodigoMateria(),
                matricula.getMateria().getNombre(),
                matricula.getGrupo(),
                matricula.getMateria().getCreditos(),
                matricula.getNotaActual(),
                matricula.getEstado().name()
        );
    }
}
