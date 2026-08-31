package com.vista360.academico.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre el endpoint interno de sincronización: quién puede llamarlo, qué pasa
 * con una petición mal formada, y sobre todo que reprocesar el mismo evento no
 * duplique la evaluación ni distorsione la nota (SUP-09, entrega al menos una vez).
 */
@SpringBootTest
@AutoConfigureMockMvc
// El perfil test carga los datos ficticios de db/testdata sin arrastrar las
// herramientas del perfil dev (consola H2, emisor de tokens, Swagger abierto).
@ActiveProfiles("test")
// Esta clase es la única que escribe en la base. Sin la transacción de prueba,
// las evaluaciones que inserta quedarían visibles para las demás clases —que
// comparten el contexto y la base en memoria— y el resultado dependería del
// orden de ejecución. Con ella, cada prueba revierte lo que escribió.
@Transactional
class SincronizacionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String RUTA = "/api/v1/interno/sincronizacion/evaluaciones";
    private static final String ESTUDIANTE = "A00123456";

    /** MAT2201 arranca sin evaluaciones en los datos de prueba, así que su nota es nula. */
    private static final String EVENTO = """
            {
              "idEvaluacionOrigen": "ERP-EVAL-9001",
              "codigoEstudiante": "A00123456",
              "codigoMateria": "MAT2201",
              "periodoAcademico": "2026-2",
              "tipo": "PARCIAL",
              "nombre": "Parcial 1",
              "valor": 3.90,
              "porcentaje": 30.00,
              "fecha": "2026-08-28"
            }
            """;

    @Test
    @DisplayName("El mismo evento aplicado dos veces deja una sola evaluación y la misma nota")
    void reprocesarElEventoEsIdempotente() throws Exception {
        mockMvc.perform(post(RUTA).with(tokenDeServicio())
                        .contentType(MediaType.APPLICATION_JSON).content(EVENTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notaActualRecalculada").value(3.90))
                .andExpect(jsonPath("$.totalEvaluacionesRegistradas").value(1));

        // Segundo intento: es el reintento que haría la plataforma de integración.
        mockMvc.perform(post(RUTA).with(tokenDeServicio())
                        .contentType(MediaType.APPLICATION_JSON).content(EVENTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalEvaluacionesRegistradas").value(1));

        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias/{materia}/evaluaciones",
                        ESTUDIANTE, "MAT2201")
                        .with(tokenDeServicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluaciones.totalElements").value(1));
    }

    @Test
    @DisplayName("Un token de estudiante no puede sincronizar")
    void estudianteNoPuedeSincronizar() throws Exception {
        mockMvc.perform(post(RUTA)
                        .with(jwt().jwt(builder -> builder.subject(ESTUDIANTE).claim("rol", "ESTUDIANTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(EVENTO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un cuerpo con JSON malformado responde 400, no 500")
    void cuerpoMalformado() throws Exception {
        mockMvc.perform(post(RUTA).with(tokenDeServicio())
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("Un evento sin identificador de origen se rechaza: sin él no hay idempotencia")
    void eventoSinIdentificadorDeOrigen() throws Exception {
        String sinId = EVENTO.replace("\"idEvaluacionOrigen\": \"ERP-EVAL-9001\",", "");

        mockMvc.perform(post(RUTA).with(tokenDeServicio())
                        .contentType(MediaType.APPLICATION_JSON).content(sinId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("Un evento para una matrícula que no existe responde 404")
    void matriculaInexistente() throws Exception {
        String otraMateria = EVENTO.replace("MAT2201", "XXX9999");

        mockMvc.perform(post(RUTA).with(tokenDeServicio())
                        .contentType(MediaType.APPLICATION_JSON).content(otraMateria))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NO_ENCONTRADO"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor tokenDeServicio() {
        return jwt().jwt(builder -> builder.subject("vista360-core").claim("rol", "SERVICE"))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SERVICE"));
    }
}
