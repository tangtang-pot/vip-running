"""Bounded local read benchmark. No AI calls or external targets."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.request import urlopen
import json, statistics, time

ENDPOINT='http://127.0.0.1:8083/api/posts?size=10'

def request(_):
    started=time.perf_counter()
    try:
        with urlopen(ENDPOINT,timeout=15) as response:
            body=json.load(response)
            success=response.status==200 and isinstance(body.get('items'),list)
        return success,(time.perf_counter()-started)*1000
    except Exception:return False,(time.perf_counter()-started)*1000

def main():
    for i in range(3):request(i)
    results=[]
    for concurrency in [1,5,10]:
        started=time.perf_counter()
        with ThreadPoolExecutor(max_workers=concurrency) as pool:rows=list(pool.map(request,range(50)))
        elapsed=time.perf_counter()-started
        durations=sorted(duration for success,duration in rows)
        result={'concurrency':concurrency,'requests':len(rows),'success':sum(success for success,duration in rows),'failed':sum(not success for success,duration in rows),'seconds':round(elapsed,3),'requestsPerSecond':round(len(rows)/elapsed,2),'meanMs':round(statistics.mean(durations),2),'p95Ms':round(durations[int(.95*len(rows))-1],2),'maxMs':round(max(durations),2)}
        results.append(result);print(json.dumps(result),flush=True)
        time.sleep(1)
    report={'endpoint':ENDPOINT,'environment':'8GB Windows laptop, local MySQL and Docker Redis/RabbitMQ/Mailpit','scope':'bounded read-only smoke benchmark, not production capacity or write/AI load testing','passed':all(row['failed']==0 for row in results),'results':results}
    path=Path(__file__).parent/'.local/concurrency-result.json'
    path.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    if not report['passed']:raise SystemExit(1)

if __name__=='__main__':main()
