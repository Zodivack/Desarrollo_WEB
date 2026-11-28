package mx.edu.backendacademico.repository;

import static org.junit.jupiter.api.Assertions.*;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.OperacionAcademicaException;
import mx.edu.backendacademico.repository.jpa.*;
import mx.edu.backendacademico.service.InscripcionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest @Testcontainers
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
    @Test void noSobrepasaElCupoNiRepiteUnaInscripcion() {
        var a = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "P1", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO, "alumno")));
        var b = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "P2", "Emmy", "emmy@u.mx", EstatusAlumno.ACTIVO, "alumno2")));
        var materia = materias.saveAndFlush(new MateriaEntity("P101", "Pruebas", 4, 1));
        servicio.inscribir(a.getId(), materia.getId());
        var repetida = assertThrows(OperacionAcademicaException.class, () -> servicio.inscribir(a.getId(), materia.getId()));
        assertEquals("INSCRIPCION_DUPLICADA", repetida.getCodigo());
        var llena = assertThrows(OperacionAcademicaException.class, () -> servicio.inscribir(b.getId(), materia.getId()));
        assertEquals("SIN_CUPO", llena.getCodigo());
        assertEquals(1, inscripciones.countByMateriaId(materia.getId()));
    }
    @Test void propietarioSePersisteYSeRecuperaDesdePostgres() {
        var guardado = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "P3", "Grace", "grace@u.mx", EstatusAlumno.ACTIVO, "alumno2")));
        var recuperado = alumnos.findById(guardado.getId()).orElseThrow();
        assertEquals("alumno2", recuperado.toDomain().propietario());
    }
}
