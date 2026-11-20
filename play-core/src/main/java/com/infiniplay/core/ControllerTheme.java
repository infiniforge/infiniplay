package com.infiniplay.core;
/**
 * Hardware-inspired controller shells and removable-media controls, drawn as scalable Android
 * shapes.
 */
public record ControllerTheme(
    int shell, int cap, int ink, int accent, String mediaLabel, boolean roundMedia) {
  public static ControllerTheme of(Console system) {
    return switch (system) {
      case NES ->
        new ControllerTheme(0xffb2b4af, 0xff323536, 0xfff4f0e7, 0xffbc3436, "⏏ EJECT", false);
      case FDS ->
        new ControllerTheme(0xffc6ad69, 0xffa02e35, 0xfff8e5ac, 0xffa02e35, "⏏ EJECT", false);
      case GBC ->
        new ControllerTheme(0xff64568f, 0xff30323d, 0xffeeeeee, 0xff64568f, "⏏ EJECT", false);
      case GB ->
        new ControllerTheme(0xffc6c7b9, 0xff8d3158, 0xfff7e9ef, 0xff8d3158, "⏏ EJECT", false);
      case SNES ->
        new ControllerTheme(0xffb8bac5, 0xff6a6399, 0xffffffff, 0xff7666a4, "⏏ EJECT", false);
      case MEGADRIVE, SEGACD, SEGA32X, MASTERSYSTEM, SG1000, GAMEGEAR ->
        new ControllerTheme(0xff30313a, 0xff40434a, 0xffeeeeee, 0xffb74145, "⏏ OPEN", false);
      case PSX ->
        new ControllerTheme(0xffb3b6bf, 0xff858994, 0xff15262d, 0xff708a9a, "⏏ OPEN", true);
      case PSP ->
        new ControllerTheme(0xff292d35, 0xff555d69, 0xffeeeeee, 0xff719bab, "⏏ OPEN", false);
      case PCE, PCECD ->
        new ControllerTheme(0xffd7d4ca, 0xff9d274f, 0xffffffff, 0xff9d274f, "⏏ OPEN", false);
      case ATARI2600, ATARI5200, ATARI7800 ->
        new ControllerTheme(0xff202125, 0xffb83a2f, 0xffeeeeee, 0xffb87d44, "⏏ EJECT", false);
      case LYNX ->
        new ControllerTheme(0xff5b6062, 0xff353b3e, 0xffeeeeee, 0xffd5bd48, "⏏ EJECT", false);
      case NEOGEO ->
        new ControllerTheme(0xff22252a, 0xff51545b, 0xffeeeeee, 0xffe6bd4d, "⏏ OPEN", false);
      case VIRTUALBOY ->
        new ControllerTheme(0xffa33235, 0xff303034, 0xffefeeee, 0xffc23136, "⏏ EJECT", false);
      case NGP ->
        new ControllerTheme(0xffd0d0ce, 0xff343638, 0xff202124, 0xff69737a, "⏏ EJECT", false);
      case NGPC ->
        new ControllerTheme(0xff365c85, 0xff343638, 0xffeeeeee, 0xffdab94e, "⏏ EJECT", false);
      case WONDERSWAN ->
        new ControllerTheme(0xffd2d3cf, 0xff7e8588, 0xff202124, 0xff737e8a, "⏏ EJECT", false);
      case WONDERSWANCOLOR ->
        new ControllerTheme(0xff3b68a1, 0xff505d71, 0xffeeeeee, 0xff82b7d5, "⏏ EJECT", false);
      case N64 ->
        new ControllerTheme(0xff9da4b1, 0xff697381, 0xffffffff, 0xffd63b40, "⏏ EJECT", false);
      case GBA ->
        new ControllerTheme(0xff6760a6, 0xff6b6f7d, 0xffffffff, 0xff6760a6, "⏏ EJECT", false);
      case DS, THREEDS ->
        new ControllerTheme(0xffb8bfc6, 0xff565d64, 0xffffffff, 0xff4b859c, "⏏ EJECT", false);
    };
  }
}
