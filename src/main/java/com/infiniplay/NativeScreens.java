package com.infiniplay;
/** Call on the Libretro GL thread, after the host has loaded its native library. */
public final class NativeScreens {
  public static native void set(float[] rectangles);
  public static native void key(int port, int code, boolean pressed);
  public static native int[] media(int command, int index);
}
