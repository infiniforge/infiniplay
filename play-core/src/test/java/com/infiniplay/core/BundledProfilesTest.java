package com.infiniplay.core;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class BundledProfilesTest {
  @Test public void existingLaterProfilesHaveCompiledDefaults() {
    Map<String, Console> expected = Map.of("mupen64plus_next", Console.N64,
        "pcsx_rearmed", Console.PSX, "ppsspp", Console.PSP,
        "desmume", Console.DS, "citra", Console.THREEDS);
    expected.forEach((id, system) -> {
      var core = CoreCatalog.get(id);
      assertTrue(id, core.bundled());
      assertEquals(system, core.system());
      assertTrue(core.file().endsWith(".so"));
    });
    assertFalse(CoreCatalog.get("melonds").bundled());
  }

  @Test public void discFormatsRemainAmbiguousInsteadOfGuessingPlayStationOrPsp() {
    for (String name : List.of("disc.iso", "disc.chd", "disc.cue", "game.pbp", "game.elf")) {
      assertNull(MediaSystem.identify(name, new byte[0]));
      assertEquals(Console.PSP, MediaSystem.fromFilename(name, Console.PSP));
      assertEquals(Console.PSX, MediaSystem.fromFilename(name, Console.PSX));
    }
    assertEquals(Console.PSP, MediaSystem.identify("game.CSO", new byte[0]));
    assertEquals(Console.THREEDS, MediaSystem.identify("homebrew.3dsx", new byte[0]));
    assertEquals(Console.PSX, MediaSystem.identify("program.exe", "PS-X EXE".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
  }

  @Test public void playStationKeepsItsExistingFirmwareLaunchGate() {
    var bios = Firmware.requirements("pcsx_rearmed");
    assertEquals(1, bios.size());
    assertEquals("scph5501.bin", bios.get(0).name());
    bios.get(0).validate(524288);
    for (String id : List.of("mupen64plus_next", "ppsspp", "desmume", "citra"))
      assertTrue(id, Firmware.requirements(id).isEmpty());
  }

  @Test public void dualScreenCoreOptionsMatchTheirActualFrameLayoutsAndTouchInputs() {
    var ds = CoreSettings.defaults("desmume");
    assertEquals("top/bottom", ds.get("desmume_screens_layout"));
    assertEquals("0", ds.get("desmume_screens_gap"));
    assertEquals("256x192", ds.get("desmume_internal_resolution"));
    assertEquals("touch", ds.get("desmume_pointer_type"));
    var three = CoreSettings.defaults("citra");
    assertEquals("Default Top-Bottom Screen", three.get("citra_layout_option"));
    assertEquals("1x (Native)", three.get("citra_resolution_factor"));
    assertEquals("enabled", three.get("citra_touch_touchscreen"));
    assertEquals("LibRetro Default", three.get("citra_use_libretro_save_path"));
    for (Console system : List.of(Console.DS, Console.THREEDS)) {
      var screens = ScreenLayout.fit(system, 1080, 1920, true, system.defaultGap);
      var lower = screens.get(1).destination();
      var touch = ScreenLayout.touch(screens, (lower.left()+lower.right())/2, (lower.top()+lower.bottom())/2);
      assertEquals(.5, touch[0], .0001);
      assertEquals(.75, touch[1], .0001);
    }
  }

  @Test public void hardwareProfilesUseGlesCompatibleRenderers() {
    assertEquals("gliden64", CoreSettings.defaults("mupen64plus_next").get("mupen64plus-rdp-plugin"));
    assertEquals("hle", CoreSettings.defaults("mupen64plus_next").get("mupen64plus-rsp-plugin"));
    assertEquals("opengl", CoreSettings.defaults("ppsspp").get("ppsspp_backend"));
    assertEquals("480x272", CoreSettings.defaults("ppsspp").get("ppsspp_internal_resolution"));
  }
}
