package mx.edu.backendacademico.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import mx.edu.backendacademico.config.SecurityConfig;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.AlumnoNoEncontradoException;
import mx.edu.backendacademico.service.AlumnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlumnoController.class) @Import(SecurityConfig.class)
class SeguridadTest {
    @Autowired MockMvc mvc;
    @MockitoBean AlumnoService service;
    private static final String DATOS = """
        {
          "matricula": "S1",
          "nombre": "Ada",
          "correo": "ada@u.mx",
          "propietario": "alumno"
        }
        """;
    @Test void sinCredencialesRecibe401() throws Exception {mvc.perform(get("/api/v1/alumnos")).andExpect(status().isUnauthorized());}
    @Test void alumnoNoPuedeListarTodosLosRegistros() throws Exception {mvc.perform(get("/api/v1/alumnos").with(httpBasic("alumno", "alumno123"))).andExpect(status().isForbidden());}
    @Test void alumnoPuedeConsultarSuPropioRegistro() throws Exception {
        when(service.buscarPropio(1L, "alumno")).thenReturn(new Alumno(1L, "A1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO, "alumno"));
        mvc.perform(get("/api/v1/alumnos/1").with(httpBasic("alumno", "alumno123"))).andExpect(status().isOk()).andExpect(jsonPath("$.propietario").value("alumno"));
    }
    @Test void alumnoNoPuedeConsultarUnRegistroAjeno() throws Exception {
        when(service.buscarPropio(2L, "alumno")).thenThrow(new AlumnoNoEncontradoException(2L));
        mvc.perform(get("/api/v1/alumnos/2").with(httpBasic("alumno", "alumno123"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("ALUMNO_NO_ENCONTRADO"));
    }
    @Test void docentePuedeConsultarUnRegistroDeOtroAlumno() throws Exception {
        when(service.buscar(2L)).thenReturn(new Alumno(2L, "A2", "Emmy", "emmy@u.mx", EstatusAlumno.ACTIVO, "alumno2"));
        mvc.perform(get("/api/v1/alumnos/2").with(httpBasic("docente", "docente123"))).andExpect(status().isOk()).andExpect(jsonPath("$.propietario").value("alumno2"));
    }
    @Test void alumnoNoPuedeCrearAunqueTengaCsrf() throws Exception {
        mvc.perform(post("/api/v1/alumnos").with(httpBasic("alumno", "alumno123")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isForbidden());
    }
    @Test void docenteSinCsrfRecibe403() throws Exception {
        mvc.perform(post("/api/v1/alumnos").with(httpBasic("docente", "docente123")).contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isForbidden());
    }
    @Test void docenteConCsrfPuedeCrear() throws Exception {
        when(service.crear(anyString(), anyString(), anyString(), anyString())).thenReturn(new Alumno(1L, "S1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO, "alumno"));
        mvc.perform(post("/api/v1/alumnos").with(httpBasic("docente", "docente123")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isCreated()).andExpect(jsonPath("$.propietario").value("alumno"));
    }
}