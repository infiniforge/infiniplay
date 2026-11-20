#!/usr/bin/env python3
from pathlib import Path
import json,subprocess,hashlib
root=Path(__file__).resolve().parents[1]
for filename in ('dependencies.lock.json','third-party.lock.json'):
    document=json.loads((root/filename).read_text()); dependencies=document['repositories']
    if isinstance(dependencies,dict): dependencies=[dict(d,path=document['directory']+'/'+name) for name,d in dependencies.items()]
    for dep in dependencies:
        actual=subprocess.check_output(['git','-C',str(root/dep['path']),'rev-parse','HEAD'],text=True).strip()
        assert actual==dep['commit'], f"Incorrect dependency pin: {dep['path']}"
    for item in document.get('vendored_files', []):
        assert hashlib.sha256((root/item['path']).read_bytes()).hexdigest()==item['sha256'], 'Incorrect vendored source: '+item['path']
print('All declared dependency pins match')
