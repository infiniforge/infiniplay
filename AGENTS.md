# InfiniPlay agent guidance

InfiniPlay is a planned Infiniforge universal emulator using the Libretro app as its backend. This directory is a project placeholder; do not describe emulator or core support as implemented until it exists.

## Independence and integration

- Establish a standalone Android application build in this repository. It must build from its own checkout with only its declared and pinned library dependencies and documented toolchain.
- Keep its Gradle build independent from every other application. Do not import sibling app source or depend on sibling build outputs.
- Integrate with the Libretro app backend through supported public Android interfaces. Handle a missing or incompatible backend clearly; do not assume its private source or build output is present.
- Use Android SAF and URI grants for game content and saves where supported. Infinifolder may improve file access when installed, but must remain optional.

## Project focus

- Define supported systems, cores, content, saves, and backend requirements accurately as implementation grows. “Universal” is the intended role, not a claim that every system or core is supported.
- Document the app's standalone build and keep library/backend requirements explicit.

## Ecosystem workflow for agents

1. Read `docs/ECOSYSTEM.md` first to understand the component map and ownership boundaries.
2. Before changing a public interface, read this repository's architecture and API documentation listed below and inspect its compatibility notes.
3. Check the owning shared library repository before implementing behavior that may already exist elsewhere. Do not conclude a capability is unavailable just because its implementation is outside the current repository.
4. Keep application responsibilities, shared library APIs, and other applications' private data/source separate.
5. Before a cross-repository change, check the exact dependency pin and the relevant interoperability contract. Dependency versions and protocol versions are separate; do not infer protocol incompatibility from a library-version mismatch.
6. Update the local documentation index and relevant API/behavior documentation when public behavior changes. `docs/ECOSYSTEM.md` is generated from the central manifest and must not be edited locally.

## Documentation index

- `README.md` — project purpose, prerequisites, and standalone build instructions.
- `AGENTS.md` — repository-specific development rules and this ecosystem workflow.
- `docs/README.md` — local documentation index.
- `docs/ECOSYSTEM.md` — generated ecosystem-wide inventory, boundaries, dependencies, versions, and protocols.