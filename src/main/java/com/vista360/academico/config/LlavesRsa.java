package com.vista360.academico.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Par de llaves RSA con el que trabaja el servicio.
 *
 * <p>La llave pública siempre existe: es la que valida la firma de los JWT.
 * La privada solo existe cuando el par se generó en memoria al arrancar
 * (modo de desarrollo, ver {@link LlavesRsaConfig}); cuando la pública viene
 * de la plataforma de identidad real, la privada es {@code null} porque este
 * servicio nunca firma tokens en un ambiente real.
 */
public record LlavesRsa(RSAPublicKey publica, RSAPrivateKey privada) {

    public boolean puedeFirmar() {
        return privada != null;
    }
}
