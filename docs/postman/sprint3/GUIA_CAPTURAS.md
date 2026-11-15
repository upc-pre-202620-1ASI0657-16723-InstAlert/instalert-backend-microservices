# Postman Sprint 3: instrucciones para capturas

1. Abrir Postman → Import → Files y seleccionar `InstAlert-Sprint3.postman_collection.json`.
2. Expandir la colección. Se puede dejar `No Environment`: baseUrl y encadenamiento pertenecen a la colección.
3. Ejecutar P01, P02 y P03 de la carpeta 00, en ese orden. No fotografiar sus JWT.
4. Para cada evidencia abrir la petición, no cambiar método, URL, autorización ni Body; pulsar Send.
5. Mostrar URL/método, Body o Params y la respuesta con el código HTTP. Arrastrar el separador horizontal para dar espacio a la respuesta.
6. Guardar capturas con el mismo prefijo I01… I16 y C01… C36. En 204 la respuesta está vacía; es correcto. En 400/401/403/404 se prueba un rechazo esperado.
7. Para I10/I14, si Postman no encuentra el archivo, ir a Body → form-data → file → Select Files y elegir el archivo correspondiente en la subcarpeta files. El campo es de tipo File. No agregar Content-Type en Headers.
8. Completar Incidents antes de Communities. Las secuencias usan cuentas de prueba, nunca datos personales. Para comenzar desde cero, volver a ejecutar la preparación; no repetir DELETE ni los requests que crean contactos aislados esperando otro resultado.
9. Los tokens caducan: para sesiones largas iniciar nuevamente la preparación y repetir cada bloque. No publicar ni exportar valores de tokenA/tokenB/tokenC.
10. Las capturas muestran ejecución HTTP local, no despliegue cloud ni pruebas de carga. C20/C26 son auxiliares de Emergencies para verificar los avisos de Communities.

## 01 - Incidents (I01 a I16)

### I01 Crear reporte
1. Seleccionar **I01 Crear reporte**.
2. Método y ruta: `POST {{baseUrl}}/incidents`.
3. Crear el reporte de A; capturar Body y respuesta con id, userId, coordenadas y estado ACTIVE.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `I01-Crear-reporte.jpg`.

### I02 Listar reportes paginados
1. Seleccionar **I02 Listar reportes paginados**.
2. Método y ruta: `GET {{baseUrl}}/incidents?paged=true&page=0&size=5`.
3. Consultar la primera página; mostrar content, page, size y totalElements.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `I02-Listar-reportes-paginados.jpg`.

### I03 Consultar reporte por ID
1. Seleccionar **I03 Consultar reporte por ID**.
2. Método y ruta: `GET {{baseUrl}}/incidents/{{incidentId}}`.
3. Recuperar el ID guardado automáticamente en I01.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `I03-Consultar-reporte-por-ID.jpg`.

### I04 Filtrar reportes y coordenadas
1. Seleccionar **I04 Filtrar reportes y coordenadas**.
2. Método y ruta: `GET {{baseUrl}}/incidents?paged=true&type=SPRINT3_POSTMAN&severity=HIGH&status=ACTIVE&minLat=-13&maxLat=-11&minLng=-78&maxLng=-76&page=0&size=5`.
3. En Params mostrar filtros de tipo, severidad, estado y rectángulo geográfico; la respuesta debe cumplirlos.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `I04-Filtrar-reportes-y-coordenadas.jpg`.

### I05 Rechazar coordenadas invalidas
1. Seleccionar **I05 Rechazar coordenadas invalidas**.
2. Método y ruta: `POST {{baseUrl}}/incidents`.
3. Enviar latitud 100; fotografiar el Body inválido y HTTP 400.
4. Pulsar **Send**. Resultado esperado: **HTTP 400**; comprobar **Test Results**.
5. Tomar captura y guardar como `I05-Rechazar-coordenadas-invalidas.jpg`.

### I06 Rechazar propietario suplantado
1. Seleccionar **I06 Rechazar propietario suplantado**.
2. Método y ruta: `POST {{baseUrl}}/incidents`.
3. A intenta crear un reporte con el identificador de B. Debe rechazarse con 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `I06-Rechazar-propietario-suplantado.jpg`.

