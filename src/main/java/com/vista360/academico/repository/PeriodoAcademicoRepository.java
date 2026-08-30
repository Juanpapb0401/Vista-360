package com.vista360.academico.repository;

import com.vista360.academico.domain.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, String> {

    /**
     * Periodo marcado como vigente. Se asume que a lo sumo uno lo está en un
     * momento dado (ver docs/03-modelo-datos.md); si por error hubiera más de
     * uno, se toma cualquiera y se deja como responsabilidad operativa del
     * proceso de sincronización mantenerlo consistente.
     */
    Optional<PeriodoAcademico> findFirstByVigenteTrue();
}
