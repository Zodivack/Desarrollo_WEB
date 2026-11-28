# U12 — Entrega reproducible

**Proyecto:** Backend Académico  
**Unidad:** 12 — Observación, caché, tareas y entrega  
**Spring Boot:** 4.1.0  
**Versión Maven:** `0.0.12-SNAPSHOT`  
**Rama:** `U12-reto-entrega-reproducible`  
**Fecha de resultados:** 27 de noviembre de 2026

## 1. Objetivo

Este documento explica cómo construir, probar, iniciar, observar, detener y volver a ejecutar el Backend Académico desde una carpeta limpia, sin depender de IntelliJ IDEA.

La entrega incluye API REST, persistencia PostgreSQL, migraciones Flyway, seguridad HTTP Basic y CSRF, Actuator/Micrometer, caché local, tarea programada, ejecución con Docker y autorización por propietario.

## 2. Estructura

Desde la raíz Maven `U12/inicio`:

```text
inicio/
├── Dockerfile
├── compose.yaml
├── pom.xml
├── requests.http
├── README-ENTREGA-U12.md
├── scripts/comprobar-reto-u12.ps1
├── evidencias/
│   ├── resultados-http.txt
│   ├── consultas-cache.txt
│   ├── propietarios-postgres.txt
│   └── migraciones-flyway.txt
└── src/
    ├── main/java/
    ├── main/resources/application.properties
    ├── main/resources/db/migration/
    ├── main/resources/static/openapi.yaml
    └── test/java/
```

`target/` es generado por Maven y no es código fuente.

## 3. Requisitos y versiones Java

- Windows 11 y PowerShell.
- JDK 25.
- Docker Desktop con motor Linux activo y Docker Compose.
- Conexión a Internet durante la primera descarga de dependencias e imágenes.
- No se necesita instalar Maven globalmente: el proyecto incluye Maven Wrapper.

La ejecución local usa JDK 25. El `pom.xml` contiene:

```xml
<java.version>21</java.version>
<maven.compiler.release>21</maven.compiler.release>
```

JDK 25 compila limitando bytecode y API a Java 21. La versión de clase esperada es 65 (Java 21). El contenedor usa `eclipse-temurin:25-jre` para ejecutar el JAR con Java 25. No se requiere un JDK 21 separado para este procedimiento.

Comprobar el entorno:

```powershell
java -version
.\mvnw.cmd -version
docker version
docker compose version
```

Comprobar la versión de bytecode después de compilar:

```powershell
javap -verbose .\target\classes\mx\edu\backendacademico\BackendAcademicoApplication.class |
    Select-String "major version"
```

Esperado: `major version: 65`.

Versiones observadas: JDK local 25.0.1, Maven Wrapper 3.9.11, Docker Desktop 4.98.0, Docker Engine 29.9.2, Compose 5.6.1 y PostgreSQL 17.11. El contenedor informó Java 25.0.5.1.

## 4. Preparación desde una carpeta limpia

1. Descomprimir o copiar el proyecto `U12/inicio` a una carpeta local.
2. Abrir PowerShell en esa carpeta. No hace falta abrir IntelliJ.
3. Confirmar que existen `pom.xml`, `mvnw.cmd`, `Dockerfile` y `compose.yaml`.
4. Comprobar los requisitos del apartado anterior.
5. Verificar que el puerto 8080 esté disponible:

```powershell
docker ps --filter "publish=8080" --format "table {{.Names}}`t{{.Ports}}"
```

Compose crea una red interna. La aplicación se conecta a PostgreSQL mediante `postgres:5432`; PostgreSQL no publica su puerto al anfitrión. El volumen persistente observado se llamó `inicio_datos_academicos`.

## 5. Construcción y pruebas

Desde `U12/inicio`:

```powershell
.\mvnw.cmd clean test package
```

El comando limpia, compila, ejecuta pruebas y crea el JAR. Verificar:

```powershell
Test-Path .\target\backend-academico-0.0.12-SNAPSHOT.jar
```

Resultado esperado: `True`.

**Resultado observado el 27/11/2026:** 25 pruebas ejecutadas, 0 fallos, 0 errores, 0 omitidas y `BUILD SUCCESS`. El tiempo registrado en la última ejecución compartida fue 22.078 s. Se observaron advertencias de `sun.misc.Unsafe`, Mockito y agentes dinámicos, pero no impidieron el éxito de Maven.

## 6. Inicio con Docker Compose

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=150 app
```

Estado esperado: servicio `app` en ejecución y publicado en `localhost:8080`; servicio `postgres` en ejecución con estado `healthy`. Los registros de la ejecución compartida confirmaron Spring Boot 4.1.0, perfil predeterminado `jpa`, conexión a PostgreSQL 17.11 y `Started BackendAcademicoApplication`.

## 7. Persistencia y migraciones Flyway

Flyway administra tres migraciones:

| Versión | Descripción |
|---|---|
| V1 | crear alumnos |
| V2 | crear materias e inscripciones |
| V3 | agregar propietario alumnos |

