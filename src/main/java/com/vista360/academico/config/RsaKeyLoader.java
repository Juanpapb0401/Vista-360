package com.vista360.academico.config;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Lee las llaves RSA en formato PEM usadas para firmar y validar los JWT de
 * desarrollo (ver src/main/resources/keys). En producción, la llave pública
 * la publicaría la plataforma de identidad real (Saamfi u otro emisor
 * compatible con OIDC, ver SUP-06 en docs/00-supuestos.md); aquí se carga
 * de un archivo local para poder validar el diseño de seguridad sin depender
 * de un proveedor externo.
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

    public static RSAPrivateKey cargarLlavePrivada(Resource recurso) {
        try {
            String contenido = limpiarPem(recurso, "PRIVATE KEY");
            byte[] bytes = Base64.getDecoder().decode(contenido);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo cargar la llave privada RSA de desarrollo", e);
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
