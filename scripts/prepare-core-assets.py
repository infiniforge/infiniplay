#!/usr/bin/env python3
"""Package PPSSPP's pinned open-source core data, including its replacement fonts."""
from pathlib import Path
import json,shutil,subprocess
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/'third_party/ppsspp'
out=ROOT/'build/generated/core-assets/cores/ppsspp'
out.mkdir(parents=True,exist_ok=True)
files=[]
for path in sorted((source/'assets').rglob('*')):
 if not path.is_file() or '.git' in path.parts or path.name.startswith('.') or 'debugger' in path.relative_to(source/'assets').parts:continue
 relative=path.relative_to(source/'assets').as_posix()
 target=out/'PPSSPP'/relative
 target.parent.mkdir(parents=True,exist_ok=True)
 if not target.is_file() or target.read_bytes()!=path.read_bytes():shutil.copyfile(path,target)
 files.append('PPSSPP/'+relative)
for path in (out/'PPSSPP').rglob('*'):
 if path.is_file() and path.relative_to(out).as_posix() not in files:path.unlink()
assert 'PPSSPP/compat.ini' in files and 'PPSSPP/flash0/font/ltn0.pgf' in files
manifest={'revision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=source,text=True).strip(),'files':files}
(out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Staged PPSSPP core data:',len(files),'files')