### I07 Rechazar acceso sin sesion
1. Seleccionar **I07 Rechazar acceso sin sesion**.
2. Método y ruta: `GET {{baseUrl}}/incidents`.
3. No Auth ya está configurado. Demuestra autenticación obligatoria, HTTP 401.
4. Pulsar **Send**. Resultado esperado: **HTTP 401**; comprobar **Test Results**.
5. Tomar captura y guardar como `I07-Rechazar-acceso-sin-sesion.jpg`.

### I08 Rechazar cambio de reporte ajeno
1. Seleccionar **I08 Rechazar cambio de reporte ajeno**.
2. Método y ruta: `PUT {{baseUrl}}/incidents/{{incidentId}}/status`.
3. B intenta modificar el reporte de A. Debe devolver 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `I08-Rechazar-cambio-de-reporte-ajeno.jpg`.

### I09 Resolver reporte propio
1. Seleccionar **I09 Resolver reporte propio**.
2. Método y ruta: `PUT {{baseUrl}}/incidents/{{incidentId}}/status`.
3. A cambia su reporte a RESOLVED; mostrar el JSON de entrada y salida.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `I09-Resolver-reporte-propio.jpg`.

### I10 Adjuntar evidencia PNG
1. Seleccionar **I10 Adjuntar evidencia PNG**.
2. Método y ruta: `POST {{baseUrl}}/incidents/{{incidentId}}/evidences`.
3. Body > form-data > file: seleccionar evidencia.png si Postman solicita permiso al archivo. No agregar Content-Type manualmente. Debe responder AVAILABLE.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `I10-Adjuntar-evidencia-PNG.jpg`.

### I11 Listar evidencias privadas
1. Seleccionar **I11 Listar evidencias privadas**.
2. Método y ruta: `GET {{baseUrl}}/incidents/{{incidentId}}/evidences`.
3. Mostrar metadatos del archivo privado: nombre, tipo, tamaño y estado AVAILABLE.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `I11-Listar-evidencias-privadas.jpg`.

### I12 Descargar evidencia propia
1. Seleccionar **I12 Descargar evidencia propia**.
2. Método y ruta: `GET {{baseUrl}}/incidents/{{incidentId}}/evidences/{{evidenceId}}`.
3. Mostrar Response > Headers con Content-Type image/png, Content-Disposition y HTTP 200. El Body es binario, no JSON; no es un error.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `I12-Descargar-evidencia-propia.jpg`.

### I13 Rechazar descarga ajena
1. Seleccionar **I13 Rechazar descarga ajena**.
2. Método y ruta: `GET {{baseUrl}}/incidents/{{incidentId}}/evidences/{{evidenceId}}`.
3. B intenta descargar el adjunto privado de A; HTTP 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `I13-Rechazar-descarga-ajena.jpg`.

### I14 Rechazar archivo falso
1. Seleccionar **I14 Rechazar archivo falso**.
2. Método y ruta: `POST {{baseUrl}}/incidents/{{incidentId}}/evidences`.
3. El archivo de texto se declara image/png; la firma real no coincide. Mostrar form-data y HTTP 400.
4. Pulsar **Send**. Resultado esperado: **HTTP 400**; comprobar **Test Results**.
5. Tomar captura y guardar como `I14-Rechazar-archivo-falso.jpg`.

### I15 Eliminar reporte propio
1. Seleccionar **I15 Eliminar reporte propio**.
2. Método y ruta: `DELETE {{baseUrl}}/incidents/{{incidentId}}`.
3. A elimina el reporte sintético. HTTP 204 sin cuerpo es correcto. Tomar la captura antes de cambiar de petición.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `I15-Eliminar-reporte-propio.jpg`.

### I16 Confirmar reporte no disponible
1. Seleccionar **I16 Confirmar reporte no disponible**.
2. Método y ruta: `GET {{baseUrl}}/incidents/{{incidentId}}`.
3. Consultar el ID eliminado en I15. Debe devolver 404.
4. Pulsar **Send**. Resultado esperado: **HTTP 404**; comprobar **Test Results**.
5. Tomar captura y guardar como `I16-Confirmar-reporte-no-disponible.jpg`.

## 02 - Communities (C01 a C36)

### C01 Crear comunidad publica
1. Seleccionar **C01 Crear comunidad publica**.
2. Método y ruta: `POST {{baseUrl}}/communities`.
3. A crea una comunidad pública. Mostrar id, ownerId, isPrivate y member.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `C01-Crear-comunidad-publica.jpg`.

