/* GPL-3.0-or-later. Expose the pinned PCE core's loaded CD set through
 * standard Libretro disk callbacks. Source checkout remains unchanged. */
#define retro_load_game play_pce_load_game
#define retro_unload_game play_pce_unload_game
#include "../../../third_party/pce/libretro.c"
#undef retro_load_game
#undef retro_unload_game

static unsigned play_pce_disk_index;
static bool play_pce_tray_open;
static unsigned play_pce_count(void) {
  return PCE_IsCD ? (unsigned)CDInterfaces.n : 0;
}
static unsigned play_pce_index(void) { return play_pce_disk_index; }
static bool play_pce_ejected(void) { return play_pce_tray_open; }
static bool play_pce_eject(bool open) {
  if (!play_pce_count() || play_pce_disk_index >= play_pce_count())
    return false;
  if (play_pce_tray_open != open) {
    PCECD_Drive_SetDisc(
        open, open ? NULL : CDInterfaces.items[play_pce_disk_index], false);
    play_pce_tray_open = open;
  }
  return true;
}
static bool play_pce_select(unsigned index) {
  if (!play_pce_tray_open || index >= play_pce_count()) return false;
  play_pce_disk_index = index;
  return true;
}
static bool play_pce_no_replace(unsigned index,
                                const struct retro_game_info* game) {
  (void)index;
  (void)game;
  return false;
}
static bool play_pce_no_add(void) { return false; }
static struct retro_disk_control_callback play_pce_control = {
    play_pce_eject, play_pce_ejected,    play_pce_index, play_pce_select,
    play_pce_count, play_pce_no_replace, play_pce_no_add};
bool retro_load_game(const struct retro_game_info* game) {
  bool loaded = play_pce_load_game(game);
  play_pce_disk_index = 0;
  play_pce_tray_open = false;
  environ_cb(RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE,
             loaded && PCE_IsCD ? &play_pce_control : NULL);
  return loaded;
}
void retro_unload_game(void) {
  environ_cb(RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE, NULL);
  play_pce_unload_game();
  play_pce_disk_index = 0;
  play_pce_tray_open = false;
}
