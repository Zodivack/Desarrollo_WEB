CREATE TABLE idempotencia_alumnos (
    clave VARCHAR(100) NOT NULL,
    huella VARCHAR(64) NOT NULL,
    respuesta_json TEXT,
    location_uri VARCHAR(255),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_idempotencia_alumnos PRIMARY KEY (clave),
    CONSTRAINT ck_idempotencia_respuesta_completa CHECK ((respuesta_json IS NULL AND location_uri IS NULL) OR (respuesta_json IS NOT NULL AND location_uri IS NOT NULL))
);
