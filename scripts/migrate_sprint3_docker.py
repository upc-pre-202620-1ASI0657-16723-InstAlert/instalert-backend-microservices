"""Operator-only transfer from the frozen legacy database. Never deletes source data.
Run after new Flyway schemas exist and before allowing writes through Gateway.
Private SQL backups remain under ignored target/local-run; do not publish them.
"""
import subprocess, json
from pathlib import Path
from datetime import datetime, timezone
root=Path(__file__).resolve().parents[1]
dc=['docker','compose','--env-file','.env.docker']
groups={'incidents-db':['incidents'], 'communities-db':['communities','notification_contacts','community_notification_preferences','community_processed_alert_events','community_alert_recipients','community_alert_inbox']}
def run(service, command, data=None):
    return subprocess.run(dc+['exec','-T',service,'sh','-c',command],cwd=root,input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True).stdout
def query(service, sql):
    return run(service,'MYSQL_PWD="$MYSQL_PASSWORD" mysql -N -u"$MYSQL_USER" "$MYSQL_DATABASE"',sql.encode()).decode().strip()
if __name__=='__main__':
    subprocess.run(dc+['stop','monolith','api-gateway','frontend'],cwd=root,check=True)
    folder=root/'target/local-run'/('sprint3-transfer-'+datetime.now().strftime('%Y%m%d-%H%M%S'))
    folder.mkdir(parents=True)
    report=[]
    for target,tables in groups.items():
        for table in tables:
            before=int(query('monolith-db',f'SELECT COUNT(*) FROM {table};'))
            existing=int(query(target,f'SELECT COUNT(*) FROM {table};'))
            if existing:
                raise RuntimeError(f'{target}.{table} is not empty; reconcile instead of overwriting')
            dump=run('monolith-db','MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u"$MYSQL_USER" --single-transaction --skip-lock-tables --no-create-info --complete-insert --skip-add-locks --skip-extended-insert --set-gtid-purged=OFF --no-tablespaces "$MYSQL_DATABASE" '+table)
            (folder/(table+'.sql')).write_bytes(dump)
            run(target,'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"',dump)
            after=int(query(target,f'SELECT COUNT(*) FROM {table};'))
            assert before==after,(table,before,after)
            report.append({'table':table,'sourceCount':before,'targetCount':after})
    (folder/'counts.json').write_text(json.dumps({'checkedAt':datetime.now(timezone.utc).isoformat(),'tables':report},indent=2))
    print(json.dumps(report,indent=2))
    print('Source preserved. Private backup:',folder)
