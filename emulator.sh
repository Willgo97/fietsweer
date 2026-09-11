#!/usr/bin/env bash
# Start the test emulator used while developing this app.
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
exec "$ANDROID_HOME/emulator/emulator" -avd fietsweer -no-audio -no-boot-anim \
  -gpu swiftshader_indirect -no-snapshot-save -memory 3072 -cores 4 "$@"
