#!/usr/bin/env python3
"""Execute packaged ARM64 cores on Termux with original generated test media. No game/BIOS assets needed."""
from pathlib import Path
import ctypes as C
import sys,struct,tempfile,subprocess,os
ROOT=Path(__file__).resolve().parents[1]
LIBS=ROOT/'build/native/jni/arm64-v8a'
class Game(C.Structure):_fields_=[('path',C.c_char_p),('data',C.c_void_p),('size',C.c_size_t),('meta',C.c_char_p)]
class Info(C.Structure):_fields_=[('name',C.c_char_p),('version',C.c_char_p),('extensions',C.c_char_p),('fullpath',C.c_bool),('block',C.c_bool)]
LOG=C.CFUNCTYPE(None,C.c_int,C.c_char_p)
ENV=C.CFUNCTYPE(C.c_bool,C.c_uint,C.c_void_p)
VIDEO=C.CFUNCTYPE(None,C.c_void_p,C.c_uint,C.c_uint,C.c_size_t)
AUDIO=C.CFUNCTYPE(None,C.c_int16,C.c_int16)
BATCH=C.CFUNCTYPE(C.c_size_t,C.c_void_p,C.c_size_t)
POLL=C.CFUNCTYPE(None)
INPUT=C.CFUNCTYPE(C.c_int16,C.c_uint,C.c_uint,C.c_uint,C.c_uint)
EJECT=C.CFUNCTYPE(C.c_bool,C.c_bool);EJECTED=C.CFUNCTYPE(C.c_bool);INDEX=C.CFUNCTYPE(C.c_uint);SELECT=C.CFUNCTYPE(C.c_bool,C.c_uint)
class Disk(C.Structure):_fields_=[('eject',EJECT),('ejected',EJECTED),('index',INDEX),('select',SELECT),('count',INDEX),('replace',C.c_void_p),('add',C.c_void_p)]
def check(ok,message):
    if not ok:raise AssertionError(message)
class Core:
 def __init__(self,name,directory):
    self.lib=C.CDLL(str(LIBS/('lib'+name+'.so')));self.disk=None;self.frames=0;self.samples=0;self.directory=os.fsencode(directory);self.logger=LOG(lambda level,message:None)
    def environment(command,data):
        command&=0xffff
        if command in (9,31):C.cast(data,C.POINTER(C.c_char_p))[0]=self.directory;return True
        if command==27:C.cast(data,C.POINTER(C.c_void_p))[0]=C.cast(self.logger,C.c_void_p).value;return True
        if command==13:self.disk=C.cast(data,C.POINTER(Disk)).contents if data else None;return True
        if command==10:return C.cast(data,C.POINTER(C.c_int))[0] in (0,1,2)
        if command in (52,57):C.cast(data,C.POINTER(C.c_uint))[0]=0;return True
        if command==17:C.cast(data,C.POINTER(C.c_bool))[0]=False;return True
        if command==3:C.cast(data,C.POINTER(C.c_bool))[0]=True;return True
        if command in (6,11,16,18,35):return True
        return False
    def video(data,w,h,pitch):
        check(0<w<=2048 and 0<h<=2048,'invalid core video dimensions');self.frames+=1
    def batch(data,frames):self.samples+=frames;return frames
    self.callbacks=[ENV(environment),VIDEO(video),AUDIO(lambda l,r:None),BATCH(batch),POLL(lambda:None),INPUT(lambda p,d,i,k:0)]
    for function,callback in zip(('retro_set_environment','retro_set_video_refresh','retro_set_audio_sample','retro_set_audio_sample_batch','retro_set_input_poll','retro_set_input_state'),self.callbacks):getattr(self.lib,function)(callback)
    self.lib.retro_load_game.argtypes=[C.POINTER(Game)];self.lib.retro_load_game.restype=C.c_bool
    self.lib.retro_serialize_size.restype=C.c_size_t
    self.lib.retro_serialize.argtypes=[C.c_void_p,C.c_size_t];self.lib.retro_serialize.restype=C.c_bool
    self.lib.retro_unserialize.argtypes=[C.c_void_p,C.c_size_t];self.lib.retro_unserialize.restype=C.c_bool
    self.lib.retro_init();self.info=Info();self.lib.retro_get_system_info(C.byref(self.info))
 def load(self,path):
    self.content=C.create_string_buffer(path.read_bytes());self.game=Game(os.fsencode(path),C.cast(self.content,C.c_void_p),path.stat().st_size,None)
    return self.lib.retro_load_game(C.byref(self.game))
 def run(self):
    for _ in range(120):self.lib.retro_run()
    check(self.frames>=120,'core produced too few frames');check(self.samples>0,'core produced no audio')
    size=self.lib.retro_serialize_size();check(0<size<16*1024*1024,'invalid save size');state=C.create_string_buffer(size)
    check(self.lib.retro_serialize(state,size),'serialize failed');check(self.lib.retro_unserialize(state,size),'unserialize failed')
 def close(self,loaded=True):
    if loaded:self.lib.retro_unload_game()
    self.lib.retro_deinit()

