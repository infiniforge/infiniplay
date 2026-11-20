// GPL-2.0-or-later, like the included NeoPop core. Keep upstream source unchanged.
// NeoPop otherwise exposes cartridge flash only through an unload-time .ngf file.
// Expose its writable cartridge buffer to the host's per-game SRAM snapshots so
// backgrounding can persist flash without rewriting the selected source ROM.
#define retro_get_memory_data ngp_original_get_memory_data
#define retro_get_memory_size ngp_original_get_memory_size
#define retro_load_game ngp_original_load_game
#define retro_unload_game ngp_original_unload_game
#include "../../../third_party/ngp/libretro.c"
#undef retro_load_game
#undef retro_unload_game
#undef retro_get_memory_data
#undef retro_get_memory_size

static bool play_ngp_loaded;
bool retro_load_game(const struct retro_game_info *info) {
  play_ngp_loaded = ngp_original_load_game(info);
  return play_ngp_loaded;
}
void retro_unload_game(void) {
  ngp_original_unload_game();
  play_ngp_loaded = false;
}

void *retro_get_memory_data(unsigned type) {
  if (type == RETRO_MEMORY_SAVE_RAM) return play_ngp_loaded ? ngpc_rom.data : NULL;
  return ngp_original_get_memory_data(type);
}

size_t retro_get_memory_size(unsigned type) {
  if (type == RETRO_MEMORY_SAVE_RAM) return play_ngp_loaded ? ngpc_rom.length : 0;
  return ngp_original_get_memory_size(type);
}
