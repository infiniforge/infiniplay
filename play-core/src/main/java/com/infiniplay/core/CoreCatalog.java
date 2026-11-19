package com.infiniplay.core;
import java.util.*;
public final class CoreCatalog {
  public record Core(String id, String title, Console system, boolean bundled, String file) {}
  public static final List<Core> ALL =
      List.of(built("stella", "Stella 2014", Console.ATARI2600, "stella"),
          built("atari5200", "Atari800 · 5200", Console.ATARI5200, "atari800"),
          built("prosystem", "ProSystem", Console.ATARI7800, "prosystem"),
          built("handy", "Handy", Console.LYNX, "handy"),
          built("sg1000", "Genesis Plus GX · SG-1000", Console.SG1000, "genesis"),
          built("sms", "Genesis Plus GX · Master System", Console.MASTERSYSTEM, "genesis"),
          built("gg", "Genesis Plus GX · Game Gear", Console.GAMEGEAR, "genesis"),
          built("fceumm", "FCEUmm", Console.NES, "fceumm"),
          built("quicknes", "QuickNES", Console.NES, "quicknes"),
          built("fceumm_fds", "FCEUmm · FDS", Console.FDS, "fceumm"),
          built("gambatte", "Gambatte", Console.GB, "gambatte"),
          built("gambatte_color", "Gambatte · Color", Console.GBC, "gambatte"),
          built("snes9x2010", "Snes9x 2010", Console.SNES, "snes9x"),
          built("pce", "Beetle PCE Fast", Console.PCE, "pce"),
          built("pcecd", "Beetle PCE Fast · CD", Console.PCECD, "pce"),
          built("genesis_plus_gx", "Genesis Plus GX", Console.MEGADRIVE, "genesis"),
          built("segacd_us", "Genesis Plus GX · CD USA", Console.SEGACD, "genesis"),
          built("segacd_eu", "Genesis Plus GX · CD Europe", Console.SEGACD, "genesis"),
          built("segacd_jp", "Genesis Plus GX · CD Japan", Console.SEGACD, "genesis"),
          built("picodrive_32x", "PicoDrive · 32X", Console.SEGA32X, "picodrive"),
          built("geolith_aes", "Geolith · AES", Console.NEOGEO, "geolith"),
          built("geolith_mvs", "Geolith · MVS", Console.NEOGEO, "geolith"),
          built("virtualboy", "Beetle VB", Console.VIRTUALBOY, "vb"),
          new Core("mgba", "mGBA", Console.GBA, false, ""),
          new Core("gpsp", "gpSP", Console.GBA, false, ""),
          new Core("mupen64plus_next", "Mupen64Plus-Next", Console.N64, false, ""),
          new Core("pcsx_rearmed", "PCSX-ReARMed", Console.PSX, false, ""),
          new Core("ppsspp", "PPSSPP", Console.PSP, false, ""),
          new Core("desmume", "DeSmuME", Console.DS, false, ""),
          new Core("melonds", "melonDS (external firmware)", Console.DS, false, ""),
          new Core("citra", "Citra-compatible", Console.THREEDS, false, ""));
  private static Core built(String id, String title, Console system, String library) {
    return new Core(id, title, system, true, "lib" + library + ".so");
  }
  public static Core get(String id) {
    return ALL.stream()
        .filter(c -> c.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown core"));
  }
}
