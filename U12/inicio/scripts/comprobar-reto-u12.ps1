$ErrorActionPreference = "Stop"

$script:BaseUrl = "http://localhost:8080"
$script:DirectorioEvidencias = Join-Path (Get-Location).Path "evidencias"
$script:ArchivoResultados = Join-Path $script:DirectorioEvidencias "resultados-http.txt"

New-Item -ItemType Directory -Force -Path $script:DirectorioEvidencias |
    Out-Null

"Comprobacion del reto U12 - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" |
    Set-Content -Path $script:ArchivoResultados -Encoding UTF8

function New-BasicHeaders {
    param(
        [string]$Usuario,
        [string]$Contrasena
    )

    $credenciales = [Convert]::ToBase64String(
        [Text.Encoding]::ASCII.GetBytes(
            ("{0}:{1}" -f $Usuario, $Contrasena)
        )
    )

    return @{
        Authorization = "Basic $credenciales"
    }
}

function Invoke-RetoU12 {
    param(
        [string]$Nombre,
        [ValidateSet("GET", "POST", "PUT", "DELETE")]
        [string]$Metodo,
        [string]$Ruta,
        [hashtable]$Headers,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Sesion,
        [int]$Esperado,
        [object]$Cuerpo = $null
    )

    $parametros = @{
        Uri = "$script:BaseUrl$Ruta"
        Method = $Metodo
        Headers = $Headers
        WebSession = $Sesion
        UseBasicParsing = $true
        ErrorAction = "Stop"
    }

    if ($null -ne $Cuerpo) {
        $parametros.ContentType = "application/json"
        $parametros.Body = ConvertTo-Json -InputObject $Cuerpo -Depth 10
    }

    $codigo = 0
    $contenido = ""

    try {
        $respuesta = Invoke-WebRequest @parametros
        $codigo = [int]$respuesta.StatusCode
        $contenido = [string]$respuesta.Content
    }
    catch {
        $respuestaHttp = $_.Exception.Response

        if ($null -eq $respuestaHttp) {
            throw
        }

        $codigo = [int]$respuestaHttp.StatusCode
    }

    $marca = "OK"

    if ($codigo -ne $Esperado) {
        $marca = "ERROR"
    }

    $linea = "{0} | Esperado: {1} | Obtenido: {2} | {3}" -f `
        $Nombre, $Esperado, $codigo, $marca

    Write-Host $linea

    Add-Content -Path $script:ArchivoResultados -Value $linea -Encoding UTF8

    if ($codigo -ne $Esperado) {
        if ($contenido) {
            Write-Host $contenido
        }

        throw "La prueba '$Nombre' esperaba HTTP $Esperado y recibió HTTP $codigo."
    }

    return [PSCustomObject]@{
        Nombre = $Nombre
        Codigo = $codigo
        Contenido = $contenido
    }
}

Write-Host "`n=== 1. Preparacion de sesiones ===" -ForegroundColor Cyan

$sesionAnonima = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$sesionDocente = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$sesionAlumno = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$sesionAlumno2 = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$sesionAdmin = New-Object Microsoft.PowerShell.Commands.WebRequestSession

$headersAnonimos = @{}
$headersDocente = New-BasicHeaders "docente" "docente123"
$headersAlumno = New-BasicHeaders "alumno" "alumno123"
$headersAlumno2 = New-BasicHeaders "alumno2" "alumno2123"
$headersAdmin = New-BasicHeaders "admin" "admin123"

Invoke-RetoU12 `
    -Nombre "Salud general" `
    -Metodo GET `
    -Ruta "/actuator/health" `
    -Headers $headersAnonimos `
    -Sesion $sesionAnonima `
    -Esperado 200 | Out-Null

Invoke-RetoU12 `
    -Nombre "Readiness inicial" `
    -Metodo GET `
    -Ruta "/actuator/health/readiness" `
    -Headers $headersAnonimos `
    -Sesion $sesionAnonima `
    -Esperado 200 | Out-Null

Write-Host "`n=== 2. Obtencion del token CSRF ===" -ForegroundColor Cyan

$csrfRespuesta = Invoke-RestMethod `
    -Uri "$script:BaseUrl/api/v1/csrf" `
    -Method GET `
    -Headers $headersDocente `
    -WebSession $sesionDocente `
    -UseBasicParsing `
    -ErrorAction Stop

$csrfToken = $csrfRespuesta.token

if ([string]::IsNullOrWhiteSpace($csrfToken)) {
    throw "No se recibio un token CSRF."
}

$headersDocenteWrite = @{
    Authorization = $headersDocente.Authorization
    "X-CSRF-TOKEN" = $csrfToken
}

Write-Host "Token CSRF obtenido correctamente." -ForegroundColor Green

Write-Host "`n=== 3. Creacion de propietarios ===" -ForegroundColor Cyan

$sufijo = [Guid]::NewGuid().ToString("N").Substring(0, 8).ToUpperInvariant()

$matriculaA1 = "U12A-$sufijo"
$matriculaA2 = "U12B-$sufijo"

$resultadoA1 = Invoke-RetoU12 `
    -Nombre "Crear alumno propietario alumno" `
    -Metodo POST `
    -Ruta "/api/v1/alumnos" `
    -Headers $headersDocenteWrite `
    -Sesion $sesionDocente `
    -Esperado 201 `
    -Cuerpo @{
        matricula = $matriculaA1
        nombre = "Ada Lovelace"
        correo = "ada.$sufijo@u.mx"
        propietario = "alumno"
    }

$alumnoA1 = $resultadoA1.Contenido | ConvertFrom-Json
$idA1 = [long]$alumnoA1.id

$resultadoA2 = Invoke-RetoU12 `
    -Nombre "Crear alumno propietario alumno2" `
    -Metodo POST `
    -Ruta "/api/v1/alumnos" `
    -Headers $headersDocenteWrite `
    -Sesion $sesionDocente `
    -Esperado 201 `
    -Cuerpo @{
        matricula = $matriculaA2
        nombre = "Emmy Noether"
        correo = "emmy.$sufijo@u.mx"
        propietario = "alumno2"
    }

$alumnoA2 = $resultadoA2.Contenido | ConvertFrom-Json
$idA2 = [long]$alumnoA2.id

if ($idA1 -le 0 -or $idA2 -le 0) {
    throw "No se recibieron identificadores validos para los alumnos."
}

Write-Host "Alumno de alumno: ID $idA1, matricula $matriculaA1"
Write-Host "Alumno de alumno2: ID $idA2, matricula $matriculaA2"

Write-Host "`n=== 4. Validaciones y conflictos ===" -ForegroundColor Cyan

Invoke-RetoU12 `
    -Nombre "Validacion de nombre demasiado corto" `
    -Metodo POST `
    -Ruta "/api/v1/alumnos" `
    -Headers $headersDocenteWrite `
    -Sesion $sesionDocente `
    -Esperado 400 `
    -Cuerpo @{
        matricula = "U12C-$sufijo"
        nombre = "Ad"
        correo = "valido.$sufijo@u.mx"
        propietario = "alumno"
    } | Out-Null

Invoke-RetoU12 `
    -Nombre "Propietario sin rol ALUMNO" `
    -Metodo POST `
    -Ruta "/api/v1/alumnos" `
    -Headers $headersDocenteWrite `
    -Sesion $sesionDocente `
    -Esperado 400 `
    -Cuerpo @{
        matricula = "U12D-$sufijo"
        nombre = "Grace Hopper"
        correo = "grace.$sufijo@u.mx"
        propietario = "docente"
    } | Out-Null

Invoke-RetoU12 `
    -Nombre "Matricula duplicada" `
    -Metodo POST `
    -Ruta "/api/v1/alumnos" `
    -Headers $headersDocenteWrite `
    -Sesion $sesionDocente `
    -Esperado 409 `
    -Cuerpo @{
        matricula = $matriculaA1
        nombre = "Ada duplicada"
        correo = "duplicada.$sufijo@u.mx"
        propietario = "alumno"
    } | Out-Null

Write-Host "`n=== 5. Autorizacion por propietario ===" -ForegroundColor Cyan

Invoke-RetoU12 `
    -Nombre "Docente lista todos los alumnos" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos" `
    -Headers $headersDocente `
    -Sesion $sesionDocente `
    -Esperado 200 | Out-Null

Invoke-RetoU12 `
    -Nombre "Alumno no puede listar todos" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos" `
    -Headers $headersAlumno `
    -Sesion $sesionAlumno `
    -Esperado 403 | Out-Null

Invoke-RetoU12 `
    -Nombre "Alumno consulta su registro" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos/$idA1" `
    -Headers $headersAlumno `
    -Sesion $sesionAlumno `
    -Esperado 200 | Out-Null

Invoke-RetoU12 `
    -Nombre "Alumno repite su consulta para comprobar cache" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos/$idA1" `
    -Headers $headersAlumno `
    -Sesion $sesionAlumno `
    -Esperado 200 | Out-Null

Invoke-RetoU12 `
    -Nombre "Alumno intenta consultar registro ajeno" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos/$idA2" `
    -Headers $headersAlumno `
    -Sesion $sesionAlumno `
    -Esperado 404 | Out-Null

Invoke-RetoU12 `
    -Nombre "Alumno2 consulta su registro" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos/$idA2" `
    -Headers $headersAlumno2 `
    -Sesion $sesionAlumno2 `
    -Esperado 200 | Out-Null

Invoke-RetoU12 `
    -Nombre "Alumno2 repite su consulta para comprobar cache" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos/$idA2" `
    -Headers $headersAlumno2 `
    -Sesion $sesionAlumno2 `
    -Esperado 200 | Out-Null

Invoke-RetoU12 `
    -Nombre "Alumno2 intenta consultar registro ajeno" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos/$idA1" `
    -Headers $headersAlumno2 `
    -Sesion $sesionAlumno2 `
    -Esperado 404 | Out-Null

Invoke-RetoU12 `
    -Nombre "Docente consulta registro de alumno2" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos/$idA2" `
    -Headers $headersDocente `
    -Sesion $sesionDocente `
    -Esperado 200 | Out-Null

Invoke-RetoU12 `
    -Nombre "Alumno inexistente" `
    -Metodo GET `
    -Ruta "/api/v1/alumnos/99999999" `
    -Headers $headersDocente `
    -Sesion $sesionDocente `
    -Esperado 404 | Out-Null

Write-Host "`n=== 6. Autenticacion y autorizacion de Actuator ===" -ForegroundColor Cyan

Invoke-RetoU12 `
    -Nombre "Metricas sin credenciales" `
    -Metodo GET `
    -Ruta "/actuator/metrics" `
    -Headers $headersAnonimos `
    -Sesion $sesionAnonima `
    -Esperado 401 | Out-Null

Invoke-RetoU12 `
    -Nombre "Metricas con rol ALUMNO" `
    -Metodo GET `
    -Ruta "/actuator/metrics" `
    -Headers $headersAlumno `
    -Sesion $sesionAlumno `
    -Esperado 403 | Out-Null

Invoke-RetoU12 `
    -Nombre "Metricas con rol ADMIN" `
    -Metodo GET `
    -Ruta "/actuator/metrics" `
    -Headers $headersAdmin `
    -Sesion $sesionAdmin `
    -Esperado 200 | Out-Null

Write-Host "`n=== 7. Falla controlada de PostgreSQL ===" -ForegroundColor Cyan

$postgresId = [string](docker compose ps -q postgres | Select-Object -First 1)

if ([string]::IsNullOrWhiteSpace($postgresId)) {
    throw "No se encontro el contenedor de PostgreSQL."
}

docker compose stop postgres | Out-Host
Start-Sleep -Seconds 5

try {
    Invoke-RetoU12 `
        -Nombre "Readiness con PostgreSQL detenido" `
        -Metodo GET `
        -Ruta "/actuator/health/readiness" `
        -Headers $headersAnonimos `
        -Sesion $sesionAnonima `
        -Esperado 503 | Out-Null

    Invoke-RetoU12 `
        -Nombre "Liveness permanece UP con PostgreSQL detenido" `
        -Metodo GET `
        -Ruta "/actuator/health/liveness" `
        -Headers $headersAnonimos `
        -Sesion $sesionAnonima `
        -Esperado 200 | Out-Null
}
finally {
    docker compose start postgres | Out-Host
}

Write-Host "`nEsperando a que PostgreSQL vuelva a estar saludable..." -ForegroundColor Cyan

$estadoPostgres = ""
$intentos = 0

while ($estadoPostgres -ne "healthy" -and $intentos -lt 20) {
    Start-Sleep -Seconds 3
    $estadoPostgres = [string](
        docker inspect --format '{{.State.Health.Status}}' $postgresId
    )
    $estadoPostgres = $estadoPostgres.Trim()
    $intentos++
}

if ($estadoPostgres -ne "healthy") {
    throw "PostgreSQL no recupero el estado healthy."
}

Invoke-RetoU12 `
    -Nombre "Readiness despues de recuperar PostgreSQL" `
    -Metodo GET `
    -Ruta "/actuator/health/readiness" `
    -Headers $headersAnonimos `
    -Sesion $sesionAnonima `
    -Esperado 200 | Out-Null

Write-Host "`n=== 8. Resumen ===" -ForegroundColor Cyan

$lineaFinal = "ID alumno={0}; ID alumno2={1}; matriculas={2},{3}" -f `
    $idA1, $idA2, $matriculaA1, $matriculaA2

Write-Host $lineaFinal -ForegroundColor Green
Add-Content -Path $script:ArchivoResultados -Value $lineaFinal -Encoding UTF8

Write-Host "Todas las comprobaciones HTTP y de disponibilidad finalizaron correctamente." -ForegroundColor Green
Write-Host "Evidencia guardada en: $script:ArchivoResultados"