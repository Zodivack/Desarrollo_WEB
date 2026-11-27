package mx.edu.backendacademico.exception;

public class ClaveIdempotenciaReutilizadaException extends RuntimeException {
    public ClaveIdempotenciaReutilizadaException() {super("La clave de idempotencia ya fue utilizada con otro contenido");}
}