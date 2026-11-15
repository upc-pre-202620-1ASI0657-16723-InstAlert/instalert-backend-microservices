"""Isolated snapshot restoration rehearsal. Never rewrites production/source schemas.
Temporarily pauses extracted writers; restores snapshots into disposable schemas
inside the existing DB containers; compares every table; verifies evidence archive.
No switch back to legacy binaries is claimed. Private dumps stay ignored.
"""
import hashlib,io,json,subprocess,tarfile,uuid
from pathlib import Path
from datetime import datetime,timezone
ROOT=Path(__file__).resolve().parents[1]
DC=['docker','compose','--env-file','.env.docker']
def dc(*args,data=None):
    return subprocess.run(DC+list(args),cwd=ROOT,input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True).stdout

def sql(service,query):
    return dc('exec','-T',service,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -N -uroot',data=query.encode()).decode().strip()

def main():
    suffix=uuid.uuid4().hex[:12]
    folder=ROOT/'target/local-run'/('restore-rehearsal-'+suffix);folder.mkdir(parents=True)
    pairs=[('incidents-db','instalert_incidents'),('communities-db','instalert_communities')]
    shadows=[];checks=[]
    try:
        dc('stop','ms-incidents','ms-communities')
        for service,schema in pairs:
            shadow='audit_restore_'+suffix
            dump=dc('exec','-T',service,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --skip-lock-tables --skip-add-locks --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"')
            (folder/(schema+'.sql')).write_bytes(dump)
            sql(service,f'CREATE DATABASE `{shadow}`;');shadows.append((service,shadow))
            dc('exec','-T',service,'sh','-c',f'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot {shadow}',data=dump)
            tables=sql(service,f"SELECT table_name FROM information_schema.tables WHERE table_schema='{schema}' AND table_type='BASE TABLE' ORDER BY table_name;").splitlines()
            for table in tables:
                # Compare canonical row multisets, not just counts. Hex output is
                # kept in memory; no user records are printed in the public report.
                columns=sql(service,f"SELECT column_name FROM information_schema.columns WHERE table_schema='{schema}' AND table_name='{table}' ORDER BY ordinal_position;").splitlines()
                expressions=','.join("IF(`"+col+"` IS NULL,'NULL',CONCAT('HEX:',HEX(`"+col+"`)))" for col in columns)
                def rows(database):
                    raw=sql(service,f'SELECT {expressions} FROM `{database}`.`{table}`;')
                    return sorted(raw.splitlines()) if raw else []
                assert rows(schema)==rows(shadow),(schema,table,'restored contents differ')
            checks.append({'database':schema,'tablesCompared':len(tables),'restoredRowsMatch':True})
        archive=dc('run','--rm','-T','--no-deps','--entrypoint','tar','ms-incidents','-C','/app/evidence','-cf','-','.')
        (folder/'incident-evidence.tar').write_bytes(archive)
        # Verify extraction into a separate disposable container directory.
        destination='/tmp/audit-evidence-'+suffix
        # The service is stopped, so use the image with its volume mounted read-only
        # and read the saved archive through stdin. This does not touch live files.
        result=dc('run','--rm','-T','--no-deps','--entrypoint','sh','ms-incidents','-c',f'mkdir -p {destination} && tar -C {destination} -xf - && find {destination} -type f -exec sha256sum {{}} \\;',data=archive).decode()
        restored={line.split('  ',1)[1].removeprefix(destination+'/').removeprefix('./'):line.split('  ',1)[0] for line in result.splitlines()}
        with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
            original={item.name.removeprefix('./'):hashlib.sha256(tar.extractfile(item).read()).hexdigest() for item in tar.getmembers() if item.isfile()}
        assert original==restored,'Restored evidence bytes differ'
        checks.append({'evidenceFilesCompared':len(original),'restoredBytesMatch':True})
    finally:
        try:
            for service,shadow in shadows:
                sql(service,f'DROP DATABASE `{shadow}`;')
        finally:
            dc('start','ms-incidents','ms-communities')
    report={'checkedAt':datetime.now(timezone.utc).isoformat(),'scope':'Snapshot restore in disposable schemas; existing source data preserved; no legacy route rollback','checks':checks,'passed':True}
    (ROOT/'target/local-run/sprint3-restore.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
