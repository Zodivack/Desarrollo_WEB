package mx.edu.backendacademico.exception;

public class OperacionAcademicaException extends RuntimeException {
    private final String codigo;

    public OperacionAcademicaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
    public String getCodigo() { return codigo; }
}