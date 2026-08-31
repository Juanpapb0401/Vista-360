package com.vista360.academico.config;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Lee una llave pública RSA en formato PEM. Se usa cuando un ambiente real
 * configura {@code app.security.public-key-path} con la llave que publica la
 * plataforma de identidad (Saamfi u otro emisor compatible con OIDC, ver
 * SUP-06 en docs/00-supuestos.md). En desarrollo no hay archivo: el par de
 * llaves se genera en memoria (ver {@link LlavesRsaConfig}).
 */
public final class RsaKeyLoader {

    private RsaKeyLoader() {
    }

    public static RSAPublicKey cargarLlavePublica(Resource recurso) {
        try {
            String contenido = limpiarPem(recurso, "PUBLIC KEY");
            byte[] bytes = Base64.getDecoder().decode(contenido);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo cargar la llave pública RSA de desarrollo", e);
        }
    }

    private static String limpiarPem(Resource recurso, String etiqueta) throws IOException {
        try (InputStream in = recurso.getInputStream()) {
            String texto = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return texto
                    .replace("-----BEGIN " + etiqueta + "-----", "")
                    .replace("-----END " + etiqueta + "-----", "")
                    .replaceAll("\\s", "");
        }
    }
}
