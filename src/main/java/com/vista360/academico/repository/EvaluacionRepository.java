package com.vista360.academico.repository;

import com.vista360.academico.domain.Evaluacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    /** Detalle paginado de evaluaciones de una matrícula (Endpoint 2). */
    Page<Evaluacion> findByMatricula_Id(Long matriculaId, Pageable pageable);

    /**
     * Todas las evaluaciones de una matrícula, sin paginar. Se usa
     * internamente al recalcular {@code nota_actual}; ahí sí se necesita el
     * conjunto completo, no una página.
     */
    List<Evaluacion> findByMatricula_Id(Long matriculaId);
}
