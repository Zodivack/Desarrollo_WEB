package mx.edu.backendacademico.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotenciaAlumnoRepository extends JpaRepository<IdempotenciaAlumnoEntity, String> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO idempotencia_alumnos (clave, huella)
            VALUES (:clave, :huella)
            ON CONFLICT (clave) DO NOTHING
            """, nativeQuery = true)
    int reservar(@Param("clave") String clave, @Param("huella") String huella);
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE IdempotenciaAlumnoEntity registro
            SET registro.respuestaJson = :respuestaJson,
                registro.location = :location
            WHERE registro.clave = :clave
            """)
    int completar(@Param("clave") String clave, @Param("respuestaJson") String respuestaJson, @Param("location") String location);
}