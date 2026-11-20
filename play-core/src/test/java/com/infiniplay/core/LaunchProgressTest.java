package com.infiniplay.core;

import static org.junit.Assert.*;
import org.junit.Test;

public class LaunchProgressTest {
  @Test public void repeatedSelectionsCannotStartAnotherLaunch() {
    var progress = new LaunchProgress();
    assertTrue(progress.beginSelection());
    progress.beginWork();
    assertTrue(progress.loading());
    assertFalse(progress.beginSelection());
    progress.finishWork();
    assertFalse(progress.beginSelection());
  }

  @Test public void chainedResolutionAndImportNeverUnlockTheBrowser() {
    var progress = new LaunchProgress();
    progress.beginSelection();
    progress.beginWork();
    progress.beginWork(); // Resolution callback queues import before resolution ends.
    progress.finishWork();
    assertTrue(progress.loading());
    assertFalse(progress.beginSelection());
    progress.handOff();
    progress.finishWork();
    assertTrue(progress.loading());
    assertTrue(progress.handingOff());
    assertFalse(progress.beginSelection());
  }

  @Test public void firmwarePromptKeepsSelectionBlockedWithoutCoveringThePrompt() {
    var progress = new LaunchProgress();
    progress.beginSelection();
    progress.beginWork();
    progress.finishWork();
    assertFalse(progress.loading());
    assertFalse(progress.beginSelection());
    progress.beginWork(); // Firmware picker result.
    assertTrue(progress.loading());
    progress.handOff();
    progress.finishWork();
    assertTrue(progress.loading());
  }

  @Test public void cancellationAndFailureAllowAnotherSelection() {
    var progress = new LaunchProgress();
    progress.beginSelection();
    progress.beginWork();
    progress.release();
    assertFalse(progress.beginSelection()); // In-flight work still owns the gate.
    progress.finishWork();
    assertFalse(progress.loading());
    assertTrue(progress.beginSelection());
  }

  @Test public void returningFromTheEmulatorReleasesTheHandoff() {
    var progress = new LaunchProgress();
    progress.beginSelection();
    progress.handOff();
    assertFalse(progress.beginSelection());
    progress.release();
    assertFalse(progress.handingOff());
    assertFalse(progress.loading());
    assertTrue(progress.beginSelection());
  }

  @Test public void directoryLinksReleaseAfterNavigationCompletes() {
    var progress = new LaunchProgress();
    progress.beginSelection();
    progress.beginWork();
    progress.release();
    assertTrue(progress.loading());
    progress.finishWork();
    assertFalse(progress.blocked());
    assertTrue(progress.beginSelection());
  }

  @Test(expected = IllegalStateException.class)
  public void finishingUnstartedWorkCannotSilentlyBreakTheGate() {
    new LaunchProgress().finishWork();
  }
}
