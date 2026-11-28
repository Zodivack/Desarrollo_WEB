package mx.edu.backendacademico.service;

import java.util.List;
import mx.edu.backendacademico.domain.Alumno;
import mx.edu.backendacademico.domain.EstatusAlumno;
import mx.edu.backendacademico.exception.AlumnoNoEncontradoException;
import mx.edu.backendacademico.exception.MatriculaDuplicadaException;
import mx.edu.backendacademico.exception.PropietarioInvalidoException;
import mx.edu.backendacademico.repository.AlumnoRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlumnoService {
    private final AlumnoRepository repository;
    private final UserDetailsService usuarios;
    public AlumnoService(AlumnoRepository repository, UserDetailsService usuarios) {
        this.repository = repository;
        this.usuarios = usuarios;
    }
    @Cacheable(cacheNames = "alumnosLista")
    public List<Alumno> listar() {return repository.buscarTodos();}
    @Cacheable(cacheNames = "alumnos", key = "'general:' + #id")
    public Alumno buscar(Long id) {return repository.buscarPorId(id).orElseThrow(() -> new AlumnoNoEncontradoException(id));}
    @Cacheable(cacheNames = "alumnos", key = "'propietario:' + #propietario + ':' + #id")
    public Alumno buscarPropio(Long id, String propietario) {return repository.buscarPorId(id).filter(alumno -> alumno.propietario().equals(propietario)).orElseThrow(() -> new AlumnoNoEncontradoException(id));}
    @CacheEvict(cacheNames = {"alumnos", "alumnosLista"}, allEntries = true)
    @Transactional
    public Alumno crear(String matricula, String nombre, String correo, String propietario) {
        String propietarioValidado = validarPropietario(propietario);
        if (repository.existePorMatricula(matricula)) {throw new MatriculaDuplicadaException(matricula);}
        return repository.guardar(new Alumno(null, matricula, nombre, correo, EstatusAlumno.ACTIVO, propietarioValidado));
    }
    @CacheEvict(cacheNames = {"alumnos", "alumnosLista"}, allEntries = true)
    @Transactional
    public Alumno actualizar(Long id, String nombre, String correo) {
        var actual = buscar(id);
        return repository.guardar(new Alumno(id, actual.matricula(), nombre, correo, actual.estatus(), actual.propietario()));
    }
    @CacheEvict(cacheNames = {"alumnos", "alumnosLista"}, allEntries = true)
    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        repository.eliminarPorId(id);
    }
    private String validarPropietario(String propietario) {
        if (propietario == null || propietario.isBlank()) {throw new PropietarioInvalidoException("El propietario debe corresponder a una cuenta con rol ALUMNO");
        }
        String nombreUsuario = propietario.strip();
        UserDetails usuario;
        try {
            usuario = usuarios.loadUserByUsername(nombreUsuario);
        } catch (UsernameNotFoundException ex) {throw new PropietarioInvalidoException("El propietario indicado no corresponde a una cuenta con rol ALUMNO");}
        boolean esAlumno = usuario.getAuthorities().stream().anyMatch(autoridad -> autoridad.getAuthority().equals("ROLE_ALUMNO"));
        if (!esAlumno) {throw new PropietarioInvalidoException("El propietario indicado no corresponde a una cuenta con rol ALUMNO");}
        return usuario.getUsername();
    }
}