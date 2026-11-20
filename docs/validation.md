# Validation

Version 0.4.1 was built on Android ARM64/Termux with JDK 21, Gradle 9.7.1, AGP 8.9.2, Kotlin 2.2.20, SDK 36 and Clang 21. The root `./build.sh --max-workers=2 --console=plain` runs the independent build and tests. Desktop NDK construction is configured but has not been exercised here.

The 0.4.1 build adds a selectable N64 Bob shader pass after the existing GLideN64 field weave. It uses the current field parity to interpolate missing lines; device/game validation of the filter remains pending.

The Termux native probe explicitly loads the staged `libc++_shared.so` by absolute path before opening any cores. It does not require `LD_LIBRARY_PATH` or `LD_PRELOAD`; APK libraries retain no Termux-specific RPATH. To verify the shell-independent path, run `env -u LD_LIBRARY_PATH -u LD_PRELOAD ./build.sh --max-workers=2 --console=plain`.

Automated checks:

- 60 portable JUnit tests: console layouts, unique controls, button positions/markings, theme differences, bounded edits, invalid geometry, DS hinge scaling, 3DS screen-width ratio, touch rejection/mapping, physical-gamepad dead zones, overlapping input release, disconnect cleanup, firmware launch policy, safe M3U/CUE parsing and Genesis cartridge routing, including correction of older library entries; handheld routing/controls and launch gating across duplicate selections, chained work, firmware prompts, cancellation, errors and emulator return.
- 26 InfiniSelect JVM tests, including bounded interoperable .iflink parsing, identity deduplication, independent favorites, caller action IDs/labels and rejection of invalid action arrays, directory JSON bounds, caller namespace/path confinement and initial list/icon preferences. The library also builds/tests independently.
- ABI/initialization/extension checks for all twenty-one packaged cores. Generated original programs exercise NES, GB, SG-1000/Master System/Game Gear, Mega Drive through both Sega cores, PC Engine and Atari 2600; the pinned SNES core's original renderer-test generator exercises SNES. Each checks 120 video/audio frames and a save-state round trip. A `.gen` probe also forces the app's Mega Drive hardware option and checks the pinned core's actual machine identifier after running the 8-bit Sega profiles.
- FDS rejection without `disksys.rom`; a generated original boot stub and six-side blank FDS image test actual core callbacks for eject, select every side, insert, invalid side and inserted-side-change rejection, video/audio and serialization. This does not test retail games or real firmware compatibility.
- PCE CD: a generated boot stub plus three blank CUE/BIN discs loaded through M3U verify actual eject/select/insert callbacks, inserted-disc-change rejection, frames/audio and serialization. These are test stubs, not substitute console firmware or real-game compatibility checks.
- Dependency pins, Android assembly, native SONAME/runtime dependencies, 16 KiB page alignment, packaged ABI, absence of broad storage permissions in the APK manifest and transparent icon assets.

The APK signature verifies and the packaged offline notices include explicit Libretro acknowledgments and the API header license. The build succeeds with a D8 warning about rewriting Kotlin metadata for PlayActivity; no reflection-based use of that metadata is part of the app. Broader interactive validation remains pending; user-confirmed playback is recorded below.

The user confirmed device playback on 2026-11-19 for SG-1000, Master System, Game Gear, NES, Game Boy, Game Boy Color, SNES and PC Engine. The user also confirmed Genesis after its `.gen` routing correction. The user subsequently confirmed Atari 2600, Atari 5200, Atari 7800 and Atari Lynx after the Folder 0.1.13 container-verification fix. The user also confirmed Virtual Boy and Sega 32X. The user subsequently confirmed Game Boy Advance and Neo Geo Pocket. On 2026-11-20, the user confirmed Nintendo 64 and Nintendo DS. These reports establish playback for nineteen systems, not exhaustive game, save, input or lifecycle coverage. No Android device is connected to the development tools. The 0.2.3 Folder-owned embedded UI, relay paging and storage ownership, incoming View/Share grants, firmware-assisted immediate launch and both list/icon toggles need device acceptance. Still required on hardware: USB/Bluetooth hotplug and simultaneous fingers, portrait/landscape transitions during play, InfiniSelect cancellation/relaunch and provider sibling grants, audio lifecycle, SRAM persistence after background/restart, state slots, real FDS firmware/game playback and multi-CD changes with an imported compatible core. Automated parser and FDS adapter tests do not establish multi-CD game compatibility.

The [first batch of vintage systems](systems.md) has bundled cores. N64 and DS playback are confirmed; PlayStation, PSP and 3DS have bundled defaults awaiting device confirmation. gpSP and melonDS remain optional manual imports. Core libraries must be Android ARM64 Libretro/GLES-compatible; arbitrary core binaries are not guaranteed to work. DS/3DS rendering assumes the documented zero-gap top/bottom source layout. The DS gap defaults to 90 native pixels; the 3DS gap remains provisional and adjustable rather than an exact measured hardware value. Controller art is hardware-inspired, not manufacturer artwork. Unusual controller mappings may need future per-device remapping.

