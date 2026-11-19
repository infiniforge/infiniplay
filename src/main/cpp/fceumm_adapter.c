/* GPL-3.0-or-later. Register standard removable-media callbacks after FDS
 * loads. */
#define retro_load_game play_upstream_load_game
#include "../../../third_party/fceumm/src/drivers/libretro/libretro.c"
#undef retro_load_game
extern unsigned play_fds_count(void);
extern unsigned play_fds_index(void);
extern bool play_fds_ejected(void);
extern bool play_fds_eject(bool);
extern bool play_fds_select(unsigned);
static bool play_no_replace(unsigned index,
                            const struct retro_game_info* info) {
  (void)index;
  (void)info;
  return false;
}
static bool play_no_add(void) { return false; }
static struct retro_disk_control_callback play_fds_control = {
    play_fds_eject, play_fds_ejected, play_fds_index, play_fds_select,
    play_fds_count, play_no_replace,  play_no_add};
bool retro_load_game(const struct retro_game_info* info) {
  bool loaded = play_upstream_load_game(info);
  if (loaded && GameInfo && GameInfo->type == GIT_FDS)
    environ_cb(RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE, &play_fds_control);
  return loaded;
}
