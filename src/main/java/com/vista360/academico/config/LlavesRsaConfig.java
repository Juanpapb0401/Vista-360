package com.vista360.academico.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Resuelve las llaves RSA del servicio según el ambiente:
 *
 * <ul>
 *   <li><b>Sin configuración</b> (desarrollo y pruebas): genera un par de
 *       llaves efímero en memoria al arrancar. La privada la usa únicamente
 *       {@link com.vista360.academico.controller.DevTokenController} para
 *       emitir tokens de prueba; muere con el proceso y nunca toca el disco
 *       ni el repositorio.</li>
 *   <li><b>Con {@code app.security.public-key-path}</b> (ambiente real): carga
 *       solo la llave pública que publica la plataforma de identidad (SUP-06,
 *       docs/00-supuestos.md). No hay llave privada: este servicio valida
 *       tokens, no los emite.</li>
 * </ul>
 *
 * <p>Antes las llaves de desarrollo vivían como archivos PEM versionados en el
 * repositorio. Se eliminó ese esquema porque una llave privada en un
 * repositorio público permite a cualquiera firmar tokens válidos si el
 * despliegue olvida sobrescribir la configuración; generar el par al vuelo
 * conserva la comodidad de "clonar y correr" sin ese riesgo.
 */
@Configuration
public class LlavesRsaConfig {

    private static final Logger log = LoggerFactory.getLogger(LlavesRsaConfig.class);

    @Bean
    public LlavesRsa llavesRsa(
            @Value("${app.security.public-key-path:}") String rutaLlavePublica,
            ResourceLoader resourceLoader
    ) {
        if (rutaLlavePublica == null || rutaLlavePublica.isBlank()) {
            log.warn("app.security.public-key-path no está configurado: se genera un par de llaves RSA "
                    + "efímero en memoria. Válido solo para desarrollo y pruebas; en un ambiente real, "
                    + "configure la llave pública de la plataforma de identidad.");
            KeyPair par = generarParEfimero();
            return new LlavesRsa((RSAPublicKey) par.getPublic(), (RSAPrivateKey) par.getPrivate());
        }

        RSAPublicKey publica = RsaKeyLoader.cargarLlavePublica(resourceLoader.getResource(rutaLlavePublica));
        return new LlavesRsa(publica, null);
    }

    private KeyPair generarParEfimero() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("El runtime no soporta RSA, no se pueden generar llaves de desarrollo", e);
        }
    }
}
