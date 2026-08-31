package com.vista360.academico.controller.advice;

import com.vista360.academico.dto.ErrorResponse;
import com.vista360.academico.exception.NoAutorizadoException;
import com.vista360.academico.exception.ParametroInvalidoException;
import com.vista360.academico.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Traduce las excepciones del dominio a las respuestas de error descritas
 * en docs/02-especificacion-servicio.md, con un cuerpo uniforme en todos
 * los casos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarParametroInvalido(ParametroInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("PARAMETRO_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionDeCuerpo(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("PARAMETRO_INVALIDO", mensaje));
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<ErrorResponse> manejarNoAutorizado(NoAutorizadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("NO_AUTORIZADO", ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RECURSO_NO_ENCONTRADO", ex.getMessage()));
    }

    /**
     * Un parámetro con el tipo equivocado, por ejemplo {@code ?page=abc}. Sin este
     * manejador la excepción caía en el catch-all de abajo y el servicio respondía
     * 500 a un error que es del cliente, contradiciendo el contrato.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoDeParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("PARAMETRO_INVALIDO",
                        "El parámetro " + ex.getName() + " no tiene un valor válido"));
    }

    /** Cuerpo de la petición ausente o con JSON malformado. También es un error del cliente. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("PARAMETRO_INVALIDO",
                        "El cuerpo de la petición está ausente o no es un JSON válido"));
    }

    /**
     * Ruta inexistente. Sin este manejador, el catch-all la convertía en 500;
     * el contrato define 404 para un recurso que no existe.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RECURSO_NO_ENCONTRADO",
                        "La ruta solicitada no existe"));
    }

    /**
     * Choque de escrituras concurrentes que sobrevivió al reintento del
     * controlador (ver SincronizacionController): la restricción de unicidad
     * de idempotencia o el bloqueo optimista de la matrícula. Es una condición
     * transitoria, no un fallo del servicio: 409 para que el emisor reentregue.
     */
    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> manejarConflictoDeEscritura(Exception ex) {
        log.warn("Conflicto de escritura concurrente: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICTO_CONCURRENCIA",
                        "El registro fue modificado por otra operación; reintente la entrega del evento"));
    }

    /**
     * Última red de seguridad. Hacia el cliente responde un mensaje genérico, para no
     * filtrar detalles internos, pero deja la traza completa en el log: un fallo que
     * no se registra es un fallo que no se puede diagnosticar después, y el Escenario A
     * de la Parte 4 (incidente intermitente y difícil de reproducir) depende justamente
     * de tener esa evidencia (SUP-23).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorNoPrevisto(Exception ex) {
        log.error("Error no previsto procesando la petición", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("ERROR_INTERNO", "Ocurrió un error inesperado"));
    }
}
