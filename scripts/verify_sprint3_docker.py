"""Live MySQL + Compose acceptance checks. Creates synthetic fixtures, never saves JWTs."""
import json, time, uuid, urllib.request, urllib.error, subprocess
from datetime import datetime, timezone, timedelta
from pathlib import Path
import verify_notifications_local as v
v.base='http://127.0.0.1:4201/api/v1'
root=Path(__file__).resolve().parents[1]; checks=[]
def check(name, condition=True):
    assert condition,name
    checks.append(name);print('PASS:',name,flush=True)
def dc(*args):
    return subprocess.check_output(['docker','compose','--env-file','.env.docker',*args],cwd=root,text=True)
def multipart(path,token,expected=201):
    boundary='InstAlert'+uuid.uuid4().hex
    content=b'%PDF-1.4\nSynthetic incident evidence\n%%EOF'
    body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="sample.pdf"\r\nContent-Type: application/pdf\r\n\r\n'.encode()+content+f'\r\n--{boundary}--\r\n'.encode())
    request=urllib.request.Request(v.base+path,data=body,headers={'Authorization':'Bearer '+token,'Content-Type':'multipart/form-data; boundary='+boundary},method='POST')
    try:
        with urllib.request.urlopen(request,timeout=15) as response:code=response.status;data=response.read()
    except urllib.error.HTTPError as error:code=error.code;data=error.read()
    assert code==expected,(path,code,expected)
    return json.loads(data),content
