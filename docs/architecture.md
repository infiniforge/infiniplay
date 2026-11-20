# Architecture

The root Android application `com.infiniplay` targets ARM64, SDK 35, minimum SDK 26. `play-core` is portable Java 17 for geometry, control mapping, firmware policies, media manifests and multi-source button state. `retro-host` compiles the Java/Kotlin public API of pinned LibretroDroid 0.13.2; CMake builds its native host, Oboe and sixteen emulator libraries from source.

`PlayActivity` hosts Folder's complete browser surface through the thin InfiniSelect gateway, routes View/Share through the same immediate-open flow, requests shared .iflink collections through the permission-owning Folder relay, stages through InfiniSelect, stages content and gates launch on core/firmware availability. `SessionActivity` runs non-exported in `:emulator`, keeping a native crash separate from the library UI. Its LifecycleRegistry owns the GLRetroView lifecycle. Native media, polled-keyboard, serialization and geometry calls execute on its GL thread. Android lifecycle pauses release controls and capture save memory. The pause/settings dialog pauses emulation; changing removable media leaves frames running so games observe the eject interval.

`PadView` draws hardware-inspired controls and handles multiple fingers, D-pad diagonals, analog sticks, stylus and drag/pinch editing. Positions are normalized inside the play view and size follows the shorter edge. `DigitalInput` merges independent touch/gamepad sources, avoiding premature key releases. Gamepads use Android's HID input path with dead zones, both common right-stick axis pairs, trigger axes, hotplug cleanup and persistent player assignments. Core-specific nonstandard gamepad remapping is not yet configurable.

`ScreenLayout` fits screen rectangles around controls. For DS/3DS, `play_screens.h` draws the two source rectangles during the host's final render pass. The generated `video.cpp` replaces one final draw call; pinned source is unchanged. DS cores must output top/bottom at zero gap. 3DS uses a 400×480 combined frame with a centered 320-pixel lower screen. Incompatible core layouts/hybrid arrangements need an adapter; importing a core alone does not guarantee compatibility. The hinge gap is app-owned, remains proportional across orientations and rejects touch in the bezel/upper screen. The native host is compiled with an explicit `<functional>` include for newer Clang headers.

Removable media uses standard Libretro callbacks on the emulation thread. The small FCEUmm adapter includes pinned implementation files in application-owned translation units and exposes the real FDS drive state as disk callbacks. This includes state restored from saves and disk changes from controller shortcuts. It never modifies the pinned submodule. Eject, select and insert are separate operations; side selection while inserted is rejected. The theme record supplies console-specific Open/Eject styling. Cores using the standard legacy disk interface work with the same UI; the host advertises disk interface version 0.

`LaunchProgress` tracks selection ownership, outstanding worker tasks and the emulator handoff on the main thread. Chained callbacks finish their current task only after scheduling follow-up work, keeping the browser blocked throughout. Selection ownership persists across firmware prompts, while the visual loading window only covers active work/handoff. `LoadingOverlay` uses a separate modal Android window so it also covers Folder's embedded topmost SurfaceView; Session dismisses its loader on the first rendered frame or a core error.

Fresh atomic JSON reads keep launcher and emulator-process settings consistent. Content import uses a single worker and staged copies; it cannot execute a half-imported game. UI errors retain a recoverable launch path. Additional cores must be compatible Android ARM64 ELF shared libraries; ELF screening checks architecture, not trust or core ABI correctness.

Source locks cover all direct checkouts, with upstream nested pins recorded by their parent gitlinks. Standard Android tooling resolves third-party Maven dependencies. Termux builds remove local runtime RPATHs and package their C++ runtime; NDK builds use static libc++. Both link 16 KiB-aligned native libraries. `scripts/check-pins.py` refuses mismatched direct dependencies.

The 5200 keypad uses a small generated `input.cpp` addition exposing polled Libretro keyboard state. Each keypad button maps to its real key code, rather than sharing the fire-button mapping. The 5200 controller subclass is selected explicitly. Extra-core source/define manifests are evaluated from pinned upstream Makefiles and compiled out of tree. PicoDrive uses portable interpreters with no dynamic recompiler; 32X performance needs device evaluation.

The PCE CD adapter registers the loaded CD set with the same standard removable-media callbacks used by FDS and Sega CD. It keeps eject and insertion separate so the emulated drive observes a real tray interval. Source adapters and build manifests remain in app-owned files.

mGBA is built from its pinned supported CMake Libretro target, with desktop
frontends, optional dependencies and GPU renderers disabled. NeoPop and
WonderSwan use the existing evaluated upstream Makefile source/define manifests.
All three retain the portable ARM64/16 KiB packaging checks. No upstream source
is edited. The new profiles reuse the same Folder/InfiniSelect import, collection,
gamepad, orientation, control-customization and session paths.

The app-owned `ngp_adapter.c` includes the pinned NeoPop translation unit and
adds the standard save-memory view over its writable cartridge flash. NeoPop's
normal unload-only `.ngf` path cannot supply a background SRAM snapshot by
itself. The adapter exposes the loaded cartridge buffer and length only while
loaded, allowing the existing GL-thread host save/restore path to persist it
in the game's private core-specific save directory. Source ROM bytes are never
replaced. WonderSwan's own save-memory API supplies cartridge SRAM.

WonderSwan's eight directional buttons map directly to the core's stable
landscape keymap; display rotation is independent. LibretroDroid handles the
core's rotation/geometry callbacks. Play gives it the available controller-safe
viewport so a rotated game is fitted using its current aspect ratio.
