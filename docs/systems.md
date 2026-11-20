# Bundled systems

The first batch targets systems through the SNES generation, including Sega's CD/32X add-ons, Virtual Boy and Neo Geo. Version 0.4.0 also bundles the existing N64, PlayStation, PSP, DS and 3DS profiles. Version 0.3.0 also bundles GBA, Neo Geo Pocket/Color and WonderSwan/Color cartridge emulation.

| System | Compiled core | Content | Required external firmware |
| --- | --- | --- | --- |
| Atari 2600 | Stella 2014 | `.a26`, `.bin` | None |
| Atari 5200 | Atari800, forced 5200 mode | `.a52`, `.car`, `.bin` | None: bundled open-source Altirra replacement OS |
| Atari 7800 | ProSystem | `.a78`, `.bin` | None |
| Atari Lynx | Handy | `.lnx`, `.lyx` | `lynxboot.img`, 512 bytes |
| Sega SG-1000 | Genesis Plus GX | `.sg` | None |
| Master System | Genesis Plus GX | `.sms` | None |
| Game Gear | Genesis Plus GX | `.gg` | None |
| NES/Famicom | FCEUmm, QuickNES | `.nes` | None |
| Famicom Disk System | FCEUmm + standard disk adapter | `.fds` | `disksys.rom`, 8,192 bytes |
| Game Boy / Game Boy Color | Gambatte | `.gb`, `.gbc` | None |
| Super Nintendo | Snes9x 2010 | `.sfc`, `.smc` | None for ordinary cartridges; special coprocessor content may have core-specific needs |
| PC Engine / TurboGrafx-16 | Beetle PCE Fast | `.pce` | None |
| PC Engine CD | Beetle PCE Fast | `.cue` + tracks, `.chd`, `.m3u` | `syscard3.pce`, 262,144 bytes (also accepts a 512-byte copier header) |
| Mega Drive / Genesis | Genesis Plus GX | `.md`, `.gen`, `.smd`, `.bin` | None |
| Mega-CD / Sega CD | Genesis Plus GX | `.cue` + tracks, `.chd`, `.iso`, `.m3u` | Choose USA/Europe/Japan core entry: `bios_CD_U.bin` / `bios_CD_E.bin` / `bios_CD_J.bin`, 131,072 bytes each; only the selected region is required |
| Sega 32X | PicoDrive | `.32x` | Core's replacement bootstrap; no external BIOS required for this profile |
| Neo Geo AES / MVS | Geolith | `.neo` cartridges | AES: `aes.zip` containing `neo-epo.bin`, `000-lo.lo`. MVS: `neogeo.zip` containing `sp-u2.sp1`, `000-lo.lo`, `sfix.sfix`, `sm1.sm1`. Initial profiles use USA region |
| Virtual Boy | Beetle VB | `.vb`, `.vboy` | None |
| Game Boy Advance | mGBA | `.gba`, `.agb` | None: built-in HLE BIOS |
| Neo Geo Pocket | Beetle NeoPop | `.ngp` | None: built-in HLE BIOS |
| Neo Geo Pocket Color | Beetle NeoPop | `.ngc`, `.ngpc`, color-header `.ngp` | None: built-in HLE BIOS |
| WonderSwan | Beetle WonderSwan | `.ws` | None |
| WonderSwan Color | Beetle WonderSwan | `.wsc` | None |
| Nintendo 64 | Mupen64Plus-Next + GLideN64/HLE RSP | `.z64`, `.n64`, `.v64` | None for ordinary cartridges; 64DD is not exposed |
| PlayStation | PCSX-ReARMed, ARM64 recompiler/NEON GPU | `.cue` + tracks, `.chd`, `.pbp`, `.m3u`, PS-X `.exe` | `scph5501.bin`, 524,288 bytes; selected via InfiniSelect |
| PlayStation Portable | PPSSPP, GLES | `.iso`, `.cso`, `.pbp`, homebrew `.elf` | None; pinned replacement fonts and core data ship with Play |
| Nintendo DS | DeSmuME, software renderer/ARM interpreter | `.nds` | None for the bundled HLE profile; melonDS remains a firmware-requiring manual alternative |
| Nintendo 3DS | Citra, ARM64 Dynarmic/GLES | decrypted `.3ds`, `.cci`, `.cxi`, homebrew `.3dsx` | None for supported decrypted content; proprietary keys/system archives are not included |

