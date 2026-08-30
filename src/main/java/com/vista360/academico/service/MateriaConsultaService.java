package com.vista360.academico.service;

import com.vista360.academico.domain.Matricula;
import com.vista360.academico.dto.MateriaResumenDTO;
import com.vista360.academico.dto.MateriasResponse;
import com.vista360.academico.dto.PaginatedResult;
import com.vista360.academico.exception.RecursoNoEncontradoException;
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
    private final PeriodoResolver periodoResolver;

    public MateriaConsultaService(MatriculaRepository matriculaRepository, PeriodoResolver periodoResolver) {
        this.matriculaRepository = matriculaRepository;
        this.periodoResolver = periodoResolver;
    }

    public MateriasResponse obtenerResumen(String codigoEstudiante, String periodoSolicitado, Pageable pageable) {
        String periodo = periodoResolver.resolver(periodoSolicitado);

        Page<Matricula> pagina = matriculaRepository
                .findByEstudiante_CodigoEstudianteAndPeriodoAcademico_Codigo(codigoEstudiante, periodo, pageable);

        // Un total de 0 en la página cero significa que el estudiante no existe,
        // o no tiene matrículas en ese periodo; en cualquier caso, 404 según el
        // contrato. Pedir una página fuera de rango (ej. page=5 cuando solo hay
        // una) es distinto: ahí sí existen matrículas, solo se devuelve vacío.
        if (pagina.getTotalElements() == 0) {
            throw new RecursoNoEncontradoException(
                    "El estudiante " + codigoEstudiante + " no existe o no tiene matrículas en el periodo " + periodo);
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
