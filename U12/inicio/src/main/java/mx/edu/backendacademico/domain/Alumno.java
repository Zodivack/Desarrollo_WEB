package mx.edu.backendacademico.domain;

import java.util.Locale;
import java.util.Objects;

public record Alumno(Long id, String matricula, String nombre, String correo, EstatusAlumno estatus, String propietario) {
    public Alumno {
        if (id != null && id <= 0) {throw new IllegalArgumentException("Id inválido");}
        matricula = texto(matricula, 30, "matrícula").toUpperCase(Locale.ROOT);
        nombre = texto(nombre, 120, "nombre");
        correo = texto(correo, 160, "correo");
        propietario = texto(propietario, 80, "propietario");
        Objects.requireNonNull(estatus, "El estatus es obligatorio");
    }

    private static String texto(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo) {throw new IllegalArgumentException("Valor inválido para " + campo);}
        return valor.strip();
    }

    public Alumno darDeBaja() {return new Alumno(id, matricula, nombre, correo, EstatusAlumno.BAJA, propietario);}
}
