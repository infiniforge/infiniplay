# InfiniPlay application

InfiniPlay 0.4.2 is an independently built Android frontend running Libretro cores inside an emulator process. It owns system/core selection, a private imported game library, controller layouts, session lifecycle, save states and persistent game data. Libretro cores own emulation. InfiniSelect is the gateway to Folder-owned browsing and provider-neutral source resolution; InfiniFolder remains optional.

Twenty-one bundled implementations cover the [first system batch](systems.md): Atari consoles/Lynx, Sega 8/16-bit consoles and CD/32X add-ons, NES/FDS, GB/GBC, SNES, PC Engine/CD, Neo Geo AES/MVS, Virtual Boy, GBA, Neo Geo Pocket/Color and WonderSwan/Color. N64, PlayStation, PSP, DS and 3DS now bundle Mupen64Plus-Next, PCSX-ReARMed, PPSSPP, DeSmuME and Citra respectively. gpSP and melonDS remain optional manual imports. These are not claims of tested game compatibility. The old placeholder design using a separately installed Libretro app has been superseded by the user's in-process selection.

Controller profiles cover every listed system, including the 5200 keypad, Neo Geo arcade arc and Virtual Boy dual D-pads. Buttons have hardware-inspired colors, shells and markings. Game displays use nearly the full view width in either orientation, preserve aspect ratio and align near the top of the play area; customizable touch controls overlay the image. DS/3DS screens remain stacked, with proportional widths and a gap that scales with them. The DS default gap is 80 native screen pixels and can be adjusted; the provisional 3DS gap is also adjustable for hardware variants.

Supported content imports include single files and relative M3U/CUE sets. A core exposing removable media gets an on-screen themed Eject/Open button; while ejected the user can change the selected side/disc and insert it. FCEUmm's adapter exposes FDS state through the same API. Game-specific requests for media are displayed by the emulated game; Libretro has no universal new-disk-request event.

The play view has a compact, translucent pause/settings button in its upper-left corner. N64 includes a per-game Bob deinterlacing toggle in that menu. Use it for combing in interlaced titles; it interpolates missing field lines and can soften progressive games. The mode starts off.

Missing required firmware prompts through InfiniSelect before launch. Source games and firmware are copied into app-owned storage. Save memory and manual state slots are separate for each game/core. Controller customizations persist per system and orientation.

See [architecture](architecture.md), [storage](storage.md), [validation](validation.md) and [licenses](third-party.md). Future work includes a core downloader/compatibility database, more bundled cores, verified DS/3DS devices, calibrated 3DS hardware variants, save export and richer library management. Local/SAF use works without Folder; manifests require provider access to their referenced siblings.

The main screen uses the embedded InfiniSelect View with Recents, Favorites and Open. Setup is in a hamburger menu; no persistent system chooser is shown. File selection and incoming View/Share intents share immediate launch and interoperable .iflink collections. See [browser behavior](browser.md).
