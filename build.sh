#!/usr/bin/env sh
set -eu
cd -- "$(dirname -- "$0")"
python3 scripts/update-submodules.py
python3 scripts/check-pins.py
python3 scripts/build-notices.py
PLAY_SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
if [ -z "$PLAY_SDK" ]; then
    for PLAY_CANDIDATE in "$HOME/android-sdk" "$HOME/Android/Sdk"; do
        if [ -d "$PLAY_CANDIDATE/platforms" ]; then PLAY_SDK=$PLAY_CANDIDATE; break; fi
    done
fi
if [ -n "$PLAY_SDK" ]; then set -- "-Dandroid.home=$PLAY_SDK" "$@"; fi
if [ -n "${TERMUX_VERSION:-}" ]; then
    PLAY_AAPT2=$(command -v aapt2)
    set -- "-Pandroid.aapt2FromMavenOverride=$PLAY_AAPT2" -PtermuxNative=true "$@"
fi
./gradlew :play-core:test :libinfiniselect:select-android:testDebugUnitTest assembleDebug testDebugUnitTest "$@"
if [ -n "${TERMUX_VERSION:-}" ]; then
    python3 scripts/test-native-cores.py
    python3 scripts/test-profile-cores.py
fi
cp build/outputs/apk/debug/*.apk InfiniPlay-debug.apk
python3 scripts/verify-apk.py
