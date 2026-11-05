ALTER TABLE public.alumnos
    ADD CONSTRAINT ck_alumnos_nombre_no_vacio
    CHECK (
        nombre IS NOT NULL
        AND nombre ~ '[^[:space:]]'
    );