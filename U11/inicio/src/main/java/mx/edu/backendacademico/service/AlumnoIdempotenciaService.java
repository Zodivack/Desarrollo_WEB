package mx.edu.backendacademico.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.dto.CrearAlumnoRequest;
import mx.edu.backendacademico.exception.ClaveIdempotenciaInvalidaException;
import mx.edu.backendacademico.exception.ClaveIdempotenciaReutilizadaException;
import mx.edu.backendacademico.repository.jpa.IdempotenciaAlumnoEntity;
import mx.edu.backendacademico.repository.jpa.IdempotenciaAlumnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlumnoIdempotenciaService {
    private final IdempotenciaAlumnoRepository idempotencias;
    private final AlumnoService alumnoService;
    private final JsonMapper jsonMapper;

    public AlumnoIdempotenciaService(IdempotenciaAlumnoRepository idempotencias, AlumnoService alumnoService, JsonMapper jsonMapper) {
        this.idempotencias = idempotencias;
        this.alumnoService = alumnoService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public Resultado crear(String clave, CrearAlumnoRequest request) {
        validarClave(clave);
        String huella = calcularHuella(request);
        // Sólo una transacción puede reservar una clave determinada.
        int reservada = idempotencias.reservar(clave, huella);
        if (reservada == 1) {
            // La clave es nueva: se crea el alumno dentro de esta transacción.
            Alumno alumno = alumnoService.crear(request.matricula(), request.nombre(), request.correo());
            AlumnoResponse respuesta = AlumnoMapper.toResponse(alumno);
            String respuestaJson = serializar(respuesta);
            String location = "/api/v1/alumnos/" + alumno.id();
            // El resultado queda guardado en la misma transacción.
            int actualizados = idempotencias.completar(clave, respuestaJson, location);
            if (actualizados != 1) {throw new IllegalStateException("No se pudo completar el registro de idempotencia");}
            return new Resultado(respuesta, location);
        }
        // La clave ya estaba registrada por una solicitud anterior.
        IdempotenciaAlumnoEntity existente = idempotencias.findById(clave).orElseThrow(() -> new IllegalStateException("No se encontró el registro de idempotencia reservado"));
        if (!huella.equals(existente.getHuella().trim())) {throw new ClaveIdempotenciaReutilizadaException();}
        if (existente.getRespuestaJson() == null || existente.getLocation() == null) {throw new IllegalStateException("El registro de idempotencia no contiene una respuesta completa");}
        // Se devuelve la respuesta original, no se crea otro alumno.
        return new Resultado(deserializar(existente.getRespuestaJson()), existente.getLocation());
    }

    private void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > 100) {throw new ClaveIdempotenciaInvalidaException("La clave de idempotencia debe contener entre 1 y 100 caracteres");}
    }

    private String calcularHuella(CrearAlumnoRequest request) {
        // Los prefijos de longitud evitan ambigüedades entre los campos.
        String contenido = request.matricula().length() + ":" + request.matricula() + request.nombre().length() + ":" + request.nombre() + request.correo().length() + ":" + request.correo();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resultado = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resultado);
        } catch (NoSuchAlgorithmException ex) {throw new IllegalStateException("No se encuentra disponible SHA-256", ex);}
    }

    private String serializar(AlumnoResponse respuesta) {
        try {
            return jsonMapper.writeValueAsString(respuesta);
        } catch (JacksonException ex) {
            throw new IllegalStateException("No se pudo guardar la respuesta original", ex);}
    }

    private AlumnoResponse deserializar(String respuestaJson) {
        try {
            return jsonMapper.readValue(respuestaJson, AlumnoResponse.class);
        } catch (JacksonException ex) {throw new IllegalStateException("No se pudo recuperar la respuesta original", ex);}
    }

    public record Resultado(AlumnoResponse alumno, String location) { }
}