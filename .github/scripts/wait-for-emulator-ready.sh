#!/usr/bin/env bash
set -euo pipefail

adb wait-for-device
adb shell 'until [[ "$(getprop sys.boot_completed)" == "1" ]]; do sleep 2; done'

# Let launcher and system services settle; reduces cold-start ANRs on CI.
sleep 15

adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard 2>/dev/null || true

echo "Emulator boot completed and keyguard dismissed"
