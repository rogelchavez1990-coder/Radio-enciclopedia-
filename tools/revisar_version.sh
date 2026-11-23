#!/usr/bin/env bash
set -euo pipefail
API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
if [ "$API" = 28 ]; then
  adb install -r dist/Radio_Enciclopedia.apk
  bash tools/probar_radio_nativa.sh preview/radio
else
  bash tools/revisar_diseno.sh dist/Radio_Enciclopedia.apk preview/diseno
fi
