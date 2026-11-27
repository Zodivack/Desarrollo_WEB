package mx.edu.backendacademico.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.OperacionAcademicaException;
import mx.edu.backendacademico.repository.jpa.AlumnoEntity;
import mx.edu.backendacademico.repository.jpa.AlumnoSpringDataRepository;
import mx.edu.backendacademico.repository.jpa.InscripcionRepository;
import mx.edu.backendacademico.repository.jpa.MateriaEntity;
import mx.edu.backendacademico.repository.jpa.MateriaRepository;
import mx.edu.backendacademico.service.InscripcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class PostgresIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired AlumnoSpringDataRepository alumnos;
    @Autowired MateriaRepository materias;
    @Autowired InscripcionRepository inscripciones;
    @Autowired InscripcionService servicio;
    @Autowired MockMvc mvc;

    @Test void noSobrepasaElCupoNiRepiteUnaInscripcion() {
        var a = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "P1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO)));
        var b = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "P2", "Emmy", "emmy@u.mx", EstatusAlumno.ACTIVO)));
        var materia = materias.saveAndFlush(new MateriaEntity("P101", "Pruebas", 4, 1));
        servicio.inscribir(a.getId(), materia.getId());
        var repetida = org.junit.jupiter.api.Assertions.assertThrows(OperacionAcademicaException.class, () -> servicio.inscribir(a.getId(), materia.getId()));
        assertEquals("INSCRIPCION_DUPLICADA", repetida.getCodigo());
        var llena = org.junit.jupiter.api.Assertions.assertThrows(OperacionAcademicaException.class, () -> servicio.inscribir(b.getId(), materia.getId()));
        assertEquals("SIN_CUPO", llena.getCodigo());
        assertEquals(1, inscripciones.countByMateriaId(materia.getId()));
    }

    @Test void dosSolicitudesSimultaneasConLaMismaClaveCreanUnSoloAlumno() throws Exception {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        String clave = UUID.randomUUID().toString();
        String matricula = "IDEM" + sufijo;
        String correo = "alumno+" + sufijo.toLowerCase() + "@example.com";
        String contenido = cuerpoAlumno(matricula, "Ada Lovelace", correo);
        long alumnosAntes = alumnos.count();
        CountDownLatch solicitudesListas = new CountDownLatch(2);
        CountDownLatch iniciarSolicitudes = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> primera = ejecutor.submit(() -> ejecutarSimultanea(clave, contenido, solicitudesListas, iniciarSolicitudes));
            Future<MvcResult> segunda = ejecutor.submit(() -> ejecutarSimultanea(clave, contenido, solicitudesListas, iniciarSolicitudes));
            assertTrue(solicitudesListas.await(10, TimeUnit.SECONDS), "Las dos solicitudes no quedaron listas");
            iniciarSolicitudes.countDown();
            MvcResult resultadoPrimera = primera.get(30, TimeUnit.SECONDS);
            MvcResult resultadoSegunda = segunda.get(30, TimeUnit.SECONDS);
            assertEquals(201, resultadoPrimera.getResponse().getStatus());
            assertEquals(201, resultadoSegunda.getResponse().getStatus());
            assertEquals(resultadoPrimera.getResponse().getContentAsString(), resultadoSegunda.getResponse().getContentAsString());
            assertEquals(resultadoPrimera.getResponse().getHeader("Location"), resultadoSegunda.getResponse().getHeader("Location"));
            assertEquals(alumnosAntes + 1, alumnos.count());
        } finally {
            iniciarSolicitudes.countDown();
            ejecutor.shutdownNow();
        }
    }

    @Test void unaClaveReutilizadaConContenidoDiferenteDevuelveConflicto() throws Exception {
        String clave = UUID.randomUUID().toString();
        String sufijoPrimero = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        String sufijoSegundo = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        String contenidoPrimero = cuerpoAlumno("IDEM" + sufijoPrimero, "Ada Lovelace", "ada+" + sufijoPrimero.toLowerCase() + "@example.com");
        String contenidoSegundo = cuerpoAlumno("IDEM" + sufijoSegundo, "Grace Hopper", "grace+" + sufijoSegundo.toLowerCase() + "@example.com");
        long alumnosAntes = alumnos.count();
        MvcResult primera = enviarSolicitud(clave, contenidoPrimero);
        MvcResult segunda = enviarSolicitud(clave, contenidoSegundo);
        assertEquals(201, primera.getResponse().getStatus());
        assertEquals(409, segunda.getResponse().getStatus());
        assertTrue(segunda.getResponse().getContentAsString().contains("CLAVE_IDEMPOTENCIA_REUTILIZADA"));
        assertEquals(alumnosAntes + 1, alumnos.count());
    }

    private MvcResult ejecutarSimultanea(String clave, String contenido, CountDownLatch solicitudesListas, CountDownLatch iniciarSolicitudes) throws Exception {
        solicitudesListas.countDown();
        if (!iniciarSolicitudes.await(10, TimeUnit.SECONDS)) {throw new IllegalStateException("No se recibió la señal para iniciar las solicitudes");}
        return enviarSolicitud(clave, contenido);
    }

    private MvcResult enviarSolicitud(String clave, String contenido) throws Exception {
        return mvc.perform(post("/api/v1/alumnos").with(httpBasic("docente", "docente123")).with(csrf().asHeader()).header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(contenido)).andReturn();
    }

    private String cuerpoAlumno(String matricula, String nombre, String correo) {
        return """
                {
                    "matricula": "%s",
                    "nombre": "%s",
                    "correo": "%s"
                }
                """.formatted(matricula, nombre, correo);
    }
}