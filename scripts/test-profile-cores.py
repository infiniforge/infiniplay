#!/usr/bin/env python3
"""Exercise new profiles in isolated processes with original homebrew, without retail games/BIOS."""
from pathlib import Path
import ctypes as C, os, shutil, struct, subprocess, sys, tempfile, time
from importlib import util
ROOT=Path(__file__).resolve().parents[1]
spec=util.spec_from_file_location('native_checks',ROOT/'scripts/test-native-cores.py');N=util.module_from_spec(spec);spec.loader.exec_module(N)
spec=util.spec_from_file_location('egl_frontend',ROOT/'scripts/profile-egl.py');E=util.module_from_spec(spec);spec.loader.exec_module(E)

def fixtures(directory):
    psx=bytearray(4096);psx[:8]=b'PS-X EXE'
    for offset,value in [(0x10,0x80010000),(0x18,0x80010000),(0x1c,2048),(0x30,0x801fff00)]:struct.pack_into('<I',psx,offset,value)
    # J $80010000; NOP. Original MIPS instructions, using the core's HLE boot in this probe only.
    struct.pack_into('<II',psx,2048,0x08004000,0)
    (directory/'original.exe').write_bytes(psx)
    ds=bytearray(16384);ds[:12]=b'INFINIPROBE ';ds[12:16]=b'IPLT';ds[16:18]=b'IF'
    struct.pack_into('<IIII',ds,0x20,0x200,0x02000000,0x02000000,512)
    struct.pack_into('<IIII',ds,0x30,0x400,0x03800000,0x03800000,512)
    struct.pack_into('<I',ds,0x80,len(ds));struct.pack_into('<I',ds,0x84,0x200)
    for offset in (0x200,0x400):struct.pack_into('<I',ds,offset,0xeafffffe)
    # Header checksum, without a copyrighted logo or firmware image.
    crc=0xffff
    for value in ds[:0x15e]:
        crc^=value
        for _ in range(8):crc=(crc>>1)^0xa001 if crc&1 else crc>>1
    struct.pack_into('<H',ds,0x15e,crc)
    (directory/'original.nds').write_bytes(ds)
    n64=bytearray(16384);n64[:4]=bytes.fromhex('80371240');struct.pack_into('>I',n64,8,0x80000400);n64[0x20:0x34]=b'INFINIPLAY ORIGINAL '
    instructions=[0x3c08a010,0x3c094946,0x3529504c,0xad090000,0x3c08a440]
    for offset,value in [(0,0x302),(4,0x100000),(8,320),(12,2),(0x18,0x20d),(0x1c,0xc15),(0x20,0x0c150c15),(0x24,0x006c02ec),(0x28,0x002501ff),(0x2c,0x000e0204),(0x30,0x200),(0x34,0x400)]:
        instructions += [0x3c090000|(value>>16),0x35290000|(value&0xffff),0xad090000|offset]
    loop_pc=0xa4000040+len(instructions)*4
    instructions += [0x3c08a010,0x8d090004,0x25290001,0xad090004,0x08000000|((loop_pc&0x0fffffff)>>2),0]
    for index,value in enumerate(instructions):struct.pack_into('>I',n64,0x40+index*4,value)
    (directory/'original.z64').write_bytes(n64)
    # Original ELF32 MIPS program: J $08800000; NOP.
    elf=bytearray(8192);elf[:16]=b'\x7fELF'+bytes([1,1,1])+bytes(9)
    struct.pack_into('<HHIIIIIHHHHHH',elf,16,2,8,1,0x08804000,52,0,0x10001000,52,32,1,40,0,0)
    struct.pack_into('<IIIIIIII',elf,52,1,0x1000,0x08804000,0x1100,512,4096,7,4096)
    instructions=[0x3c080880,0x35084000,0x3c094946,0x3529504c,0xad090080,0x8d090084,0x25290001,0xad090084,0x0a201005,0]
    for index,value in enumerate(instructions):struct.pack_into('<I',elf,0x1000+index*4,value)
    struct.pack_into('<HH28sIIIII',elf,0x1100,0,0x0100,b'InfiniPlayProbe',0,0,0,0,0)
    (directory/'original.elf').write_bytes(elf)
    # Original ARM 3DSX: a single-page code segment and zero relocation counts.
    three=bytearray(32+24+4096*3)
    struct.pack_into('<4sHHIIIIII',three,0,b'3DSX',32,8,0,0,4096,4096,4096,0)
    struct.pack_into('<I',three,56,0xeafffffe)
    (directory/'original.3dsx').write_bytes(three)

