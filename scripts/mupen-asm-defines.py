#!/usr/bin/env python3
"""Extract upstream ABI offsets from the target object, never from the host ABI."""
from pathlib import Path
import re,sys
symbols = re.findall(rb'@ASM_DEFINE (offsetof_struct_[A-Za-z_0-9]+) (0x[0-9a-fA-F]+)', Path(sys.argv[1]).read_bytes())
assert len(symbols)>20, 'Mupen64Plus target structure offsets missing'
Path(sys.argv[2]).write_text(''.join('#define '+name.decode()+' ('+value.decode()+')\n' for name,value in symbols))
