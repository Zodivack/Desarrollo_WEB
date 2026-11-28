package mx.edu.backendacademico.exception;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(AlumnoNoEncontradoException.class)
    ProblemDetail noEncontrado(AlumnoNoEncontradoException ex) { return problema(HttpStatus.NOT_FOUND, "ALUMNO_NO_ENCONTRADO", ex.getMessage()); }
    @ExceptionHandler(MatriculaDuplicadaException.class)
    ProblemDetail conflicto(MatriculaDuplicadaException ex) {return problema(HttpStatus.CONFLICT, "MATRICULA_DUPLICADA", ex.getMessage());}
    @ExceptionHandler(PropietarioInvalidoException.class)
    ProblemDetail propietarioInvalido(PropietarioInvalidoException ex) { return problema(HttpStatus.BAD_REQUEST, "PROPIETARIO_INVALIDO", ex.getMessage()); }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var detalle = problema(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Revisa los campos indicados");
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream().map(error -> new CampoInvalido(error.getField(), error.getDefaultMessage())).toList();
        detalle.setProperty("campos", campos);
        return handleExceptionInternal(ex, detalle, headers, status, request);
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ProblemDetail integridad(org.springframework.dao.DataIntegrityViolationException ex) {
        Throwable causa = ex;
        while (causa != null) {
            if (causa instanceof org.hibernate.exception.ConstraintViolationException restriccion) {
                String nombre = restriccion.getConstraintName();
                if ("fk_inscripcion_alumno".equalsIgnoreCase(nombre) || "fk_inscripcion_materia".equalsIgnoreCase(nombre)) {
                    return problema(HttpStatus.CONFLICT, "RECURSO_REFERENCIADO", "El recurso participa en una inscripción y no puede eliminarse");
                }
            }
            causa = causa.getCause();
        }
        return inesperado(ex);
    }
    @ExceptionHandler(OperacionAcademicaException.class)
    ProblemDetail academica(OperacionAcademicaException ex) {
        var status = ex.getCodigo().equals("MATERIA_NO_ENCONTRADA") ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return problema(status, ex.getCodigo(), ex.getMessage());
    }
    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex) {
        String referencia = UUID.randomUUID().toString();
        log.error("Falla no prevista referencia={} tipo={}", referencia, ex.getClass().getName());
        var detalle = problema(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "No fue posible completar la operación");
        detalle.setProperty("referencia", referencia);
        return detalle;
    }
    private ProblemDetail problema(HttpStatus status, String codigo, String mensaje) {
        var detalle = ProblemDetail.forStatusAndDetail(status, mensaje);
        detalle.setTitle(status.getReasonPhrase());
        detalle.setType(URI.create("urn:backend-academico:problema:" + codigo));
        detalle.setProperty("codigo", codigo);
        return detalle;
    }
    public record CampoInvalido(String campo, String mensaje) {}
}