Consultar el historial:

```powershell
docker compose exec postgres psql -U academico -d academico -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Consultar propietarios:

```powershell
docker compose exec postgres psql -U academico -d academico -c "SELECT id, matricula, nombre, propietario FROM alumnos ORDER BY id;"
```

Comprobar valores nulos:

```powershell
docker compose exec postgres psql -U academico -d academico -c "SELECT COUNT(*) AS propietarios_nulos FROM alumnos WHERE propietario IS NULL;"
```

Guardar la consulta de propietarios como evidencia:

```powershell
docker compose exec -T postgres psql -U academico -d academico -c "SELECT id, matricula, nombre, propietario FROM alumnos WHERE matricula LIKE 'U12A-%' OR matricula LIKE 'U12B-%' ORDER BY id;" |
    Out-File -FilePath .\\evidencias\\propietarios-postgres.txt -Encoding utf8
```

Guardar el historial de Flyway:

```powershell
docker compose exec -T postgres psql -U academico -d academico -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;" |
    Out-File -FilePath .\\evidencias\\migraciones-flyway.txt -Encoding utf8
```

Resultados observados: Flyway V1, V2 y V3 tienen `success = t`; V3 dejó el esquema en versión 3; `propietarios_nulos = 0`. V3 asignó `alumno` como propietario de los registros históricos y conservó sus filas.

## 8. Seguridad y acceso por propietario

Cuentas de laboratorio:

| Usuario | Contraseña | Rol |
|---|---|---|
| `alumno` | `alumno123` | ALUMNO |
| `alumno2` | `alumno2123` | ALUMNO |
| `docente` | `docente123` | DOCENTE |
| `admin` | `admin123` | ADMIN |

Son credenciales didácticas; no deben utilizarse en producción.

Reglas:

- DOCENTE y ADMIN pueden listar y consultar cualquier alumno.
- ALUMNO no puede consultar el listado general.
- ALUMNO puede consultar un registro individual sólo cuando el propietario coincide con su usuario autenticado.
- Un registro ajeno responde 404 para no revelar su existencia.
- DOCENTE o ADMIN asignan el propietario al crear; el servicio comprueba que la cuenta exista y tenga `ROLE_ALUMNO`.
- PUT conserva el propietario y no permite cambiarlo.

La propiedad se identifica mediante el nombre de usuario porque las cuentas están en `InMemoryUserDetailsManager`. Es una mejora didáctica de autorización por recurso. En producción se recomienda asociar el recurso a una identidad persistente o a un identificador estable del proveedor de identidad.

## 9. Pruebas HTTP sin IDE

Con Compose activo, desde la raíz Maven:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\comprobar-reto-u12.ps1
```

El script crea sesiones, obtiene el token CSRF para DOCENTE, genera matrículas únicas, envía solicitudes y compara el código obtenido con el esperado. Escribe el resumen en `evidencias/resultados-http.txt`.

Cada ejecución crea dos alumnos nuevos; las filas permanecen en PostgreSQL porque el volumen es persistente. El script detiene PostgreSQL durante la prueba de disponibilidad y después lo vuelve a iniciar. No elimina el volumen.

| Escenario | Esperado | Observado |
|---|---:|---:|
| Salud general y readiness inicial | 200 | 200 |
| Crear alumno para alumno / alumno2 | 201 | 201 |
| Nombre demasiado corto | 400 | 400 |
| Propietario sin rol ALUMNO | 400 | 400 |
| Matrícula duplicada | 409 | 409 |
| DOCENTE lista alumnos | 200 | 200 |
| ALUMNO intenta listar | 403 | 403 |
| ALUMNO consulta propio | 200 | 200 |
| ALUMNO consulta ajeno | 404 | 404 |
| ALUMNO2 consulta propio | 200 | 200 |
| ALUMNO2 consulta ajeno | 404 | 404 |
| DOCENTE consulta registro ajeno | 200 | 200 |
| Alumno inexistente | 404 | 404 |
| Métricas sin credenciales | 401 | 401 |
| Métricas como ALUMNO | 403 | 403 |
| Métricas como ADMIN | 200 | 200 |
| Readiness con PostgreSQL detenido | 503 | 503 |
| Liveness con PostgreSQL detenido | 200 | 200 |
| Readiness después de recuperar PostgreSQL | 200 | 200 |

En la ejecución más reciente compartida se crearon IDs 24 y 25, con matrículas `U12A-E8141912` y `U12B-E8141912`.

### Rechazos de seguridad por causas distintas

- **401:** `/actuator/metrics` sin credenciales: la solicitud no se autenticó.
- **403:** `/actuator/metrics` con credenciales válidas de `alumno`: la identidad se autenticó, pero no tiene el rol ADMIN.

También se demuestra 403 cuando ALUMNO intenta obtener el listado completo de alumnos.

## 10. Falla controlada de PostgreSQL y readiness