def main():
 if not LIBS.is_dir():print('Native execution requires the Termux native build');return
 with tempfile.TemporaryDirectory(prefix='core-test-',dir=ROOT/'build') as tmp:
    directory=Path(tmp)
    # NROM: SEI; CLD; INC $00; JMP $8002. Original generated code only.
    prg=bytearray(16384);prg[:7]=bytes.fromhex('78d8e6004c0280');prg[-6:]=struct.pack('<HHH',0x8000,0x8000,0x8000)
    nes=directory/'probe.nes';nes.write_bytes(b'NES\x1a'+bytes([1,1])+bytes(10)+prg+bytes(8192))
    # GB: JP $0150; LD HL,$c000; INC (HL); JP $0153. No boot ROM required.
    gb=bytearray(32768);gb[0x100:0x104]=bytes.fromhex('c3500100');gb[0x134:0x13b]=b'PROBE  ';gb[0x150:0x157]=bytes.fromhex('2100c034c35301');checksum=0
    for v in gb[0x134:0x14d]:checksum=(checksum-v-1)&255
    gb[0x14d]=checksum;gameboy=directory/'probe.gb';gameboy.write_bytes(gb)
    for name,path in [('quicknes',nes),('gambatte',gameboy),('fceumm',nes)]:
        core=Core(name,directory);check(core.load(path),name+' could not load test program');core.run();core.close();print(name+': generated program, 120 video/audio frames and state round-trip passed',flush=True)
    fds=directory/'probe.fds';fds.write_bytes(b'FDS\x1a'+bytes([6])+bytes(11)+bytes(65500*6))
    core=Core('fceumm',directory);check(b'fds' in core.info.extensions,'FDS extension missing');check(not core.load(fds),'FDS unexpectedly loaded without firmware');core.close(False)
    # Own minimal 6502 boot stub, not replacement firmware and not a game compatibility test.
    bios=bytearray(8192);bios[:5]=bytes.fromhex('78d84c02e0');bios[-6:]=struct.pack('<HHH',0xe000,0xe000,0xe000);(directory/'disksys.rom').write_bytes(bios)
    core=Core('fceumm',directory);check(core.load(fds),'FDS test image did not load');check(core.disk is not None,'No FDS disk callbacks');disk=core.disk
    check(disk.count()==6,'Wrong FDS side count')
    check(disk.eject(False),'Could not insert disk');check(not disk.select(1),'Side changed while inserted')
    for index in range(6):
        check(disk.eject(True) and disk.ejected(),'Eject failed');check(disk.select(index) and disk.index()==index,'Side selection failed');check(disk.eject(False) and not disk.ejected(),'Insert failed')
    check(disk.eject(True),'Eject failed');check(not disk.select(6),'Invalid side accepted');check(disk.index()==5,'Invalid select changed side');core.run();check(disk.index()==5 and disk.ejected(),'FDS save restore lost drive state');core.close()
    print('FDS: missing BIOS rejection, 6-side eject/select/insert, invalid-side rejection, frames and state passed',flush=True)
    # Core initialization/export checks include BIOS-dependent systems without using real firmware.
    for name in ('genesis','picodrive','pce','handy','stella','atari800','prosystem','snes9x','vb','geolith'):
        core=Core(name,directory);check(core.info.name and core.info.extensions,name+' did not report its capabilities');core.close(False);print(name+': ABI, initialization and advertised media passed',flush=True)
    # Generated Z80, 68000, HuC6280 and 6502 cartridges exercise additional runtimes.
    sms=bytearray(32768);sms[:6]=bytes.fromhex('2130c03418fd');sms[0x7ff0:0x7ff8]=b'TMR SEGA';sms[0x7fff]=0x40
    md=bytearray(65536);md[:8]=struct.pack('>II',0xff0000,0x200);md[0x100:0x110]=b'SEGA GENESIS    ';md[0x1f0:0x1f3]=b'USA';md[0x200:0x202]=bytes.fromhex('60fe')
    pce=bytearray(8192);pce[:4]=bytes.fromhex('78d880fe');pce[-6:]=struct.pack('<HHH',0xe000,0xe000,0xe000)
    stella=bytearray(4096);stella[:5]=bytes.fromhex('78d84c02f0');stella[-6:]=struct.pack('<HHH',0xf000,0xf000,0xf000)
    for name,extension,content in [('genesis','sms',sms),('genesis','sg',sms),('genesis','gg',sms),('genesis','md',md),('picodrive','md',md),('pce','pce',pce),('stella','a26',stella)]:
        path=directory/('probe.'+extension);path.write_bytes(content);core=Core(name,directory);check(core.load(path),name+' rejected generated '+extension);core.run();core.close();print(name+': generated '+extension+' frames/audio/state passed',flush=True)
    # A generated BIOS stub and three blank CDs validate the real PCE tray API,
    # not game compatibility. The stub spins inside its own ROM and reads no disc content.
    syscard=bytearray(262144)
    for bank in range(32):
        syscard[bank*8192:bank*8192+4]=bytes.fromhex('78d880fe');syscard[(bank+1)*8192-6:(bank+1)*8192]=struct.pack('<HHH',0xe000,0xe000,0xe000)
    (directory/'syscard3.pce').write_bytes(syscard)
    cues=[]
    for index in range(3):
        track='disc '+str(index+1)+'.bin';cue=directory/('disc '+str(index+1)+'.cue');(directory/track).write_bytes(bytes(2048*128));cue.write_text('FILE "'+track+'" BINARY\n  TRACK 01 MODE1/2048\n    INDEX 01 00:00:00\n');cues.append(cue.name)
    playlist=directory/'set.m3u';playlist.write_text('\n'.join(cues)+'\n');core=Core('pce',directory);check(core.load(playlist),'PCE CD rejected generated set');disk=core.disk;check(disk is not None and disk.count()==3,'PCE CD did not expose all discs')
    for index in range(3):
        check(disk.eject(True) and disk.ejected(),'PCE tray did not open');check(disk.select(index) and disk.index()==index,'PCE disc selection failed');check(disk.eject(False) and not disk.ejected(),'PCE tray did not close');check(not disk.select((index+1)%3),'PCE switched while inserted')
    core.run();core.close();print('PCE CD: generated BIOS/3-disc M3U eject/select/insert, frames/audio/state passed',flush=True)
    # Upstream's original, distributable SNES renderer-test program (no commercial assets).
    generator=directory/'make-snes-probe';subprocess.run(['clang',str(ROOT/'third_party/snes9x/tools/gen_framehash_rom.c'),'-o',str(generator)],check=True)
    snes=directory/'probe.sfc';subprocess.run([str(generator),str(snes)],check=True,stdout=subprocess.DEVNULL);core=Core('snes9x',directory);check(core.load(snes),'SNES rejected generated test program');core.run();core.close();print('snes9x: generated test program frames/audio/state passed',flush=True)
 for filename in (p.name for p in LIBS.glob('*.so')):
    data=subprocess.check_output(['readelf','-d',str(LIBS/filename)],text=True)
    check('/data/data/' not in data,'Nonportable native runtime path in '+filename)
 print('All native core checks passed')
if __name__=='__main__':main()
