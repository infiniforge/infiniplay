package com.infiniplay.core;
import java.util.*;

public final class ScreenLayout {
  public record Rect(double left, double top, double right, double bottom) {
    public Rect {
      if (!Double.isFinite(left + top + right + bottom) || right <= left || bottom <= top)
        throw new IllegalArgumentException("Invalid screen rectangle");
    }
    public double width() {
      return right - left;
    }
    public double height() {
      return bottom - top;
    }
    public boolean contains(double x, double y) {
      return x >= left && x <= right && y >= top && y <= bottom;
    }
  }
  public record Screen(Rect source, Rect destination) {}
  public static List<Screen> fit(
      Console system, double width, double height, boolean portrait, double gap) {
    if (width <= 0 || height <= 0 || !Double.isFinite(width + height + gap) || gap < 0 || gap > 300)
      throw new IllegalArgumentException("Invalid screen layout");
    // Keep room below in portrait and beside the displays in landscape.
    Rect area = availableArea(width, height, portrait);
    if (!system.dual())
      return List.of(new Screen(new Rect(0, 0, 1, 1), fitRect(area, system.aspect)));
    double w = system.topWidth(), h = system.screenHeight(), total = 2 * h + gap;
    double scale = Math.min(area.width() / w, area.height() / total),
           x = (area.left + area.right - w * scale) / 2,
           y = (area.top + area.bottom - total * scale) / 2;
    Rect upper = new Rect(x, y, x + w * scale, y + h * scale);
    double bw = system.bottomWidth();
    Rect lower = new Rect((width - bw * scale) / 2, y + (h + gap) * scale, (width + bw * scale) / 2,
        y + total * scale);
    double sourceLeft = (w - bw) / (2 * w);
    return List.of(new Screen(new Rect(0, 0, 1, .5), upper),
        new Screen(new Rect(sourceLeft, .5, 1 - sourceLeft, 1), lower));
  }
  /** The host fits a core's dynamic aspect/rotation inside this controller-safe area. */
  public static Rect availableArea(double width, double height, boolean portrait) {
    if (!Double.isFinite(width + height) || width <= 0 || height <= 0)
      throw new IllegalArgumentException("Invalid screen area");
    return portrait ? new Rect(width * .04, height * .02, width * .96, height * .57)
                    : new Rect(width * .26, height * .03, width * .74, height * .84);
  }
  private static Rect fitRect(Rect area, double aspect) {
    double w = Math.min(area.width(), area.height() * aspect), h = w / aspect,
           x = (area.left + area.right - w) / 2, y = (area.top + area.bottom - h) / 2;
    return new Rect(x, y, x + w, y + h);
  }
  /**
   * Return source framebuffer coordinates, rejecting the bezel/gap instead of pressing the stylus.
   */
  public static double[] touch(List<Screen> screens, double x, double y) {
    Screen s = screens.get(screens.size() - 1);
    Rect d = s.destination;
    if (!d.contains(x, y))
      return new double[] {-10, -10};
    return new double[] {s.source.left + (x - d.left) / d.width() * s.source.width(),
        s.source.top + (y - d.top) / d.height() * s.source.height()};
  }
}
