package mx.edu.backendacademico.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import mx.edu.backendacademico.dto.ActualizarAlumnoRequest;
import mx.edu.backendacademico.dto.AlumnoMapper;
import mx.edu.backendacademico.dto.AlumnoResponse;
import mx.edu.backendacademico.dto.CrearAlumnoRequest;
import mx.edu.backendacademico.service.AlumnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alumnos")
public class AlumnoController {
    private final AlumnoService service;
    public AlumnoController(AlumnoService service) {this.service = service;}

    @GetMapping
    public List<AlumnoResponse> listar() {return service.listar().stream().map(AlumnoMapper::toResponse).toList();}

    @GetMapping("/mi-expediente")
    public AlumnoResponse miExpediente(Principal principal) {return AlumnoMapper.toResponse(service.buscarMiExpediente(principal.getName()));}

    @GetMapping("/{id}")
    public AlumnoResponse buscar(@PathVariable Long id, Principal principal, Authentication autenticacion) {
        boolean puedeConsultarCualquierExpediente = autenticacion.getAuthorities().stream().anyMatch(autoridad -> "ROLE_DOCENTE".equals(autoridad.getAuthority()) || "ROLE_ADMIN".equals(autoridad.getAuthority()));
        return AlumnoMapper.toResponse(service.buscarExpediente(id, principal.getName(), puedeConsultarCualquierExpediente));
    }

    @PostMapping
    public ResponseEntity<AlumnoResponse> crear(@RequestBody @Valid CrearAlumnoRequest request) {
        var creado = service.crear(request.matricula(), request.nombre(), request.correo());
        return ResponseEntity.created(URI.create("/api/v1/alumnos/" + creado.id())).body(AlumnoMapper.toResponse(creado));
    }

    @PutMapping("/{id}")
    public AlumnoResponse actualizar(@PathVariable Long id, @RequestBody @Valid ActualizarAlumnoRequest request) {
        return AlumnoMapper.toResponse(service.actualizar(id, request.nombre(), request.correo()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
