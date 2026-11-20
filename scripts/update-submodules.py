#!/usr/bin/env python3
"""Initialize pinned sources, fetching only dependencies needed by large core frontends."""
from pathlib import Path
import json,subprocess
ROOT=Path(__file__).resolve().parents[1]
large={
 'ppsspp': ['libretro/libretro-common','ext/armips','ext/glslang','ext/SPIRV-Cross','ext/rapidjson',
            'ext/miniupnp','ext/zstd','ext/cpu_features','ext/rcheevos','ext/libchdr',
            'ext/libadrenotools','ext/OpenXR-SDK','ext/lua','ext/freetype','ext/nanosvg',
            'ext/aemu_postoffice','ext/oboe'],
 'citra': ['externals/boost','externals/nihstro','externals/soundtouch','externals/catch2',
           'externals/dynarmic','externals/fmt','externals/enet','externals/inih/inih',
           'externals/libressl','externals/teakra','externals/lodepng/lodepng','externals/zstd',
           'externals/libyuv','externals/cryptopp-cmake','externals/cryptopp','externals/dds-ktx',
           'externals/glslang','externals/vma','externals/vulkan-headers','externals/sirit',
           'externals/faad2/faad2','externals/library-headers','externals/libadrenotools','externals/oaknut']
}
def run(args,cwd=ROOT): subprocess.run(['git',*args],cwd=cwd,check=True)
run(['submodule','update','--init','--depth','1'])
for item in json.loads((ROOT/'third-party.lock.json').read_text())['repositories']:
 path=ROOT/item['path']
 if not (path/'.gitmodules').is_file(): continue
 run(['submodule','update','--init','--recursive','--depth','1','--',*large.get(item['name'],[])],path)
run(['submodule','update','--init','--recursive','--depth','1','--','libraries/libinfiniselect'])