### C02 Listar comunidades
1. Seleccionar **C02 Listar comunidades**.
2. Método y ruta: `GET {{baseUrl}}/communities?page=0&size=50`.
3. Consultar las comunidades visibles para A; respuesta en lista.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C02-Listar-comunidades.jpg`.

### C03 Consultar comunidad por ID
1. Seleccionar **C03 Consultar comunidad por ID**.
2. Método y ruta: `GET {{baseUrl}}/communities/{{communityId}}`.
3. Consultar el ID creado en C01.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C03-Consultar-comunidad-por-ID.jpg`.

### C04 Crear comunidad privada
1. Seleccionar **C04 Crear comunidad privada**.
2. Método y ruta: `POST {{baseUrl}}/communities`.
3. Crear una comunidad privada de muestra.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `C04-Crear-comunidad-privada.jpg`.

### C05 Rechazar consulta privada ajena
1. Seleccionar **C05 Rechazar consulta privada ajena**.
2. Método y ruta: `GET {{baseUrl}}/communities/{{privateCommunityId}}`.
3. B no es propietario ni miembro; HTTP 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `C05-Rechazar-consulta-privada-ajena.jpg`.

### C06 Rechazar apoyo sin membresia
1. Seleccionar **C06 Rechazar apoyo sin membresia**.
2. Método y ruta: `POST {{baseUrl}}/communities/{{communityId}}/support-requests`.
3. B solicita apoyo antes de unirse. Debe recibir 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `C06-Rechazar-apoyo-sin-membresia.jpg`.

### C07 Unirse a comunidad publica
1. Seleccionar **C07 Unirse a comunidad publica**.
2. Método y ruta: `POST {{baseUrl}}/communities/{{communityId}}/members/me`.
3. B se incorpora a la comunidad pública. HTTP 204 sin cuerpo.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C07-Unirse-a-comunidad-publica.jpg`.

### C08 Crear solicitud de apoyo
1. Seleccionar **C08 Crear solicitud de apoyo**.
2. Método y ruta: `POST {{baseUrl}}/communities/{{communityId}}/support-requests`.
3. Ahora B, como miembro, puede crear una solicitud de apoyo.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `C08-Crear-solicitud-de-apoyo.jpg`.

### C09 Consultar solicitudes de apoyo
1. Seleccionar **C09 Consultar solicitudes de apoyo**.
2. Método y ruta: `GET {{baseUrl}}/communities/{{communityId}}/support-requests`.
3. A consulta las solicitudes de su comunidad; mostrar la creada por B.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C09-Consultar-solicitudes-de-apoyo.jpg`.

### C10 Resolver solicitud de apoyo
1. Seleccionar **C10 Resolver solicitud de apoyo**.
2. Método y ruta: `PUT {{baseUrl}}/communities/{{communityId}}/support-requests/{{supportId}}/resolve`.
3. El propietario A resuelve la solicitud. HTTP 204 sin cuerpo.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C10-Resolver-solicitud-de-apoyo.jpg`.

### C11 Salir de comunidad publica
1. Seleccionar **C11 Salir de comunidad publica**.
2. Método y ruta: `DELETE {{baseUrl}}/communities/{{communityId}}/members/me`.
3. B abandona la comunidad. El propietario A conserva su acceso.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C11-Salir-de-comunidad-publica.jpg`.

### C12 Rechazar eliminacion ajena
1. Seleccionar **C12 Rechazar eliminacion ajena**.
2. Método y ruta: `DELETE {{baseUrl}}/communities/{{communityId}}`.
3. C intenta eliminar la comunidad de A. Debe recibir 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `C12-Rechazar-eliminacion-ajena.jpg`.

### C13 Invitar contacto de confianza
1. Seleccionar **C13 Invitar contacto de confianza**.
2. Método y ruta: `POST {{baseUrl}}/communities/contacts`.
3. A invita a B como contacto; el vínculo inicia PENDING.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `C13-Invitar-contacto-de-confianza.jpg`.

