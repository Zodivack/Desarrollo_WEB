package mx.edu.backendacademico.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AlumnoTest {
    @Test void bajaConservaIdentidadYPropietarioSinModificarElOriginal() {
        var alumno = new Alumno(1L, " a001 ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO, "alumno");
        var baja = alumno.darDeBaja();
        assertEquals("A001", baja.matricula());
        assertEquals(alumno.id(), baja.id());
        assertEquals(alumno.propietario(), baja.propietario());
        assertEquals(EstatusAlumno.BAJA, baja.estatus());
        assertEquals(EstatusAlumno.ACTIVO, alumno.estatus());
    }

    @Test void rechazaMatriculaVacia() {
        assertThrows(IllegalArgumentException.class, () -> new Alumno(null, " ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO, "alumno"));
    }
    @Test void rechazaPropietarioVacio() {
        assertThrows(IllegalArgumentException.class, () -> new Alumno(null, "A1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO, " "));
    }
    @Test void materiaRechazaCreditosNoPositivos() { assertThrows(IllegalArgumentException.class, () -> new Materia(null, "M1", "Análisis", 0)); }
}
