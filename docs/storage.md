# Storage and media

All browsing uses `libinfiniselect` with a stable `infiniplay` client ID. Selection preserves the authoritative source reference and reads through the current content URI. Sibling media resolves through InfiniSelect's relative-location API. There is no application-owned network/archive/filesystem implementation and no required companion application.

Import and existing library reads share cartridge-extension detection. Unambiguous extensions override the selected/stored system; `.md`, `.gen` and `.smd` select Mega Drive/Genesis. Existing entries retain their IDs and files when their system is corrected, so reimport is unnecessary. Generic `.bin` and disc formats keep the selected system.

InfiniSelect hosts the complete Folder browser and delegates collection writes to Folder; InfiniPlay has no broad storage permissions. Shared links live at `Internal storage/infini/play/{recents,favorites}/<system>/`; see [browser](browser.md) for permissions and deduplication. Source identities remain in links; native working files and saves remain private.

Games are staged under `files/games/<UUID>/`, with their source identity stored in `games.json`. A manifest imports every referenced sibling recursively (M3U → CUE → BIN/WAV); order and relative subdirectories are retained. Absolute/network paths, parent traversal, nested M3U files and malformed/empty manifests are rejected. Limits: 512 referenced files, three nesting levels, 1 MiB manifest, 2 GiB individual media and 8 GiB complete set. Import failure removes its staged game directory. `.part` files never become catalog entries. Files needing siblings must be selected from a provider/directory granting access to those files; use InfiniFolder when SAF only grants one file. Import never writes back to selected sources.

FDS content selects FCEUmm/FDS and requires `firmware/fceumm_fds/disksys.rom` (8,192 bytes). gpSP requires `gba_bios.bin` (16,384). Legacy melonDS requires `bios7.bin` (16,384), `bios9.bin` (4,096), `firmware.bin` (131,072/262,144/524,288). PCSX-ReARMed uses a required selected 512 KiB BIOS stored as `scph5501.bin` rather than relying on HLE fallback. The first bundle also declares the [Lynx/CD/Neo Geo firmware requirements](systems.md). Neo Geo BIOS archives must contain the required files for the chosen USA AES/MVS profile. Other profiles declare no mandatory firmware; additional core/game requirements remain core-specific. Byte-size checks catch empty/truncated files; they do not authenticate firmware. The user supplies these files; no console BIOS is included.

Every required missing/wrong-size file is prompted at launch, selected through InfiniSelect and validated as a candidate before replacing its canonical copy. The pending game is launched only after all requirements pass; cancelling aborts that launch.

External cores are privately staged under `codeCacheDir/cores/`, checked as little-endian ELF64 ARM64 and marked read-only. Android may clear code cache after updates; launch then requests a fresh import. Imported native code runs with app privileges. Only select cores from a source you trust. Core IDs select a compatibility profile, not cryptographic verification.

Save memory is `saves/<gameUUID>/<coreID>/save.srm`; manual state slot is `slot.state`. Atomic replacements prevent a partial write from replacing the last good file. SRAM is captured before activity pause and written on a worker. State capture/restore occurs on the emulation thread. No automatic state restore is attempted. FDS writes use the core's save-memory representation in this separate directory. There is no source-file replacement or disk commit action. Save/BIOS export and app backup are not implemented in this initial version.

GBA, Pocket/Color and WonderSwan/Color imports use the same private staging and
authoritative-source identity rules. Their bundled profiles require no external
BIOS; mGBA uses its built-in HLE implementation. The imported gpSP profile still
requires `gba_bios.bin`. Pocket flash is captured as a private whole-cartridge
SRAM snapshot (`save.srm`) through the app adapter, so changes persist through
the existing background save path as well as NeoPop's normal unload path. This
private snapshot is not the portable NeoPop `.ngf` format. WonderSwan SRAM and
mGBA save memory use their normal Libretro interfaces. Saves stay per game/core,
and no selected ROM is rewritten. Actual background/restart and retail-game
save compatibility still require device confirmation.
