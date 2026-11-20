# InfiniPlay roadmap

Status: `[ ]` unimplemented · `[?]` implemented but untested · `[x]` tested.

A system is **tested** here only after interactive device/game validation. Automated tests are listed separately in [validation](validation.md); native synthetic probes do not certify game compatibility. A controller profile/manual core-import path counts as implemented but untested and is identified below.

Device playback confirmed by the user on 2026-11-19 for the seventeen checked systems below. This records reported playback, not exhaustive game, save, controller or lifecycle coverage.

The first core bundle focuses on systems through the SNES era plus Sega add-ons, Neo Geo and Virtual Boy. Version 0.3.0 adds GBA, Neo Geo Pocket/Color and WonderSwan/Color; generated-program checks do not mark them device-tested. Version 0.4.0 bundles the existing N64/PlayStation/PSP/DS/3DS profiles; these remain `[?]` pending interactive game validation. This document preserves the full longer-term system list. See [bundled systems](systems.md) for formats and firmware.

## Atari

- [x] Atari 2600
- [x] Atari 5200
- [x] Atari 7800
- [x] Atari Lynx
- [ ] Atari Jaguar
- [ ] Atari Jaguar CD

## Nintendo

- [x] Famicom / NES
- [?] Famicom Disk System
- [x] Super Famicom / SNES
- [x] Game Boy
- [x] Game Boy Color
- [x] Virtual Boy
- [?] Nintendo 64 — bundled Mupen64Plus-Next
- [x] Game Boy Advance — bundled mGBA; gpSP remains a manual core import
- [ ] GameCube
- [?] Nintendo DS / DSi — DS bundles DeSmuME; DSi support remains unimplemented
- [?] Nintendo 3DS — bundled Citra; decrypted content and OpenGL ES 3.2 required
- [ ] Nintendo Wii
- [ ] Nintendo Wii U

## Sega

- [x] SG-1000
- [x] Master System / Mark III
- [x] Game Gear
- [x] Mega Drive / Genesis
- [?] Mega-CD / Sega CD
- [x] 32X
- [ ] Sega CD + 32X
- [ ] Saturn
- [ ] Dreamcast

## NEC

- [x] PC Engine / TurboGrafx-16
- [?] PC Engine CD / TurboGrafx-CD
- [?] Super CD-ROM² — included by PC Engine CD with System Card 3
- [ ] SuperGrafx
- [ ] PC-FX

## SNK

- [?] Neo Geo AES / MVS
- [ ] Neo Geo CD
- [x] Neo Geo Pocket
- [?] Neo Geo Pocket Color

## Sony

- [?] PlayStation — bundled PCSX-ReARMed; user BIOS required
- [ ] PlayStation 2
- [?] PlayStation Portable — bundled PPSSPP
- [ ] PlayStation Vita

## Microsoft

- [ ] Original Xbox

## Other consoles and handhelds

- [ ] ColecoVision
- [ ] Mattel Intellivision
- [ ] Vectrex
- [ ] Magnavox Odyssey²
- [ ] Fairchild Channel F
- [ ] Bally Astrocade
- [ ] Emerson Arcadia 2001
- [ ] 3DO Interactive Multiplayer
- [ ] Philips CD-i
- [?] Bandai WonderSwan
- [?] WonderSwan Color
- [ ] Amiga CD32
- [ ] FM Towns Marty
- [ ] Bandai Playdia
- [ ] Watara Supervision
