package com.vista360.academico.service;

import com.vista360.academico.domain.Evaluacion;
import com.vista360.academico.domain.Matricula;
import com.vista360.academico.domain.TipoEvaluacion;
import com.vista360.academico.dto.sync.EvaluacionSyncRequest;
import com.vista360.academico.dto.sync.EvaluacionSyncResponse;
import com.vista360.academico.exception.RecursoNoEncontradoException;
import com.vista360.academico.repository.EvaluacionRepository;
import com.vista360.academico.repository.MatriculaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica el cálculo del promedio ponderado, que es la decisión de diseño
 * central de este servicio: la nota se normaliza por la suma de los
 * porcentajes ya registrados, no por 100, para que un corte parcial del
 * semestre muestre una nota interpretable (ver docs/02-especificacion-servicio.md).
 */
@ExtendWith(MockitoExtension.class)
class EvaluacionSincronizacionServiceTest {

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @InjectMocks
    private EvaluacionSincronizacionService servicio;

    private static final String ESTUDIANTE = "A00123456";
    private static final String MATERIA = "ING1234";
    private static final String PERIODO = "2026-2";

    @Test
    @DisplayName("Con una sola evaluación, la nota es el valor de esa evaluación")
    void unaSolaEvaluacion() {
        // Con una única evaluación al 30%, la nota no es 4.00 * 0.30 = 1.20:
        // es 4.00, porque se normaliza por el 30% efectivamente registrado.
        EvaluacionSyncResponse respuesta = sincronizarCon(
                evaluacion("4.00", "30.00")
        );

        assertThat(respuesta.notaActualRecalculada()).isEqualByComparingTo("4.00");
        assertThat(respuesta.totalEvaluacionesRegistradas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Con porcentajes que suman 100, la nota es el promedio ponderado completo")
    void porcentajesQueSuman100() {
        // (4.00*50 + 3.00*50) / 100 = 3.50
        EvaluacionSyncResponse respuesta = sincronizarCon(
                evaluacion("4.00", "50.00"),
                evaluacion("3.00", "50.00")
        );

        assertThat(respuesta.notaActualRecalculada()).isEqualByComparingTo("3.50");
    }

    @Test
    @DisplayName("Con porcentajes que no suman 100 (corte parcial), se normaliza por la suma registrada")
    void porcentajesQueNoSuman100() {
        // (4.00*25 + 4.50*5 + 4.80*5) / 35 = 4.1857... -> 4.19 con HALF_UP a 2 decimales
        EvaluacionSyncResponse respuesta = sincronizarCon(
                evaluacion("4.00", "25.00"),
                evaluacion("4.50", "5.00"),
                evaluacion("4.80", "5.00")
        );

        assertThat(respuesta.notaActualRecalculada()).isEqualByComparingTo("4.19");
        assertThat(respuesta.totalEvaluacionesRegistradas()).isEqualTo(3);
    }

    @Test
    @DisplayName("La nota recalculada queda persistida en la matrícula, no solo en la respuesta")
    void actualizaLaMatricula() {
        Matricula matricula = new Matricula();
        when(matriculaRepository
                .findByEstudiante_CodigoEstudianteAndMateria_CodigoMateriaAndPeriodoAcademico_Codigo(
                        ESTUDIANTE, MATERIA, PERIODO))
                .thenReturn(Optional.of(matricula));
        when(evaluacionRepository.findByIdEvaluacionOrigen(any())).thenReturn(Optional.empty());
        when(evaluacionRepository.findByMatricula_Id(any()))
                .thenReturn(List.of(evaluacion("3.00", "40.00")));

        servicio.sincronizar(peticion());

        assertThat(matricula.getNotaActual()).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("Si la matrícula no existe, falla en vez de crearla")
    void matriculaInexistente() {
        when(matriculaRepository
                .findByEstudiante_CodigoEstudianteAndMateria_CodigoMateriaAndPeriodoAcademico_Codigo(
                        ESTUDIANTE, MATERIA, PERIODO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.sincronizar(peticion()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(ESTUDIANTE);
    }

    private EvaluacionSyncResponse sincronizarCon(Evaluacion... evaluacionesTrasGuardar) {
        when(matriculaRepository
                .findByEstudiante_CodigoEstudianteAndMateria_CodigoMateriaAndPeriodoAcademico_Codigo(
                        ESTUDIANTE, MATERIA, PERIODO))
                .thenReturn(Optional.of(new Matricula()));
        when(evaluacionRepository.findByIdEvaluacionOrigen(any())).thenReturn(Optional.empty());
        when(evaluacionRepository.findByMatricula_Id(any()))
                .thenReturn(List.of(evaluacionesTrasGuardar));

        return servicio.sincronizar(peticion());
    }

    @Test
    @DisplayName("Reprocesar el mismo evento actualiza la evaluación, no la duplica")
    void reprocesarElMismoEventoEsIdempotente() {
        // La plataforma de integración entrega al menos una vez (SUP-09): el mismo
        // evento puede llegar dos veces y no debe sumar una evaluación repetida.
        Evaluacion yaRegistrada = evaluacion("2.00", "25.00");
        yaRegistrada.setIdEvaluacionOrigen("ERP-EVAL-001");

        when(matriculaRepository
                .findByEstudiante_CodigoEstudianteAndMateria_CodigoMateriaAndPeriodoAcademico_Codigo(
                        ESTUDIANTE, MATERIA, PERIODO))
                .thenReturn(Optional.of(new Matricula()));
        when(evaluacionRepository.findByIdEvaluacionOrigen("ERP-EVAL-001"))
                .thenReturn(Optional.of(yaRegistrada));
        when(evaluacionRepository.findByMatricula_Id(any()))
                .thenReturn(List.of(yaRegistrada));

        EvaluacionSyncResponse respuesta = servicio.sincronizar(peticion());

        // Se reusó la fila existente, con los valores del evento reprocesado.
        verify(evaluacionRepository).save(yaRegistrada);
        assertThat(yaRegistrada.getValor()).isEqualByComparingTo("4.00");
        assertThat(respuesta.totalEvaluacionesRegistradas()).isEqualTo(1);
    }

    private EvaluacionSyncRequest peticion() {
        return new EvaluacionSyncRequest(
                "ERP-EVAL-001",
                ESTUDIANTE, MATERIA, PERIODO,
                TipoEvaluacion.PARCIAL, "Parcial 1",
                new BigDecimal("4.00"), new BigDecimal("25.00"),
                LocalDate.of(2026, 8, 14));
    }

    private Evaluacion evaluacion(String valor, String porcentaje) {
        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setValor(new BigDecimal(valor));
        evaluacion.setPorcentaje(new BigDecimal(porcentaje));
        return evaluacion;
    }
}
