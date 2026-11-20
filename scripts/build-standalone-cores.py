#!/usr/bin/env python3
"""Build large upstream CMake projects in isolated caches, without editing their sources."""
from pathlib import Path
import argparse, os, shutil, subprocess
try:
    import fcntl
except ImportError:
    fcntl = None
ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--ndk')
parser.add_argument('--core', choices=['ppsspp','citra'])
parser.add_argument('--configure-only', action='store_true')
args = parser.parse_args()
termux = not args.ndk and 'TERMUX_VERSION' in os.environ
base = ROOT / 'build/standalone-cores'
stage = base / 'jni/arm64-v8a'
stage.mkdir(parents=True, exist_ok=True)
common = ['-DCMAKE_POLICY_VERSION_MINIMUM=3.5', '-DCMAKE_BUILD_TYPE=Release',
          '-DCMAKE_POSITION_INDEPENDENT_CODE=ON', '-DBUILD_SHARED_LIBS=OFF',
          '-DCMAKE_SHARED_LINKER_FLAGS=-L/system/lib64 -Wl,-z,max-page-size=16384 -Wl,--no-undefined' if termux else '-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384 -Wl,--no-undefined',
          '-DCMAKE_C_FLAGS_RELEASE=-O2 -DNDEBUG', '-DCMAKE_CXX_FLAGS_RELEASE=-O2 -DNDEBUG']
if termux:
    prefix = Path(os.environ['PREFIX'])
    shim = base/'ndk-compat/sources/android/cpufeatures'
    shim.parent.mkdir(parents=True, exist_ok=True)
    if not shim.exists(): shim.symlink_to(ROOT/'third_party/ndk-cpufeatures', target_is_directory=True)
    common += ['-DPLAY_TERMUX=ON', '-DANDROID_NDK='+str(base/'ndk-compat'), '-DANDROID=ON', '-DANDROID_ABI=arm64-v8a', '-DANDROID_PLATFORM=android-26',
               '-DCMAKE_C_FLAGS=-DANDROID -I'+str(prefix/'include'), '-DCMAKE_CXX_FLAGS=-DANDROID -I'+str(prefix/'include'),
               '-DCMAKE_LIBRARY_PATH=/system/lib64', '-DCMAKE_INSTALL_PREFIX='+str(base/'install')]
else:
    if not args.ndk: raise SystemExit('Pass --ndk /path/to/android-ndk for desktop builds')
    common += ['-DCMAKE_TOOLCHAIN_FILE='+str(Path(args.ndk)/'build/cmake/android.toolchain.cmake'),
               '-DANDROID_ABI=arm64-v8a', '-DANDROID_PLATFORM=android-26', '-DANDROID_STL=c++_static']
