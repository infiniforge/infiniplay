package com.infiniplay.core;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class PlayCoreTest {
  @Rule public TemporaryFolder temp = new TemporaryFolder();
  @Test
  public void allProfilesAreBoundedAndUnique() {
    for (Console s : Console.values())
      for (boolean portrait : List.of(true, false)) {
        var c = PadLayout.defaults(s, portrait);
        assertTrue(c.size() >= 4);
        assertEquals(c.size(), c.stream().map(PadLayout.Control::id).distinct().count());
        for (var b : c) {
          assertTrue(b.x() >= 0 && b.x() <= 1);
          assertTrue(b.y() >= 0 && b.y() <= 1);
          assertTrue(b.size() >= .035 && b.size() <= .6);
        }
      }
  }
  @Test
  public void nesHasRedTwoButtonLayout() {
    var c = PadLayout.defaults(Console.NES, true);
    assertEquals(2, c.stream().filter(b -> b.kind() == PadLayout.Kind.BUTTON).count());
    var a = c.stream().filter(b -> b.id().equals("A")).findFirst().orElseThrow();
    var b = c.stream().filter(x -> x.id().equals("B")).findFirst().orElseThrow();
    assertTrue(a.x() > b.x());
    assertEquals(a.y(), b.y(), 0);
    assertEquals(0xffd44242, a.color());
  }
  @Test
  public void playstationHasOriginalSymbols() {
    var c = PadLayout.defaults(Console.PSX, false);
    for (String id : List.of("□", "△", "○", "×", "L2", "R2"))
      assertTrue(c.stream().anyMatch(b -> b.id().equals(id)));
  }
  @Test
  public void n64CButtonsAreSeparateFromShoulders() {
    var c = PadLayout.defaults(Console.N64, true);
    assertEquals(4, c.stream().filter(b -> b.id().startsWith("C") && b.key() > 1000).count());
    assertFalse(c.stream().anyMatch(b -> b.id().equals("select")));
  }
  @Test
  public void themeChangesWithConsole() {
    assertNotEquals(ControllerTheme.of(Console.NES), ControllerTheme.of(Console.FDS));
    assertTrue(ControllerTheme.of(Console.PSX).roundMedia());
    assertTrue(ControllerTheme.of(Console.PSX).mediaLabel().contains("OPEN"));
    assertFalse(ControllerTheme.of(Console.FDS).roundMedia());
  }
  @Test
  public void movedControlsClampToSafeArea() {
    var c = PadLayout.defaults(Console.NES, true).get(0).move(-2, 5).resize(99);
    assertEquals(0, c.x(), 0);
    assertEquals(1, c.y(), 0);
    assertEquals(.6, c.size(), 0);
  }
  @Test(expected = IllegalArgumentException.class)
  public void invalidPositionRejected() {
    PadLayout.defaults(Console.NES, true).get(0).move(Double.NaN, 0);
  }
  @Test(expected = IllegalArgumentException.class)
  public void invalidSizeRejected() {
    PadLayout.defaults(Console.NES, true).get(0).resize(Double.NaN);
  }
  @Test
  public void hardwareDeadZoneAndRange() {
    assertEquals(0, PadLayout.deadZone(.1, .05), 0);
    assertEquals(1, PadLayout.deadZone(2, .05), 0);
    assertEquals(-1, PadLayout.deadZone(-1, .1), 0);
    assertEquals(0, PadLayout.deadZone(Double.NaN, 0), 0);
    assertEquals(.5, PadLayout.deadZone(.6, .2), .0001);
  }
  @Test
  public void physicalButtonsFollowPosition() {
    assertEquals(97, PadLayout.physicalKey(96));
    assertEquals(96, PadLayout.physicalKey(97));
    assertEquals(100, PadLayout.physicalKey(99));
    assertEquals(19, PadLayout.physicalKey(19));
  }
  @Test
  public void allScreensFitBothOrientations() {
    for (Console s : Console.values())
      for (boolean portrait : List.of(true, false)) {
        double w = portrait ? 1080 : 1920, h = portrait ? 1920 : 1080;
        var screens = ScreenLayout.fit(s, w, h, portrait, s.defaultGap);
        assertEquals(s.dual() ? 2 : 1, screens.size());
        for (var sc : screens) {
          var d = sc.destination();
          assertTrue(d.left() >= 0 && d.top() >= 0 && d.right() <= w && d.bottom() <= h);
        }
      }
  }
  @Test
  public void gameScreensAnchorAtTopOfAvailableArea() {
    for (Console s : Console.values())
      for (boolean portrait : List.of(true, false)) {
        double w = portrait ? 1080 : 1920, h = portrait ? 1920 : 1080;
        var screens = ScreenLayout.fit(s, w, h, portrait, s.defaultGap);
        assertEquals(ScreenLayout.availableArea(w, h, portrait).top(),
            screens.get(0).destination().top(), .00001);
      }
  }
  @Test
  public void dsHingeKeepsHardwareProportion() {
    assertEquals(80, Console.DS.defaultGap, 0);
    for (boolean portrait : List.of(true, false)) {
      var c = ScreenLayout.fit(Console.DS, 1080, 1920, portrait, Console.DS.defaultGap);
      double scale = c.get(0).destination().width() / 256;
      assertEquals(
          Console.DS.defaultGap * scale,
          c.get(1).destination().top() - c.get(0).destination().bottom(), .00001);
    }
  }
  @Test
  public void threeDsBottomRetainsSmallerWidth() {
    var c = ScreenLayout.fit(Console.THREEDS, 1920, 1080, false, 90);
    assertEquals(.8, c.get(1).destination().width() / c.get(0).destination().width(), .00001);
    assertEquals(c.get(0).destination().height(), c.get(1).destination().height(), .00001);
  }
  @Test
  public void touchMapsOnlyBottomScreen() {
    var c = ScreenLayout.fit(Console.THREEDS, 1080, 1920, true, 90);
    var d = c.get(1).destination();
    var p = ScreenLayout.touch(c, (d.left() + d.right()) / 2, (d.top() + d.bottom()) / 2);
    assertArrayEquals(new double[] {.5, .75}, p, .00001);
    assertArrayEquals(new double[] {-10, -10},
        ScreenLayout.touch(c, 540, c.get(0).destination().bottom() + 1), 0);
  }
  @Test(expected = IllegalArgumentException.class)
  public void invalidGapRejected() {
    ScreenLayout.fit(Console.DS, 100, 100, true, -1);
  }
  @Test
  public void twoSourcesHoldSameKey() {
    var d = new DigitalInput();
    assertEquals(List.of(new DigitalInput.Change(96, true)), d.update("finger", Set.of(96)));
    assertTrue(d.update("hardware:1:A", Set.of(96)).isEmpty());
    assertTrue(d.update("finger", Set.of()).isEmpty());
    assertEquals(Set.of(96), d.held());
    assertEquals(List.of(new DigitalInput.Change(96, false)), d.removePrefix("hardware:1:"));
  }
  @Test
  public void disconnectDoesNotReleaseAnotherController() {
    var d = new DigitalInput();
    d.update("hardware:1:A", Set.of(96));
    d.update("hardware:2:A", Set.of(96, 97));
    assertTrue(d.removePrefix("hardware:1:").isEmpty());
    assertEquals(Set.of(96, 97), d.held());
    assertEquals(2, d.clear().size());
    assertTrue(d.held().isEmpty());
  }
  @Test
  public void fdsHasBundledCapableCoreAndRequiredBios() {
    var core =
        CoreCatalog.ALL.stream().filter(c -> c.system() == Console.FDS).findFirst().orElseThrow();
    assertTrue(core.bundled());
    assertEquals("libfceumm.so", core.file());
    var bios = Firmware.requirements(core.id()).get(0);
    assertEquals("disksys.rom", bios.name());
    bios.validate(8192);
    assertTrue(Firmware.requirements("quicknes").isEmpty());
  }
  @Test
  public void firmwareGateFindsWrongAndMissingFiles() throws Exception {
    File d = temp.newFolder();
    assertEquals(1, Firmware.missing("fceumm_fds", d).size());
    File bios = new File(d, "disksys.rom");
    try (var out = new RandomAccessFile(bios, "rw")) {
      out.setLength(1024);
    }
    assertEquals(1, Firmware.missing("fceumm_fds", d).size());
    try (var out = new RandomAccessFile(bios, "rw")) {
      out.setLength(8192);
    }
    assertTrue(Firmware.missing("fceumm_fds", d).isEmpty());
  }
  @Test
  public void dsRequiresAllThreeFiles() {
    assertEquals(3, Firmware.requirements("melonds").size());
  }
  @Test(expected = IllegalArgumentException.class)
  public void unsafeBiosNameRejected() {
    new Firmware.Required("../bios", "bad", List.of());
  }
  @Test
  public void playlistPreservesDiscOrderAndSpaces() {
    assertEquals(List.of("Disc One.chd", "media/Disc Two.cue"),
        MediaManifest.references(
            "set.m3u", "\ufeff# comment\nDisc One.chd\r\nmedia/Disc Two.cue\n"));
  }
  @Test
  public void cueCollectsAllTracks() {
    assertEquals(List.of("Track 1.bin", "track2.wav"),
        MediaManifest.references("disc.cue",
            "FILE \"Track 1.bin\" BINARY\n TRACK 01 MODE2/2352\nFILE track2.wav WAVE\n"));
  }
  @Test(expected = IllegalArgumentException.class)
  public void emptyPlaylistRejected() {
    MediaManifest.references("game.m3u", "#empty\n");
  }
  @Test(expected = IllegalArgumentException.class)
  public void nestedPlaylistsRejected() {
    MediaManifest.references("game.m3u", "other.m3u");
  }
  @Test(expected = IllegalArgumentException.class)
  public void traversalRejected() {
    MediaManifest.path("media/../../secret.bin");
  }
  @Test(expected = IllegalArgumentException.class)
  public void networkMediaRejected() {
    MediaManifest.path("https://host/disc.bin");
  }
  @Test(expected = IllegalArgumentException.class)
  public void absoluteMediaRejected() {
    MediaManifest.path("/sdcard/disc.bin");
  }
  @Test(expected = IllegalArgumentException.class)
  public void playlistLimitEnforced() {
    MediaManifest.references("game.m3u", "disc.chd\n".repeat(513));
  }
  @Test
  public void windowsMediaPathsNormalize() {
    assertEquals("disc/tracks/data.bin", MediaManifest.path(".\\disc\\tracks\\data.bin"));
  }

  @Test
  public void requestedSystemsHaveCompiledCores() {
    for (Console system : List.of(Console.SG1000, Console.MASTERSYSTEM, Console.GAMEGEAR,
             Console.GBC, Console.PCE, Console.PCECD, Console.MEGADRIVE, Console.SEGACD,
             Console.SEGA32X, Console.SNES, Console.LYNX, Console.ATARI2600, Console.ATARI5200,
             Console.ATARI7800, Console.NEOGEO, Console.VIRTUALBOY))
      assertTrue(system.title,
          CoreCatalog.ALL.stream().anyMatch(c -> c.system() == system && c.bundled()));
  }
  @Test
  public void multiMachineCoresUseSelectedHardware() {
    assertEquals("5200", CoreSettings.defaults("atari5200").get("atari800_system"));
    assertEquals("game gear", CoreSettings.defaults("gg").get("genesis_plus_gx_system_hw"));
    assertEquals("mvs", CoreSettings.defaults("geolith_mvs").get("geolith_system_type"));
    assertEquals("6 button pad", CoreSettings.defaults("picodrive_32x").get("picodrive_input1"));
  }
  @Test
  public void cdFirmwareUsesOnlySelectedRegion() {
    assertEquals("bios_CD_U.bin", Firmware.requirements("segacd_us").get(0).name());
    assertEquals("bios_CD_E.bin", Firmware.requirements("segacd_eu").get(0).name());
    assertEquals("bios_CD_J.bin", Firmware.requirements("segacd_jp").get(0).name());
    assertEquals(1, Firmware.requirements("segacd_us").size());
    assertTrue(Firmware.requirements("genesis_plus_gx").isEmpty());
    assertEquals("syscard3.pce", Firmware.requirements("pcecd").get(0).name());
  }
  @Test
  public void virtualBoyKeepsTwoDistinctDpads() {
    var c = PadLayout.defaults(Console.VIRTUALBOY, true);
    assertEquals(2, c.stream().filter(b -> b.kind() == PadLayout.Kind.DPAD).count());
    assertEquals(
        2, c.stream().filter(b -> b.id().equals("right_dpad")).findFirst().orElseThrow().key());
  }
  @Test
  public void atari5200HasCompletePhysicalKeypad() {
    var c = PadLayout.defaults(Console.ATARI5200, false);
    assertEquals(12, c.stream().filter(b -> b.id().startsWith("key_")).count());
    assertTrue(c.stream().anyMatch(b -> b.key() == 2042));
    assertTrue(c.stream().anyMatch(b -> b.key() == 2035));
    assertTrue(c.stream().anyMatch(b -> b.label().equals("PAUSE")));
  }
  @Test
  public void neoGeoHasFourArcadeColors() {
    var c = PadLayout.defaults(Console.NEOGEO, true);
    var colors = c.stream()
                     .filter(b -> b.kind() == PadLayout.Kind.BUTTON)
                     .map(PadLayout.Control::color)
                     .distinct()
                     .count();
    assertEquals(4, colors);
  }
  @Test
  public void lynxAndNeoGeoFirmwareAreDeclared() {
    Firmware.requirements("handy").get(0).validate(512);
    assertEquals("aes.zip", Firmware.requirements("geolith_aes").get(0).name());
    assertEquals("neogeo.zip", Firmware.requirements("geolith_mvs").get(0).name());
  }

  @Test
  public void neoGeoLaunchGateChecksArchiveContents() throws Exception {
    File d = temp.newFolder();
    File file = new File(d, "aes.zip");
    try (var zip = new java.util.zip.ZipOutputStream(new FileOutputStream(file))) {
      zip.putNextEntry(new java.util.zip.ZipEntry("wrong.bin"));
      zip.write(1);
      zip.closeEntry();
    }
    assertEquals(1, Firmware.missing("geolith_aes", d).size());
    try (var zip = new java.util.zip.ZipOutputStream(new FileOutputStream(file))) {
      for (String name : List.of("neo-epo.bin", "000-lo.lo")) {
        zip.putNextEntry(new java.util.zip.ZipEntry(name));
        zip.write(new byte[128]);
        zip.closeEntry();
      }
    }
    assertTrue(Firmware.missing("geolith_aes", d).isEmpty());
  }
  @Test
  public void usbJoystickCanDriveDigitalPads() {
    assertEquals(Set.of(21, 20), PadLayout.digitalAxes(-1, 1));
    assertTrue(PadLayout.digitalAxes(.3, -.3).isEmpty());
    var inputs = new DigitalInput();
    inputs.update("stick:digital", PadLayout.digitalAxes(1, 0));
    inputs.update("hat", Set.of(22));
    assertTrue(inputs.update("stick:digital", Set.of()).isEmpty());
    assertEquals(Set.of(22), inputs.held());
  }
  @Test
  public void genesisExtensionsOverridePreviouslySelectedMasterSystem() {
    for (String extension : List.of("gen", "GEN", "md", "smd")) {
      assertEquals(Console.MEGADRIVE,
          MediaSystem.fromFilename("game." + extension, Console.MASTERSYSTEM));
      assertEquals(Console.MEGADRIVE,
          MediaSystem.fromFilename("old-library-id/game." + extension, Console.MASTERSYSTEM));
    }
    assertEquals("mega drive / genesis",
        CoreSettings.defaults(CoreCatalog.ALL.stream()
            .filter(c -> c.system() == MediaSystem.fromFilename("game.gen", Console.MASTERSYSTEM))
            .findFirst().orElseThrow().id()).get("genesis_plus_gx_system_hw"));
  }

  @Test
  public void ambiguousMediaKeepsSelectedSystem() {
    for (String extension : List.of("bin", "rom", "cue", "iso", "chd", "m3u")) {
      assertEquals(Console.SEGACD, MediaSystem.fromFilename("game." + extension, Console.SEGACD));
      assertEquals(Console.MASTERSYSTEM,
          MediaSystem.fromFilename("game." + extension, Console.MASTERSYSTEM));
    }
    assertEquals(Console.NES, MediaSystem.fromFilename("folder.gen/game", Console.NES));
    assertEquals(Console.GAMEGEAR, MediaSystem.fromFilename("game.gg", Console.MEGADRIVE));
    assertEquals(Console.MASTERSYSTEM, MediaSystem.fromFilename("game.sms", Console.MEGADRIVE));
    assertEquals(Console.SG1000, MediaSystem.fromFilename("game.sg", Console.MEGADRIVE));
  }
  @Test
  public void automaticOpenRecognizesSystemsAndDoesNotGuessAmbiguousDiscs() {
    assertEquals(Console.GBA,MediaSystem.identify("game.gba",new byte[0]));
    assertEquals(Console.DS,MediaSystem.identify("game.nds",new byte[0]));
    assertEquals(Console.N64,MediaSystem.identify("game.Z64",new byte[0]));
    assertEquals(Console.THREEDS,MediaSystem.identify("game.3ds",new byte[0]));
    assertNull(MediaSystem.identify("disc.chd",new byte[0]));
    assertNull(MediaSystem.identify("ambiguous.bin",new byte[512]));
    byte[] sega=new byte[512];sega[256]='S';sega[257]='E';sega[258]='G';sega[259]='A';
    assertEquals(Console.MEGADRIVE,MediaSystem.identify("cartridge.bin",sega));
    assertEquals(Console.NES,MediaSystem.identify("cartridge.rom",new byte[]{'N','E','S',0x1a}));
  }
}