### C14 Listar contactos
1. Seleccionar **C14 Listar contactos**.
2. Método y ruta: `GET {{baseUrl}}/communities/contacts`.
3. Consultar el vínculo pendiente y los UUID de sus participantes.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C14-Listar-contactos.jpg`.

### C15 Rechazar autoaceptacion
1. Seleccionar **C15 Rechazar autoaceptacion**.
2. Método y ruta: `PUT {{baseUrl}}/communities/contacts/{{contactId}}/accept`.
3. A no puede aceptar su propia invitación en nombre de B; HTTP 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `C15-Rechazar-autoaceptacion.jpg`.

### C16 Aceptar invitacion recibida
1. Seleccionar **C16 Aceptar invitacion recibida**.
2. Método y ruta: `PUT {{baseUrl}}/communities/contacts/{{contactId}}/accept`.
3. B acepta explícitamente el vínculo. HTTP 204.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C16-Aceptar-invitacion-recibida.jpg`.

### C17 Rechazar monitoreo sin permiso
1. Seleccionar **C17 Rechazar monitoreo sin permiso**.
2. Método y ruta: `GET {{baseUrl}}/communities/monitoring/{{accountA}}`.
3. Ser contacto aceptado no basta para monitorear: aún no hay permiso explícito; HTTP 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `C17-Rechazar-monitoreo-sin-permiso.jpg`.

### C18 Conceder permiso temporal
1. Seleccionar **C18 Conceder permiso temporal**.
2. Método y ruta: `POST {{baseUrl}}/communities/monitoring-grants`.
3. A autoriza a B a consultar sus alertas activas durante una hora.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `C18-Conceder-permiso-temporal.jpg`.

### C19 Listar permisos
1. Seleccionar **C19 Listar permisos**.
2. Método y ruta: `GET {{baseUrl}}/communities/monitoring-grants`.
3. B ve el permiso recibido con propietario, destinatario, alcance y expiración.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C19-Listar-permisos.jpg`.

### C20 Generar alerta para probar avisos
1. Seleccionar **C20 Generar alerta para probar avisos**.
2. Método y ruta: `POST {{baseUrl}}/emergencies`.
3. Petición auxiliar a Emergencies: genera una alerta real de A que debe llegar a la bandeja de B. No simula ni inyecta eventos internos.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `C20-Generar-alerta-para-probar-avisos.jpg`.

### C21 Consultar aviso recibido
1. Seleccionar **C21 Consultar aviso recibido**.
2. Método y ruta: `GET {{baseUrl}}/communities/notifications`.
3. Espera tres segundos y consulta la bandeja de B. Debe contener EmergencyAlertCreated y el alert_id de C20. Si aún no aparece, espera y repite solo C21.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C21-Consultar-aviso-recibido.jpg`.

### C22 Consultar monitoreo autorizado
1. Seleccionar **C22 Consultar monitoreo autorizado**.
2. Método y ruta: `GET {{baseUrl}}/communities/monitoring/{{accountA}}`.
3. Con vínculo y permiso vigentes, B obtiene la alerta activa real de A.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C22-Consultar-monitoreo-autorizado.jpg`.

### C23 Marcar aviso como leido
1. Seleccionar **C23 Marcar aviso como leido**.
2. Método y ruta: `PUT {{baseUrl}}/communities/notifications/{{noticeId}}/read`.
3. B marca el aviso de C21 como leído. HTTP 204.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C23-Marcar-aviso-como-leido.jpg`.

### C24 Consultar preferencias
1. Seleccionar **C24 Consultar preferencias**.
2. Método y ruta: `GET {{baseUrl}}/communities/notification-preferences`.
3. B consulta si está habilitada la recepción de nuevos avisos.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C24-Consultar-preferencias.jpg`.

### C25 Desactivar nuevos avisos
1. Seleccionar **C25 Desactivar nuevos avisos**.
2. Método y ruta: `PUT {{baseUrl}}/communities/notification-preferences`.
3. B desactiva recepción de nuevas alertas. Una cancelación de una alerta ya recibida todavía debe notificarse.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C25-Desactivar-nuevos-avisos.jpg`.