El script ejecuta `docker compose stop postgres`, consulta readiness y liveness, y luego ejecuta `docker compose start postgres`.

Resultados observados durante la interrupción:

- Readiness: HTTP 503 / DOWN porque la dependencia PostgreSQL no está disponible.
- Liveness: HTTP 200 / UP porque el proceso de la aplicación sigue vivo.
- Después de recuperar PostgreSQL y esperar `healthy`, readiness vuelve a HTTP 200 / UP.

Esto diferencia la disponibilidad de la dependencia de la vida del proceso Java.

## 11. Decisión argumentada sobre la caché

Se utiliza una caché local `ConcurrentMapCacheManager` para `alumnos` y `alumnosLista`. Es apropiada para el laboratorio porque hay una sola instancia y pocos datos; permite estudiar lecturas repetidas e invalidación sin añadir Redis u otra infraestructura.

- `@Cacheable` reutiliza resultados de lectura.
- La búsqueda general y la búsqueda por propietario usan claves diferentes.
- La clave de consulta propia incluye propietario e ID, para no compartir una entrada autorizada de otra cuenta.
- `@CacheEvict` invalida las cachés al crear, actualizar o eliminar.
- `TransactionAwareCacheManagerProxy` difiere cambios de caché hasta el commit de la transacción.

Limitaciones: la caché vive en memoria del proceso, no tiene TTL ni límite de tamaño configurado, no coordina réplicas y se pierde al reiniciar la aplicación. Para producción con varias instancias se evaluaría Redis o una caché compartida, expiración y una estrategia de invalidación distribuida.

`evidencias/consultas-cache.txt` contiene SQL capturado durante una ejecución diagnóstica con `logging.level.org.hibernate.SQL=DEBUG`. Esa propiedad fue temporal y se retiró antes de reconstruir la imagen final. El extracto debe interpretarse junto con las peticiones: las comprobaciones de matrícula, consultas ajenas y búsquedas generales producen SQL distinto. No se debe contar todo SELECT a `alumnos` como un fallo de caché.

### Repetir la observación SQL de forma controlada

1. Añadir temporalmente `logging.level.org.hibernate.SQL=DEBUG` a `application.properties`.
2. Ejecutar `.\mvnw.cmd clean package`.
3. Ejecutar `docker compose up -d --build` y `docker compose restart app` para vaciar la caché en memoria.
4. Consultar dos veces seguidas el mismo `GET /api/v1/alumnos/{id}` con el mismo usuario.
5. Consultar `docker compose logs --since=2m app | Select-String "from alumnos"`. Debe verse la consulta de la primera lectura y no una consulta equivalente de la segunda lectura cacheada.
6. Repetir con otra cuenta/propietario o después de una escritura para observar claves separadas e invalidación.
7. Retirar la propiedad temporal y reconstruir la imagen definitiva.

## 12. Tarea programada y métricas

`ResumenProgramado` ejecuta `InscripcionRepository.count()` tras un retraso inicial de 60 segundos y repite con `fixedDelay` de 60 segundos después de finalizar cada ejecución. Registra el total y aumenta `academico.resumen.ejecuciones`. En una ejecución anterior el contador llegó a 22.0; el valor depende del tiempo activo de la aplicación.

Consultar con ADMIN:

```powershell
$adminBase64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
$headersAdmin = @{ Authorization = "Basic $adminBase64" }
Invoke-RestMethod -Uri "http://localhost:8080/actuator/metrics/academico.resumen.ejecuciones" -Headers $headersAdmin | ConvertTo-Json -Depth 8
```

## 13. Reinicio, cierre y persistencia

Reiniciar sólo la aplicación:

```powershell
docker compose restart app
```

Detener contenedores y red conservando datos:

```powershell
docker compose down
docker volume ls
```

Para volver a iniciar:

```powershell
docker compose up -d
```

No usar `docker compose down -v` cuando se quieran conservar los datos: `-v` elimina los volúmenes administrados por Compose.

## 14. Inventario de evidencias

| Archivo | Contenido |
|---|---|
| `evidencias/resultados-http.txt` | Códigos HTTP esperados/obtenidos y datos de prueba. |
| `evidencias/consultas-cache.txt` | Consultas SQL de la ejecución diagnóstica. |
| `evidencias/propietarios-postgres.txt` | Consulta de propietarios en PostgreSQL. |
| `evidencias/migraciones-flyway.txt` | Estado de Flyway V1, V2 y V3. |

El documento Word complementario presenta los resultados de consola compartidos y su interpretación. Si el docente exige capturas de pantalla, deben insertarse capturas auténticas del equipo; los registros textuales no se presentan como imágenes.

## 15. Resultado observado

La última ejecución compartida registró 25 pruebas sin fallos, `BUILD SUCCESS`, imagen Docker construida, servicios activos, PostgreSQL `healthy`, readiness `UP`, todas las respuestas HTTP con los códigos esperados y recuperación correcta de readiness después de detener e iniciar PostgreSQL.
