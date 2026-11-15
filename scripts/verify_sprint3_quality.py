"""Bounded local load smoke test; not a production capacity certification."""
import concurrent.futures,json,math,time,uuid
from datetime import datetime,timezone
from pathlib import Path
import verify_notifications_local as api
api.base='http://127.0.0.1:4201/api/v1'
ROOT=Path(__file__).resolve().parents[1]
def main():
    account=api.register('quality-'+uuid.uuid4().hex[:8])
    def request(index):
        start=time.perf_counter()
        path='/incidents?paged=true&size=5' if index%2==0 else '/communities?size=5'
        api.call('GET',path,token=account['token'])
        return (time.perf_counter()-start)*1000
    start=time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        samples=list(pool.map(request,range(24)))
    ordered=sorted(samples);p95=ordered[math.ceil(len(samples)*.95)-1]
    report={'checkedAt':datetime.now(timezone.utc).isoformat(),'target':api.base,'requests':24,'concurrency':4,'failed':0,'elapsedSeconds':round(time.perf_counter()-start,3),'p95Milliseconds':round(p95,2),'maxMilliseconds':round(max(samples),2),'thresholdP95Milliseconds':5000,'passed':p95<5000,'scope':'Local read-only smoke load; synthetic user; no production scalability claim'}
    (ROOT/'target/local-run/sprint3-quality.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report,indent=2));assert report['passed'],'Local p95 exceeded 5 seconds'
if __name__=='__main__':main()
