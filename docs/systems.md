# Bundled systems

The first batch targets systems through the SNES generation, including Sega's CD/32X add-ons, Virtual Boy and Neo Geo. Later-console profiles remain available for manually imported Android ARM64 Libretro cores. Neo Geo Pocket is outside this batch.

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
| Mega Drive / Genesis | Genesis Plus GX | `.md`, `.gen`, `.bin` | None |
| Mega-CD / Sega CD | Genesis Plus GX | `.cue` + tracks, `.chd`, `.iso`, `.m3u` | Choose USA/Europe/Japan core entry: `bios_CD_U.bin` / `bios_CD_E.bin` / `bios_CD_J.bin`, 131,072 bytes each; only the selected region is required |
| Sega 32X | PicoDrive | `.32x` | Core's replacement bootstrap; no external BIOS required for this profile |
| Neo Geo AES / MVS | Geolith | `.neo` cartridges | AES: `aes.zip` containing `neo-epo.bin`, `000-lo.lo`. MVS: `neogeo.zip` containing `sp-u2.sp1`, `000-lo.lo`, `sfix.sfix`, `sm1.sm1`. Initial profiles use USA region |
| Virtual Boy | Beetle VB | `.vb`, `.vboy` | None |

Neo Geo uses the TerraOnion `.neo` format; Geolith does not load MAME-style ZIP game sets. No game conversion or copyrighted game content is distributed. The core also contains Neo Geo CD functionality, but the app does not expose a Neo Geo CD profile in this batch.

Unambiguous filename extensions select their system automatically. Generic `.bin`, `.rom`, `.iso`, `.cue`, `.chd` and `.m3u` files use the system selected before import. SG-1000/Master System/Game Gear force their machine type rather than relying solely on cartridge heuristics. Sega CD entries force the selected region. The 32X profile uses a six-button pad; Mega Drive/Genesis and Sega CD default to the original three-button arrangement. The 5200 profile includes the complete keypad and analog stick. Neo Geo uses the four colored arcade buttons, and Virtual Boy uses two distinct D-pads.

Virtual Boy defaults to red/cyan anaglyph. Settings → Virtual Boy display also offers original red/black without glasses and red/blue anaglyph; changes apply at next launch. The right hardware stick controls the second D-pad.

Source compatibility references: [Genesis Plus GX](https://docs.libretro.com/library/genesis_plus_gx/), [PicoDrive](https://docs.libretro.com/library/picodrive/), [Beetle PCE Fast](https://docs.libretro.com/library/beetle_pce_fast/), [Atari800](https://docs.libretro.com/library/atari800/), [Geolith](https://docs.libretro.com/library/geolith/), [Beetle VB](https://docs.libretro.com/library/beetle_vb/). See [validation](validation.md) for the distinction between a bundled core and tested games/devices.
