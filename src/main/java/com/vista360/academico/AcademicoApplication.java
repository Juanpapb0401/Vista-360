package com.vista360.academico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio Académico de Vista 360° del Estudiante.
 *
 * <p>Dado el identificador de un estudiante, expone sus materias matriculadas
 * y sus notas actuales en el periodo académico vigente (o en uno específico).
 * Ver docs/02-especificacion-servicio.md para el contrato completo.
 *
 * <p>Este servicio es una proyección propia sincronizada desde el ERP institucional,
 * no consulta al ERP en cada lectura. Ver docs/01-arquitectura.md.
 */
@SpringBootApplication
public class AcademicoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicoApplication.class, args);
    }
}
