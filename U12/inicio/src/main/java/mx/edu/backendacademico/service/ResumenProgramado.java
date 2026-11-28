package mx.edu.backendacademico.service;

import io.micrometer.core.instrument.MeterRegistry;
import mx.edu.backendacademico.repository.jpa.InscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResumenProgramado {
    private static final Logger log = LoggerFactory.getLogger(ResumenProgramado.class);
    private final InscripcionRepository repository;
    private final MeterRegistry metricas;

    public ResumenProgramado(InscripcionRepository repository, MeterRegistry metricas) {
        this.repository = repository;
        this.metricas = metricas;
    }

    @Scheduled(fixedDelayString = "${resumen.intervalo-ms:60000}", initialDelay = 60000)
    public void resumir() {
        long total = repository.count();
        log.info("Total de inscripciones={}", total);
        metricas.counter("academico.resumen.ejecuciones").increment();
    }
}