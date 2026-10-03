package mx.edu.backendacademico.controller;

// Asocia una ruta HTTP con un método y entrega datos para serialización JSON.

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SaludoController {
    @GetMapping("/saludo")
    public Map<String, String> saludar(@RequestParam(defaultValue = "Mundo") String nombre) {
        // Spring resuelve el parámetro antes de invocar este método.
        // El saludo se construye en Java y después Jackson serializa el Map.
        return Map.of("mensaje", "Hola " + nombre);
    }
}