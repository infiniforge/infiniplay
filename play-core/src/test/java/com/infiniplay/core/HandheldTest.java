package com.infiniplay.core;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class HandheldTest {
  @Test public void cartridgeExtensionsOverrideOldLibrarySystems() {
    Map<String, Console> files = Map.of("game.gba", Console.GBA, "game.AGB", Console.GBA,
        "game.ngp", Console.NGP, "game.ngc", Console.NGPC, "game.NGPC", Console.NGPC,
        "game.ws", Console.WONDERSWAN, "game.WSC", Console.WONDERSWANCOLOR);
    files.forEach((name, expected) -> {
      assertEquals(expected, MediaSystem.identify("folder/" + name, new byte[0]));
      assertEquals(expected, MediaSystem.fromFilename(name, Console.NES));
    });
    assertNull(MediaSystem.identify("unknown.bin", new byte[0]));
    assertEquals(Console.GBC, MediaSystem.fromFilename("original.gbc", Console.GBA));
  }

  @Test public void pocketColorHeaderSurvivesImportAndLibraryReloadWithNgpSuffix() {
    byte[] header = new byte[0x40];
    header[0x23] = 0x10;
    assertEquals(Console.NGPC, MediaSystem.identify("color.ngp", header));
    assertEquals(Console.NGPC, MediaSystem.fromFilename("color.ngp", Console.NGPC));
    header[0x23] = 0;
    assertEquals(Console.NGP, MediaSystem.identify("mono.ngp", header));
  }

  @Test public void everyHandheldHasABundledCoreWithoutRequiredFirmware() {
    for (Console system : List.of(Console.GBA, Console.NGP, Console.NGPC,
             Console.WONDERSWAN, Console.WONDERSWANCOLOR)) {
      var core = CoreCatalog.ALL.stream().filter(c -> c.system() == system).findFirst().orElseThrow();
      assertTrue(system.title, core.bundled());
      assertTrue(Firmware.requirements(core.id()).isEmpty());
    }
    assertEquals("libmgba.so", CoreCatalog.get("mgba").file());
    assertFalse(CoreCatalog.get("gpsp").bundled());
    assertEquals("libngp.so", CoreCatalog.get("ngpc").file());
    assertEquals("libwswan.so", CoreCatalog.get("wswan_color").file());
  }

  @Test public void pocketKeepsTwoButtonsJoystickAndOption() {
    for (Console s : List.of(Console.NGP, Console.NGPC)) {
      for (boolean portrait : List.of(true, false)) {
        var controls = PadLayout.defaults(s, portrait);
        assertEquals(Set.of("Joystick", "A", "B", "OPTION"),
            new HashSet<>(controls.stream().map(PadLayout.Control::label).toList()));
        assertEquals(Set.of(96, 97, 108), new HashSet<>(controls.stream()
            .filter(c -> c.kind() != PadLayout.Kind.DPAD).map(PadLayout.Control::key).toList()));
      }
    }
    assertNotEquals(ControllerTheme.of(Console.NGP).shell(), ControllerTheme.of(Console.NGPC).shell());
  }

  @Test public void wonderSwanExposesBothPhysicalDirectionalGroups() {
    Map<String, Integer> expected = Map.of("X1", 19, "X2", 22, "X3", 20, "X4", 21,
        "Y1", 105, "Y2", 103, "Y3", 104, "Y4", 102);
    for (Console s : List.of(Console.WONDERSWAN, Console.WONDERSWANCOLOR)) {
      for (boolean portrait : List.of(true, false)) {
        var controls = PadLayout.defaults(s, portrait);
        expected.forEach((label, key) -> assertTrue(label,
            controls.stream().anyMatch(c -> c.label().equals(label) && c.key() == key)));
        assertTrue(controls.stream().anyMatch(c -> c.label().equals("ROTATE") && c.key() == 109));
        assertTrue(controls.stream().anyMatch(c -> c.label().equals("START") && c.key() == 108));
        var x1 = controls.stream().filter(c -> c.id().equals("X1")).findFirst().orElseThrow();
        var x3 = controls.stream().filter(c -> c.id().equals("X3")).findFirst().orElseThrow();
        var y1 = controls.stream().filter(c -> c.id().equals("Y1")).findFirst().orElseThrow();
        assertTrue(x1.y() < x3.y());
        assertTrue(y1.y() < x1.y());
      }
    }
  }

  @Test public void rotatingTheScreenKeepsPhysicalButtonLabelsStable() {
    for (String id : List.of("wswan", "wswan_color")) {
      var options = CoreSettings.defaults(id);
      assertEquals("manual", options.get("wswan_rotate_display"));
      assertEquals("disabled", options.get("wswan_rotate_keymap"));
    }
    for (boolean portrait : List.of(true, false)) {
      double width = portrait ? 1080 : 1920, height = portrait ? 1920 : 1080;
      var area = ScreenLayout.availableArea(width, height, portrait);
      assertTrue(area.left() >= 0 && area.right() <= width);
      assertTrue(area.top() >= 0 && area.bottom() <= height);
      var screen = ScreenLayout.fit(Console.WONDERSWAN, width, height, portrait, 0)
                       .get(0).destination();
      assertTrue(area.width() >= screen.width());
      assertTrue(area.height() >= screen.height());
    }
  }
}
