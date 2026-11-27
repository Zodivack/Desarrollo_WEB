package mx.edu.backendacademico.exception;

public class AlumnoNoEncontradoException extends RuntimeException {
    public AlumnoNoEncontradoException(Long id) {
        super("No existe el alumno con id " + id);
    }
    public AlumnoNoEncontradoException() {super("No existe un alumno asociado a la identidad autenticada");}
}
