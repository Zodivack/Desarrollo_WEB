package mx.edu.backendacademico.exception;

public class ClaveIdempotenciaInvalidaException extends RuntimeException {
    public ClaveIdempotenciaInvalidaException(String mensaje) {super(mensaje);}
}