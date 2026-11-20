package com.infiniplay.core;
/** Controller/screen profiles are independent of core availability. */
public enum Console {
  ATARI2600("Atari 2600", 4.0 / 3, 0),
  ATARI5200("Atari 5200", 4.0 / 3, 0),
  ATARI7800("Atari 7800", 4.0 / 3, 0),
  LYNX("Atari Lynx", 160.0 / 102, 0),
  SG1000("Sega SG-1000", 4.0 / 3, 0),
  MASTERSYSTEM("Sega Master System", 4.0 / 3, 0),
  GAMEGEAR("Sega Game Gear", 160.0 / 144, 0),
  NES("NES", 4.0 / 3, 0),
  FDS("Famicom Disk System", 4.0 / 3, 0),
  SNES("Super Nintendo", 4.0 / 3, 0),
  GB("Game Boy", 160.0 / 144, 0),
  GBC("Game Boy Color", 160.0 / 144, 0),
  PCE("PC Engine / TurboGrafx-16", 4.0 / 3, 0),
  PCECD("PC Engine CD", 4.0 / 3, 0),
  MEGADRIVE("Mega Drive / Genesis", 4.0 / 3, 0),
  SEGACD("Mega-CD / Sega CD", 4.0 / 3, 0),
  SEGA32X("Sega 32X", 4.0 / 3, 0),
  NEOGEO("Neo Geo AES / MVS", 4.0 / 3, 0),
  VIRTUALBOY("Nintendo Virtual Boy", 12.0 / 7, 0),
  GBA("Game Boy Advance", 1.5, 0),
  NGP("Neo Geo Pocket", 160.0 / 152, 0),
  NGPC("Neo Geo Pocket Color", 160.0 / 152, 0),
  WONDERSWAN("WonderSwan", 224.0 / 144, 0),
  WONDERSWANCOLOR("WonderSwan Color", 224.0 / 144, 0),
  N64("Nintendo 64", 4.0 / 3, 0),
  PSX("PlayStation", 4.0 / 3, 0),
  PSP("PlayStation Portable", 480.0 / 272, 0),
  DS("Nintendo DS", 256.0 / 192, 64),
  THREEDS("Nintendo 3DS", 400.0 / 240, 90);
  public final String title;
  public final double aspect;
  public final double defaultGap;
  Console(String title, double aspect, double gap) {
    this.title = title;
    this.aspect = aspect;
    this.defaultGap = gap;
  }
  public boolean dual() {
    return this == DS || this == THREEDS;
  }
  public int topWidth() {
    return this == THREEDS ? 400 : 256;
  }
  public int screenHeight() {
    return this == THREEDS ? 240 : 192;
  }
  public int bottomWidth() {
    return this == THREEDS ? 320 : 256;
  }
}
