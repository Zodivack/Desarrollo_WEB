package mx.edu.backendacademico.config;

import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.csrf(withDefaults());
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/saludo", "/openapi.yaml", "/error").permitAll().requestMatchers("/api/v1/csrf").authenticated()
                // Un alumno consulta su propio expediente desde el principal.
                .requestMatchers(HttpMethod.GET, "/api/v1/alumnos/mi-expediente").hasRole("ALUMNO")
                // El listado general queda reservado al personal académico.
                .requestMatchers(HttpMethod.GET, "/api/v1/alumnos").hasAnyRole("DOCENTE", "ADMIN")
                // La consulta individual requiere comprobar la propiedad en el servicio.
                .requestMatchers(HttpMethod.GET, "/api/v1/alumnos/*").hasAnyRole("ALUMNO", "DOCENTE", "ADMIN")
                // Las demás operaciones requieren DOCENTE o ADMIN.
                .requestMatchers("/api/v1/**").hasAnyRole("DOCENTE", "ADMIN").anyRequest().denyAll()
        );
        http.httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {return PasswordEncoderFactories.createDelegatingPasswordEncoder();}

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        var alumnoS1 = User.withUsername("S1").password(encoder.encode("s1123")).roles("ALUMNO").build();
        var alumnoS2 = User.withUsername("S2").password(encoder.encode("s2123")).roles("ALUMNO").build();
        var docente = User.withUsername("docente").password(encoder.encode("docente123")).roles("DOCENTE").build();
        var administrador = User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build();
        return new InMemoryUserDetailsManager(alumnoS1, alumnoS2, docente, administrador);
    }
}