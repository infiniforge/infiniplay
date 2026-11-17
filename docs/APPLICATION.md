# InfiniPlay application

## Purpose

InfiniPlay is the planned game and legacy-software launcher/emulation frontend. It should provide a unified library and execution experience while integrating existing emulator engines, including a Libretro/RetroArch-compatible backend where its public Android contract permits. It owns the frontend and session experience, not filesystem drivers or emulation cores.

## Primary use cases

- Browse and organize supported games/software by platform and launch them in a compatible available engine.
- Discover/select emulator cores and show BIOS/firmware requirements where the backend exposes that information.
- Configure controllers and manage game-specific launch options.
- Preserve save states and persistent game data.
- Launch content from cartridges, disks, multidisk sets, or other supported media; stage/cache content when random access or performance requires it.
- Open games stored locally, through SAF, through supported remote/container resource APIs, or via Android intent handoff.
- Run supported retro console, classic computer, DOS, or other legacy environments as backend support becomes available.

## Current implementation status

This repository is a planned placeholder. It contains no Android application source, Gradle build, app build script, emulator frontend, backend protocol, game library, save manager, or declared library dependency. No platform/core support can be claimed from the repository's current state.

## Intended responsibilities and shared libraries

InfiniPlay should own game catalog presentation, system/core selection, launch/session lifecycle, controller configuration, app-specific saves, and safe write overlays. It should integrate external emulator engines through their supported public interfaces rather than copying emulator source or relying on sibling build artifacts.

The application should not implement network storage, archive readers, or filesystem-image drivers. Android SAF and URI grants are the baseline; optional resource resolution may use libinfiniselect and provider-neutral resource contracts from libinfinicore/libinfinistream if their APIs fit. Metadata detection could use libinfinimeta. These are candidates only; this placeholder declares no dependencies.

Original game media should not be modified by implicit save writes. Use explicit copy-on-write overlays or app-owned save data, keep source identity, and explain export/commit actions to the user.

## Planned and exploratory functionality

Core discovery/selection, BIOS and firmware checking, multidisc management, controller profiles, per-game settings, save states, game-library organization, and local caching are potential features. Supported environments will depend on actual backend/core availability. “Universal” names the direction and is not a claim of universal hardware or software compatibility.

## Interoperability

The [generated ecosystem map](ECOSYSTEM.md) provides the wider application/library map. Infinifolder may optionally resolve remote, archive, and filesystem-image resources. A Libretro-compatible app or another engine may provide execution. The precise intents, URI grants, launch request/response data, version negotiation, and error behavior must be designed against that backend's public interface. No such contract is implemented or documented in this placeholder. Infiniplay should continue to work with local/SAF content when Folder is absent and handle missing or incompatible backends gracefully.

## Documentation audit

- Consistent: repository guidance describes a planned frontend and requires a separately installed public backend contract.
- Incomplete: no emulator backend request/response, core discovery, game catalog, save format, or cache policy exists.
- Unverified: no platform, core, BIOS, controller, or game support can be claimed from this placeholder.

## Limitations and outstanding work

- No app code, build, emulator backend integration, core support, compatibility protocol, or save implementation exists.
- The Libretro app integration API, supported core discovery and licensing/distribution requirements remain unverified.
- Random-access sources may require bounded local caching; no policy or cache implementation exists.
- Safe write-overlay and persistent-save semantics must be designed before exposing media modification.
- Do not claim any console, computer, BIOS, controller, or disk-image support until the selected backend and application path are verified.
