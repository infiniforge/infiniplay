#!/usr/bin/env python3
"""Collect pinned-source license documents for the offline About dialog."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
version=re.search(r'versionName\s*=\s*"([^"]+)"',(ROOT/'build.gradle.kts').read_text()).group(1)
parts=['InfiniPlay '+version+'\nCopyright 2026 InfiniForge contributors\nApp code: GPL-3.0-or-later. Emulator plugins retain their respective licenses.\nSource: https://github.com/infiniforge/infiniplay\nClone with --recurse-submodules for corresponding source.\nThe following includes notices from vendored source trees; not every source file is compiled.']
parts.append((ROOT/'docs/acknowledgments.md').read_text())
header=ROOT/'third_party/libretrodroid/libretrodroid/src/main/cpp/libretro/libretro-common/include/libretro.h'
parts.append('Libretro API header license\n'+header.read_text().split('*/',1)[0]+'*/')
for filename in ('dependencies.lock.json','third-party.lock.json','LICENSE','licenses/llvm-libcxx.txt'):
    parts.append(filename+'\n'+(ROOT/filename).read_text())
names={'license','license.txt','license.md','copying','copying.txt','copyright','copyright.txt','notice','notice.txt','unlicense'}
seen=set()
for directory in ('third_party','libraries'):
    for file in sorted((ROOT/directory).rglob('*')):
        if not file.is_file() or file.name.lower() not in names:continue
        text=file.read_text(errors='replace').strip()
        if text in seen:continue
        seen.add(text);parts.append(str(file.relative_to(ROOT))+'\n'+text)
assets=ROOT/'src/main/assets';assets.mkdir(exist_ok=True)
# Preserve notice wording while normalizing trailing whitespace from upstream documents.
(assets/'notices.txt').write_text('\n'.join(line.rstrip() for line in '\n\n'.join(parts).splitlines())+'\n')
print('Updated offline third-party notices')
