package mx.edu.backendacademico.service;

import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:postgresql://localhost:5432/academico", "spring.datasource.username=academico", "spring.datasource.password=academico", "spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
class InscripcionConcurrenciaTest {
    @Autowired
    private InscripcionService servicio;
    @Autowired
    private AlumnoSpringDataRepository alumnos;
    @Autowired
    private MateriaRepository materias;
    @Autowired
    private InscripcionRepository inscripciones;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;
    private record DatosPrueba(Long alumno1, Long alumno2, Long materia, String identificador) {}
    private record ResultadoIntento(Long inscripcionId, String error) {}
    @Test
    void dosAlumnosCompitenPorUnSoloCupoYSoloUnoConsigueInscribirse() throws Exception {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        // Prepara los datos y confirma la transacción antes de iniciar los hilos.
        DatosPrueba datos = transaccion.execute(status -> crearDatos());
        assertThat(datos).isNotNull();
        String nombreFuncion = "fn_pausa_concurrencia_" + datos.identificador();
        String nombreTrigger = "trg_pausa_concurrencia_" + datos.identificador();
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            // Instala un trigger temporal que mantiene abierta la primera
            // transacción durante cinco segundos al insertar su inscripción.
            instalarTrigger(nombreFuncion, nombreTrigger, datos.materia());
            Future<ResultadoIntento> intento1 = ejecutor.submit(() -> {
                preparados.countDown();
                inicio.await();
                return intentarInscripcion(datos.alumno1(), datos.materia());
            });
            Future<ResultadoIntento> intento2 = ejecutor.submit(() -> {
                preparados.countDown();
                inicio.await();
                return intentarInscripcion(datos.alumno2(), datos.materia());
            });
            // Espera hasta que ambos hilos estén listos para competir.
            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
            // Libera ambos hilos para iniciar las operaciones simultáneamente.
            inicio.countDown();
            // Consulta PostgreSQL mientras la primera transacción sigue activa.
            List<Map<String, Object>> esperaPostgres = esperarBloqueoPostgres();
            // Exige evidencia de una sesión esperando un bloqueo en PostgreSQL.
            assertThat(esperaPostgres).as("PostgreSQL debe mostrar una sesión esperando un bloqueo").isNotEmpty();
            System.out.println("\n========== EVIDENCIA DE BLOQUEO POSTGRESQL ==========");
            esperaPostgres.forEach(sesion -> {
                System.out.println("PID: " + sesion.get("pid"));
                System.out.println("Evento de espera: " + sesion.get("wait_event"));
                System.out.println("Consulta SQL: " + sesion.get("query"));
            });
            System.out.println("=====================================================\n");
            String consultaEnEspera = String.valueOf(esperaPostgres.get(0).get("query"));
            assertThat(consultaEnEspera.toLowerCase()).as("La consulta en espera debe corresponder al bloqueo de materias").contains("materias");
            // Espera los resultados de las dos operaciones del servicio real.
            ResultadoIntento resultado1 = intento1.get(20, TimeUnit.SECONDS);
            ResultadoIntento resultado2 = intento2.get(20, TimeUnit.SECONDS);
            List<ResultadoIntento> resultados = List.of(resultado1, resultado2);
            // Exactamente un alumno consiguió una inscripción.
            assertThat(resultados).filteredOn(resultado -> resultado.inscripcionId() != null).hasSize(1);
            // El otro alumno fue rechazado por falta de cupo.
            assertThat(resultados).filteredOn(resultado -> "SIN_CUPO".equals(resultado.error())).hasSize(1);
            // PostgreSQL conserva exactamente una inscripción para la materia.
            assertThat(inscripciones.countByMateriaId(datos.materia())).isEqualTo(1);
        } finally {
            // Libera las tareas en caso de que alguna aserción falle.
            inicio.countDown();
            ejecutor.shutdownNow();
            ejecutor.awaitTermination(10, TimeUnit.SECONDS);
            // Elimina el trigger, su función y exclusivamente los datos
            // creados por esta prueba.
            transaccion.executeWithoutResult(status -> {
                jdbc.execute("DROP TRIGGER IF EXISTS " + nombreTrigger + " ON inscripciones");
                jdbc.execute("DROP FUNCTION IF EXISTS " + nombreFuncion + "()");
                jdbc.update("DELETE FROM inscripciones WHERE materia_id = ?", datos.materia());
                materias.deleteById(datos.materia());
                alumnos.deleteAllById(List.of(datos.alumno1(), datos.alumno2()));
            });
        }
    }
    private DatosPrueba crearDatos() {
        String identificador = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        AlumnoEntity alumno1 = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "U08R1" + identificador, "Alumno Concurrente 1", "concurrente1-" + identificador + "@test.mx", EstatusAlumno.ACTIVO)));
        AlumnoEntity alumno2 = alumnos.saveAndFlush(new AlumnoEntity(new Alumno(null, "U08R2" + identificador, "Alumno Concurrente 2", "concurrente2-" + identificador + "@test.mx", EstatusAlumno.ACTIVO)));
        MateriaEntity materia = materias.saveAndFlush(new MateriaEntity("MAT-RACE-" + identificador, "Materia de prueba concurrente", 8, 1));
        return new DatosPrueba(alumno1.getId(), alumno2.getId(), materia.getId(), identificador);
    }
    private void instalarTrigger(String nombreFuncion, String nombreTrigger, Long materiaId) {
        jdbc.execute("CREATE FUNCTION " + nombreFuncion + "() " + "RETURNS trigger " + "LANGUAGE plpgsql " + "AS $$ " + "BEGIN " + "    PERFORM pg_sleep(5); " + "    RETURN NEW; " + "END; " + "$$");
        jdbc.execute("CREATE TRIGGER " + nombreTrigger + " " + "BEFORE INSERT ON inscripciones " + "FOR EACH ROW " + "WHEN (NEW.materia_id = " + materiaId + ") " + "EXECUTE FUNCTION " + nombreFuncion + "()");
    }
    private List<Map<String, Object>> esperarBloqueoPostgres() throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        List<Map<String, Object>> sesionesEnEspera = List.of();
        // Consulta periódicamente las sesiones activas de PostgreSQL.
        // La consulta observadora no se incluye a sí misma.
        while (System.nanoTime() < limite && sesionesEnEspera.isEmpty()) {
            sesionesEnEspera = jdbc.queryForList(
                    """
                    SELECT pid, wait_event, query
                    FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND pid <> pg_backend_pid()
                      AND state = 'active'
                      AND wait_event_type = 'Lock'
                      AND query ILIKE '%materias%'
                    LIMIT 1
                    """
            );
            if (sesionesEnEspera.isEmpty()) {Thread.sleep(50);}
        }
        return sesionesEnEspera;
    }
    private ResultadoIntento intentarInscripcion(Long alumnoId, Long materiaId) {
        try {
            Long id = servicio.inscribir(alumnoId, materiaId);
            return new ResultadoIntento(id, null);
        } catch (OperacionAcademicaException excepcion) {return new ResultadoIntento(null, excepcion.getCodigo());}
    }
}
