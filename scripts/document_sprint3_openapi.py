"""Enrich generated Spring contracts using the verified Sprint 3 operations."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
UUID='11111111-1111-4111-8111-111111111111'
incident={'id':1,'userId':1,'type':'SPRINT3_POSTMAN','severity':'HIGH','address':'Ubicacion de muestra Lima','description':'Reporte de prueba','status':'ACTIVE','latitude':-12.0464,'longitude':-77.0428,'timeReported':'2026-11-14T10:00:00Z'}
community={'id':1,'name':'Comunidad Sprint 3','description':'Grupo de prueba','isPrivate':False,'ownerId':1,'member':True}
evidence={'id':UUID,'original_name':'evidencia.png','content_type':'image/png','byte_size':68,'state':'AVAILABLE'}
support={'id':UUID,'community_id':1,'account_id':UUID,'description':'Solicitud de apoyo','status':'ACTIVE','created_at':'2026-11-14T10:00:00Z'}
contact={'id':UUID,'requester':UUID,'recipient':'22222222-2222-4222-8222-222222222222','status':'PENDING'}
grant={'id':UUID,'owner_account_id':UUID,'recipient_account_id':'22222222-2222-4222-8222-222222222222','scope':'EMERGENCY_STATUS','expires_at':'2026-11-14T11:00:00Z','revoked':0}
notice={'id':UUID,'event_id':UUID,'alert_id':UUID,'event_type':'EmergencyAlertCreated','payload':json.dumps({'id':UUID,'accountId':UUID,'status':'ACTIVE','latitude':-12.0464,'longitude':-77.0428}),'received_at':'2026-11-14T10:00:00Z','read_at':None}
def row(method,path,title,tag,permission,response=None,errors=(400,401,403,404)):
    return (method,'/api/v1'+path,title,tag,permission,response,errors)
rows={
'incidents':[
row('post','/incidents','Crear reporte','Reportes','Cuenta autenticada. El propietario se obtiene de Account. userId heredado es opcional y solo se acepta si coincide con la sesión.',incident,(400,401,403)),
row('get','/incidents','Listar y filtrar reportes','Reportes','Cuenta autenticada. paged=false devuelve una lista; paged=true devuelve content y metadatos. size: 1–100; page: 0–10000. from/to son fechas ISO. Los cuatro límites geográficos deben enviarse juntos.',{'content':[incident],'page':0,'size':5,'totalElements':1,'totalPages':1},(400,401)),
row('get','/incidents/{id}','Consultar reporte por ID','Reportes','Cuenta autenticada. Devuelve un reporte no eliminado.',incident,(401,404)),
row('put','/incidents/{id}/status','Cambiar estado del reporte','Reportes','Propietario o ROLE_ADMIN. Estados permitidos: ACTIVE, RESOLVED, CLOSED.',{**incident,'status':'RESOLVED'}),
row('delete','/incidents/{id}','Eliminar reporte','Reportes','Propietario o ROLE_ADMIN. Eliminación lógica: el reporte deja de aparecer en consultas.',None,(401,403,404)),
row('post','/incidents/{incidentId}/evidences','Adjuntar evidencia','Evidencias','Propietario o ROLE_ADMIN. multipart/form-data con file obligatorio. PNG/JPEG/PDF, máximo 5 MB, firma y tipo compatibles. Estado AVAILABLE tras guardar en almacenamiento privado.',evidence,(400,401,403,404,413,503)),
row('get','/incidents/{incidentId}/evidences','Listar evidencias privadas','Evidencias','Propietario o ROLE_ADMIN. Devuelve metadatos, no el contenido del archivo.',[evidence],(401,403,404)),
row('get','/incidents/{incidentId}/evidences/{id}','Descargar evidencia privada','Evidencias','Propietario o ROLE_ADMIN. Devuelve bytes con Content-Disposition attachment y Cache-Control no-store.',None,(401,403,404,409,503)),
],
'communities':[
row('post','/communities','Crear comunidad','Comunidades','Cuenta autenticada. El servidor asigna el propietario y su membresía. ownerId heredado solo se acepta si coincide con la sesión.',community,(400,401,403)),
row('get','/communities','Listar comunidades visibles','Comunidades','Cuenta autenticada. Públicas y privadas propias o con membresía. page: 0–10000; size: 1–100. Devuelve una lista.',[community],(400,401)),
row('get','/communities/{id}','Consultar comunidad','Comunidades','Pública para cuentas autenticadas; privada para propietario, miembro o administrador.',community,(401,403,404)),
row('delete','/communities/{id}','Eliminar comunidad','Comunidades','Propietario o ROLE_ADMIN. Elimina también sus membresías y solicitudes de apoyo.',None,(401,403,404)),
row('post','/communities/{id}/members/me','Unirse a comunidad pública','Membresías y apoyo','Cuenta autenticada. No admite incorporación libre a comunidades privadas. La incorporación repetida no duplica la membresía.',None,(401,403,404)),
row('delete','/communities/{id}/members/me','Salir de comunidad','Membresías y apoyo','Elimina la membresía propia. El propietario no puede abandonar su comunidad mediante esta operación.',None,(401,404,409)),
row('post','/communities/{id}/support-requests','Crear solicitud de apoyo','Membresías y apoyo','Miembro o propietario. description obligatoria, máximo 1000 caracteres.',{'id':UUID,'status':'ACTIVE'}),
row('get','/communities/{id}/support-requests','Consultar solicitudes de apoyo','Membresías y apoyo','Miembro o propietario. page desde 0; páginas de 50 solicitudes.',[support]),
row('put','/communities/{id}/support-requests/{requestId}/resolve','Resolver solicitud de apoyo','Membresías y apoyo','Requiere pertenencia o propiedad. Puede resolver su solicitante, el propietario de la comunidad o un administrador con acceso.',None),
row('post','/communities/contacts','Invitar contacto de confianza','Contactos de confianza','Cuenta autenticada. accountId destinatario UUID, distinto de la propia cuenta. Un vínculo existente no se duplica.',{'id':UUID,'status':'PENDING'},(400,401)),
row('get','/communities/contacts','Listar contactos propios','Contactos de confianza','Cuenta autenticada. Solo vínculos en los que participa.',[contact],(401,)),
row('put','/communities/contacts/{id}/accept','Aceptar invitación','Contactos de confianza','Solo destinatario. Vínculo PENDING o ya ACCEPTED. Registra consentimiento; no permite reactivar vínculos revocados/rechazados.',None,(401,403,404,409)),
row('put','/communities/contacts/{id}/reject','Rechazar invitación','Contactos de confianza','Solo destinatario de un vínculo PENDING. Una invitación no aplicable produce 409.',None,(401,409)),
row('delete','/communities/contacts/{id}','Revocar vínculo de confianza','Contactos de confianza','Cualquiera de los participantes. Cambia estado a REVOKED.',None,(401,404)),
row('get','/communities/notification-preferences','Consultar preferencia de avisos','Avisos y preferencias','Cuenta autenticada. Preferencia propia; por defecto enabled=true.',{'enabled':True},(401,)),
row('put','/communities/notification-preferences','Actualizar preferencia de avisos','Avisos y preferencias','Cuenta autenticada. enabled boolean obligatorio. Opt-out bloquea nuevas creaciones, no las cancelaciones de alertas ya recibidas.',None,(400,401)),
row('get','/communities/notifications','Consultar bandeja propia','Avisos y preferencias','Cuenta autenticada. Solo avisos del destinatario. page: 0–10000; páginas de 50. Entrega asíncrona desde Emergencies; no se envían correos ni push.',[notice],(400,401)),
row('put','/communities/notifications/{id}/read','Marcar aviso como leído','Avisos y preferencias','Solo destinatario. Aviso ajeno o inexistente: 404. Conserva la primera fecha de lectura.',None,(401,404)),
row('post','/communities/monitoring-grants','Conceder permiso temporal','Monitoreo autorizado','Requiere vínculo ACCEPTED. accountId UUID, scope EMERGENCY_STATUS, expiresAt ISO futuro con máximo 24 horas. Solo comparte alertas activas, no rastreo GPS continuo.',{'id':UUID,'expiresAt':'2026-11-14T11:00:00Z'},(400,401,403)),
row('get','/communities/monitoring-grants','Listar permisos propios y recibidos','Monitoreo autorizado','Cuenta autenticada. Permisos en los que participa como propietario o destinatario.',[grant],(401,)),
row('delete','/communities/monitoring-grants/{id}','Revocar permiso de monitoreo','Monitoreo autorizado','Solo propietario del permiso. Permiso ajeno o inexistente: 404.',None,(401,404)),
row('get','/communities/monitoring/{accountId}','Consultar alertas autorizadas','Monitoreo autorizado','Vínculo ACCEPTED y permiso vigente no revocado, con el solicitante como destinatario. Communities consulta Emergencies internamente; máximo 50 alertas activas.',[{'id':UUID,'accountId':UUID,'type':'PANIC','status':'ACTIVE','latitude':-12.0464,'longitude':-77.0428}],(400,401,403,503)),
]}
examples={'incidents':{'Create':{k:v for k,v in incident.items() if k not in ['id','userId','status']},'State':{'status':'RESOLVED'}},'communities':{'Create':{'name':'Comunidad Sprint 3','description':'Grupo de prueba','isPrivate':False},'Support':{'description':'Necesito apoyo vecinal'},'Invite':{'accountId':'22222222-2222-4222-8222-222222222222'},'Preference':{'enabled':True},'Grant':{'accountId':'22222222-2222-4222-8222-222222222222','scope':'EMERGENCY_STATUS','expiresAt':'2026-11-14T11:00:00Z'}}}
messages={400:'Datos o parámetros inválidos.',401:'Sesión ausente o inválida.',403:'Cuenta autenticada sin permiso para esta operación.',404:'Recurso no encontrado o no accesible mediante esta operación.',409:'Conflicto de estado o recurso no disponible.',413:'Solicitud multipart excede el límite.',503:'Dependencia o almacenamiento temporalmente no disponible.'}
if __name__=='__main__':
 for svc,entries in rows.items():
    path=ROOT/'services'/('ms-'+svc)/'openapi.json';spec=json.loads(path.read_text(encoding='utf-8'))
    spec['info']={'title':'InstAlert '+svc.title()+' — Sprint 3','version':'1.0.0-local','description':'Contrato OpenAPI 3.1.0 del microservicio independiente. Operaciones verificadas localmente mediante Postman. JWT emitido por Account. Los ejemplos son ilustrativos: sustituir los identificadores por los obtenidos en la ejecución. No representa despliegue cloud.'}
    spec['tags']=[{'name':name} for name in dict.fromkeys(x[3] for x in entries)]
    for method,route,title,tag,permission,example,errors in entries:
        op=spec['paths'][route][method];op.update(summary=title,tags=[tag],description=permission,operationId=method+'_'+re.sub('[^a-zA-Z0-9]+','_',route).strip('_'))
        for p in op.get('parameters',[]):
            if p['in']=='path':p['example']=UUID if p['schema'].get('format')=='uuid' else 1
            if p['name']=='paged':p['example']=True
            if p['name']=='size':p['example']=5
        for kind,media in op.get('requestBody',{}).get('content',{}).items():
            ref=media.get('schema',{}).get('$ref','').split('/')[-1]
            if ref in examples[svc]:media['example']=examples[svc][ref]
        success=next(k for k in op['responses'] if k.startswith('2'))
        response={'description':'Operación completada sin cuerpo de respuesta.' if success=='204' else ('Recurso creado.' if success=='201' else 'Consulta u operación completada.')}
        if example is not None:response['content']={'application/json':{'example':example}}
        if svc=='incidents' and method=='get' and route.endswith('/evidences/{id}'):
            response['content']={t:{'schema':{'type':'string','format':'binary'}} for t in ['image/png','image/jpeg','application/pdf']}
            response['headers']={'Content-Disposition':{'schema':{'type':'string'},'example':'attachment; filename="evidence-UUID"'},'Cache-Control':{'schema':{'type':'string'},'example':'no-store'}}
        op['responses']={success:response,**{str(code):{'description':messages[code]} for code in errors}}
    assert len(entries)==sum(len([m for m in ops if m in ['get','post','put','delete','patch']]) for ops in spec['paths'].values())
    path.write_text(json.dumps(spec,ensure_ascii=False,indent=2),encoding='utf-8')
    print(svc,len(entries),'operations documented')
