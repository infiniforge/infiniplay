# InfiniPlay agent guidance

InfiniPlay owns its Android library, controller UI, session lifecycle, imported content and saves. The user selected **in-process Libretro cores**; no separately installed emulator app is required. Keep this build independent of sibling applications.

1. Read `docs/ECOSYSTEM.md` and `docs/APPLICATION.md` before changing component boundaries.
2. Read `docs/architecture.md`, `docs/storage.md` and `docs/validation.md` before changing runtime, content or input behavior.
3. Use public pinned library APIs. Do not copy sibling app implementations or depend on sibling build output. InfiniFolder remains optional; InfiniSelect owns source selection/resolution.
4. Keep upstream submodules unmodified. Application adapters belong under `src/main/cpp`; document source patches generated into `build/`. Maintain exact gitlinks and lock files together.
5. Run `./build.sh` for changes to runtime/input/import behavior. On Termux this also executes the compiled cores with generated test content. Do not use commercial ROMs/BIOS files for fixtures.
6. Never infer runtime console support from a controller profile. Document bundled cores, import-only profiles, firmware requirements and device validation separately.
7. Controller buttons and Eject/Insert controls should use each console's hardware styling. Removable media uses on-screen controls; do not add a disk menu. Standard Libretro does not signal when a game asks for a new disk.
8. Preserve source media; writes belong in per-game/core app-owned save directories.
9. Update local behavior docs and index with changes. `docs/ECOSYSTEM.md` is generated from `infiniforge/.github`; update the central manifest/generator rather than editing this copy.

Documentation index: `README.md`, `docs/README.md`, `docs/APPLICATION.md`, `docs/architecture.md`, `docs/storage.md`, `docs/validation.md`, `docs/third-party.md`, `docs/acknowledgments.md`, `docs/ECOSYSTEM.md`, `docs/ROADMAP.md` and `docs/systems.md`.

The user-owned roadmap lives in `docs/ROADMAP.md`. Preserve its system list and three statuses: `[ ]` unimplemented, `[?]` implemented but untested, `[x]` tested. Record the test scope; do not turn synthetic-core checks into claims of device/game validation.

The home screen uses libinfiniselect EmbeddedFileBrowser. All game entry points share immediate launch and shared .iflink collections; see `docs/browser.md`. Do not restore a persistent system chooser or separate import-and-tap flow.
