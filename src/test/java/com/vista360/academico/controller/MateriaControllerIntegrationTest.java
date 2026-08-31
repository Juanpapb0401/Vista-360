package com.vista360.academico.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
// El perfil test carga los datos ficticios de db/testdata sin arrastrar las
// herramientas del perfil dev (consola H2, emisor de tokens, Swagger abierto).
@ActiveProfiles("test")
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

    @Test
    @DisplayName("Un parámetro con el tipo equivocado responde 400, no 500")
    void parametroConTipoInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", ESTUDIANTE)
                        .param("page", "abc")
                        .with(tokenDeEstudiante(ESTUDIANTE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("Una ruta inexistente responde 404 con el formato del contrato, no 500")
    void rutaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/ruta-que-no-existe")
                        .with(tokenDeEstudiante(ESTUDIANTE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Al paginar de a una, ninguna materia se repite ni se pierde")
    void paginacionSinRepetirNiPerderFilas() throws Exception {
        // El estudiante tiene tres materias en el periodo vigente. Recorriendo el
        // listado de a una por página deben salir las tres, sin repeticiones: es
        // lo que garantiza el orden explícito de la consulta.
        Set<String> vistas = new HashSet<>();
        for (int pagina = 0; pagina < 3; pagina++) {
            String cuerpo = mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", ESTUDIANTE)
                            .param("page", String.valueOf(pagina))
                            .param("size", "1")
                            .with(tokenDeEstudiante(ESTUDIANTE)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            vistas.add(JsonPath.read(cuerpo, "$.materias.content[0].codigoMateria"));
        }

        assertThat(vistas).containsExactlyInAnyOrder("ING1234", "MAT2201", "ING2345");
    }

    @Test
    @DisplayName("Un estudiante que no existe responde 404")
    void estudianteInexistente() throws Exception {
        // Con token de servicio: la regla de autorización (un estudiante solo
        // consulta su propio código) se evalúa antes que la existencia, así que
        // con token de estudiante este caso daría 403, no 404.
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", "A00000000")
                        .with(jwt().jwt(builder -> builder.subject("vista360-core").claim("rol", "SERVICE"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Un estudiante que existe pero no matriculó nada en el periodo responde 200 con lista vacía")
    void estudianteSinMatriculasEnElPeriodo() throws Exception {
        // A00987654 solo tiene matrículas en 2026-2; en 2026-1 existe pero no
        // cursó nada. Eso no es un 404: el identificador es válido.
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", OTRO_ESTUDIANTE)
                        .param("periodo", "2026-1")
                        .with(tokenDeEstudiante(OTRO_ESTUDIANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materias.totalElements").value(0));
    }

    @Test
    @DisplayName("Un token con rol desconocido responde 403")
    void rolDesconocido() throws Exception {
        // El token está bien firmado y autenticado, pero su rol no habilita
        // ninguna regla de autorización: 403, no 401 (el 401 es para tokens
        // ausentes, expirados o con firma inválida).
        mockMvc.perform(get("/api/v1/estudiantes/{codigo}/materias", ESTUDIANTE)
                        .with(jwt().jwt(builder -> builder.subject(ESTUDIANTE).claim("rol", "AUDITOR"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NO_AUTORIZADO"));
    }

    @Test
    @DisplayName("La salud del servicio responde sin autenticación")
    void saludPublica() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /** Token de usuario con el claim {@code rol} que espera AutorizacionHelper. */
    private static org.springframework.test.web.servlet.request.RequestPostProcessor tokenDeEstudiante(
            String codigoEstudiante) {
        return jwt().jwt(builder -> builder
                .subject(codigoEstudiante)
                .claim("rol", "ESTUDIANTE"));
    }
}
