#!/usr/bin/env python3
from pathlib import Path
import os,shutil,subprocess,fcntl
ROOT=Path(__file__).resolve().parents[1]
PREFIX=Path(os.environ.get('PREFIX',Path(shutil.which('clang')).resolve().parents[1]))
out=ROOT/'build/native';jni=out/'jni/arm64-v8a';jni.mkdir(parents=True,exist_ok=True)
# A native build owns the output tree until compilation and staging finish.
lock=(out/"build.lock").open("w");fcntl.flock(lock,fcntl.LOCK_EX)
subprocess.run(['cmake','-S',str(ROOT/'src/main/cpp'),'-B',str(out/'cmake'),'-G','Ninja',
 '-DCMAKE_POLICY_VERSION_MINIMUM=3.5','-DCMAKE_BUILD_TYPE=Release','-DPLAY_TERMUX=ON','-DPLAY_JNI_INCLUDE='+str(PREFIX/'include'),
 '-DCMAKE_SHARED_LINKER_FLAGS=-L/system/lib64','-DCMAKE_INSTALL_PREFIX='+str(PREFIX)],check=True)
subprocess.run(['cmake','--build',str(out/'cmake'),'--parallel',os.environ.get('PLAY_NATIVE_JOBS','2')],check=True)
for name in ['libquicknes.so','libgambatte.so','libfceumm.so','liblibretrodroid.so',*[f'lib{n}.so' for n in ('genesis','picodrive','pce','handy','stella','atari800','prosystem','snes9x','vb','geolith','mgba','ngp','wswan')]]:
    files=list((out/'cmake').rglob(name));assert len(files)==1,(name,files)
    shutil.copy2(files[0],jni/name)

shutil.copy2(PREFIX/"lib/libc++_shared.so",jni/"libc++_shared.so")

# Termux's compiler adds a local runtime search path; Android resolves our packaged runtime by SONAME.
for library in jni.glob('*.so'):
    subprocess.run(['patchelf','--remove-rpath',str(library)],check=True)
