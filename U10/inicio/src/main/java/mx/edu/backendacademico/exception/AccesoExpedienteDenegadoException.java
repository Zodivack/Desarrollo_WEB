package mx.edu.backendacademico.exception;

public class AccesoExpedienteDenegadoException extends RuntimeException {
    public AccesoExpedienteDenegadoException() {
        super("No tienes permiso para consultar este expediente");
    }
}