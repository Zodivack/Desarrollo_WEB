package mx.edu.backendacademico.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.AlumnoNoEncontradoException;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import mx.edu.backendacademico.exception.PropietarioInvalidoException;
import mx.edu.backendacademico.repository.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AlumnoServiceTest {
    private UserDetailsService usuariosConAlumno() {
        var usuarios = mock(UserDetailsService.class);
        when(usuarios.loadUserByUsername("alumno")).thenReturn(User.withUsername("alumno").password("no-utilizada").roles("ALUMNO").build());
        return usuarios;
    }
    @Test void unDuplicadoNoDebeIntentarGuardarse() {
        var repository = mock(AlumnoRepository.class);
        var usuarios = usuariosConAlumno();
        when(repository.existePorMatricula("A1")).thenReturn(true);
        var servicio = new AlumnoService(repository, usuarios);
        assertThrows(MatriculaDuplicadaException.class, () -> servicio.crear("A1", "Ada", "ada@u.mx", "alumno"));
        verify(repository, never()).guardar(any());
    }
    @Test void unaAusenciaTieneSignificadoDeDominio() {
        var repository = mock(AlumnoRepository.class);
        var servicio = new AlumnoService(repository, usuariosConAlumno());
        when(repository.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(AlumnoNoEncontradoException.class, () -> servicio.buscar(99L));
    }
    @Test void alumnoPuedeConsultarSuPropioRegistro() {
        var repository = mock(AlumnoRepository.class);
        var alumno = new Alumno(7L, "A7", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO, "alumno");
        when(repository.buscarPorId(7L)).thenReturn(Optional.of(alumno));
        var servicio = new AlumnoService(repository, usuariosConAlumno());
        assertEquals(alumno, servicio.buscarPropio(7L, "alumno"));
    }

    @Test void alumnoNoPuedeConsultarUnRegistroDeOtroPropietario() {
        var repository = mock(AlumnoRepository.class);
        var alumno = new Alumno(7L, "A7", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO, "alumno2");
        when(repository.buscarPorId(7L)).thenReturn(Optional.of(alumno));
        var servicio = new AlumnoService(repository, usuariosConAlumno());
        assertThrows(AlumnoNoEncontradoException.class, () -> servicio.buscarPropio(7L, "alumno"));
    }
    @Test void noPermiteAsignarUnPropietarioQueNoSeaAlumno() {
        var repository = mock(AlumnoRepository.class);
        var usuarios = mock(UserDetailsService.class);
        when(usuarios.loadUserByUsername("docente")).thenReturn(User.withUsername("docente").password("no-utilizada").roles("DOCENTE").build());
        var servicio = new AlumnoService(repository, usuarios);
        assertThrows(PropietarioInvalidoException.class, () -> servicio.crear("A1", "Ada", "ada@u.mx", "docente"));
        verify(repository, never()).guardar(any());
    }
    @Test void noPermiteAsignarUnUsuarioInexistente() {
        var repository = mock(AlumnoRepository.class);
        var usuarios = mock(UserDetailsService.class);
        when(usuarios.loadUserByUsername("desconocido")).thenThrow(new UsernameNotFoundException("No existe"));
        var servicio = new AlumnoService(repository, usuarios);
        assertThrows(PropietarioInvalidoException.class, () -> servicio.crear("A1", "Ada", "ada@u.mx", "desconocido"));
        verify(repository, never()).guardar(any());
    }
}