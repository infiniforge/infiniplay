package com.infiniplay.core;
import java.util.*;

public final class PadLayout {
  public enum Kind { BUTTON, PILL, DPAD, STICK }
  /** Size is relative to the shorter display edge; position is normalized to the safe play area. */
  public record Control(
      String id, String label, Kind kind, int key, int color, double x, double y, double size) {
    public Control {
      if (id == null || label == null || kind == null || !Double.isFinite(x) || !Double.isFinite(y)
          || !Double.isFinite(size) || x < 0 || x > 1 || y < 0 || y > 1 || size < .035 || size > .6)
        throw new IllegalArgumentException("Invalid control");
    }
    public Control move(double x, double y) {
      return new Control(id, label, kind, key, color, clamp(x), clamp(y), size);
    }
    public Control resize(double size) {
      return new Control(id, label, kind, key, color, x, y, Math.max(.035, Math.min(.6, size)));
    }
    private static double clamp(double n) {
      if (!Double.isFinite(n))
        throw new IllegalArgumentException("Invalid position");
      return Math.max(0, Math.min(1, n));
    }
  }
  public static List<Control> defaults(Console system, boolean portrait) {
    List<Control> vintage = VintageLayouts.create(system, portrait);
    if (vintage != null)
      return vintage;
    List<Control> c = new ArrayList<>();
    double y = portrait ? .80 : .65;
    c.add(new Control("dpad", "D-pad", Kind.DPAD, 0, 0xff343c48, .15, y, .26));
    if (system == Console.THREEDS) {
      c.set(0,
          new Control("dpad", "D-pad", Kind.DPAD, 0, 0xff343c48, .15, portrait ? .89 : .77, .20));
    }
    if (EnumSet.of(Console.N64, Console.PSP, Console.THREEDS).contains(system))
      c.add(new Control("stick", "Analog", Kind.STICK, 1, 0xff8b939b,
          system == Console.N64 ? (portrait ? .38 : .25) : .15,
          system == Console.N64       ? (portrait ? .83 : .84)
              : system == Console.PSP ? (portrait ? .92 : .88)
                                      : (portrait ? .69 : .48),
          system == Console.PSP ? .18 : .23));
    if (system == Console.PSP)
      c.set(0,
          new Control("dpad", "D-pad", Kind.DPAD, 0, 0xff343c48, .15, portrait ? .74 : .60, .23));
    double ry = y;
    if (EnumSet.of(Console.NES, Console.FDS, Console.GB, Console.GBC, Console.GBA)
            .contains(system)) {
      int color = system == Console.GB ? 0xff99385e
          : system == Console.GBC      ? 0xff30323d
          : system == Console.GBA      ? 0xff6b6f7d
                                       : 0xffd44242;
      boolean horizontal = system == Console.NES || system == Console.FDS;
      c.add(b("B", 97, color, .75, ry + (horizontal ? 0 : .045)));
      c.add(b("A", 96, color, .88, ry - (horizontal ? 0 : .025)));
    } else if (system == Console.MEGADRIVE) {
      c.add(b("A", 100, 0xff373d46, .68, ry + .05));
      c.add(b("B", 97, 0xff373d46, .80, ry));
      c.add(b("C", 96, 0xff373d46, .92, ry - .05));
    } else if (system == Console.PSX || system == Console.PSP) {
      c.add(b("□", 100, 0xffd58cba, .71, ry));
      c.add(b("△", 99, 0xff77c4a2, .82, ry - .10));
      c.add(b("○", 96, 0xffe97f87, .93, ry));
      c.add(b("×", 97, 0xff8caee9, .82, ry + .10));
    } else if (system == Console.N64) {
      c.add(b("A", 97, 0xff3986df, .72, ry + .10));
      c.add(b("B", 100, 0xff55b16b, .65, ry));
      c.add(b("C↑", 1001, 0xffe4c84d, .86, ry - .12));
      c.add(b("C↓", 1002, 0xffe4c84d, .86, ry + .08));
      c.add(b("C←", 1003, 0xffe4c84d, .78, ry - .02));
      c.add(b("C→", 1004, 0xffe4c84d, .94, ry - .02));
    } else {
      boolean snes = system == Console.SNES;
      c.add(b("Y", 100, snes ? 0xff4fbd72 : 0xff555e68, .71, ry));
      c.add(b("X", 99, snes ? 0xff5485df : 0xff555e68, .82, ry - .10));
      c.add(b("A", 96, snes ? 0xffdf626b : 0xff555e68, .93, ry));
      c.add(b("B", 97, snes ? 0xffe5c65d : 0xff555e68, .82, ry + .10));
    }
    double shoulder = portrait ? .66 : .12;
    if (!EnumSet.of(Console.NES, Console.FDS, Console.GB, Console.GBC, Console.MEGADRIVE)
            .contains(system)) {
      c.add(new Control(
          "L", system == Console.PSX ? "L1" : "L", Kind.PILL, 102, 0xff596778, .12, shoulder, .15));
      c.add(new Control(
          "R", system == Console.PSX ? "R1" : "R", Kind.PILL, 103, 0xff596778, .88, shoulder, .15));
    }
    if (system == Console.PSX) {
      c.add(new Control("L2", "L2", Kind.PILL, 104, 0xff596778, .12, portrait ? .58 : .24, .15));
      c.add(new Control("R2", "R2", Kind.PILL, 105, 0xff596778, .88, portrait ? .58 : .24, .15));
    }
    if (system == Console.N64)
      c.add(new Control("Z", "Z", Kind.PILL, 104, 0xff697681, .15, portrait ? .61 : .31, .15));
    if (system != Console.N64)
      c.add(new Control(
          "select", "SELECT", Kind.PILL, 109, 0xff6c7581, .43, portrait ? .94 : .90, .115));
    c.add(
        new Control("start", "START", Kind.PILL, 108, 0xff6c7581, .57, portrait ? .94 : .90, .115));
    return List.copyOf(c);
  }
  private static Control b(String label, int key, int color, double x, double y) {
    return new Control(label, label, Kind.BUTTON, key, color, x, y, .11);
  }
  public static double deadZone(double value, double flat) {
    if (!Double.isFinite(value) || !Double.isFinite(flat))
      return 0;
    double dead = Math.max(.12, Math.min(.95, Math.abs(flat)));
    return Math.abs(value) <= dead
        ? 0
        : Math.copySign(Math.min(1, (Math.abs(value) - dead) / (1 - dead)), value);
  }
  /** Android's physical Xbox-style A is below B; the RetroPad uses Nintendo positions. */
  public static int physicalKey(int key) {
    return switch (key) {
      case 96 -> 97;
      case 97 -> 96;
      case 99 -> 100;
      case 100 -> 99;
      default -> key;
    };
  }
  /** Digital controllers must still respond to a USB joystick's analog axes. */
  public static Set<Integer> digitalAxes(double x, double y) {
    Set<Integer> keys = new HashSet<>();
    if (x < -.5)
      keys.add(21);
    if (x > .5)
      keys.add(22);
    if (y < -.5)
      keys.add(19);
    if (y > .5)
      keys.add(20);
    return Set.copyOf(keys);
  }
}
