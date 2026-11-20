package com.infiniplay.core;
import java.util.*;
/** Required machine selection prevents multi-system cores from booting a different console. */
public final class CoreSettings {
  public static Map<String, String> defaults(String id) {
    return switch (id) {
      case "sg1000" -> Map.of("genesis_plus_gx_system_hw", "sg-1000 II");
      case "sms" -> Map.of("genesis_plus_gx_system_hw", "master system");
      case "gg" -> Map.of("genesis_plus_gx_system_hw", "game gear");
      case "genesis_plus_gx" -> Map.of("genesis_plus_gx_system_hw", "mega drive / genesis");
      case "segacd_us" -> Map.of("genesis_plus_gx_region_detect", "ntsc-u");
      case "segacd_eu" -> Map.of("genesis_plus_gx_region_detect", "pal");
      case "segacd_jp" -> Map.of("genesis_plus_gx_region_detect", "ntsc-j");
      case "picodrive_32x" ->
        Map.of("picodrive_input1", "6 button pad", "picodrive_input2", "6 button pad");
      case "pcecd" -> Map.of("pce_fast_cdbios", "System Card 3");
      case "atari5200" -> Map.of("atari800_system", "5200", "atari800_keyboard", "poll");
      case "geolith_aes" -> Map.of("geolith_system_type", "aes");
      case "geolith_mvs" -> Map.of("geolith_system_type", "mvs");
      case "mgba" -> Map.of("mgba_use_bios", "OFF", "mgba_skip_bios", "ON");
      case "ngp", "ngpc" -> Map.of("ngp_language", "english");
      case "wswan", "wswan_color" -> Map.of("wswan_language", "english",
          "wswan_rotate_display", "manual", "wswan_rotate_keymap", "disabled");
      case "virtualboy" ->
        Map.of("vb_3dmode", "anaglyph", "vb_anaglyph_preset", "red & cyan", "vb_color_mode",
            "black & red", "vb_right_analog_to_digital", "On");
      case "mupen64plus_next" -> Map.of("mupen64plus-rdp-plugin", "gliden64", "mupen64plus-rsp-plugin", "hle", "mupen64plus-43screensize", "640x480");
      case "pcsx_rearmed" -> Map.of("pcsx_rearmed_bios", "auto", "pcsx_rearmed_drc", "enabled");
      case "ppsspp" -> Map.of("ppsspp_backend", "opengl", "ppsspp_internal_resolution", "480x272");
      case "desmume" -> Map.of("desmume_screens_layout", "top/bottom", "desmume_screens_gap", "0", "desmume_pointer_type", "touch", "desmume_internal_resolution", "256x192");
      case "melonds" -> Map.of("melonds_screen_layout", "Top/Bottom", "melonds_screen_gap", "0");
      case "citra" -> Map.of("citra_layout_option", "Default Top-Bottom Screen", "citra_resolution_factor", "1x (Native)", "citra_touch_touchscreen", "enabled", "citra_mouse_touchscreen", "disabled", "citra_use_libretro_save_path", "LibRetro Default");
      default -> Map.of();
    };
  }
}
