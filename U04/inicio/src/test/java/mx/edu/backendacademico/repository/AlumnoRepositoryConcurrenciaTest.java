package mx.edu.backendacademico.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import org.junit.jupiter.api.Test;

class AlumnoRepositoryConcurrenciaTest {
    @Test
    void dosTareasConLaMismaMatriculaProducenUnGuardadoYUnConflicto() throws Exception{
        var repository = new AlumnoRepositoryEnMemoria();
        var preparadas = new CountDownLatch(2);
        var inicio = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<Boolean> intentoDeGuardado = () -> {
                preparadas.countDown();
                if (!inicio.await(5, TimeUnit.SECONDS)) {throw new IllegalStateException("Las tareas no recibieron la señal de inicio");}
                var alumno = new Alumno(null, " a004 ", "Ada", "ada@u.mx", EstatusAlumno.ACTIVO);
                try {
                    repository.guardar(alumno);
                    return true;
                } catch (MatriculaDuplicadaException ex) {return false;}
        };
            Future<Boolean> primera = executor.submit(intentoDeGuardado);
            Future<Boolean> segunda = executor.submit(intentoDeGuardado);

            assertTrue(preparadas.await(5, TimeUnit.SECONDS), "Las dos tareas deben estar preparadas");
            inicio.countDown();

            boolean primeraGuardada = primera.get(5, TimeUnit.SECONDS);
            boolean segundaGuardada = segunda.get(5, TimeUnit.SECONDS);
            int guardados = (primeraGuardada ? 1 : 0) + (segundaGuardada ? 1 : 0);
            int conflictos = (primeraGuardada ? 0 : 1) + (segundaGuardada ? 0 : 1);

            assertEquals(1, guardados, "Debe existir exactamente un guardado");
            assertEquals(1, conflictos, "Debe existir exactamente un conflicto");
            assertEquals(1, repository.buscarTodos().size(), "El almacén debe contener un solo alumno");
        } finally {
            inicio.countDown();
            executor.shutdownNow();
        }
    }
}