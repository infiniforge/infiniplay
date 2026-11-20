package com.infiniplay.core;

/** Main-thread launch gate. User prompts block new selections without showing a busy spinner. */
public final class LaunchProgress {
  private int tasks;
  private boolean selected;
  private boolean handingOff;

  public boolean beginSelection() {
    if (blocked()) return false;
    selected = true;
    return true;
  }

  public void beginWork() { tasks++; }

  public void finishWork() {
    if (tasks == 0) throw new IllegalStateException("No loading task to finish");
    tasks--;
  }

  public void handOff() {
    selected = true;
    handingOff = true;
  }

  public void release() {
    selected = false;
    handingOff = false;
  }

  public boolean blocked() { return selected || tasks > 0; }
  public boolean loading() { return tasks > 0 || handingOff; }
  public boolean handingOff() { return handingOff; }
}
