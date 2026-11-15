# Sprint 3: Incidents y Communities

Los contextos se ejecutan como aplicaciones Spring Boot independientes, con bases MySQL y usuarios propios. Account conserva los UUID públicos y referencias heredadas. Gateway dirige cada contexto a su servicio; el monolito queda fuera del perfil predeterminado y ya no escanea los contextos extraídos.

## Ejecución local con Docker

Desde la raíz del backend, con Docker Desktop iniciado:

```powershell
.\scripts\Start-DockerLocal.ps1
# Si las imágenes ya están construidas:
.\scripts\Start-DockerLocal.ps1 -SkipBuild
python .\scripts\verify_sprint3_docker.py
```

El proyecto Compose conserva el nombre `instalert-sprint2` para reutilizar los volúmenes de Account y Emergencies. Las imágenes de esta versión utilizan la etiqueta `sprint3-local`. No ejecutar `down -v`: borraría las bases y los adjuntos.

| Componente | Dirección |
|---|---|
| Angular servido por Nginx | http://localhost:4201 |
| Gateway publicado | http://localhost:8085/api/v1 |
| Account | ms-account:8081 (red Docker) |
| Emergencies | ms-emergencies:8083 (red Docker) |
| Incidents | ms-incidents:8084 (red Docker) |
| Communities | ms-communities:8086 (red Docker) |

Las bases y microservicios no publican puertos al host. El navegador usa `/api/v1` mediante Nginx. Las credenciales permanecen en `.env.docker`, ignorado por Git; el inicializador preserva las existentes y genera únicamente las que falten.

## Transferencia desde Sprint 2

Se conserva la base anterior y se detiene su escritor antes de transferir datos. Tras iniciar los nuevos servicios para aplicar Flyway, `scripts/migrate_sprint3_docker.py` copia únicamente las tablas de cada contexto desde `monolith-db`. Conserva IDs, contactos, preferencias, eventos procesados, destinatarios y bandejas. Rechaza destinos no vacíos y compara conteos tabla por tabla. Los SQL privados y el informe quedan en `target/local-run/sprint3-transfer-*`; no deben publicarse porque contienen datos de usuarios.

El acceso a la base de origen corresponde a una operación de migración, no a una dependencia en ejecución de los nuevos servicios. No se borran ni reescriben los datos de origen. Antes de repetir una transferencia o revertir después de nuevas escrituras se requiere conciliarlas; cambiar una ruta al monolito por sí solo perdería la visibilidad de datos nuevos. Mantener el Gateway detenido mientras se hace esa conciliación.

## Operaciones

Incidents: POST y GET `/incidents`, GET/DELETE `/incidents/{id}`, PUT `/incidents/{id}/status`, GET/POST `/incidents/{id}/evidences`, GET `/incidents/{id}/evidences/{uuid}`. Listado paginado con `paged=true&page=0&size=50` y filtros `type`, `severity`, `status`, `from`, `to` y rectángulo `minLat/maxLat/minLng/maxLng`. Los reportes son visibles para usuarios autenticados; cambios y archivos requieren propietario o administrador. El propietario siempre se deriva de la sesión.

Los archivos PNG/JPEG/PDF se verifican por firma, tipo y límite de 5 MB. Los objetos usan UUID generados y un volumen privado; se descargan mediante un endpoint autenticado. Los estados PENDING/AVAILABLE/FAILED permiten recuperar interrupciones entre almacenamiento y confirmación en base.

Communities: creación, listado, consulta y eliminación; ingreso/salida de comunidades públicas; solicitudes de apoyo persistentes y resolución autorizada. Los contactos de confianza, preferencias y avisos del Sprint 2 se trasladan al nuevo servicio sin cambiar sus rutas públicas.

El monitoreo de alertas activas exige vínculo aceptado y permiso explícito `EMERGENCY_STATUS`, con expiración máxima de 24 horas y revocación. La pantalla concede una hora. Communities consulta Emergencies por HTTP interno autenticado; Gateway bloquea la ruta interna. No hay rastreo GPS continuo ni ubicación simulada presentada como real.

## Validación

