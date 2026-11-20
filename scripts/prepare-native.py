#!/usr/bin/env python3
"""Generate the small documented host patch without modifying pinned library checkouts."""
from pathlib import Path
import re
def write(path,text):
    if not path.is_file() or path.read_text()!=text:
        path.write_text(text)
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'build/native-source';out.mkdir(parents=True,exist_ok=True)
source=ROOT/'third_party/libretrodroid/libretrodroid/src/main/cpp/video.cpp'
s=source.read_text()
needle='        glDrawArrays(GL_TRIANGLES, 0, 6);'
assert s.count(needle)==1, 'Pinned host draw path changed'
s=s.replace('#include "video.h"','#include "video.h"\n#include "play_screens.h"')
s=s.replace(needle,'''        if (isLastPass && playScreenCount==2) {
            playDrawScreens(shader.gvPositionHandle,shader.gvCoordinateHandle,vertices[1]<vertices[3]);
        } else glDrawArrays(GL_TRIANGLES,0,6);''')
write(out/'video.cpp',s)
# Read only the explicit upstream source lists, not every implementation file.
for name,prefix in [('quicknes','.'),('gambatte','libgambatte/src')]:
    root=ROOT/'third_party'/name
    s=(root/'Makefile.common').read_text()
    source_lines=[]
    for match in re.finditer(r'^SOURCES_C(?:XX)?\s*:=\s*\\\n(.*?)(?=\n\s*\n|\Z)',s,re.M|re.S):
        source_lines += re.findall(r'\$\(CORE_DIR\)/[^\s\\]+\.(?:cpp|c)',match.group(1))
    if name=='gambatte':
        source_lines += re.findall(r'\$\(LIBRETRO_COMM_DIR\)/[^\s\\]+\.c',s)
    paths=[]
    for item in source_lines:
        item=item.replace('$(CORE_DIR)',prefix).replace('$(LIBRETRO_COMM_DIR)','libgambatte/libretro-common')
        p=(root/item).resolve();assert p.is_file(), p
        paths.append(str(p))
    write(out/(name+'-sources.cmake'),'set('+name.upper()+'_SOURCES\n'+''.join('  "'+p+'"\n' for p in dict.fromkeys(paths))+')\n')
    assert len(paths)>20, name

# FCEUmm: same explicit base configuration as Makefile.common (no HD/NTSC extras).
root=ROOT/'third_party/fceumm'
s=(root/'Makefile.common').read_text()
base=s[s.index('SOURCES_C +=',s.index('endif',s.index('ifeq ($(HAVE_NTSC)'))):].split('ifeq ($(HAVE_HDPACK)',1)[0]
compat=s.split('ifneq ($(STATIC_LINKING)',1)[1]
paths=list((root/'src/boards').glob('*.c'))+list((root/'src/input').glob('*.c'))
for item in re.findall(r'\$\(CORE_DIR\)/[^\s\\]+\.c',base)+re.findall(r'\$\(LIBRETRO_COMM_DIR\)/[^\s\\]+\.c',compat):
    paths.append(root/item.replace('$(CORE_DIR)','src').replace('$(LIBRETRO_COMM_DIR)','src/drivers/libretro/libretro-common'))
assert all(p.is_file() for p in paths)
write(out/'fceumm-sources.cmake','set(FCEUMM_SOURCES\n'+''.join('  "'+str(p.resolve())+'"\n' for p in paths)+')\n')

import subprocess
subprocess.run(["python3",str(ROOT/"scripts/prepare-extra-cores.py")],check=True)

source=ROOT/'third_party/libretrodroid/libretrodroid/src/main/cpp/input.cpp'
s=source.read_text().replace('#include "input.h"','#include "input.h"\n#include "play_keyboard.h"')
needle='    switch (device) {'
assert s.count(needle)==1
s=s.replace(needle,needle+'\n        case RETRO_DEVICE_KEYBOARD: return id<512 ? playKeyboard[port][id] : 0;')
write(out/'input.cpp',s)

# PCSX consumes a generated version header; keep it out of the pinned checkout.
write(out/'revision.h', '#define REV "InfiniPlay-pinned"\n')
