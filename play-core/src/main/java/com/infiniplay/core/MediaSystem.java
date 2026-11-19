package com.infiniplay.core;

import java.util.Locale;

/** Unambiguous cartridge extensions override the import selection, including older library entries. */
public final class MediaSystem {
  private MediaSystem() {}

  public static Console fromFilename(String filename, Console selected) {
    String name = filename.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1);
    String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    return switch (extension) {
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
