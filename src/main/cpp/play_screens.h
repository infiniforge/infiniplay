// InfiniPlay's final-pass dual-screen compositor. GPL-3.0-or-later.
#pragma once
#include <GLES2/gl2.h>
#include <jni.h>

#include <array>
static std::array<float, 16> playScreens{};
static int playScreenCount = 0;
static bool playN64BobEnabled = false;
static int playN64BobFieldParity = 0;
extern "C" JNIEXPORT void JNICALL
Java_com_infiniplay_NativeScreens_setN64Deinterlace(JNIEnv*, jclass, jboolean enabled) {
  playN64BobEnabled = enabled == JNI_TRUE;
  playN64BobFieldParity = 0;
}
static void playN64BobSetFieldParity(int parity) {
  if (playN64BobEnabled) playN64BobFieldParity = parity & 1;
}
extern "C" JNIEXPORT void JNICALL
Java_com_infiniplay_NativeScreens_set(JNIEnv* env, jclass, jfloatArray values) {
  const int size = values ? env->GetArrayLength(values) : 0;
  playScreenCount = (size == 16) ? 2 : 0;
  if (playScreenCount)
    env->GetFloatArrayRegion(values, 0, 16, playScreens.data());
}
static void playDrawScreens(GLint position, GLint coordinate, bool flip) {
  for (int n = 0; n < playScreenCount; n++) {
    const float* s = playScreens.data() + 8 * n;
    float l = 2 * s[4] - 1, r = 2 * s[6] - 1, t = 1 - 2 * s[5],
          b = 1 - 2 * s[7];
    float st = flip ? 1 - s[1] : s[1], sb = flip ? 1 - s[3] : s[3];
    float xy[] = {l, t, l, b, r, t, r, t, l, b, r, b};
    float uv[] = {s[0], st, s[0], sb, s[2], st, s[2], st, s[0], sb, s[2], sb};
    glVertexAttribPointer(position, 2, GL_FLOAT, GL_FALSE, 0, xy);
    glVertexAttribPointer(coordinate, 2, GL_FLOAT, GL_FALSE, 0, uv);
    glDrawArrays(GL_TRIANGLES, 0, 6);
  }
}

#include "environment.h"
// Shared removable-media API for FDS sides and multi-CD cores. Always called on
// GL thread.
extern "C" JNIEXPORT jintArray JNICALL Java_com_infiniplay_NativeScreens_media(
    JNIEnv* env, jclass, jint command, jint index) {
  auto* disk = Environment::getInstance().getRetroDiskControlCallback();
  jint result[4] = {0, 0, 0, 0};
  if (disk && disk->get_num_images && disk->get_image_index &&
      disk->get_eject_state && disk->set_eject_state && disk->set_image_index) {
    result[0] = disk->get_num_images();
    if (command == 1 && index >= 0 && index < result[0]) {
      bool ok = disk->set_eject_state(true);
      if (ok) ok = disk->set_image_index(index);
      if (ok) ok = disk->set_eject_state(false);
      result[3] = ok ? 1 : -1;
    } else if (command == 3 && disk->get_eject_state() && index >= 0 &&
               index < result[0])
      result[3] = disk->set_image_index(index) ? 1 : -1;
    else if (command == 2)
      result[3] = disk->set_eject_state(!disk->get_eject_state()) ? 1 : -1;
    result[1] = disk->get_image_index();
    result[2] = disk->get_eject_state() ? 1 : 0;
  }
  auto values = env->NewIntArray(4);
  env->SetIntArrayRegion(values, 0, 4, result);
  return values;
}
