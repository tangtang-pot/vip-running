"""One real model call through the authenticated forum; never reads the API key."""
import json, time
from pathlib import Path
from urllib.request import Request, urlopen

BASE='http://127.0.0.1:8083'

def main():
    from urllib.parse import urlencode
    login=Request(BASE+'/api/auth/login',data=json.dumps({'username':'student','password':'local-student-change-me'}).encode(),headers={'Content-Type':'application/json'})
    with urlopen(login,timeout=20) as response:token=json.load(response)['token']
    question='根据论坛使用说明，注册验证码的有效期是多久？'
    request=Request(BASE+'/api/assistant/stream?'+urlencode({'q':question}),headers={'Authorization':'Bearer '+token})
    started=time.monotonic()
    with urlopen(request,timeout=85) as response:stream=response.read().decode('utf-8').replace('\r\n','\n')
    events=[]
    for block in stream.split('\n\n'):
        name='message'; data=[]
        for line in block.splitlines():
            if line.startswith('event:'):name=line[6:].strip()
            elif line.startswith('data:'):data.append(line[5:].removeprefix(' '))
        if data:events.append((name,'\n'.join(data)))
    modes=[data for name,data in events if name=='mode']
    answer=''.join(data for name,data in events if name=='token')
    passed=any('在线模型问答' in mode for mode in modes) and not any('失败' in mode for mode in modes) and ('done','complete') in events and bool(answer.strip())
    report={'passed':passed,'question':question,'modes':modes,'answer':answer,'sourceCount':sum(name=='source' for name,data in events),'seconds':round(time.monotonic()-started,2)}
    output=Path(__file__).parent/'.local/online-ai-result.json'
    output.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False),flush=True)
    if not passed:raise SystemExit(1)

if __name__=='__main__':main()
