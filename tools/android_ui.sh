#!/usr/bin/env bash
# Utilidades de las comprobaciones de diseño y reproducción en un dispositivo de prueba.
: "${OUT:?Define la carpeta OUT}"
PKG="${RADIO_TEST_PACKAGE:-cu.radioenciclopedia.app}"
ACTIVITY=cu.radioenciclopedia.app.MainActivity
mkdir -p "$OUT"

ui_dump() {
  local attempt
  for attempt in 1 2 3; do
    rm -f "$OUT/ui.xml"
    timeout 5 adb shell rm -f /sdcard/radio-ui.xml
    if timeout 15 adb shell uiautomator dump /sdcard/radio-ui.xml > "$OUT/volcado.log" 2>&1 \
       && timeout 8 adb pull /sdcard/radio-ui.xml "$OUT/ui.xml" >/dev/null 2>&1 \
       && [ -s "$OUT/ui.xml" ]; then
      return 0
    fi
    sleep 2
  done
  cat "$OUT/volcado.log" >&2
  echo 'No se obtuvo una jerarquía actual de la pantalla.' >&2
  return 1
}
ui_photo() {
  timeout 20 adb shell screencap -p /sdcard/radio-preview.png
  timeout 12 adb pull /sdcard/radio-preview.png "$OUT/$1.png" >/dev/null
}
ui_touch() {
  local attempt coords size w h
  for attempt in 0 1 2 3 4 5 6; do
    ui_dump
    coords=$(python3 - "$OUT/ui.xml" "$1" <<'PY'
import sys,re,xml.etree.ElementTree as E
for n in E.parse(sys.argv[1]).getroot().iter('node'):
    if sys.argv[2] in (n.get('content-desc'), n.get('text')):
        b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
        if len(b)==4 and b[2]>b[0] and b[3]>b[1]:
            print((b[0]+b[2])//2,(b[1]+b[3])//2);break
PY
)
    if [ -n "$coords" ]; then
      read -r w h <<< "$coords"
      timeout 10 adb shell input tap "$w" "$h"
      sleep 0.8
      return 0
    fi
    size=$(adb shell wm size | sed -n 's/.*: //p' | tail -1 | tr -d '\r')
    w=${size%x*};h=${size#*x}
    adb shell input swipe $((w/2)) $((h*3/4)) $((w/2)) $((h/4)) 220
  done
  echo "No está visible: $1" >&2
  return 1
}
ui_open() {
  timeout 25 adb shell am start -W -n "$PKG/$ACTIVITY" --ei screen "${1:-0}" > "$OUT/arranque.txt"
  if grep -q '^Error' "$OUT/arranque.txt"; then cat "$OUT/arranque.txt"; return 1; fi
  sleep 2
  ui_dump
}
ui_restore() {
  timeout 5 adb shell settings put system font_scale 1 >/dev/null 2>&1 || true
  timeout 5 adb shell wm size reset >/dev/null 2>&1 || true
  timeout 5 adb shell wm density reset >/dev/null 2>&1 || true
  timeout 5 adb shell svc wifi enable >/dev/null 2>&1 || true
  timeout 5 adb shell svc data enable >/dev/null 2>&1 || true
  timeout 5 adb logcat -d -b crash > "$OUT/crashes.txt" 2>&1 || true
}
