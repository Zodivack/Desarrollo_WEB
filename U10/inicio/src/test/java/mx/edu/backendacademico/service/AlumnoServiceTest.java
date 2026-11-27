package mx.edu.backendacademico.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.AccesoExpedienteDenegadoException;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import mx.edu.backendacademico.exception.AlumnoNoEncontradoException;
import mx.edu.backendacademico.repository.AlumnoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AlumnoServiceTest {
    @Test void unDuplicadoNoDebeIntentarGuardarse() {
        var repository = mock(AlumnoRepository.class);
        when(repository.existePorMatricula("A1")).thenReturn(true);
        var servicio = new AlumnoService(repository);
        assertThrows(MatriculaDuplicadaException.class, () -> servicio.crear("A1", "Ada", "ada@u.mx"));
        verify(repository, never()).guardar(any());
    }

    @Test void unaAusenciaTieneSignificadoDeDominio() {
        var repository = mock(AlumnoRepository.class);
        when(repository.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(AlumnoNoEncontradoException.class, () -> new AlumnoService(repository).buscar(99L));
    }

    @Test void unAlumnoPuedeConsultarSuPropioExpediente() {
        var repository = mock(AlumnoRepository.class);
        var alumno = new Alumno(1L, "S1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(alumno));
        var servicio = new AlumnoService(repository);
        assertEquals(alumno, servicio.buscarExpediente(1L, "S1", false));
    }

    @Test void unAlumnoNoPuedeConsultarElExpedienteDeOtro() {
        var repository = mock(AlumnoRepository.class);
        var alumnoAjeno = new Alumno(2L, "S2", "Emmy", "emmy@u.mx", EstatusAlumno.ACTIVO);
        when(repository.buscarPorId(2L)).thenReturn(Optional.of(alumnoAjeno));
        var servicio = new AlumnoService(repository);
        assertThrows(AccesoExpedienteDenegadoException.class, () -> servicio.buscarExpediente(2L, "S1", false));
    }

    @Test void administradorPuedeConsultarCualquierExpediente() {
        var repository = mock(AlumnoRepository.class);
        var alumnoAjeno = new Alumno(2L, "S2", "Emmy", "emmy@u.mx", EstatusAlumno.ACTIVO);
        when(repository.buscarPorId(2L)).thenReturn(Optional.of(alumnoAjeno));
        var servicio = new AlumnoService(repository);
        assertEquals(alumnoAjeno, servicio.buscarExpediente(2L, "admin", true));
    }

    @Test void miExpedienteSeBuscaPorLaMatriculaAutenticada() {
        var repository = mock(AlumnoRepository.class);
        var alumno = new Alumno(1L, "S1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);
        when(repository.buscarPorMatricula("S1")).thenReturn(Optional.of(alumno));
        var servicio = new AlumnoService(repository);
        assertEquals(alumno, servicio.buscarMiExpediente("S1"));
        verify(repository).buscarPorMatricula("S1");
    }
}