Neo Geo uses the TerraOnion `.neo` format; Geolith does not load MAME-style ZIP game sets. No game conversion or copyrighted game content is distributed. The core also contains Neo Geo CD functionality, but the app does not expose a Neo Geo CD profile in this batch.

Unambiguous filename extensions select their system automatically. Generic `.bin`, `.rom`, `.iso`, `.cue`, `.chd` and `.m3u` files use the system selected before import. SG-1000/Master System/Game Gear force their machine type rather than relying solely on cartridge heuristics. Sega CD entries force the selected region. The 32X profile uses a six-button pad; Mega Drive/Genesis and Sega CD default to the original three-button arrangement. The 5200 profile includes the complete keypad and analog stick. Neo Geo uses the four colored arcade buttons, and Virtual Boy uses two distinct D-pads.

Virtual Boy defaults to red/cyan anaglyph. Settings → Virtual Boy display also offers original red/black without glasses and red/blue anaglyph; changes apply at next launch. The right hardware stick controls the second D-pad.

Source compatibility references: [Genesis Plus GX](https://docs.libretro.com/library/genesis_plus_gx/), [PicoDrive](https://docs.libretro.com/library/picodrive/), [Beetle PCE Fast](https://docs.libretro.com/library/beetle_pce_fast/), [Atari800](https://docs.libretro.com/library/atari800/), [Geolith](https://docs.libretro.com/library/geolith/), [Beetle VB](https://docs.libretro.com/library/beetle_vb/). See [validation](validation.md) for the distinction between a bundled core and tested games/devices.

GBA and Neo Geo Pocket playback are user-confirmed; Pocket Color and WonderSwan/Color still await device/game confirmation.
GBA keeps its A/B, L/R and Start/Select controls. Pocket/Color use a joystick,
A/B and OPTION. WonderSwan/Color show the eight physical X1–X4/Y1–Y4 buttons,
A/B and START. The additional **ROTATE** control sends the core's display-toggle
button; physical X/Y labels retain their mappings across display rotation.
USB gamepads use the D-pad/left stick for X1–X4 and R2/R/L2/L for Y1–Y4.
The host fits both horizontal and vertical games inside the available screen
area in either Android orientation. Cartridge `.ngp` headers distinguish Pocket
Color titles using that shared suffix, and saved system hints survive import.
Sources: [mGBA](https://github.com/libretro/mgba),
[Beetle NeoPop](https://github.com/libretro/beetle-ngp-libretro),
[Beetle WonderSwan](https://github.com/libretro/beetle-wswan-libretro).


The user confirmed N64 and DS playback on 2026-11-20. PlayStation, PSP and 3DS
remain implemented but unconfirmed on Android with retail games. N64 and PSP
use GLES; Citra requires OpenGL ES 3.2 and the launcher rejects older GPUs
clearly. DS/3DS retain separate hardware-spaced screens and bottom-screen touch
input. Citra uses the exact zero-gap top/bottom core layout and native
resolution; the app supplies the physical gap. Encrypted 3DS games, CIA
installation and DSi are not supported by this profile. PSP core assets are
installed in private storage automatically before launch. PlayStation uses the
existing missing-BIOS prompt and removable-media controls. Generic disc/ELF/PBP
extensions remain ambiguous and ask for a system once. `.cso` selects PSP;
`.3dsx` selects 3DS; a PS-X EXE signature selects PlayStation.