if __name__=='__main__':
    owner,member,stranger=[v.register('sprint3-'+name) for name in ('owner','member','stranger')]
    a,b,c=owner['token'],member['token'],stranger['token']
    v.call('GET','/incidents',expected=401);check('Anonymous incidents rejected')
    payload={'type':'SPRINT3_TEST','severity':'HIGH','address':'Synthetic location','description':'Migration acceptance fixture','latitude':-12.0464,'longitude':-77.0428,'timeReported':datetime.now(timezone.utc).isoformat()}
    incident=v.call('POST','/incidents',payload,a,expected=201);iid=incident['id'];check('Incident created in independent service')
    v.call('POST','/incidents',{**payload,'userId':member['profile']['id']},a,expected=403);check('Client cannot impersonate incident owner')
    v.call('POST','/incidents',{**payload,'latitude':100},a,expected=400);check('Invalid report coordinates rejected')
    page=v.call('GET','/incidents?paged=true&type=SPRINT3_TEST&severity=HIGH&page=0&size=1',token=a);check('Filtered report pagination',len(page['content'])==1 and page['totalElements']>=1)
    v.call('PUT',f'/incidents/{iid}/status',{'status':'RESOLVED'},b,expected=403);check('Foreign report modification rejected')
    evidence,content=multipart(f'/incidents/{iid}/evidences',a);check('Multipart evidence crosses Nginx and Gateway')
    evidence_path=f'/incidents/{iid}/evidences/'+evidence['id']
    request=urllib.request.Request(v.base+evidence_path,headers={'Authorization':'Bearer '+a})
    with urllib.request.urlopen(request) as response:check('Stored evidence bytes preserved',response.read()==content)
    v.call('GET',evidence_path,token=b,expected=403);check('Private evidence protected')
    v.call('PUT',f'/incidents/{iid}/status',{'status':'RESOLVED'},a);check('Owner resolves incident')
    community=v.call('POST','/communities',{'name':'Sprint3 '+uuid.uuid4().hex[:8],'description':'Synthetic acceptance community','isPrivate':False},a,expected=201);cid=community['id'];check('Community created in own database')
    private=v.call('POST','/communities',{'name':'Private acceptance','isPrivate':True},a,expected=201)
    v.call('GET','/communities/'+str(private['id']),token=b,expected=403);check('Private community access rejected')
    v.call('POST',f'/communities/{cid}/support-requests',{'description':'Help fixture'},b,expected=403);check('Support requires membership')
    v.call('POST',f'/communities/{cid}/members/me',{},b,expected=204);check('Public community membership persisted')
    support=v.call('POST',f'/communities/{cid}/support-requests',{'description':'Synthetic assistance request'},b,expected=201)
    check('Community support query',any(x['id']==support['id'] for x in v.call('GET',f'/communities/{cid}/support-requests',token=a)))
    v.call('PUT',f'/communities/{cid}/support-requests/{support["id"]}/resolve',{},a,expected=204);check('Community owner resolves request')
    link=v.call('POST','/communities/contacts',{'accountId':member['accountId']},a,expected=201)['id']
    v.call('PUT','/communities/contacts/'+link+'/accept',{},b,expected=204)
    v.call('GET','/communities/monitoring/'+owner['accountId'],token=b,expected=403);check('Accepted contact alone cannot monitor')
    grant=v.call('POST','/communities/monitoring-grants',{'accountId':member['accountId'],'scope':'EMERGENCY_STATUS','expiresAt':(datetime.now(timezone.utc)+timedelta(hours=1)).isoformat()},a,expected=201)
    alert,_,_=v.create(owner);aid=alert['id'];v.wait_notice(member,aid,'EmergencyAlertCreated');check('Emergency creation reaches extracted Communities inbox')
    monitored=v.call('GET','/communities/monitoring/'+owner['accountId'],token=b);check('Explicit consent resolves real Emergency state',any(x['id']==aid for x in monitored))
    v.call('GET','/emergencies/internal/monitoring/'+owner['accountId'],token=a,expected=404);check('Gateway blocks internal monitoring')
    v.call('DELETE','/communities/monitoring-grants/'+grant['id'],token=a,expected=204)
    v.call('GET','/communities/monitoring/'+owner['accountId'],token=b,expected=403);check('Monitoring revocation applied immediately')
    v.call('PATCH','/emergencies/'+aid,{'status':'CANCELLED'},a);v.wait_notice(member,aid,'EmergencyAlertCancelled');check('Emergency cancellation delivered')
    try:
        dc('stop','ms-communities')
        recovery,_,_=v.create(owner);rid=recovery['id'];time.sleep(3)
        query=f"SELECT publication_status FROM emergency_outbox_events WHERE alert_id='{rid}'"
        pending=dc('exec','-T','emergencies-db','sh','-c','MYSQL_PWD="$MYSQL_PASSWORD" mysql -N -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "'+query+'"')
        check('Receiver outage retains PENDING event',pending.strip()=='PENDING')
    finally:dc('start','ms-communities')
    for attempt in range(90):
        try:
            notices=v.inbox(member)
            if any(x['alert_id']==rid for x in notices):break
        except Exception:pass
        time.sleep(1)
    else:raise AssertionError('Receiver recovery failed')
    check('Recovery delivers exactly one notice',len([x for x in notices if x['alert_id']==rid])==1)
    for attempt in range(10):
        published=dc('exec','-T','emergencies-db','sh','-c','MYSQL_PWD="$MYSQL_PASSWORD" mysql -N -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "'+query+'"').strip()
        if published=='PUBLISHED':break
        time.sleep(1)
    check('Recovered outbox event acknowledged PUBLISHED',published=='PUBLISHED')
    dc('restart','ms-incidents','ms-communities')
    for attempt in range(90):
        try:
            saved=v.call('GET',f'/incidents/{iid}',token=a);notices=v.inbox(member);break
        except Exception:time.sleep(1)
    else:raise AssertionError('Restart readiness failed')
    check('Independent service data survives restart',saved['status']=='RESOLVED' and any(x['alert_id']==rid for x in notices))
    with urllib.request.urlopen(request) as response:check('Evidence volume survives restart',response.read()==content)
    v.call('DELETE',f'/communities/{cid}',token=c,expected=403);check('Foreign community deletion denied')
    v.call('DELETE',f'/incidents/{iid}',token=a,expected=204)
    v.call('GET',f'/incidents/{iid}',token=a,expected=404);check('Owner report deletion hides resource')
    report={'checkedAt':datetime.now(timezone.utc).isoformat(),'target':v.base,'passed':len(checks),'checks':checks}
    (root/'target/local-run/sprint3-verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('Passed',len(checks),'live checks')
