package mx.edu.backendacademico.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name = "idempotencia_alumnos")
public class IdempotenciaAlumnoEntity {
    @Id @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    @Column(name = "huella", nullable = false, length = 64)
    private String huella;

    @Column(name = "respuesta_json", columnDefinition = "TEXT")
    private String respuestaJson;

    @Column(name = "location_uri", length = 255)
    private String location;

    protected IdempotenciaAlumnoEntity() {}

    public String getClave() {return clave;}

    public String getHuella() {return huella;}

    public String getRespuestaJson() {return respuestaJson;}

    public String getLocation() {return location;}
}