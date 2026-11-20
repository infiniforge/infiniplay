# InfiniPlay

Standalone Android emulator frontend that runs Libretro cores inside the app. Version 0.4.2 targets ARM64 Android 8.0+ and OpenGL ES 3.0. RetroArch is not required.

- Twenty-one compiled-in cores cover [vintage systems and cartridge handhelds](docs/systems.md), including Atari 2600/5200/7800/Lynx, SG-1000/Master System/Game Gear, NES/FDS, GB/GBC, PC Engine/CD, Mega Drive/CD/32X, SNES, Neo Geo AES/MVS, Virtual Boy, GBA, Neo Geo Pocket/Color and WonderSwan/Color, plus N64, PlayStation, PSP, DS and 3DS.
- USB/Bluetooth Android HID gamepads and joysticks, up to four player assignments, touch D-pads, sticks and buttons.
- Portrait and landscape profiles with controller-specific colors and markings. Drag/pinch controls or edit size/position numerically; each console and orientation retains its own layout.
- Game displays use nearly the full view width in either orientation, preserve aspect ratio and align near the top of the play area. DS/3DS keep their stacked screen proportions, scaled hinge gap and bottom-screen touch mapping; DS defaults to an 80-pixel gap. A compact translucent pause/settings button overlays the play area, while touch controls remain customizable. DS bundles DeSmuME; 3DS bundles Citra and requires OpenGL ES 3.2 and decrypted content.
- Embedded InfiniSelect browsing, immediate file/View/Share launch, Recents and Favorites links. Missing required firmware interrupts launch and resumes the same game after selection.
- FDS and multi-CD games share on-screen hardware-themed Eject/Open, Insert and previous/next media controls. No disk menu. Import M3U sets, including their CUE/BIN companions.
- Per-game/core save memory and a manual save-state slot. N64 offers a per-game Bob deinterlacing toggle for interlaced titles. Source media remains unchanged.

Tap Open to browse Virtual root, or Recents/Favorites for their system folders. File selection launches immediately. A modal loading indicator blocks repeat selections while content is prepared and the emulator starts. Ambiguous formats ask for their system once. Long-press a file for Open in emulator or add/remove favorites. InfiniFolder writes links under `Internal storage/infini/play/{recents,favorites}/<system>/` through InfiniSelect; InfiniPlay requests no shared-storage permissions. Without Folder, links remain app-private. Setup is in the hamburger menu; the breadcrumb bar has a list/icon toggle. `.fds` automatically selects the FDS profile. FDS requires your own 8,192-byte `disksys.rom`; InfiniSelect opens when it is missing. For CD sets select the `.m3u` file. References must be relative and inside its directory. InfiniFolder can provide sibling access when a generic SAF provider only grants the selected manifest.

## Build independently

Install JDK 17+, Android SDK platform 36, Python 3, GNU Make, Clang and Git. Desktop builds also need NDK 27+ and CMake 3.22.1; specify your installed NDK with the Android Gradle toolchain configuration if necessary. The wrapper downloads Gradle/AGP/Kotlin and dependencies. Large PSP/3DS source builds need several GiB of free disk space; `PLAY_NATIVE_JOBS` controls compiler parallelism (default 2). No sibling application source or build artifacts are used.

```sh
git clone --recurse-submodules https://github.com/infiniforge/infiniplay.git
cd infiniplay
export ANDROID_HOME=/path/to/android-sdk
./build.sh --max-workers=2 --console=plain
```

Output: `InfiniPlay-debug.apk`. Termux builds use `clang`, `cmake`, `ninja`, `patchelf`, `aapt2`, Python 3 and the packaged C++ runtime; desktop builds use the NDK. Native libraries are aligned for 16 KiB pages. The first build needs network access. Submodule and lock-file commits must match. The build initializes the pinned core dependencies, omitting unused large-project frontend/test dependencies. PSP/3DS use isolated upstream CMake builds; their outputs are packaged through the same app build.

Regenerate the icon with `python3 scripts/build-app-icons.py` (requires `rsvg-convert`). It uses the suite's checked-in SVG outline/infinity-overlay method. `artwork/play.svg` has a transparent base background.

[Roadmap](docs/ROADMAP.md) · [Documentation](docs/README.md) · [Architecture](docs/architecture.md) · [Validation and limitations](docs/validation.md) · [Acknowledgments](docs/acknowledgments.md) · [Third-party notices](docs/third-party.md)

GPL-3.0-or-later; component licenses and source pins are included. No commercial games or proprietary console firmware are distributed.
