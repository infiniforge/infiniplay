package com.infiniplay.core;
import java.util.*;
import java.util.regex.*;

/**
 * Bounded local media references. Import never lets a manifest reach outside its selected
 * directory.
 */
public final class MediaManifest {
  public static final int MAX_FILES = 512;
  public static String path(String value) {
    String p = value.trim().replace('\\', '/');
    if (p.isEmpty() || p.length() > 512 || p.startsWith("/") || p.contains(":")
        || p.contains("\u0000") || p.contains("\n") || p.contains("\r"))
      throw new IllegalArgumentException("Use relative media filenames");
    List<String> parts = new ArrayList<>();
    for (String s : p.split("/")) {
      if (s.equals(".."))
        throw new IllegalArgumentException("Media must be inside the selected directory");
      if (!s.isEmpty() && !s.equals("."))
        parts.add(s);
    }
    if (parts.isEmpty())
      throw new IllegalArgumentException("Empty media filename");
    return String.join("/", parts);
  }
  public static List<String> references(String filename, String text) {
    if (text.length() > 1024 * 1024)
      throw new IllegalArgumentException("Media manifest is too large");
    List<String> result = new ArrayList<>();
    String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    for (String raw : text.replace("\ufeff", "").split("\\R")) {
      String line = raw.trim();
      if (line.isEmpty() || line.startsWith("#"))
        continue;
      if (ext.equals("m3u")) {
        result.add(path(line));
      } else if (ext.equals("cue")) {
        Matcher m =
            Pattern.compile("(?i)^FILE\\s+(?:\"([^\"]+)\"|(\\S+))\\s+\\S+\\s*$").matcher(line);
        if (m.matches())
          result.add(path(m.group(1) != null ? m.group(1) : m.group(2)));
        else if (line.toUpperCase(Locale.ROOT).startsWith("FILE "))
          throw new IllegalArgumentException("Invalid CUE FILE line");
      }
      if (result.size() > MAX_FILES)
        throw new IllegalArgumentException("Too many media files");
    }
    if ((ext.equals("m3u") || ext.equals("cue")) && result.isEmpty())
      throw new IllegalArgumentException("No media in " + filename);
    if (ext.equals("m3u")
        && result.stream().anyMatch(p -> p.toLowerCase(Locale.ROOT).endsWith(".m3u")))
      throw new IllegalArgumentException("Nested M3U playlists are not supported");
    return List.copyOf(result);
  }
}
