package mx.edu.backendacademico.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import mx.edu.backendacademico.domain.*;
import mx.edu.backendacademico.service.AlumnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlumnoController.class)
@AutoConfigureMockMvc(addFilters=false)
class AlumnoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AlumnoService service;

    @Test void listarDevuelveJson() throws Exception {
        when(service.listar()).thenReturn(List.of(new Alumno(1L,"A1","Ada","ada@u.mx",EstatusAlumno.ACTIVO)));
        mvc.perform(get("/api/v1/alumnos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].matricula").value("A1"));
    }

    @Test void rechazaUnContratoInvalido() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/alumnos").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"matricula\":\"\",\"nombre\":\"A\",\"correo\":\"incorrecto\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test void crearDevuelve201YLocationCorrecto() throws Exception {when(service.crear("A2", "Grace", "grace@u.mx")).thenReturn(new Alumno(1L, "A2", "Grace", "grace@u.mx", EstatusAlumno.ACTIVO));
        mvc.perform(post("/api/v1/alumnos").contentType(MediaType.APPLICATION_JSON).content("{\"matricula\":\"A2\",\"nombre\":\"Grace\",\"correo\":\"grace@u.mx\"}")).andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/alumnos/1")).andExpect(jsonPath("$.matricula").value("A2"));
    }
}