Libretro does not provide a universal game-wants-new-disk event, so the Eject/Open control is available whenever the core exposes removable media. Games show their own disk prompts. M3U/CUE references outside the selected directory, nested M3U and inaccessible siblings are rejected clearly. Compressed-ROM archives are not unpacked by the app; use a supported core/source provider or extract them first. Save export, arbitrary library deletion and a core downloader are not implemented in this initial build.


The 0.2.3 gateway change passed the standalone selector AAR/JVM build, compilation
of its Android device-test APK, the Play build, 42 portable core checks and all
13 native core probes/generated programs. The selector has 19 JVM tests;
Folder's filename/view code is app-owned again, and its existing regression tests
remain there. The new EmbeddedBrowserDeviceTest checks Folder-owned accessibility
nodes, actual breadcrumb navigation, selected-file grants and Folder state
restoration after surface re-creation. It has been compiled, not executed:
`adb devices -l` reports no attached device. Cross-app input/rendering, Favorites
menus, rotation, process-death recovery and fallback still need device acceptance.
The APK check verifies that Play requests no broad storage permissions. Install
both updated APKs for complete Folder embedding; see [browser behavior](browser.md).

The 0.2.4 callback transport fix builds against InfiniSelect 0.2.1. The selector
AAR and its 19 JVM tests pass; the Android instrumentation APK also compiles.
Two new Android parcel tests cover callback unmarshalling with a platform-only
class loader, original handler dispatch and payload delivery, including progress
callbacks. These Android tests have not been executed here because no device is
attached. The reported ClassNotFoundException identifies the callback subclass
in 0.2.3; cross-app browser acceptance on the updated APK still needs hardware.

The full 0.2.4 Play build passed 42 portable application tests, 19 selector JVM
tests and all thirteen native-core probes/generated-program checks. APK
verification passed ARM64/16 KiB/native-dependency and notices checks; its
signature verifies and its manifest declares no permissions.

The 0.2.5 browser update pins InfiniSelect 0.2.2 and requires Folder 0.1.9 for
locked collection roots and a flat embedded frame. Recents/Favorites use locked
roots; Open uses unrestricted Virtual root. The Android instrumentation suite
now injects physical taps instead of accessibility click actions, hosts the
browser away from screen origin, and covers locked-root Back, surface
restoration and switching back to Open. Its APK compiles; execution and visual
alignment confirmation remain pending on an attached device.

The full 0.2.5 Play build passed 42 portable application tests, 19 selector JVM
tests and all thirteen native-core checks. The signed APK verifies ARM64,
16 KiB native alignment, portable dependencies, notices and no permissions.

Version 0.2.6 uses InfiniSelect 0.2.3's general .iflink model, including
directory targets and arbitrary absolute URI schemes. A directly delivered
directory shortcut navigates through Folder's embedded gateway; game-specific
Favorites checks belong to Play. The selector's standalone AAR/JVM build passes
21 tests and its instrumentation APK compiles. Folder's target-routing tests
cover local/network/SAF directories, linked archives, app/custom URI handlers
and preservation of original game-link metadata. UI and actual URI-handler
acceptance still require device execution.

The full 0.2.6 Play build passed 42 portable application tests, 21 selector JVM
tests and all thirteen native-core checks. APK signature, ARM64/native
dependencies, 16 KiB alignment, notices and permission checks pass.

The 0.2.7 resource-URI fix pins Select 0.2.4. Five new regression tests reproduced
the strict Java URI failure for names with spaces before the fix; all 26 selector
JVM tests now pass. They cover the reported filename pattern, raw/encoded identity
round trips, existing-link deduplication and directory JSON, while preserving
relative-resource and temporary-relay rejection. Folder 0.1.12 uses Store 0.1.1
to encode the SFTP filename component at its source. Actual SFTP import still
needs confirmation on the user's device.

The full 0.2.7 Play build passed 42 application tests, 26 selector JVM tests and
all thirteen native-core checks. APK signature, ARM64/16 KiB/native-dependency,
notices and permission checks pass. Store 0.1.1 separately passes 124 JVM tests,
including SFTP encoding of spaces/exclamation marks and Unicode-preserving
filename round trips; its standalone AAR also builds.

Version 0.2.8 adds caller-defined **Open in emulator** to the embedded and
floating game pickers, and **Use as firmware** to firmware selection. It uses
existing selector action IDs/results and Folder's actual long-press menu. Game
ZIPs are accepted by the picker and staged intact; a compatible core is still
required. No browsing, core, protocol or storage-permission changes were needed.
The full standalone build passes 42 application tests, 26 selector JVM tests
and all thirteen native-core checks/generated programs. APK ARM64/16 KiB,
portable dependency, notices, permission and signature checks pass. The new
long-press paths still need touch confirmation on the user's device.

