package com.vista360.academico.service;

import com.vista360.academico.domain.Evaluacion;
import com.vista360.academico.domain.Matricula;
import com.vista360.academico.dto.sync.EvaluacionSyncRequest;
import com.vista360.academico.dto.sync.EvaluacionSyncResponse;
import com.vista360.academico.exception.RecursoNoEncontradoException;
import com.vista360.academico.repository.EvaluacionRepository;
import com.vista360.academico.repository.MatriculaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Simula lo que en producción haría el componente de sincronización real:
 * recibe una evaluación (que llegaría como evento desde el ERP a través de
 * la plataforma de integración, ver docs/01-arquitectura.md) y, en la misma
 * transacción, recalcula {@code nota_actual} de la matrícula correspondiente.
 *
 * <p>Es el mismo patrón que usan sistemas de gestión de cursos como Moodle:
 * el recálculo nunca depende de un paso manual, ocurre automáticamente en
 * cada escritura (ver la decisión de modelado en docs/03-modelo-datos.md).
 *
 * <p>Asume que la matrícula (estudiante + materia + periodo) ya existe,
 * sincronizada previamente por otro proceso; este componente no la crea.
 *
 * <p>La operación es idempotente: el mismo evento aplicado dos veces deja el
 * sistema en el mismo estado, porque la evaluación se identifica por su clave
 * en el sistema de origen y no por el hecho de haber llegado.
 */
@Service
@Transactional
public class EvaluacionSincronizacionService {

    private final MatriculaRepository matriculaRepository;
    private final EvaluacionRepository evaluacionRepository;

    public EvaluacionSincronizacionService(MatriculaRepository matriculaRepository,
                                            EvaluacionRepository evaluacionRepository) {
        this.matriculaRepository = matriculaRepository;
        this.evaluacionRepository = evaluacionRepository;
    }

    public EvaluacionSyncResponse sincronizar(EvaluacionSyncRequest request) {
        Matricula matricula = matriculaRepository
                .findByEstudiante_CodigoEstudianteAndMateria_CodigoMateriaAndPeriodoAcademico_Codigo(
                        request.codigoEstudiante(), request.codigoMateria(), request.periodoAcademico())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una matrícula para el estudiante " + request.codigoEstudiante()
                                + " en la materia " + request.codigoMateria()
                                + " para el periodo " + request.periodoAcademico()
                                + ". El sincronizador de evaluaciones asume que la matrícula ya fue sincronizada."));

        // Si este evento ya se procesó antes, se actualiza la evaluación existente
        // en vez de insertar otra. Reprocesar un evento es lo normal cuando la
        // entrega es "al menos una vez" (SUP-09); sin esto, cada reintento sumaba
        // una evaluación duplicada y distorsionaba la nota.
        Evaluacion evaluacion = evaluacionRepository
                .findByIdEvaluacionOrigen(request.idEvaluacionOrigen())
                .orElseGet(Evaluacion::new);

        evaluacion.setIdEvaluacionOrigen(request.idEvaluacionOrigen());
        evaluacion.setMatricula(matricula);
        evaluacion.setTipo(request.tipo());
        evaluacion.setNombre(request.nombre());
        evaluacion.setValor(request.valor());
        evaluacion.setPorcentaje(request.porcentaje());
        evaluacion.setFecha(request.fecha());
        evaluacionRepository.save(evaluacion);

        List<Evaluacion> todasLasEvaluaciones = evaluacionRepository.findByMatricula_Id(matricula.getId());
        BigDecimal notaRecalculada = recalcularNotaPonderada(todasLasEvaluaciones);

        matricula.setNotaActual(notaRecalculada);
        matriculaRepository.save(matricula);

        return new EvaluacionSyncResponse(
                request.codigoEstudiante(),
                request.codigoMateria(),
                request.periodoAcademico(),
                notaRecalculada,
                todasLasEvaluaciones.size()
        );
    }

    /**
     * Promedio ponderado de las evaluaciones registradas, normalizado por la
     * suma de los porcentajes ya registrados (no por 100), tal como se
     * declara en docs/02-especificacion-servicio.md.
     */
    private BigDecimal recalcularNotaPonderada(List<Evaluacion> evaluaciones) {
        if (evaluaciones.isEmpty()) {
            return null;
        }

        BigDecimal sumaPonderada = BigDecimal.ZERO;
        BigDecimal sumaPorcentajes = BigDecimal.ZERO;

        for (Evaluacion evaluacion : evaluaciones) {
            sumaPonderada = sumaPonderada.add(evaluacion.getValor().multiply(evaluacion.getPorcentaje()));
            sumaPorcentajes = sumaPorcentajes.add(evaluacion.getPorcentaje());
        }

        if (sumaPorcentajes.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return sumaPonderada.divide(sumaPorcentajes, 2, RoundingMode.HALF_UP);
    }
}
