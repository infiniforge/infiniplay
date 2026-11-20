package com.infiniplay.core;
import static com.infiniplay.core.PadLayout.Kind.*;

import java.util.*;
/** Original controller markings and arrangements for the first-generation core bundle. */
final class VintageLayouts {
  private static PadLayout.Control c(String id, String label, PadLayout.Kind kind, int key,
      int color, double x, double y, double size) {
    return new PadLayout.Control(id, label, kind, key, color, x, y, size);
  }
  private static PadLayout.Control b(String label, int key, int color, double x, double y) {
    return c(label, label, BUTTON, key, color, x, y, .11);
  }
  private static PadLayout.Control pill(String label, int key, double x, double y) {
    return c(label, label, PILL, key, 0xff59616c, x, y, .115);
  }
  static List<PadLayout.Control> create(Console s, boolean p) {
    Set<Console> systems = EnumSet.of(Console.SG1000, Console.MASTERSYSTEM, Console.GAMEGEAR,
        Console.PCE, Console.PCECD, Console.SEGACD, Console.SEGA32X, Console.ATARI2600,
        Console.ATARI5200, Console.ATARI7800, Console.LYNX, Console.NEOGEO, Console.VIRTUALBOY,
        Console.NGP, Console.NGPC, Console.WONDERSWAN, Console.WONDERSWANCOLOR);
    if (!systems.contains(s))
      return null;
    List<PadLayout.Control> out = new ArrayList<>();
    double y = p ? .81 : .65;
    out.add(c("dpad", "D-pad", DPAD, 0, 0xff303238, .15, y, .25));
    switch (s) {
      case SG1000, MASTERSYSTEM, GAMEGEAR -> {
        int color = s == Console.GAMEGEAR ? 0xff414348 : 0xffbd3437;
        out.add(b("1", 97, color, .74, y));
        out.add(b("2", 96, color, .89, y));
        out.add(pill(s == Console.GAMEGEAR ? "START" : "PAUSE", 108, .57, p ? .94 : .90));
      }
      case PCE, PCECD -> {
        out.add(b("II", 97, 0xff982849, .74, y));
        out.add(b("I", 96, 0xff982849, .89, y));
        out.add(pill("SELECT", 109, .43, p ? .94 : .90));
        out.add(pill("RUN", 108, .57, p ? .94 : .90));
      }
      case SEGACD, SEGA32X -> {
        out.add(b("A", 100, 0xff383940, .67, y + .06));
        out.add(b("B", 97, 0xff383940, .80, y));
        out.add(b("C", 96, 0xff383940, .93, y - .06));
        if (s == Console.SEGA32X) {
          out.add(b("X", 102, 0xff777e87, .67, y - .10));
          out.add(b("Y", 99, 0xff777e87, .80, y - .16));
          out.add(b("Z", 103, 0xff777e87, .93, y - .22));
          out.add(pill("MODE", 109, .43, p ? .94 : .90));
        }
        out.add(pill("START", 108, .57, p ? .94 : .90));
      }
      case ATARI2600 -> {
        out.set(0, c("dpad", "Joystick", DPAD, 0, 0xff222222, .42, y, .27));
        out.add(b("FIRE", 97, 0xffc13e36, .20, y - .12));
        out.add(pill("SELECT", 109, .62, p ? .94 : .90));
        out.add(pill("RESET", 108, .79, p ? .94 : .90));
      }
      case ATARI7800 -> {
        out.set(0, c("dpad", "Joystick", DPAD, 0, 0xff202124, .26, y, .27));
        out.add(b("1", 97, 0xffc13e36, .10, y - .15));
        out.add(b("2", 96, 0xffc13e36, .42, y - .15));
        out.add(pill("SELECT", 109, .62, p ? .94 : .90));
        out.add(pill("PAUSE", 108, .77, p ? .94 : .90));
        out.add(pill("RESET", 99, .92, p ? .94 : .90));
      }
      case ATARI5200 -> {
        out.set(0, c("stick", "Analog", STICK, 1, 0xff222222, .23, p ? .78 : .61, .26));
        out.add(b("FIRE 1", 97, 0xffb13732, .09, p ? .68 : .40));
        out.add(b("FIRE 2", 96, 0xffb13732, .40, p ? .68 : .40));
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
        for (int i = 0; i < 12; i++) {
          int key = labels[i].equals("*") ? 42 : labels[i].equals("#") ? 35 : labels[i].charAt(0);
          out.add(c("key_" + labels[i], labels[i], BUTTON, 2000 + key, 0xffa0a1a4,
              .67 + .14 * (i % 3), (p ? .71 : .40) + (p ? .07 : .16) * (i / 3), .09));
        }
        out.add(pill("START", 2285, .67, p ? .63 : .23));
        out.add(pill("PAUSE", 2112, .81, p ? .63 : .23));
        out.add(pill("RESET", 2114, .95, p ? .63 : .23));
      }
      case LYNX -> {
        out.add(b("B", 97, 0xff5e6267, .75, y + .04));
        out.add(b("A", 96, 0xff5e6267, .89, y - .03));
        out.add(pill("OPTION 1", 102, .35, p ? .94 : .90));
        out.add(pill("PAUSE", 108, .53, p ? .94 : .90));
        out.add(pill("OPTION 2", 103, .71, p ? .94 : .90));
      }
      case NEOGEO -> {
        out.set(0, c("dpad", "Joystick", DPAD, 0, 0xff202023, .18, y, .26));
        out.add(b("A", 97, 0xffd14544, .61, y + .06));
        out.add(b("B", 96, 0xffe1cc43, .72, y - .02));
        out.add(b("C", 100, 0xff50ae6e, .83, y - .06));
        out.add(b("D", 99, 0xff4982d0, .94, y - .02));
        out.add(pill("SELECT", 109, .43, p ? .95 : .92));
        out.add(pill("START", 108, .57, p ? .95 : .92));
      }
      case NGP, NGPC -> {
        out.set(0, c("dpad", "Joystick", DPAD, 0, 0xff303235, .17, y, .25));
        out.add(b("B", 97, 0xff3e4247, .76, y + .025));
        out.add(b("A", 96, 0xff3e4247, .90, y - .025));
        out.add(pill("OPTION", 108, .58, p ? .94 : .90));
      }
      case WONDERSWAN, WONDERSWANCOLOR -> {
        out.clear();
        int color = s == Console.WONDERSWAN ? 0xff7e8588 : 0xff505d71;
        String[] groups = {"X", "Y"};
        int[][] keys = {{19, 22, 20, 21}, {105, 103, 104, 102}};
        double[][] offsets = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int group = 0; group < 2; group++) {
          double cy = group == 0 ? (p ? .86 : .73) : (p ? .65 : .31);
          for (int i = 0; i < 4; i++) {
            String label = groups[group] + (i + 1);
            out.add(c(label, label, BUTTON, keys[group][i], color,
                .15 + offsets[i][0] * .075, cy + offsets[i][1] * (p ? .046 : .12), .075));
          }
        }
        out.add(b("B", 97, color, .77, y + .035));
        out.add(b("A", 96, color, .91, y - .035));
        out.add(pill("START", 108, .57, p ? .94 : .90));
        out.add(pill("ROTATE", 109, .39, p ? .94 : .90));
      }
      case VIRTUALBOY -> {
        out.set(0, c("dpad", "Left D-pad", DPAD, 0, 0xff303034, .15, p ? .83 : .70, .23));
        out.add(c("right_dpad", "Right D-pad", DPAD, 2, 0xff303034, .85, p ? .87 : .75, .23));
        out.add(b("B", 97, 0xff68686b, .68, p ? .70 : .40));
        out.add(b("A", 96, 0xff68686b, .82, p ? .66 : .32));
        out.add(pill("L", 102, .14, p ? .66 : .12));
        out.add(pill("R", 103, .86, p ? .58 : .12));
        out.add(pill("SELECT", 109, .43, p ? .94 : .92));
        out.add(pill("START", 108, .57, p ? .94 : .92));
      }
      default -> throw new IllegalArgumentException("No vintage layout");
    }
    return List.copyOf(out);
  }
}
