package mx.edu.backendacademico.service;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CalendarioClient {
    private final RestClient client;

    public CalendarioClient(RestClient calendarioRestClient) {this.client = calendarioRestClient;}

    public Periodo consultarPeriodo(String clave) {
        var periodo = client.get().uri("/periodos/{clave}", clave).retrieve().body(Periodo.class);
        if (periodo == null) throw new IllegalStateException("El calendario respondió sin cuerpo");
        return periodo;
    }

    public record Periodo(String clave, LocalDate inicio, LocalDate fin) { }
}