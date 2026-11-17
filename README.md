# InfiniPlay
InfiniPlay is a planned emulator and legacy-software frontend using supported external engines. This
repository is currently a placeholder; see the [application scope and implementation
status](docs/APPLICATION.md).

**Status: planned placeholder.** This checkout currently contains project guidance
only. It has no Android application source, Gradle build, app-level `build.sh`, or
APK, so it cannot be built or installed yet.

The planned app is a universal-emulator frontend using the Libretro app as its
backend. The supported Android contract and required backend capabilities remain
to be defined; the app must handle a missing or incompatible Libretro app. Android
SAF and URI grants are the baseline for game content and saves. Infinifolder may
provide optional storage, but it must not be required to launch or build the app.

No emulator frontend, core support, or backend integration is implemented in this
placeholder.
