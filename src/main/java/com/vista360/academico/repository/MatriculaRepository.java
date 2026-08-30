package com.vista360.academico.repository;

import com.vista360.academico.domain.Matricula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    /** Resumen de materias de un estudiante en un periodo (Endpoint 1). */
    Page<Matricula> findByEstudiante_CodigoEstudianteAndPeriodoAcademico_Codigo(
            String codigoEstudiante, String periodoAcademico, Pageable pageable);

    /**
     * Matrícula puntual de un estudiante en una materia, en un periodo.
     * Se usa para validar, en el Endpoint 2, que la materia solicitada
     * de verdad pertenece a ese estudiante en ese periodo antes de listar
     * sus evaluaciones (evita filtrar existencia de matrículas ajenas).
     */
    Optional<Matricula> findByEstudiante_CodigoEstudianteAndMateria_CodigoMateriaAndPeriodoAcademico_Codigo(
            String codigoEstudiante, String codigoMateria, String periodoAcademico);
}
