"""Real middleware smoke test; run only against the dedicated local test stack."""
import json, re, time, uuid
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError

BASE = 'http://127.0.0.1:8083'
MAIL = 'http://127.0.0.1:8025'

def call(path, method='GET', body=None, token=None, base=BASE):
    headers = {'Content-Type':'application/json'}
    if token: headers['Authorization'] = 'Bearer '+token
    req = Request(base+path, data=None if body is None else json.dumps(body).encode(), headers=headers, method=method)
    with urlopen(req, timeout=20) as response:
        return json.load(response)

def until(check, seconds=35):
    end=time.monotonic()+seconds
    while time.monotonic()<end:
        result=check()
        if result: return result
        time.sleep(1)
    raise AssertionError('Timed out waiting for middleware result')

def main():
    config=dict(line.split('=',1) for line in Path(__file__).with_name('.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
    token=call('/api/auth/login','POST',{'username':'student','password':config['DEMO_PASSWORD']})['token']
    suffix=uuid.uuid4().hex[:10]
    post=call('/api/posts','POST',{'title':'真实中间件联调-'+suffix,'body':'Dedicated integration test data.','category':'学习求助'},token)['id']
    call('/api/posts/'+post,token=token)
    for _ in range(2):
        result=call('/api/posts/'+post+'/reactions','PUT',{'kind':'like','active':True},token)
        assert result['sync']=='pending', 'Redis infrastructure path was not enabled'
    until(lambda:call('/api/posts/'+post,token=token).get('likes')==1)
    call('/api/posts/'+post+'/comments','POST',{'body':'真实数据库评论'},token)
    call('/api/posts/'+post+'/reactions','PUT',{'kind':'bookmark','active':True},token)
    until(lambda:call('/api/posts?bookmarks=true',token=token)['total']>=1)
    print('PASS: real MySQL post/comment and asynchronous Redis reactions/bookmark',flush=True)
    email='infra_'+suffix+'@example.test'
    queued=call('/api/auth/code','POST',{'email':email})
    assert 'devCode' not in queued, 'Development mailbox must be disabled'
    def find_mail():
        messages=call('/api/v1/messages',base=MAIL).get('messages',[])
        return next((message for message in messages if any(x.get('Address')==email for x in message.get('To',[]))),None)
    message=until(find_mail,50)
    full=call('/api/v1/message/'+message['ID'],base=MAIL)
    code=re.search(r'\b\d{6}\b',full.get('Text',''))
    assert code, 'Registration code not delivered to Mailpit'
    call('/api/auth/register','POST',{'username':'infra_'+suffix,'email':email,'password':'integration-password123','code':code.group()})
    registered=call('/api/auth/login','POST',{'username':'infra_'+suffix,'password':'integration-password123'})['token']
    call('/api/auth/logout','POST',{},registered)
    try:
        call('/api/auth/me',token=registered)
        raise AssertionError('Revoked token still accepted')
    except HTTPError as error:
        assert error.code==401
    report={'passed':True,'postId':post,'email':email,'mailService':'Mailpit','checks':['login','post/comment','asynchronous Redis reactions and bookmark','RabbitMQ -> SMTP -> Mailpit verification code','registration','JWT revocation']}
    Path(__file__).with_name('middleware-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False))

if __name__=='__main__':main()