configs = {
 'ppsspp': ['-DCMAKE_DISABLE_FIND_PACKAGE_X11=ON', '-DLIBRETRO=ON', '-DUSING_GLES2=ON', '-DUSING_EGL=ON', '-DUSING_FBDEV=ON', '-DUSE_FFMPEG=ON',
            '-DUSE_SYSTEM_FFMPEG=OFF', '-DUSE_SYSTEM_SNAPPY=OFF', '-DUSE_SYSTEM_LIBPNG=OFF',
            '-DUSE_SYSTEM_LIBZIP=OFF', '-DUSE_SYSTEM_ZSTD=OFF', '-DUSE_SYSTEM_LIBCHDR=OFF',
            '-DUSE_SYSTEM_MINIUPNPC=OFF', '-DUSE_SYSTEM_RAPIDJSON=OFF', '-DUSING_X11_VULKAN=OFF',
            '-DUSE_WAYLAND_WSI=OFF', '-DHEADLESS=OFF', '-DUNITTEST=OFF', '-DATLAS_TOOL=OFF',
            '-DUSE_DISCORD=OFF', '-DARM64=ON'],
 'citra': ['-DCMAKE_PROJECT_citra_INCLUDE='+str(ROOT/'src/main/cpp/standalone-overrides.cmake'), '-DENABLE_LIBRETRO=ON', '-DENABLE_QT=OFF', '-DENABLE_SDL2=OFF', '-DENABLE_TESTS=OFF',
           '-DENABLE_WEB_SERVICE=OFF', '-DENABLE_SCRIPTING=OFF', '-DENABLE_CUBEB=OFF',
           '-DENABLE_OPENAL=OFF', '-DENABLE_LIBUSB=OFF', '-DENABLE_DEDICATED_ROOM=OFF',
           '-DCITRA_ENABLE_BUNDLE_TARGET=OFF', '-DENABLE_LTO=OFF', '-DCITRA_USE_PRECOMPILED_HEADERS=ON',
           '-DCITRA_WARNINGS_AS_ERRORS=OFF', '-DENABLE_OPT=OFF', '-DENABLE_COMPATIBILITY_LIST_DOWNLOAD=OFF']
}
for name, options in configs.items():
    if args.core and args.core != name: continue
    lock = (base/(name+'.lock')).open('w')
    if fcntl is not None: fcntl.flock(lock,fcntl.LOCK_EX)
    source = ROOT/'third_party'/name
    revision = subprocess.check_output(['git','rev-parse','HEAD'],cwd=source,text=True).strip()
    if name == 'ppsspp':
        # Upstream's version script only recognizes .git directories, not submodule files.
        # Use its supported no-update marker with the authoritative pinned commit.
        version_file = base/name/'git-version.cpp'
        version_file.parent.mkdir(parents=True,exist_ok=True)
        version_source = 'const char *PPSSPP_GIT_VERSION = "'+revision+'";\n#define PPSSPP_GIT_VERSION_NO_UPDATE 1\n'
        if not version_file.exists() or version_file.read_text()!=version_source: version_file.write_text(version_source)
    if name == 'citra':
        # This upstream CMake assumes .git is a directory and installs a developer hook.
        # Use a source view with only that hook block removed; never modify the gitlink.
        view = ROOT/'build/native-source/citra'
        view.mkdir(parents=True, exist_ok=True)
        for child in source.iterdir():
            if child.name in ('.git', 'CMakeLists.txt'): continue
            target = view/child.name
            if not target.exists(): target.symlink_to(child, target_is_directory=child.is_dir())
        cmake = (source/'CMakeLists.txt').read_text()
        start = cmake.index('if(NOT EXISTS ${PROJECT_SOURCE_DIR}/.git/hooks/pre-commit)')
        end = cmake.index('endif()', start)+len('endif()')
        patched = cmake[:start]+cmake[end:]
        project_file = view/'CMakeLists.txt'
        if not project_file.exists() or project_file.read_text()!=patched: project_file.write_text(patched)
        gitdir = subprocess.check_output(['git','rev-parse','--absolute-git-dir'],cwd=source,text=True).strip()
        git_file = view/'.git'
        reference = 'gitdir: '+gitdir+'\n'
        if not git_file.exists() or git_file.read_text()!=reference: git_file.write_text(reference)
        # Bionic also exposes GNU strerror_r when _GNU_SOURCE is enabled.
        error_source = (source/'src/common/error.cpp').read_text()
        needle = '#if defined(__GLIBC__) &&'
        assert error_source.count(needle)==1, 'Pinned Citra error adapter changed'
        error_source = error_source.replace(needle, '#if (defined(__GLIBC__) || defined(__BIONIC__)) &&')
        error_file = ROOT/'build/native-source/citra-error.cpp'
        if not error_file.exists() or error_file.read_text()!=error_source: error_file.write_text(error_source)
        options = [*options, '-DPLAY_PATCHED_CITRA_ERROR='+str(error_file)]
        metadata_changed = False
        for filename in ('GIT-COMMIT','GIT-TAG'):
            metadata = view/filename
            value = revision+'\n'
            if not metadata.exists() or metadata.read_text()!=value:
                metadata.write_text(value)
                metadata_changed = True
        # Citra's generated revision is part of its save-state header, so UNKNOWN is unsafe.
        if metadata_changed: (base/name/'src/common/scm_rev.cpp').unlink(missing_ok=True)
        source = view
    out = base / name
    subprocess.run(['cmake', '-S', str(source), '-B', str(out), '-G', 'Ninja', *common, *options], check=True)
    if args.configure_only:
        lock.close()
        continue
    subprocess.run(['cmake', '--build', str(out), '--target', name+'_libretro', '--parallel', os.environ.get('PLAY_NATIVE_JOBS', '2')], check=True)
    files = list(out.glob(name+'_libretro*.so'))
    assert len(files)==1, (name, files)
    target=stage/('lib'+name+'.so')
    shutil.copy2(files[0], target)
    if termux: subprocess.run(['patchelf', '--remove-rpath', str(target)], check=True)
    lock.close()