### C26 Cancelar alerta ya notificada
1. Seleccionar **C26 Cancelar alerta ya notificada**.
2. Método y ruta: `PATCH {{baseUrl}}/emergencies/{{alertId}}`.
3. Petición auxiliar: A cancela la alerta de C20 para comprobar el aviso de cancelación.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C26-Cancelar-alerta-ya-notificada.jpg`.

### C27 Consultar aviso de cancelacion
1. Seleccionar **C27 Consultar aviso de cancelacion**.
2. Método y ruta: `GET {{baseUrl}}/communities/notifications`.
3. La bandeja debe tener EmergencyAlertCreated y EmergencyAlertCancelled para el mismo ID, pese al opt-out. El aviso original conserva read_at. Si falta la cancelación, espera y repite solo C27.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C27-Consultar-aviso-de-cancelacion.jpg`.

### C28 Revocar permiso de monitoreo
1. Seleccionar **C28 Revocar permiso de monitoreo**.
2. Método y ruta: `DELETE {{baseUrl}}/communities/monitoring-grants/{{grantId}}`.
3. A revoca el permiso concedido en C18.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C28-Revocar-permiso-de-monitoreo.jpg`.

### C29 Rechazar monitoreo revocado
1. Seleccionar **C29 Rechazar monitoreo revocado**.
2. Método y ruta: `GET {{baseUrl}}/communities/monitoring/{{accountA}}`.
3. B ya no puede consultar las alertas de A. HTTP 403.
4. Pulsar **Send**. Resultado esperado: **HTTP 403**; comprobar **Test Results**.
5. Tomar captura y guardar como `C29-Rechazar-monitoreo-revocado.jpg`.

### C30 Revocar contacto
1. Seleccionar **C30 Revocar contacto**.
2. Método y ruta: `DELETE {{baseUrl}}/communities/contacts/{{contactId}}`.
3. A revoca el vínculo de confianza con B.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C30-Revocar-contacto.jpg`.

### C31 Invitar segundo contacto
1. Seleccionar **C31 Invitar segundo contacto**.
2. Método y ruta: `POST {{baseUrl}}/communities/contacts`.
3. Crear otra invitación A a C para probar rechazo sin alterar la secuencia anterior.
4. Pulsar **Send**. Resultado esperado: **HTTP 201**; comprobar **Test Results**.
5. Tomar captura y guardar como `C31-Invitar-segundo-contacto.jpg`.

### C32 Rechazar invitacion recibida
1. Seleccionar **C32 Rechazar invitacion recibida**.
2. Método y ruta: `PUT {{baseUrl}}/communities/contacts/{{rejectedContactId}}/reject`.
3. C rechaza la invitación recibida; HTTP 204.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C32-Rechazar-invitacion-recibida.jpg`.

### C33 Confirmar estados de contactos
1. Seleccionar **C33 Confirmar estados de contactos**.
2. Método y ruta: `GET {{baseUrl}}/communities/contacts`.
3. Mostrar el primer vínculo REVOKED y el segundo REJECTED.
4. Pulsar **Send**. Resultado esperado: **HTTP 200**; comprobar **Test Results**.
5. Tomar captura y guardar como `C33-Confirmar-estados-de-contactos.jpg`.

### C34 Rechazar bandeja sin sesion
1. Seleccionar **C34 Rechazar bandeja sin sesion**.
2. Método y ruta: `GET {{baseUrl}}/communities/notifications`.
3. No Auth ya configurado. Sin sesión no se puede consultar la bandeja; HTTP 401.
4. Pulsar **Send**. Resultado esperado: **HTTP 401**; comprobar **Test Results**.
5. Tomar captura y guardar como `C34-Rechazar-bandeja-sin-sesion.jpg`.

### C35 Eliminar comunidad publica propia
1. Seleccionar **C35 Eliminar comunidad publica propia**.
2. Método y ruta: `DELETE {{baseUrl}}/communities/{{communityId}}`.
3. A elimina únicamente la comunidad de prueba de C01. También elimina sus solicitudes de apoyo por la FK.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C35-Eliminar-comunidad-publica-propia.jpg`.

### C36 Eliminar comunidad privada propia
1. Seleccionar **C36 Eliminar comunidad privada propia**.
2. Método y ruta: `DELETE {{baseUrl}}/communities/{{privateCommunityId}}`.
3. A elimina la comunidad privada sintética de C04.
4. Pulsar **Send**. Resultado esperado: **HTTP 204**; comprobar **Test Results**.
5. Tomar captura y guardar como `C36-Eliminar-comunidad-privada-propia.jpg`.
