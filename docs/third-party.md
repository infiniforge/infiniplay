# Third-party source and notices

InfiniPlay's application code and LibretroDroid-derived host are GPL-3.0-or-later; see [LICENSE](../LICENSE). Emulator plugins retain their own licenses. Source is distributed as pinned recursive Git submodules, with application build scripts and adapters in this repository. Clone recursively to obtain the corresponding source. No proprietary console firmware or commercial game content is included.

| Component | Source | License / notice location |
| --- | --- | --- |
| Libretro API, The RetroArch team and Libretro contributors | [Libretro](https://www.libretro.com/) / [libretro-common](https://github.com/libretro/libretro-common) | MIT notice in the pinned `libretro.h`, included in offline app notices |
| LibretroDroid 0.13.2, Filippo Scognamiglio and contributors | [Swordfish90/LibretroDroid](https://github.com/Swordfish90/LibretroDroid) | `third_party/libretrodroid/LICENSE`, GPL-3.0-or-later |
| QuickNES Libretro core, Blargg and contributors | [libretro/QuickNES_Core](https://github.com/libretro/QuickNES_Core) | `third_party/quicknes/LICENSE`, GPL-2.0 core wrapper; engine files carry LGPL-2.1-or-later notices |
| Gambatte, Sindre Aamås and contributors | [libretro/gambatte-libretro](https://github.com/libretro/gambatte-libretro) | `third_party/gambatte/COPYING`, GPL-2.0; retain per-file notices |
| FCEUmm, Xodnizel, CaH4e3 and contributors | [libretro/libretro-fceumm](https://github.com/libretro/libretro-fceumm) | `third_party/fceumm/Copying`, GPL-2.0-or-later; retain per-file notices |
| Genesis Plus GX, Eke-Eke / Charles MacDonald and contributors | [libretro/Genesis-Plus-GX](https://github.com/libretro/Genesis-Plus-GX) | `third_party/genesis/LICENSE.txt`, non-commercial license and embedded component notices |
| PicoDrive and contributors | [libretro/picodrive](https://github.com/libretro/picodrive) | `third_party/picodrive/COPYING` and component-specific notices |
| Beetle PCE Fast / Beetle VB, Mednafen and contributors | Pinned `third_party/pce`, `third_party/vb` | Respective `COPYING`, GPL-2.0 and per-file notices |
| Handy, K. Wilkins and contributors | [libretro/libretro-handy](https://github.com/libretro/libretro-handy) | `third_party/handy/lynx/license.txt`, zlib-style notice |
| Stella / Atari800 / ProSystem and contributors | Pinned `third_party/stella`, `third_party/atari800`, `third_party/prosystem` | Respective license documents and per-file notices; Altirra replacement OS is included by Atari800 |
| Snes9x 2010 and contributors | [libretro/snes9x2010](https://github.com/libretro/snes9x2010) | `third_party/snes9x/LICENSE.txt`, Snes9x non-commercial terms |
| Geolith, Rupert Carmichael and contributors | [libretro/geolith-libretro](https://github.com/libretro/geolith-libretro) | `third_party/geolith/LICENSE`, BSD-3-Clause |
| Oboe, Google and contributors | LibretroDroid's pinned Oboe submodule | `third_party/libretrodroid/libretrodroid/src/main/cpp/oboe/LICENSE`, Apache-2.0 |
| libretro-common contributors | Pinned source inside respective upstream checkouts | Per-file notices in each `libretro-common` tree |
| libinfiniselect / libinfinistream, InfiniForge contributors | `libraries/` pins in [dependencies.lock.json](../dependencies.lock.json) | Respective library `LICENSE` files |
| LLVM libc++ (Termux packaging only) | [LLVM llvm-project/libcxx](https://github.com/llvm/llvm-project/tree/main/libcxx) | Apache-2.0 with LLVM exceptions and retained legacy MIT notices, included in app notices |
| AndroidX, Kotlin and coroutine libraries | Google Maven / Maven Central declarations in Gradle files | Apache-2.0 and respective dependency notices |

[third-party.lock.json](../third-party.lock.json) records exact direct native source commits. Each upstream gitlink pins its nested source. App-owned FCEUmm and PCE CD adapters expose standard disk callbacks without modifying those upstream checkouts. `scripts/prepare-native.py` creates the documented final-pass compositor patch in the ignored build tree. The standard suite SVG icon compositor is checked in; the gamepad base artwork is original and transparent. Infinity artwork follows the existing InfiniForge asset method.

See [acknowledgments](acknowledgments.md) for project credits. Application Settings → About / acknowledgments includes full bundled license notices and source locations. Runtime-imported cores are user-selected and are not included in this distribution.
