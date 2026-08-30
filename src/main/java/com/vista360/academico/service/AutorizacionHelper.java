package com.vista360.academico.service;

import com.vista360.academico.exception.NoAutorizadoException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Aplica la regla de autorización descrita en docs/02-especificacion-servicio.md:
 *
 * <ul>
 *   <li>Token de usuario (claim {@code rol=ESTUDIANTE}): solo puede consultar
 *       su propio {@code codigoEstudiante} (el claim {@code sub} debe coincidir).</li>
 *   <li>Token de servicio (claim {@code rol=SERVICE}), emitido a Vista 360° Core:
 *       puede consultar cualquier estudiante. Este servicio no valida la
 *       asignación estudiante-profesional porque no es dueño de ese dato
 *       (SUP-02 en docs/00-supuestos.md); confía en que Vista 360° Core ya
 *       la validó antes de reenviar la solicitud.</li>
 * </ul>
 */
@Component
public class AutorizacionHelper {

    private static final String ROL_ESTUDIANTE = "ESTUDIANTE";
    private static final String ROL_SERVICE = "SERVICE";

    public void verificarAccesoAEstudiante(Jwt token, String codigoEstudianteSolicitado) {
        String rol = token.getClaimAsString("rol");

        if (ROL_SERVICE.equals(rol)) {
            return;
        }

        if (ROL_ESTUDIANTE.equals(rol)) {
            String sub = token.getSubject();
            if (sub == null || !sub.equals(codigoEstudianteSolicitado)) {
                throw new NoAutorizadoException(
                        "El token no autoriza a consultar la información del estudiante " + codigoEstudianteSolicitado);
            }
            return;
        }

        throw new NoAutorizadoException("El rol del token ('" + rol + "') no está habilitado para este servicio");
    }
}
