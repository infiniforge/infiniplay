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