Version 0.3.0 bundles mGBA, Beetle NeoPop and Beetle WonderSwan, exposing five
new handheld profiles. Six new portable tests cover extension/header routing
(including Color `.ngp` files and persisted system correction), bundled-core
selection, firmware policy, physical controls and rotation-safe viewports.
The full build passes 48 portable application tests and 26 selector JVM tests.
All 16 cores pass native ABI/initialization checks. Original generated programs
for GBA, NGP/NGPC and WS/WSC each run 120 video/audio frames and a state round
trip without external BIOS. GBA's program performs an actual SRAM write.
NGP flash and GBA/WS save memory survive snapshot/reload/restore, and the source
cartridge bytes remain unchanged. The NGP adapter exposes no freed save buffer
after unload. WS probes read the actual emulated input port for every X/Y and
A/B/Start button and exercise both directions of the rotation toggle.

These checks establish native integration, not retail-game or device acceptance.
The user subsequently confirmed GBA and Neo Geo Pocket playback, now `[x]`; Pocket Color and WonderSwan/Color remain `[?]`.
USB gamepads, real saves after Android background/restart, display rotation and
custom control geometry still need interactive device coverage. The 0.3.0 APK
passes signature, ARM64/16 KiB/native-dependency, notices and permission checks;
its upstream submodules are unmodified and its direct source pins match.


Version 0.3.1 adds a 50% dark/light modal loading scrim above the embedded
Folder surface, a 64dp animated spinner and Opening/Preparing/Starting labels.
It stays active through import and emulator handoff; Session dismisses its
indicator after the first frame or on error. Seven new portable launch-state
tests cover repeat selections, chained workers, firmware prompts, cancellation
or failure, emulator return, directory links and task-count underflow.
The full build passes 55 application tests, 26 selector tests and all 16 native
core checks. Signature, ARM64/16 KiB/native dependencies, notices and permission
checks pass. Actual scrim visibility, touch blocking over Folder, dark/light
colors, large-image startup, firmware prompts and lifecycle transitions still
need device acceptance; no device is attached to the development tools.


Version 0.4.0 bundles the existing N64, PlayStation, PSP, DS and 3DS profiles.
Five new portable tests cover compiled defaults, ambiguous disc routing,
PS-X executable/header detection, CSO/3DSX routing, retained PlayStation firmware
gating, exact two-screen options/touch mapping and compatible renderer choices.
All 60 application and 26 selector JVM tests pass. Generated original programs
already verify PlayStation and DS video/audio, save-state round trips and source
preservation. A generated N64 program exercises the ARM64 recompiler, writes
RDRAM, advances through emulated frames, produces a GLES framebuffer and restores
a save state. A static image can legitimately reuse a framebuffer, so this probe
checks 120 emulation steps and the CPU-written memory rather than requiring a
new image on every step. GPU probes use an actual offscreen EGL/GLES 3.2 context;
they do not establish Activity/touch/controller/lifecycle compatibility.

PSP replacement fonts and core data are shipped in the APK and prepared privately
under the loading overlay. The source build uses pinned upstream CMake projects
for PSP/3DS, plus generated compatibility adapters; upstream source checkouts
remain unmodified by this work. Desktop NDK construction remains unexecuted here.
The five profiles remain `[?]` until user game/device confirmation. Still needed:
real selected PlayStation BIOS and CUE/BIN/CHD/M3U playback/disc changes, PSP
movies/audio/Memory Stick persistence, DS/3DS touch coordinates and screen spacing,
3DS decrypted-game playback and GLES compatibility, save persistence after app
background/restart, gamepads and display-orientation changes.


The completed 0.4.0 source build passes all 21 core integration checks. PSP's
original ELF waits for asynchronous boot, verifies a CPU-written RAM marker,
runs 120 GLES frames and restores a state. Citra's original 3DSX runs 120 GLES
frames and restores a state. Both preserve the original executable bytes.
Citra's upstream revision generator assumes `.git` is a directory: without a
pinned source-archive revision, its own saved-state header uses `UNKNOWN` and
cannot round-trip. The build now embeds the exact commit for Citra and PPSSPP;
the generated-program state checks pass with those final binaries. PSP's CMake
configuration also disables desktop X11 discovery, avoiding Termux-only dynamic
dependencies in the Android library. The final standalone build, APK signature,
ARM64 ABI, 16 KiB alignment, portable dependency, packaged PSP data/notices and
permission checks pass. All five new roadmap entries remain implemented but
untested for real games and Android UI/lifecycle acceptance.
