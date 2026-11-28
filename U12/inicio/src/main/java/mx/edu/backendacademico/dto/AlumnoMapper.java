package mx.edu.backendacademico.dto;

import mx.edu.backendacademico.domain.Alumno;
public final class AlumnoMapper {
    private AlumnoMapper() {}
    public static AlumnoResponse toResponse(Alumno alumno) {
        return new AlumnoResponse(alumno.id(), alumno.matricula(), alumno.nombre(), alumno.correo(), alumno.estatus(), alumno.propietario());
    }
}
