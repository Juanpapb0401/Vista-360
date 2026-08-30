package com.vista360.academico.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de los dos endpoints del contrato, contra los datos
 * de V2__datos_prueba.sql. Se simula el JWT con spring-security-test en vez
 * de pasar por el flujo HTTP del DevTokenController: lo que se quiere probar
 * aquí es el contrato y la regla de autorización, no la emisión del token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MateriaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ESTUDIANTE = "A00123456";
    private static final String OTRO_ESTUDIANTE = "A00987654";

    @Test
    @DisplayName("Un estudiante consulta sus propias materias del periodo vigente")
    void materiasDelPeriodoVigente() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", ESTUDIANTE)
                        .with(tokenDeEstudiante(ESTUDIANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoEstudiante").value(ESTUDIANTE))
                .andExpect(jsonPath("$.periodoAcademico").value("2026-2"))
                .andExpect(jsonPath("$.materias.totalElements").value(3))
                // ING1234 trae tres evaluaciones cargadas: (4.00*25 + 4.50*5 + 4.80*5) / 35 = 4.19
                .andExpect(jsonPath("$.materias.content[?(@.codigoMateria=='ING1234')].notaActual")
                        .value(4.19))
                // MAT2201 no tiene evaluaciones aún, así que su nota es nula. El filtro
                // de JSONPath devuelve una lista, de ahí el contains(nullValue()).
                .andExpect(jsonPath("$.materias.content[?(@.codigoMateria=='MAT2201')].notaActual")
                        .value(contains(nullValue())));
    }

    @Test
    @DisplayName("El detalle de evaluaciones responde con las de la materia pedida")
    void evaluacionesDeUnaMateria() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias/{materia}/evaluaciones",
                        ESTUDIANTE, "ING1234")
                        .with(tokenDeEstudiante(ESTUDIANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreMateria").value("Estructuras de Datos"))
                .andExpect(jsonPath("$.evaluaciones.totalElements").value(3));
    }

    @Test
    @DisplayName("Un estudiante NO puede consultar las materias de otro estudiante")
    void estudianteNoAccedeAOtroEstudiante() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", OTRO_ESTUDIANTE)
                        .with(tokenDeEstudiante(ESTUDIANTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NO_AUTORIZADO"));
    }

    @Test
    @DisplayName("Un token de servicio sí puede consultar a cualquier estudiante")
    void tokenDeServicioAccedeACualquiera() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", OTRO_ESTUDIANTE)
                        .with(jwt().jwt(builder -> builder
                                .subject("vista360-core")
                                .claim("rol", "SERVICE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoEstudiante").value(OTRO_ESTUDIANTE));
    }

    @Test
    @DisplayName("Sin token, la petición se rechaza como no autenticada")
    void sinTokenNoAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", ESTUDIANTE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un periodo con formato inválido responde 400 con el código del contrato")
    void periodoInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", ESTUDIANTE)
                        .param("periodo", "2026")
                        .with(tokenDeEstudiante(ESTUDIANTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("Un size por encima del máximo permitido responde 400")
    void sizeSobreElMaximo() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", ESTUDIANTE)
                        .param("size", "100")
                        .with(tokenDeEstudiante(ESTUDIANTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("Una materia que el estudiante no tiene matriculada responde 404")
    void materiaNoMatriculada() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias/{materia}/evaluaciones",
                        ESTUDIANTE, "XXX9999")
                        .with(tokenDeEstudiante(ESTUDIANTE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NO_ENCONTRADO"));
    }

    /** Token de usuario con el claim {@code rol} que espera AutorizacionHelper. */
    private static org.springframework.test.web.servlet.request.RequestPostProcessor tokenDeEstudiante(
            String codigoEstudiante) {
        return jwt().jwt(builder -> builder
                .subject(codigoEstudiante)
                .claim("rol", "ESTUDIANTE"));
    }
}
