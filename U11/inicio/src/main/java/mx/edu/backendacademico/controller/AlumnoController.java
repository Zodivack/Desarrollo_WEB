package mx.edu.backendacademico.controller;

// Traduce HTTP a llamadas de aplicación y construye respuestas mediante DTO.

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import mx.edu.backendacademico.dto.ActualizarAlumnoRequest;
import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.dto.CrearAlumnoRequest;
import mx.edu.backendacademico.service.AlumnoIdempotenciaService;
import mx.edu.backendacademico.service.AlumnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alumnos")
public class AlumnoController {
    private final AlumnoService service;
    private final AlumnoIdempotenciaService idempotenciaService;

    public AlumnoController(AlumnoService service, AlumnoIdempotenciaService idempotenciaService) {
        this.service = service;
        this.idempotenciaService = idempotenciaService;
    }
    // GET consulta sin solicitar cambios de estado de negocio.
    @GetMapping
    public List<AlumnoResponse> listar() { return service.listar().stream().map(AlumnoMapper::toResponse).toList(); }
    // GET consulta sin solicitar cambios de estado de negocio.
    @GetMapping("/{id}")
    public AlumnoResponse buscar(@PathVariable Long id) { return AlumnoMapper.toResponse(service.buscar(id)); }
    // El servicio asigna identidad; ResponseEntity publica 201 y Location.
    @PostMapping
    public ResponseEntity<AlumnoResponse> crear(@RequestBody @Valid CrearAlumnoRequest request, @RequestHeader("Idempotency-Key") String clave) {
        AlumnoIdempotenciaService.Resultado resultado = idempotenciaService.crear(clave, request);
        return ResponseEntity.created(URI.create(resultado.location())).body(resultado.alumno());}
    // PUT sustituye todos los campos de la representación editable.
    @PutMapping("/{id}")
    public AlumnoResponse actualizar(@PathVariable Long id, @RequestBody @Valid ActualizarAlumnoRequest request) {
        return AlumnoMapper.toResponse(service.actualizar(id, request.nombre(), request.correo()));
    }
    // DELETE exitoso termina con 204, que no permite cuerpo de respuesta.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
