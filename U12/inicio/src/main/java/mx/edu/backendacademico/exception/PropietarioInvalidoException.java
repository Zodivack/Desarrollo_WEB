package mx.edu.backendacademico.exception;

public class PropietarioInvalidoException extends RuntimeException {
    public PropietarioInvalidoException(String mensaje) {
        super(mensaje);
    }
}