-- Agrega la identidad del usuario al que pertenece cada alumno.
ALTER TABLE alumnos
    ADD COLUMN propietario VARCHAR(80);
-- Los registros existentes quedan asociados a la cuenta de laboratorio original.
UPDATE alumnos
SET propietario = 'alumno'
WHERE propietario IS NULL;
-- A partir de esta migración todos los registros deben tener propietario.
ALTER TABLE alumnos
    ALTER COLUMN propietario SET NOT NULL;
ALTER TABLE alumnos
    ADD CONSTRAINT ck_alumnos_propietario_no_vacio
        CHECK (LENGTH(TRIM(propietario)) > 0);