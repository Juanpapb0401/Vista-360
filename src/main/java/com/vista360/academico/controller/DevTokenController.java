package com.vista360.academico.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vista360.academico.config.RsaKeyLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Emite JWT de prueba, firmados con la llave privada de desarrollo (par de la
 * llave pública que usa {@link com.vista360.academico.config.SecurityConfig}
 * para validar). Solo existe con el perfil {@code dev} activo.
 *
 * <p>Reemplaza, únicamente para poder probar este servicio de forma
 * aislada, a la plataforma de identidad real descrita en SUP-06
 * (docs/00-supuestos.md). No representa una decisión de arquitectura: en
 * ningún ambiente real existiría un endpoint que emite tokens sin
 * autenticar credenciales.
 */
@RestController
@Profile("dev")
@RequestMapping("/api/v1/dev")
public class DevTokenController {

    private static final Set<String> ROLES_VALIDOS = Set.of("ESTUDIANTE", "SERVICE");

    private final RSAPrivateKey llavePrivada;

    public DevTokenController(
            @Value("${app.security.private-key-path:classpath:keys/dev-private.pem}") Resource llavePrivadaResource
    ) {
        this.llavePrivada = RsaKeyLoader.cargarLlavePrivada(llavePrivadaResource);
    }

    /**
     * Ejemplo: GET /api/v1/dev/token?rol=ESTUDIANTE&codigoEstudiante=A00123456
     * Ejemplo: GET /api/v1/dev/token?rol=SERVICE
     */
    @GetMapping("/token")
    public Map<String, String> emitirToken(
            @RequestParam String rol,
            @RequestParam(required = false) String codigoEstudiante
    ) {
        if (!ROLES_VALIDOS.contains(rol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "rol debe ser uno de " + ROLES_VALIDOS);
        }
        if ("ESTUDIANTE".equals(rol) && (codigoEstudiante == null || codigoEstudiante.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "codigoEstudiante es obligatorio cuando rol=ESTUDIANTE");
        }

        String sujeto = "ESTUDIANTE".equals(rol) ? codigoEstudiante : "vista360-core";
        Instant ahora = Instant.now();

        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(sujeto)
                    .issuer("vista360-dev-issuer")
                    .issueTime(Date.from(ahora))
                    .expirationTime(Date.from(ahora.plus(1, ChronoUnit.HOURS)))
                    .claim("rol", rol)
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(llavePrivada));

            return Map.of(
                    "access_token", jwt.serialize(),
                    "token_type", "Bearer",
                    "rol", rol,
                    "sub", sujeto
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo firmar el token", e);
        }
    }
}
