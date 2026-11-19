/* GPL-3.0-or-later. Small bridge for the pinned FCEUmm FDS implementation.
 * Upstream maps disk changes to controller L/R; expose the same drive through
 * Libretro's standard disk callbacks so UI state reflects saves and controller
 * changes. Include without editing upstream; static state stays owned by this
 * one translation unit. */
#include "../../../third_party/fceumm/src/fds.c"
unsigned play_fds_count(void) { return TotalSides; }
unsigned play_fds_index(void) { return SelectDisk; }
bool play_fds_ejected(void) { return InDisk == 255; }
bool play_fds_eject(bool ejected) {
  if (!TotalSides) return false;
  if (ejected)
    FCEU_FDSEject();
  else if (InDisk == 255)
    FCEU_FDSInsert(-1);
  return play_fds_ejected() == ejected;
}
bool play_fds_select(unsigned index) {
  if (InDisk != 255 || index >= TotalSides) return false;
  SelectDisk = (uint8_t)index;
  return true;
}
