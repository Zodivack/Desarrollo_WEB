package mx.edu.backendacademico.dto;

import mx.edu.backendacademico.domain.EstatusAlumno;

public record AlumnoResponse(Long id, String matricula, String nombre, String correo, EstatusAlumno estatus, String propietario) {}