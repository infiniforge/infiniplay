// GPL-3.0-or-later. Polled Libretro keyboard state for the 5200's physical
// keypad.
#pragma once
#include <jni.h>

#include <array>
static std::array<std::array<bool, 512>, 4> playKeyboard{};
extern "C" JNIEXPORT void JNICALL Java_com_infiniplay_NativeScreens_key(
    JNIEnv*, jclass, jint port, jint code, jboolean pressed) {
  if (port < 0 || port >= 4) return;
  if (code == -1)
    playKeyboard[port].fill(false);
  else if (code >= 0 && code < 512)
    playKeyboard[port][code] = pressed;
}
