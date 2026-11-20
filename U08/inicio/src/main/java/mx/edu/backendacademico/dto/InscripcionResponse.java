package mx.edu.backendacademico.dto;

import java.time.LocalDateTime;

public record InscripcionResponse(Long id, Long materiaId, String materia, LocalDateTime fecha) { }