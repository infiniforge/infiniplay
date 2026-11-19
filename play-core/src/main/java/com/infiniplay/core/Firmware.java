package com.infiniplay.core;
import java.io.*;
import java.util.*;

public final class Firmware {
  public record Required(String name, String description, List<Long> sizes) {
    public Required {
      if (name == null || !name.matches("[A-Za-z0-9_.-]+") || name.equals(".."))
        throw new IllegalArgumentException("Invalid firmware name");
      sizes = List.copyOf(sizes);
    }
    public void validate(long size) {
      if (size <= 0 || size > 32 * 1024 * 1024L || !sizes.isEmpty() && !sizes.contains(size))
        throw new IllegalArgumentException(
            "Wrong file size for " + name + "; expected " + sizes + " bytes");
    }
  }
  public static List<Required> requirements(String core) {
    return switch (core) {
      case "handy" -> List.of(new Required("lynxboot.img", "Atari Lynx boot ROM", List.of(512L)));
      case "pcecd" ->
        List.of(
            new Required("syscard3.pce", "PC Engine CD System Card 3", List.of(262144L, 262656L)));
      case "segacd_us" ->
        List.of(new Required("bios_CD_U.bin", "Sega CD USA BIOS", List.of(131072L)));
      case "segacd_eu" ->
        List.of(new Required("bios_CD_E.bin", "Mega-CD Europe BIOS", List.of(131072L)));
      case "segacd_jp" ->
        List.of(new Required("bios_CD_J.bin", "Mega-CD Japan BIOS", List.of(131072L)));
      case "geolith_aes" -> List.of(new Required("aes.zip", "Neo Geo AES BIOS archive", List.of()));
      case "geolith_mvs" ->
        List.of(new Required("neogeo.zip", "Neo Geo MVS BIOS archive", List.of()));
      case "fceumm_fds" ->
        List.of(new Required("disksys.rom", "Famicom Disk System BIOS", List.of(8192L)));
      case "pcsx_rearmed" ->
        List.of(new Required("scph5501.bin",
            "PlayStation BIOS (512 KiB; selected BIOS will use this core filename)",
            List.of(524288L)));
      case "gpsp" ->
        List.of(new Required("gba_bios.bin", "Game Boy Advance BIOS", List.of(16384L)));
      case "melonds" ->
        List.of(new Required("bios7.bin", "Nintendo DS ARM7 BIOS", List.of(16384L)),
            new Required("bios9.bin", "Nintendo DS ARM9 BIOS", List.of(4096L)),
            new Required(
                "firmware.bin", "Nintendo DS firmware", List.of(131072L, 262144L, 524288L)));
      default -> List.of();
    };
  }
  public static void validateFile(Required required, File file) {
    required.validate(file.isFile() ? file.length() : 0);
    List<String> entries = switch (required.name()) {
      case "aes.zip" -> List.of("neo-epo.bin", "000-lo.lo");
      case "neogeo.zip" -> List.of("sp-u2.sp1", "000-lo.lo", "sfix.sfix", "sm1.sm1");
      default -> List.of();
    };
    if (!entries.isEmpty())
      try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file)) {
        for (String name : entries) {
          var entry = zip.getEntry(name);
          if (entry == null || entry.isDirectory() || entry.getSize() <= 0
              || entry.getSize() > 8 * 1024 * 1024)
            throw new IllegalArgumentException(
                "BIOS archive needs " + name + " for the selected USA profile");
        }
      } catch (IOException e) {
        throw new IllegalArgumentException("Unreadable BIOS archive", e);
      }
  }
  public static List<Required> missing(String core, File directory) {
    List<Required> missing = new ArrayList<>();
    for (Required r : requirements(core)) {
      File f = new File(directory, r.name);
      try {
        validateFile(r, f);
      } catch (IllegalArgumentException e) {
        missing.add(r);
      }
    }
    return missing;
  }
}