def probe(name):
    runtime=C.CDLL(str(N.LIBS/'libc++_shared.so'),mode=C.RTLD_GLOBAL)
    with tempfile.TemporaryDirectory(prefix='profile-test-',dir=ROOT/'build') as tmp:
        directory=Path(tmp);fixtures(directory)
        if name=='ppsspp':shutil.copytree(ROOT/'build/generated/core-assets/cores/ppsspp/PPSSPP',directory/'PPSSPP')
        variables={'pcsx_rearmed_bios':'hle','desmume_screens_layout':'top/bottom','desmume_screens_gap':'0','desmume_pointer_type':'touch','desmume_internal_resolution':'256x192','desmume_num_cores':'1'}
        variables.update({'mupen64plus-rdp-plugin':'gliden64','mupen64plus-rsp-plugin':'hle','mupen64plus-cpucore':'dynamic_recompiler','ppsspp_backend':'opengl','ppsspp_internal_resolution':'480x272','citra_layout_option':'Default Top-Bottom Screen','citra_resolution_factor':'1x (Native)','citra_use_cpu_jit':'enabled'})
        hardware=E.Hardware() if name in ('mupen64plus','ppsspp','citra') else None
        core=N.Core(name,directory,variables,hardware)
        N.check(core.info.name and core.info.extensions,name+' did not advertise content')
        suffix={'pcsx':'exe','desmume':'nds','mupen64plus':'z64','ppsspp':'elf','citra':'3dsx'}[name]
        path=directory/('original.'+suffix);original=path.read_bytes()
        N.check(core.load(path),name+' rejected original test program')
        if hardware:hardware.reset()
        if name=='ppsspp':
            for _ in range(2400):
                core.lib.retro_run()
                # PPSSPP reserves memory before its asynchronous boot has mapped every view.
                # Its documented 128 MiB state placeholder means the GPU/boot is not ready.
                ready_size=core.lib.retro_serialize_size()
                ram=core.lib.retro_get_memory_data(2) if 0<ready_size<128*1024*1024 else None
                if ram and core.lib.retro_get_memory_size(2)>0x804084 and C.c_uint32.from_address(ram+0x804080).value==0x4946504c:break
                N.check(not core.shutdown,'PSP rejected the generated ELF during asynchronous boot')
                time.sleep(.002)
            else:raise AssertionError('PSP original CPU program never started')
        # Empty homebrew need not submit audio; software probes still verify audio callbacks.
        core.run(audio=hardware is None,state=True,max_runs=240 if hardware else 120,min_frames=1 if name=='mupen64plus' else 120)
        if name=='mupen64plus':
            ram=core.lib.retro_get_memory_data(2)
            N.check(ram and C.c_uint32.from_address(ram+0x100000).value==0x4946504c,'N64 original CPU program did not execute')
            N.check(C.c_uint32.from_address(ram+0x100004).value>0,'N64 program did not advance')
        if hardware:hardware.close()
        core.close()
        if hardware:hardware.release()
        N.check(path.read_bytes()==original,name+' modified source content')
        print(name+': original program, '+('120 emulation steps and a framebuffer' if name=='mupen64plus' else '120 frames')+('/audio' if hardware is None else ' with GLES')+', state round trip'+' and source preservation passed',flush=True)

def main():
    if len(sys.argv)>1:probe(sys.argv[1]);return
    for name in ('pcsx','desmume','mupen64plus','ppsspp','citra'):
        subprocess.run([sys.executable,__file__,name],check=True,timeout=90)
    print('Generated programs verify integration; retail games and Android touch/gamepad/lifecycle acceptance remain untested.',flush=True)

if __name__=='__main__':main()
