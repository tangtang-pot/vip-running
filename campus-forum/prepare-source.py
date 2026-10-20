"""Produce a reviewed source-only archive without initializing or uploading Git."""
from pathlib import Path
import json, re, zipfile

ROOT=Path(__file__).resolve().parent
EXCLUDED_DIRS={'.git','.local','.idea','target','data','node_modules','__pycache__'}
EXCLUDED_NAMES={'.env','local-ai.properties','maven-local-settings.xml','middleware-result.json','wsl-setup-status.json'}
EXTENSIONS={'.java','.xml','.sql','.yml','.yaml','.md','.cmd','.ps1','.py','.sh','.html','.css','.js','.json','.example','.png','.jpg','.svg','.properties'}

def source_files():
    for path in sorted(ROOT.rglob('*')):
        if not path.is_file():continue
        relative=path.relative_to(ROOT)
        if any(part in EXCLUDED_DIRS for part in relative.parts[:-1]):continue
        if path.name in EXCLUDED_NAMES or path.suffix.lower() in {'.log','.clixml','.iml'}:continue
        if path.suffix.lower() in EXTENSIONS or path.name in {'Dockerfile','.gitignore','.dockerignore'}:yield path

def main():
    private_values=[]
    env=ROOT/'.env'
    if env.exists():
        for line in env.read_text().splitlines():
            if '=' in line:
                key,value=line.split('=',1)
                if key in {'DB_ROOT_PASSWORD','DB_PASSWORD','JWT_SECRET','RABBITMQ_PASSWORD'} and len(value)>16:private_values.append(value)
    ai=ROOT/'local-ai.properties'
    if ai.exists():
        for line in ai.read_text().splitlines():
            if line.startswith('app.ai.api-key=') and line.split('=',1)[1]:private_values.append(line.split('=',1)[1])
    files=list(source_files())
    failures=[]
    patterns=[re.compile(r'\bsk-[A-Za-z0-9_-]{20,}'),re.compile(r'\bAKIA[A-Z0-9]{16}\b'),re.compile(r'-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----')]
    for path in files:
        if path.suffix.lower() in {'.png','.jpg'}:continue
        content=path.read_text(encoding='utf-8-sig')
        if any(value in content for value in private_values) or any(pattern.search(content) for pattern in patterns):
            failures.append(str(path.relative_to(ROOT)))
    if failures:raise SystemExit('Potential secret in source files (values hidden): '+', '.join(failures))
    output=ROOT.parent/'campus-forum-source-reviewed.zip'
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
        for path in files:archive.write(path,Path('campus-forum')/path.relative_to(ROOT))
    with zipfile.ZipFile(output) as archive:
        assert len(archive.namelist())==len(files)
        assert not any('/.local/' in name or '/target/' in name or name.endswith('/.env') or name.endswith('/local-ai.properties') or name.endswith('.clixml') for name in archive.namelist())
    report={'sourceFiles':len(files),'knownLocalSecretScan':'passed','credentialAndDataExclusions':'passed','gitCreated':False,'uploaded':False,'archive':str(output),'files':[str(p.relative_to(ROOT)) for p in files]}
    (ROOT/'.local').mkdir(exist_ok=True)
    (ROOT/'.local/source-review.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({key:value for key,value in report.items() if key!='files'},ensure_ascii=False))

if __name__=='__main__':main()
