# Docker local — Sprint 3

El entorno actual ejecuta Account, Emergencies, Incidents y Communities como servicios independientes, además de Gateway, Angular/Nginx y cuatro bases MySQL 8.4: **diez contenedores** en el perfil predeterminado. El monolito y su base solo pertenecen al perfil `legacy` para operaciones históricas/migración; no son el receptor actual de avisos.

## Preparación desde una clonación limpia

1. Instalar Docker Desktop y habilitar contenedores Linux.
2. Clonar backend y frontend. Si no se conserva la estructura de carpetas original, ejecutar `scripts/Initialize-DockerLocal.ps1` y añadir a `.env.docker` la ruta `FRONTEND_BUILD_CONTEXT=../instalert-frontend-microservices` (ajustarla respecto del backend).
3. Ejecutar desde backend:

```powershell
.\scripts\Start-DockerLocal.ps1
# En siguientes arranques:
.\scripts\Start-DockerLocal.ps1 -SkipBuild
```

El inicializador genera únicamente secretos ausentes y conserva los existentes. `.env.docker`, respaldos SQL, tokens y resultados privados no se versionan. No regenerar contraseñas para bases ya inicializadas sin un procedimiento de cambio explícito.

## Direcciones y persistencia

- Frontend: http://localhost:4201 (Nginx :80).
- API Gateway: http://localhost:8085/api/v1 (Gateway :8080).
- Account :8081, Emergencies :8083, Incidents :8084 y Communities :8086 solo en la red Docker.
- Cada contexto utiliza una base y usuario exclusivos. Las bases no publican puertos al host.
- Los datos usan volúmenes nombrados; los adjuntos privados de Incidents usan `incident-evidence:/app/evidence`.
- Se conserva el nombre Compose `instalert-sprint2` para reutilizar los volúmenes existentes; las imágenes actuales usan `sprint3-local`.

Los healthchecks de las aplicaciones Java esperan 401 de una ruta protegida: comprueban disponibilidad HTTP, no todas las reglas del negocio. El build Docker omite tests; ejecutarlos aparte y comprobar el conjunto real.

## Validación real

```powershell
python scripts/verify_sprint3_docker.py
python scripts/verify_sprint3_quality.py
python scripts/verify_sprint3_restore.py
docker compose --env-file .env.docker ps
```

- `verify_sprint3_docker.py`: cuentas sintéticas, reportes/archivos, comunidades/apoyo, consentimiento, avisos, interrupción real de Communities y persistencia después de reiniciar. Reinicia los servicios afectados; no ejecutarlo durante una demostración sin avisar.
- `verify_sprint3_quality.py`: 24 consultas con concurrencia 4; registra p95 y errores. Es una prueba de carga local acotada, no una certificación de capacidad productiva.
- `verify_sprint3_restore.py`: pausa Incidents y Communities, respalda sus bases, restaura en esquemas temporales y compara todas las filas; restaura el archivo de evidencias en un contenedor desechable y compara SHA-256. No sobrescribe esquemas actuales. Los servicios se reanudan en finally y los esquemas temporales se eliminan. Los dumps quedan en `target/local-run`, ignorado por Git.

La recuperación comprobada es restauración de un snapshot del esquema actual. No se promete compatibilidad de todos los datos nuevos con binarios del monolito. Para una reversión operativa: congelar escrituras, preservar snapshot y archivos, restaurar una versión compatible con esos esquemas, validar y cambiar rutas únicamente después de conciliar nuevas escrituras. Volver ciegamente al monolito queda fuera del procedimiento soportado.

Los scripts `verify_docker_local.py` y los resultados de 21 comprobaciones documentan el Sprint 2 histórico. Para Sprint 3 usar los scripts anteriores; el worker ahora entrega a `ms-communities:8086`. PUBLISHED significa recepción persistida y confirmada, no lectura humana ni push.

## Swagger reproducible

Los contratos están en `services/ms-incidents/openapi.json` y `services/ms-communities/openapi.json`. El servidor documental extrae CSS y JS del webjar Swagger incluido en el JAR Spring Boot: no depende de una caché manual ni descarga una versión flotante.

```powershell
# Requiere JDK 21; prepara los JAR locales (incluye tests):
.\scripts\Build-LocalStack.ps1
# En dos terminales:
python scripts/serve_sprint3_docs.py --service incidents --port 8092
python scripts/serve_sprint3_docs.py --service communities --port 8093
```

Alternativamente copiar los JAR construidos en Docker a `services/ms-incidents/target/` y `services/ms-communities/target/` con `docker compose cp ms-incidents:/app/app.jar services/ms-incidents/target/ms-incidents-docker.jar` y el equivalente de Communities, creando antes los directorios. El servidor requiere un JAR empaquetado que contenga el webjar.

Abrir http://localhost:8092/swagger-ui/index.html y http://localhost:8093/swagger-ui/index.html. Son servidores locales de documentación fuera de Docker; las solicitudes se envían al Gateway Docker fijo de puerto 8085. Los JWT permanecen en la sesión del navegador y se ocultan en la representación Curl.

## Apagado

```powershell
docker compose --env-file .env.docker stop
```

No usar `down -v`: elimina los volúmenes. Este alcance es Docker local; no se afirma despliegue cloud ni pipeline CI ejecutado.
