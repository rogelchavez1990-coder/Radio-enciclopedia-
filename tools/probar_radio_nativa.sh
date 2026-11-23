#!/usr/bin/env bash
# Comprueba reproducción real, MediaSession, segundo plano, controles y fallo sin red.
set -euo pipefail
OUT="${1:-radio-nativa}"
source tools/android_ui.sh
trap ui_restore EXIT
adb shell svc wifi enable
adb shell svc data enable
adb shell wm size 720x1600
adb shell wm density 280
adb shell settings put system font_scale 1
API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
if [ "$API" -le 28 ]; then adb shell settings put global animator_duration_scale 0
else adb shell settings put global animator_duration_scale 1; fi
if [ "$API" -ge 33 ]; then adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS; fi
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb logcat -c
{
  echo "user_setup_complete=$(adb shell settings get secure user_setup_complete | tr -d '\r')"
  echo "device_provisioned=$(adb shell settings get global device_provisioned | tr -d '\r')"
} > "$OUT/configuracion-emulador.txt"
# Android 9 expone la utilidad «media»; el comando Binder se añadió después.
media_command() {
  if [ "$API" -le 28 ]; then adb shell media dispatch "$1"
  else adb shell cmd media_session dispatch "$1"; fi
}

media_state() {
  timeout 10 adb shell dumpsys media_session > "$OUT/media-session.txt"
  python3 - "$OUT/media-session.txt" "$PKG" <<'PY'
import sys,re
s=open(sys.argv[1]).read();p=s.find('package='+sys.argv[2])
m=re.search(r'state=PlaybackState\s*\{state=(?:[A-Z_]+\()?(\d+)',s[p:p+4000]) if p>=0 else None
print(m.group(1) if m else '0')
PY
}
wait_state() {
  for attempt in $(seq 1 25); do
    if [ "$(media_state)" = "$1" ]; then return 0; fi
    sleep 2
  done
  cat "$OUT/media-session.txt"
  adb logcat -d -s MediaPlayer RadioService AndroidRuntime MediaSessionService MediaSessionRecord > "$OUT/audio-log.txt"
  echo "No se alcanzó el estado multimedia $1" >&2
  return 1
}

ui_open 2
ui_touch 'Ver lista de horarios'
ui_photo lista_horarios
adb shell input keyevent KEYCODE_BACK
ui_open 1
if [ "$(media_state)" = 3 ]; then echo 'Se reprodujo audio sin intervención'; exit 1; fi
ui_photo escuchar_inactivo
ui_touch 'Reproducir radio desde el dial'
if ! wait_state 3; then
  ui_dump
  if python3 tools/ui_nodos.py "$OUT/ui.xml" | grep -q '^Reintentar radio|'; then
    echo '[AVISO] La primera conexión agotó su tiempo; se comprueba el reintento explícito.'
    ui_touch 'Reintentar radio'
    wait_state 3
  else
    exit 1
  fi
fi
cp "$OUT/media-session.txt" "$OUT/reproduciendo.txt"
ui_photo reproduciendo
adb shell dumpsys activity services "$PKG" > "$OUT/servicio.txt"
grep -q 'isForeground=true' "$OUT/servicio.txt"
adb shell dumpsys activity activities > "$OUT/actividad.txt"
grep -E 'topResumedActivity|mResumedActivity' "$OUT/actividad.txt" | grep -F "$PKG/" | grep -Fq 'MainActivity'
echo '[OK] Audio real dentro de la aplicación, sesión activa y servicio multimedia.'
ui_touch 'Ir a Inicio'
ui_dump
# El servidor puede entrar en buffering mientras se cambia de sección.
grep -Eq 'Reproduciendo en directo|Conectando con la emisora' "$OUT/ui.xml"
ui_photo inicio_reproduciendo
wait_state 3
ui_touch 'Ir a Escuchar'

adb shell input keyevent KEYCODE_HOME
sleep 3
wait_state 3
adb shell input keyevent KEYCODE_SLEEP
sleep 3
wait_state 3
echo '[OK] La señal continúa en segundo plano y con la pantalla apagada.'
media_command pause
wait_state 2
cp "$OUT/media-session.txt" "$OUT/pausado.txt"
media_command play
wait_state 3
media_command stop
sleep 2
if [ "$(media_state)" = 3 ]; then echo 'La señal no se detuvo'; exit 1; fi
echo '[OK] Pausa, reanudación y detención desde los controles del sistema.'

adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
ui_open 1
adb shell svc wifi disable
adb shell svc data disable
ui_touch 'Reproducir radio'
RETRY=0
for attempt in $(seq 1 10); do
  ui_dump
  if python3 tools/ui_nodos.py "$OUT/ui.xml" | grep -q '^Reintentar radio|'; then RETRY=1; break; fi
  sleep 3
done
if [ "$RETRY" -ne 1 ]; then echo 'No se mostró el estado de reintento sin red'; exit 1; fi
ui_photo error_sin_red
adb shell svc wifi enable
adb shell svc data enable
sleep 4
ui_touch 'Reintentar radio'
wait_state 3
ui_photo reanudado
ui_touch 'Ir a Inicio'
ui_touch 'Reproductor de Radio Enciclopedia'
wait_state 2
ui_photo inicio_pausado
ui_touch 'Ir a Escuchar'
ui_photo pausa
echo '[OK] Error sin red y recuperación de la señal al reintentar.'

adb logcat -d -b crash > "$OUT/crashes.txt"
if grep -q "Process: $PKG" "$OUT/crashes.txt"; then exit 1; fi
media_command stop
echo 'Radio nativa: comprobaciones aprobadas.'
