package mx.edu.backendacademico.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;

@Entity
@Table(name = "alumnos")
public class AlumnoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String matricula;
    private String nombre;
    private String correo;
    @Enumerated(EnumType.STRING)
    private EstatusAlumno estatus;
    @Column(nullable = false, length = 80)
    private String propietario;

    protected AlumnoEntity() {}
    public AlumnoEntity(Alumno alumno) {
        this.id = alumno.id();
        this.matricula = alumno.matricula();
        this.nombre = alumno.nombre();
        this.correo = alumno.correo();
        this.estatus = alumno.estatus();
        this.propietario = alumno.propietario();
    }
    public Alumno toDomain() {return new Alumno(id, matricula, nombre, correo, estatus, propietario);}

    public Long getId() {return id;}
}