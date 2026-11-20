package com.infiniplay.core;

import java.util.Locale;

/** Unambiguous cartridge extensions override the import selection, including older library entries. */
public final class MediaSystem {
  private MediaSystem() {}

  /** Null means ambiguous content: request a system once rather than guessing a core. */
  public static Console identify(String filename, byte[] header) {
    Console extension = fromFilename(filename, null);
    if (extension != null) return extension;
    if (header.length >= 4 && header[0]=='N' && header[1]=='E' && header[2]=='S' && header[3]==0x1a) return Console.NES;
    if (header.length >= 4 && header[0]=='F' && header[1]=='D' && header[2]=='S' && header[3]==0x1a) return Console.FDS;
    if (header.length >= 260 && header[256]=='S' && header[257]=='E' && header[258]=='G' && header[259]=='A') return Console.MEGADRIVE;
    return null;
  }

  public static Console fromFilename(String filename, Console selected) {
    String name = filename.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1);
    String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    return switch (extension) {
      case "gba" -> Console.GBA;
      case "nds" -> Console.DS;
      case "3ds", "cci", "cxi" -> Console.THREEDS;
      case "z64", "n64", "v64" -> Console.N64;
      case "fds" -> Console.FDS;
      case "nes" -> Console.NES;
      case "gb" -> Console.GB;
      case "gbc" -> Console.GBC;
      case "sg" -> Console.SG1000;
      case "sms" -> Console.MASTERSYSTEM;
      case "gg" -> Console.GAMEGEAR;
      case "md", "gen", "smd" -> Console.MEGADRIVE;
      case "32x" -> Console.SEGA32X;
      case "pce" -> Console.PCE;
      case "lnx", "lyx" -> Console.LYNX;
      case "a26" -> Console.ATARI2600;
      case "a52", "car" -> Console.ATARI5200;
      case "a78" -> Console.ATARI7800;
      case "vb", "vboy" -> Console.VIRTUALBOY;
      case "neo" -> Console.NEOGEO;
      case "sfc", "smc" -> Console.SNES;
      default -> selected;
    };
  }
}
