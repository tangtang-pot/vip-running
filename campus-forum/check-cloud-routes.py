"""Sequential real Nacos/Gateway checks; start the matching role first."""
import json, sys, time, uuid
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

ROOT=Path(__file__).parent
STATE=ROOT/'.local/cloud-test-state.json'

def call(path,method='GET',body=None,token=None,base='http://127.0.0.1:8082'):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    with urlopen(Request(base+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method),timeout=10) as response:
        return json.load(response)

def login(user):
    deadline=time.monotonic()+40
    while True:
        try:return call('/api/auth/login','POST',{'username':user,'password':'local-'+user+'-change-me'})['token']
        except HTTPError as error:
            if error.code not in (502,503,504) or time.monotonic()>deadline:raise
        except (URLError,OSError):
            if time.monotonic()>deadline:raise
        time.sleep(2)

def check(role):
    path='/nacos/v1/ns/instance/list?serviceName='+role+'-service'
    until=time.monotonic()+70
    while True:
        try:
            registration=call(path,base='http://127.0.0.1:18848')
            if any(host.get('healthy') for host in registration.get('hosts',[])):break
        except (HTTPError,URLError,OSError):pass
        if time.monotonic()>until:raise AssertionError('Service not healthy in Nacos: '+role)
        time.sleep(2)
    state=json.loads(STATE.read_text()) if STATE.exists() else {'passedRoles':[]}
    student=login('student')
    if role=='content':
        assert call('/api/posts')['total']>=6
        post=call('/api/posts','POST',{'title':'网关真实路由-'+uuid.uuid4().hex[:6],'body':'Nacos discovery and gateway integration test','category':'学习求助'},student)['id']
        assert call('/api/posts/'+post,token=student)['id']==post
        state['postId']=post
    elif role=='interaction':
        result=call('/api/posts/'+state['postId']+'/reactions','PUT',{'kind':'like','active':True},student)
        assert result['sync']=='pending'
    elif role=='operations':
        admin=login('admin')
        assert isinstance(call('/api/admin/summary',token=admin),dict)
        assert isinstance(call('/api/hot'),list)
        try:call('/api/admin/summary',token=student);raise AssertionError('Role restriction missing')
        except HTTPError as error:assert error.code==403
    elif role=='log':
        logs=call('/api/admin/logs',token=login('admin'))
        assert any(row.get('target')==state['postId'] for row in logs)
    else:raise ValueError('Unknown role')
    state['passedRoles']=sorted(set(state['passedRoles']+[role]))
    state['authGatewayPassed']=True
    state['nacosDiscovery']=True
    state['execution']='sequential real service processes, not simultaneous load testing'
    STATE.write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8')
    print('PASS: Nacos registration, gateway auth and '+role+' route')

if __name__=='__main__':check(sys.argv[1])
