#!/usr/bin/env python3
"""Check ARM64 packaging, 16 KiB ELF segments and portable native dependencies."""
from pathlib import Path
import sys,zipfile,tempfile,subprocess,struct,re
ROOT=Path(__file__).resolve().parents[1]
apk=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'InfiniPlay-debug.apk'
expected={'quicknes','gambatte','fceumm','genesis','picodrive','pce','handy','stella','atari800','prosystem','snes9x','vb','geolith','libretrodroid'}
names={'lib'+name+'.so'for name in expected}
allowed={'libc.so','libm.so','libdl.so','libz.so','liblog.so','libandroid.so','libEGL.so','libGLESv2.so','libGLESv3.so','libOpenSLES.so','libc++_shared.so'}
with zipfile.ZipFile(apk) as z,tempfile.TemporaryDirectory(prefix='apk-check-',dir=ROOT/'build') as temp:
    native={name for name in z.namelist()if name.startswith('lib/')and name.endswith('.so')}
    required={'lib/arm64-v8a/'+name for name in names}
    assert native in (required,required|{'lib/arm64-v8a/libc++_shared.so'}),native
    for name in sorted(native):
        data=z.read(name);assert data[:6]==b'\x7fELF\x02\x01'and struct.unpack_from('<H',data,18)[0]==183,name
        offset=struct.unpack_from('<Q',data,32)[0];size,count=struct.unpack_from('<HH',data,54)
        for i in range(count):
            segment=struct.unpack_from('<IIQQQQQQ',data,offset+i*size)
            if segment[0]==1:assert segment[7]>=16384 and segment[2]%16384==segment[3]%16384,(name,'unaligned LOAD')
        file=Path(temp)/Path(name).name;file.write_bytes(data);dynamic=subprocess.check_output(['readelf','-d',str(file)],text=True)
        deps=set(re.findall(r'\(NEEDED\).*\[([^]]+)\]',dynamic));assert deps<=allowed,(name,deps-allowed)
        assert 'libc++_shared.so'not in deps or 'lib/arm64-v8a/libc++_shared.so'in native,(name,'missing C++ runtime')
        assert '/data/data/'not in dynamic,(name,'local runtime path')
    assert 'classes.dex'in z.namelist()and 'assets/notices.txt'in z.namelist()
    notices=z.read('assets/notices.txt').decode()
    assert 'Thank you to the Libretro project'in notices and 'Libretro API header license'in notices
print('APK verified: 13 cores, host, ARM64, 16 KiB pages, portable native dependencies and Libretro acknowledgments')
