"""Docker end-to-end evidence. Uses real Compose services, stores no session tokens."""
import json,subprocess,time,urllib.request
from pathlib import Path
from datetime import datetime,timezone
import verify_notifications_local as v
v.base='http://127.0.0.1:4201/api/v1'
root=Path(__file__).resolve().parents[1]
compose=['docker','compose','--env-file','.env.docker']
checks=[]
def check(name,ok=True):
 assert ok,name
 checks.append(name);print('PASS:',name,flush=True)
def dc(*args):return subprocess.check_output(compose+list(args),cwd=root,text=True)
def outbox(aid):
 query="SELECT publication_status,delivery_attempts FROM emergency_outbox_events WHERE alert_id='"+aid+"';"
 # Password is resolved inside the container; never passed through CLI arguments or reports.
 return dc('exec','-T','emergencies-db','sh','-c','MYSQL_PWD="$MYSQL_PASSWORD" mysql -N -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "'+query+'"').strip()
if __name__=='__main__':
 with urllib.request.urlopen('http://127.0.0.1:4201/') as r:check('Angular served by containerized Nginx',r.status==200)
 owner,recipient,stranger=[v.register(x) for x in ('docker-owner','docker-recipient','docker-stranger')]
 check('Account registration through Nginx and Gateway')
 v.call('POST','/authentication/sign-in',{'email':owner['email'],'password':'incorrect'},expected=401);check('Incorrect credentials rejected')
 session=v.call('POST','/authentication/sign-in',{'email':owner['email'],'password':'LocalNoticeTest2026!'})
 check('Account login returns its original public UUID',session['accountId']==owner['accountId'])
 link=v.call('POST','/communities/contacts',{'accountId':recipient['accountId']},owner['token'],expected=201)['id']
 v.call('PUT','/communities/contacts/'+link+'/accept',{},owner['token'],expected=403);check('Invitation acceptance requires recipient')
 v.call('PUT','/communities/contacts/'+link+'/accept',{},recipient['token'],expected=204)
 alert,payload,key=v.create(owner);aid=alert['id'];notice=v.wait_notice(recipient,aid,'EmergencyAlertCreated')
 check('Emergency creation persisted ACTIVE',alert['status']=='ACTIVE')
 check('Real creation notice received once',len(notice)==1)
 replay=v.call('POST','/emergencies',payload,owner['token'],{'Idempotency-Key':key});check('Retry preserves alert UUID',replay['id']==aid)
 v.call('POST','/emergencies',{**payload,'type':'OTHER'},owner['token'],{'Idempotency-Key':key},409);check('Idempotency content conflict rejected')
 v.call('GET','/emergencies/'+aid,token=stranger['token'],expected=403);check('Foreign account cannot read alert')
 v.call('PATCH','/emergencies/'+aid,{'status':'CANCELLED'},stranger['token'],expected=403);check('Foreign account cannot cancel alert')
 v.call('GET','/emergencies',expected=401);check('Missing session denied')
 check('Unlinked account has no received notices',not v.inbox(stranger))
 v.call('PUT','/communities/notification-preferences',{'enabled':False},recipient['token'],expected=204)
 cancelled=v.call('PATCH','/emergencies/'+aid,{'status':'CANCELLED'},owner['token']);check('Owner cancellation persists',cancelled['status']=='CANCELLED')
 v.wait_notice(recipient,aid,'EmergencyAlertCancelled');check('Cancellation reaches original recipient after opt-out')
 v.call('PATCH','/emergencies/'+aid,{'status':'CANCELLED'},owner['token']);time.sleep(2)
 check('No duplicate creation or cancellation notices',len([n for n in v.inbox(recipient) if n['alert_id']==aid])==2)
 # Restore preference for the real outage/recovery case.
 v.call('PUT','/communities/notification-preferences',{'enabled':True},recipient['token'],expected=204)
 try:
  dc('stop','monolith')
  recovering,_,_=v.create(owner);rid=recovering['id'];time.sleep(3)
  pending=outbox(rid);check('Alert still accepted while Communities is stopped',pending.startswith('PENDING'))
 finally:
  dc('start','monolith')
  # HTTP readiness can lag container start; keep recovery polling bounded.
 for attempt in range(70):
  try:
   found=[n for n in v.inbox(recipient) if n['alert_id']==rid]
   if found:break
  except Exception:pass
  time.sleep(1)
 else:raise AssertionError('Recovery notice not delivered')
 check('Exactly one notice delivered after receiver restart',len(found)==1)
 published=outbox(rid);check('Outbox acknowledged PUBLISHED',published.startswith('PUBLISHED'))
 # Recreate receiver container: data must survive its process/container lifecycle.
 dc('restart','monolith')
 for attempt in range(60):
  try:
   persisted=v.inbox(recipient);break
  except Exception:time.sleep(1)
 else:raise AssertionError('Inbox unavailable after restart')
 check('Inbox survives container restart',len([n for n in persisted if n['alert_id']==aid])==2)
 v.base='http://127.0.0.1:8085/api/v1'
 check('Published Gateway port reaches same Account',v.call('GET','/account/session',token=owner['token'])['accountId']==owner['accountId'])
 report={'environment':'Docker Compose, 8 real containers','executedAt':datetime.now(timezone.utc).isoformat(),'passed':len(checks),'checks':checks,'alertId':aid,'recoveryAlertId':rid,'pendingState':pending,'publishedState':published,'ownerEmail':owner['email'],'recipientEmail':recipient['email'],'channel':'persisted in-app inbox'}
 output=root/'target/local-run/docker-verification.json';output.write_text(json.dumps(report,indent=2),encoding='utf-8')
 print('PASS total:',len(checks),'Report:',output)
