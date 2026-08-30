package com.vista360.academico.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vista360.academico.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;

/**
 * Resource Server de JWT. Valida la firma del token de forma local, con la
 * llave pública de desarrollo (ver RsaKeyLoader), sin llamada de red por
 * petición: es la decisión declarada en SUP-06 (docs/00-supuestos.md) y en
 * la sección 3.1 de seguridad de la Parte 3. En producción, la llave la
 * publicaría la plataforma de identidad real.
 *
 * <p>La autorización de negocio (¿puede este token ver a este estudiante?)
 * vive en {@link com.vista360.academico.service.AutorizacionHelper}, no
 * aquí; esta clase solo resuelve autenticación y el corte grueso por rol
 * para el endpoint interno de sincronización.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.public-key-path:classpath:keys/dev-public.pem}")
    private Resource llavePublicaResource;

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        RSAPublicKey llavePublica = RsaKeyLoader.cargarLlavePublica(llavePublicaResource);
        return NimbusJwtDecoder.withPublicKey(llavePublica).build();
    }

    /**
     * Mapea el claim {@code rol} del token a una autoridad Spring ({@code ROLE_ESTUDIANTE},
     * {@code ROLE_SERVICE}).
     *
     * <p>Deliberadamente no se expone como {@code @Bean}: Spring recolecta todos los beans
     * de tipo {@link Converter} para armar el {@code ConversionService} de MVC, y un
     * converter definido como lambda no permite inferir sus tipos genéricos, lo que hace
     * fallar el arranque del contexto. Solo lo necesita el filtro de seguridad de abajo,
     * así que se resuelve como método privado.
     */
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            String rol = jwt.getClaimAsString("rol");
            Collection<GrantedAuthority> authorities = rol == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentación y salud, sin autenticación.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health")
                        .permitAll()
                        // Consola web de H2: solo existe con el perfil "dev" activo.
                        .requestMatchers("/h2-console/**").permitAll()
                        // Emisor de tokens de prueba: solo existe cuando el perfil "dev" está activo
                        // (ver DevTokenController); si no está activo, la ruta simplemente no existe.
                        .requestMatchers("/api/v1/dev/**").permitAll()
                        // Endpoint de demo que simula el consumidor de eventos de sincronización:
                        // en producción esto no sería HTTP público, lo llamaría el propio proceso
                        // de sincronización con un token de servicio.
                        .requestMatchers("/api/v1/interno/**").hasAuthority("ROLE_SERVICE")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(entryPointNoAutenticado())
                        .accessDeniedHandler(handlerNoAutorizado())
                );

        return http.build();
    }

    /** 401 con el mismo formato de error que usa el resto del contrato. */
    private org.springframework.security.web.AuthenticationEntryPoint entryPointNoAutenticado() {
        return (request, response, authException) -> escribirError(
                response, 401, "NO_AUTENTICADO", "Token ausente, expirado o con firma inválida");
    }

    /** 403 con el mismo formato de error que usa el resto del contrato. */
    private AccessDeniedHandler handlerNoAutorizado() {
        return (request, response, accessDeniedException) -> escribirError(
                response, 403, "NO_AUTORIZADO", "El token no tiene permiso para acceder a este recurso");
    }

    private void escribirError(jakarta.servlet.http.HttpServletResponse response, int status,
                                String codigo, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // El charset es obligatorio: sin él, getWriter() serializa con la codificación
        // por defecto del contenedor y los mensajes en español salen con caracteres
        // corruptos ("firma inválida" -> "firma inv?lida").
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(codigo, mensaje)));
    }
}