`IncidentsIntegrationTest` y `CommunitiesIntegrationTest` usan MockMvc, H2 y Flyway para probar validación, permisos, archivos, comunidades, membresía y consentimiento. El script `verify_sprint3_docker.py` realiza verificaciones adicionales contra las APIs reales, MySQL, Nginx y Gateway: adjuntos, autorizaciones, avisos, caída del receptor, reintento y persistencia tras reinicios. El resultado se guarda en `target/local-run/sprint3-verification.json` sin tokens.

## Límites del alcance

Despliegue exclusivamente local; no se afirma despliegue cloud. La clasificación automática de zonas de riesgo, invitaciones de miembros a comunidades privadas, chat, push y alertas por proximidad necesitan trabajo posterior. La interfaz muestra severidad declarada de los reportes y las comunidades privadas permanecen accesibles a sus propietarios. Los avisos existentes se entregan a contactos de confianza autorizados mediante bandeja persistente.

## Cierre de pendientes de validación

Los servicios Incidents y Communities incluyen ahora escenarios ejecutables Cucumber en `src/test/resources/features`, con pasos MockMvc y persistencia H2. Incidents incluye además `EvidenceRecoveryTest`, que ejercita confirmación interrumpida, archivos parciales, archivo ausente y protección de una carga reciente.

El Gateway prueba que las rutas públicas pueden conservarse al sustituir destinos configurados (escenario acotado de modificabilidad) y bloquea interfaces internas. No se interpreta como una medición general del costo de evolución arquitectónica.

La prueba `verify_sprint3_quality.py` registra carga local con 24 consultas y concurrencia 4. `verify_sprint3_restore.py` comprueba recuperación desde snapshot en bases temporales separadas, con comparación del contenido de tablas y archivos. La reversión a binarios heredados con un esquema incompatible no está soportada: requiere conciliación y una versión compatible, como explica DOCKER_LOCAL.md.

La clasificación automática de riesgo sigue fuera del alcance; S3-T10 se limita a su definición. La difusión por proximidad, push e invitación a comunidades privadas no se declaran terminadas. Los resultados actuales deben consultarse en `docs/sprint3-validation.json`, distinguiendo pruebas H2 y verificaciones Docker/MySQL.


## Resultado de cierre local — 14/11/2026

- Java: 52 pruebas aprobadas (Incidents 13, Communities 9, Gateway 5, Account 13, Emergencies 12). Incidents y Communities incluyen tres escenarios Gherkin ejecutables cada uno; Incidents incluye cuatro casos de recuperación de cargas.
- Frontend: 21 pruebas aprobadas (Sprint 3: 5; Account: 8; Emergencies: 8). Las seis imágenes de aplicación se reconstruyeron correctamente.
- Docker: diez contenedores saludables y 29 verificaciones de integración aprobadas.
- Postman: 55 solicitudes y 78 aserciones aprobadas. Ejecutar desde el backend con `postman collection run docs/postman/sprint3/InstAlert-Sprint3.postman_collection.json --working-dir docs/postman/sprint3 --no-report-events`. En Desktop, seleccionar los archivos de `files` si solicita autorización de acceso.
- Swagger: contratos, interfaz y recursos estáticos verificados para ambos servicios; proxy rechaza solicitudes sin sesión con 401. Los recursos se obtienen de los JAR empaquetados.
- Carga local de lectura: 24 solicitudes, concurrencia 4, cero fallos; p95 108.76 ms. Es una comprobación limitada del equipo local, no una certificación de escalabilidad.
- Restauración: coincidencia de datos de tres tablas de Incidents y diez de Communities en esquemas desechables; seis archivos restaurados con bytes idénticos. No se sobrescribieron las bases activas.

Resultados resumidos versionables: `docs/sprint3-validation.json`. Los logs, respaldos y resultados detallados permanecen en `target/local-run`, excluidos de Git.

### Límites del alcance

La severidad de los reportes es indicada por el autor; no constituye clasificación validada de zonas de riesgo. Los controles de monitoreo corresponden a permisos explícitos sobre alertas, no a seguimiento GPS continuo. La recuperación probada conserva datos y entrega avisos sin repetir el efecto; no demuestra entrega push externa. La restauración de snapshots no equivale a revertir automáticamente rutas y datos al monolito. No se ha realizado despliegue cloud ni publicación en GitHub en esta revisión.
