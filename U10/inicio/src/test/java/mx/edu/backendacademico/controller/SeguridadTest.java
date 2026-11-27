package mx.edu.backendacademico.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import mx.edu.backendacademico.config.SecurityConfig;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.AccesoExpedienteDenegadoException;
import mx.edu.backendacademico.service.AlumnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlumnoController.class)
@Import(SecurityConfig.class)
class SeguridadTest {
    @Autowired MockMvc mvc;
    @MockitoBean AlumnoService service;
    private static final String DATOS = """
        {"matricula":"S1","nombre":"Ada","correo":"ada@u.mx"}
        """;
    private static final Alumno ALUMNO_S1 = new Alumno(1L, "S1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);
    private static final Alumno ALUMNO_S2 = new Alumno(2L, "S2", "Emmy", "emmy@u.mx", EstatusAlumno.ACTIVO);

    @Test void sinCredencialesRecibe401() throws Exception {mvc.perform(get("/api/v1/alumnos")).andExpect(status().isUnauthorized());}

    @Test void alumnoNoPuedeCrearAunqueTengaCsrf() throws Exception {
        mvc.perform(post("/api/v1/alumnos").with(httpBasic("S1", "s1123")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isForbidden());
    }

    @Test void docenteSinCsrfRecibe403() throws Exception {
        mvc.perform(post("/api/v1/alumnos").with(httpBasic("docente", "docente123")).contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isForbidden());
    }

    @Test void docenteConCsrfPuedeCrear() throws Exception {
        when(service.crear(anyString(), anyString(), anyString())).thenReturn(ALUMNO_S1);
        mvc.perform(post("/api/v1/alumnos").with(httpBasic("docente", "docente123")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isCreated());
    }

    @Test void alumnoNoPuedeConsultarElListadoCompleto() throws Exception {mvc.perform(get("/api/v1/alumnos").with(httpBasic("S1", "s1123"))).andExpect(status().isForbidden());}

    @Test void miExpedienteIgnoraLaMatriculaEnviadaPorCliente() throws Exception {
        when(service.buscarMiExpediente("S1")).thenReturn(ALUMNO_S1);
        mvc.perform(get("/api/v1/alumnos/mi-expediente").param("matricula", "S2").with(httpBasic("S1", "s1123"))).andExpect(status().isOk()).andExpect(jsonPath("$.matricula").value("S1"));
        verify(service).buscarMiExpediente("S1");
    }

    @Test void propietarioPuedeConsultarSuExpediente() throws Exception {
        when(service.buscarExpediente(1L, "S1", false)).thenReturn(ALUMNO_S1);
        mvc.perform(get("/api/v1/alumnos/1").with(httpBasic("S1", "s1123"))).andExpect(status().isOk()).andExpect(jsonPath("$.matricula").value("S1"));
    }

    @Test void alumnoAjenoNoPuedeConsultarOtroExpediente() throws Exception {
        when(service.buscarExpediente(2L, "S1", false)).thenThrow(new AccesoExpedienteDenegadoException());
        mvc.perform(get("/api/v1/alumnos/2").with(httpBasic("S1", "s1123"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACCESO_EXPEDIENTE_DENEGADO"));
    }

    @Test void administradorPuedeConsultarCualquierExpediente() throws Exception {
        when(service.buscarExpediente(2L, "admin", true)).thenReturn(ALUMNO_S2);
        mvc.perform(get("/api/v1/alumnos/2").with(httpBasic("admin", "admin123"))).andExpect(status().isOk()).andExpect(jsonPath("$.matricula").value("S2"));
    }
}