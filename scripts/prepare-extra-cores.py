#!/usr/bin/env python3
"""Evaluate pinned upstream build manifests, then generate out-of-tree CMake sources/flags."""
from pathlib import Path
import subprocess,shlex
def write(path,text):
    if not path.is_file() or path.read_text()!=text:
        path.write_text(text)
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/native-source';OUT.mkdir(parents=True,exist_ok=True)
CORES={'genesis':'Makefile.libretro','picodrive':'Makefile.libretro','pce':'Makefile','handy':'Makefile','stella':'Makefile','atari800':'Makefile','prosystem':'Makefile','snes9x':'Makefile.libretro','vb':'Makefile','geolith':'libretro/Makefile','ngp':'Makefile','wswan':'Makefile'}
cmake=[]
for name,makefile in CORES.items():
 root=ROOT/'third_party'/name
 makepath=root/makefile;root=makepath.parent;makefile=makepath.name
 capture=OUT/(name+'.variables')
 probe=OUT/(name+'.mk')
 probe.write_text('''.PHONY: play-manifest
play-manifest:
\t$(file >CAPTURE,sources=$(SOURCES_C) $(SOURCES_CXX) $(SOURCES) $(OBJS))
\t$(file >>CAPTURE,flags=$(CFLAGS) $(CXXFLAGS) $(INCFLAGS) $(INCDIRS) $(LIBRETRO_CFLAGS))
\t@:
'''.replace('CAPTURE',str(capture)))
 args=['make','--no-print-directory','-s','-f',makefile,'-f',str(probe),'play-manifest','platform=unix','ARCH=arm64','CC=clang','CXX=clang++','NO_ARM_ASM=1','use_sh2drc=0','use_svpdrc=0','PLATFORM_TREMOR=1','PLATFORM_ZLIB=1']
 result=subprocess.run(args,cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
 if result.returncode:raise RuntimeError(name+': '+result.stderr)
 data=dict(line.split('=',1)for line in capture.read_text().splitlines())
 sources=[]
 for item in shlex.split(data['sources']):
    p=root/item
    if p.suffix=='.o':
        candidates=[p.with_suffix(ext) for ext in ('.c','.cpp','.cxx','.S','.s')];p=next((x for x in candidates if x.is_file()),p)
    if p.suffix not in ('.c','.cpp','.cxx','.S','.s'):continue
    assert p.is_file(), (name,p)
    sources.append(p.resolve())
 assert len(sources)>5,(name,sources)
 flags=[]
 tokens=shlex.split(data['flags']);i=0
 while i<len(tokens):
    token=tokens[i];i+=1
    if token=='-I':token+=tokens[i];i+=1
    if token.startswith('-I'):token='-I'+str((root/token[2:]).resolve())
    elif not(token.startswith('-D')or token in ('-fno-strict-aliasing','-fwrapv','-fno-common','-fsigned-char','-funsigned-char','-fno-strict-overflow')):continue
    if 'GIT_VERSION' in token:continue
    flags.append(token)
 cmake.append('add_library('+name+' SHARED\n'+''.join('  "'+str(p)+'"\n'for p in dict.fromkeys(sources))+')\n')
 cmake.append('target_compile_options('+name+' PRIVATE\n'+''.join('  [=['+f+']=]\n'for f in dict.fromkeys(flags))+')\n')
 cmake.append('target_link_libraries('+name+' PRIVATE m z)\n')
 print(name,len(set(sources)),'sources')
write(OUT/'extra-cores.cmake','\n'.join(cmake))
