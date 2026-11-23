#!/usr/bin/env bash
# Prueba de humo con adb en un emulador o teléfono conectado.
# Uso: tools/probar_en_emulador.sh [apk] [carpeta_resultados]
# Comprueba: instalación, reinstalación (actualización), arranque sin red, las 5 pantallas (Inicio,
# Escuchar, Programas, Comunidad y Contacto), los 9 enlaces + 3 teléfonos + correo, apertura de cada
# uno, retorno a la app, que no se inicie ninguna llamada sola, ausencia de cierres inesperados,
# pantalla pequeña y que las animaciones se muevan. Guarda capturas de pantalla.
set -u
APK="${1:-dist/Radio_Enciclopedia.apk}"
OUT="${2:-resultados}"
PKG="${RADIO_TEST_PACKAGE:-cu.radioenciclopedia.app}"
ACT="$PKG/.MainActivity"
TABS="Inicio Escuchar Programas Comunidad Contacto"
mkdir -p "$OUT"
LOG="$OUT/prueba.txt"; : > "$LOG"
FALLOS=0
log()   { echo "$*" | tee -a "$LOG"; }
ok()    { log "[OK]     $*"; }
aviso() { log "[AVISO]  $*"; }
fallo() { log "[FALLO]  $*"; FALLOS=$((FALLOS+1)); }

API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
log "Dispositivo: $(adb shell getprop ro.product.model | tr -d '\r') | Android $(adb shell getprop ro.build.version.release | tr -d '\r') (API $API)"

# uiautomator puede fallar de forma pasajera en emuladores lentos con animaciones continuas: se reintenta.
volcar() {
  local k
  for k in 1 2 3 4; do
    adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
    if adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1; then break; fi
    sleep 3
  done
  python3 tools/ui_nodos.py "$OUT/ui.xml"
}
actividad() { adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | head -1 | tr -d '\r'; }
tamano() { adb shell wm size | grep -oE "[0-9]+x[0-9]+" | tail -1; }
deslizar_arriba() { local s w h; s=$(tamano); w=${s%x*}; h=${s#*x}; adb shell input swipe $((w/2)) $((h*3/4)) $((w/2)) $((h/4)) 250; sleep 0.3; }
deslizar_abajo()  { local s w h; s=$(tamano); w=${s%x*}; h=${s#*x}; adb shell input swipe $((w/2)) $((h/4)) $((w/2)) $((h*3/4)) 250; sleep 0.3; }
ir_arriba() { local j; for j in 1 2 3 4 5 6; do deslizar_abajo; done; }
captura() { adb exec-out screencap -p > "$1" 2>/dev/null; }

# Busca un elemento por su content-desc (desplazando si hace falta) y lo toca.
tocar() {
  local i linea x y
  linea=$(volcar | grep -F -- "$1|" | head -1)
  if [ -n "$linea" ]; then
    x=$(echo "$linea" | cut -d'|' -f2); y=$(echo "$linea" | cut -d'|' -f3)
    adb shell input tap "$x" "$y"; sleep 1; return 0
  fi
  ir_arriba
  for i in 1 2 3 4 5 6; do
    linea=$(volcar | grep -F -- "$1|" | head -1)
    if [ -n "$linea" ]; then
      x=$(echo "$linea" | cut -d'|' -f2); y=$(echo "$linea" | cut -d'|' -f3)
      adb shell input tap "$x" "$y"; sleep 2; return 0
    fi
    deslizar_arriba
  done
  return 1
}

abrir_pantalla() {
  if [ "$1" = "Comunidad" ]; then
    tocar "Ir a Inicio" || return 1
    tocar "Abrir menú" || return 1
  fi
  tocar "Ir a $1"
}

# Recorre la pantalla actual de arriba abajo y lista las descripciones visibles.
recorrer() {
  ir_arriba
  : > "$OUT/descs.txt"
  local i
  for i in 1 2 3 4 5 6; do volcar | cut -d'|' -f1 >> "$OUT/descs.txt"; deslizar_arriba; done
  # Incluye los enlaces que aparecen después del último desplazamiento.
  volcar | cut -d'|' -f1 >> "$OUT/descs.txt"
  sort -u "$OUT/descs.txt" | grep -v '^$'
}

# Visita las 5 pantallas y deja en $1 líneas "pantalla|elemento" (enlaces, teléfonos y correo).
inventario() {
  : > "$1"
  local T
  for T in $TABS; do
    if abrir_pantalla "$T"; then
      [ -n "${2:-}" ] && captura "$OUT/${2}_${T}_api$API.png"
      recorrer | grep -E '^Abrir |^Llamar al |^Enviar un correo' | grep -v '^Abrir menú$' | sed "s/^/$T|/" >> "$1"
      [ -n "${2:-}" ] && captura "$OUT/${2}_${T}_abajo_api$API.png"
    else
      fallo "no se encontró la pestaña '$T'"
    fi
  done
}

hay_crash() { adb logcat -d -b crash 2>/dev/null | grep -q "Process: $PKG"; }
estado_llamada() { adb shell dumpsys telephony.registry 2>/dev/null | grep -m1 -E "mCallState" | tr -d '\r'; }

adb logcat -c >/dev/null 2>&1
adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard >/dev/null 2>&1

# 1) Instalación y reinstalación con la misma firma
if adb install -r "$APK" 2>&1 | tee -a "$LOG" | grep -q Success; then ok "instalación mediante el instalador del sistema"; else fallo "instalación"; fi
if adb install -r "$APK" 2>&1 | grep -q Success; then ok "reinstalación/actualización sobre la misma app (misma firma)"; else fallo "reinstalación"; fi
adb shell dumpsys package "$PKG" | grep -E "versionName|minSdk|targetSdk" | head -3 | sed 's/^ */         /' | tee -a "$LOG"

# 2) Arranque SIN conexión
adb shell svc wifi disable >/dev/null 2>&1; adb shell svc data disable >/dev/null 2>&1
adb shell am force-stop "$PKG"
adb shell am start -W -n "$ACT" >/dev/null 2>&1
sleep 4
if [ -n "$(adb shell pidof "$PKG" | tr -d '\r')" ]; then ok "la app arranca sin conexión a Internet"; else fallo "la app no arrancó"; fi
echo "$(actividad)" | grep -q "MainActivity" && ok "inicia directamente en la pantalla principal" || fallo "la pantalla principal no está en primer plano: $(actividad)"
# Emuladores lentos: espera (hasta ~80 s) a que la app dibuje su pantalla; si no, la vuelve a abrir una vez
for i in $(seq 1 16); do
  volcar | grep -q "Ir a Inicio" && break
  [ "$i" -eq 8 ] && { adb shell input keyevent KEYCODE_WAKEUP; adb shell am start -n "$ACT" >/dev/null 2>&1; }
  sleep 5
done
captura "$OUT/inicio_arranque_api$API.png"
volcar > /dev/null
if python3 tools/verificar_inicio.py "$OUT/ui.xml" | tee -a "$LOG"; then ok "Inicio completo sin deslizar"; else fallo "Inicio recortado o incompleto"; fi
if tocar "Abrir menú"; then
  captura "$OUT/menu_api$API.png"
  tocar "Cerrar menú" || adb shell input keyevent KEYCODE_BACK
else
  fallo "no se abre el menú lateral"
fi

# 3) Las 5 pantallas y sus elementos
inventario "$OUT/mapa.txt" pantalla
TODO=$(cut -d'|' -f2 "$OUT/mapa.txt" | sort -u)
N_BOT=$(echo "$TODO" | grep -c '^Abrir ')
N_TEL=$(echo "$TODO" | grep -c '^Llamar al ')
N_MAIL=$(echo "$TODO" | grep -c '^Enviar un correo')
for T in $TABS; do
  if grep -q "^$T|" "$OUT/mapa.txt"; then
    ok "pantalla $T accesible desde la barra inferior"
  elif [ "$T" = "Programas" ]; then
    # Programas ya no tiene enlaces «Abrir …»: solo muestra la parrilla, por lo que no es un fallo.
    aviso "pantalla Programas sin enlaces pulsables (solo parrilla de horarios)"
  else
    fallo "pantalla $T sin elementos pulsables"
  fi
done
[ "$N_BOT" -eq 9 ] && ok "los 9 enlaces están presentes en la app" || fallo "enlaces encontrados: $N_BOT (se esperaban 9)"
for N in "SITIO WEB" "SITIO WEB EN INGLÉS" "AUDIO REAL EN INTERNET" "PODCAST" "VIDEOS" "FACEBOOK" "X" "TELEGRAM" "INSTAGRAM"; do
  echo "$TODO" | grep -qx "Abrir $N" || fallo "falta el enlace: $N"
done
[ "$N_TEL" -eq 3 ] && ok "se muestran los 3 teléfonos" || fallo "teléfonos encontrados: $N_TEL (se esperaban 3)"
[ "$N_MAIL" -eq 1 ] && ok "se muestra el correo electrónico" || fallo "correo no encontrado"
echo "$TODO" | sed 's/^/         /' | tee -a "$LOG"

# 4) Cada elemento: abre algo, no se cierra la app y se puede volver
probar_toque() {  # $1 = pantalla, $2 = descripción
  adb shell am start -n "$ACT" >/dev/null 2>&1; sleep 1
  abrir_pantalla "$1" || { fallo "no se pudo abrir la pantalla $1"; return; }
  if ! tocar "$2"; then fallo "no se pudo localizar '$2' (pantalla $1)"; return; fi
  sleep 4
  local act; act=$(actividad)
  if echo "$act" | grep -q "MainActivity"; then
    aviso "'$2': la app siguió en primer plano (sin aplicación/navegador disponible o solo se mostró un aviso)"
  else
    ok "'$2' abrió: $(echo "$act" | grep -oE '[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+' | head -1)"
  fi
  hay_crash && fallo "cierre inesperado tras '$2'"
  adb shell input keyevent KEYCODE_BACK; sleep 2
  echo "$(actividad)" | grep -q "MainActivity" || { adb shell input keyevent KEYCODE_BACK; sleep 2; }
  if echo "$(actividad)" | grep -q "MainActivity"; then ok "retorno correcto a la app tras '$2'"; else aviso "tras '$2' hubo que reabrir la app manualmente"; fi
}
# (se lee por el descriptor 3 para que "adb shell" no consuma la lista por la entrada estándar)
LISTA="$(awk -F'|' '!visto[$2]++' "$OUT/mapa.txt")"
N_PROBADOS=0
while IFS='|' read -r pantalla desc <&3; do
  [ -n "$desc" ] || continue
  probar_toque "$pantalla" "$desc" < /dev/null
  N_PROBADOS=$((N_PROBADOS+1))
done 3<<< "$LISTA"
[ "$N_PROBADOS" -eq 13 ] && ok "se probaron los 13 elementos pulsables (9 enlaces, 3 teléfonos, correo)" || fallo "solo se probaron $N_PROBADOS de 13 elementos"

# 5) Ninguna llamada debe iniciarse sola
case "$(estado_llamada)" in
  *"mCallState=0"*|"") ok "ninguna llamada se inició automáticamente" ;;
  *) fallo "estado de llamada inesperado: $(estado_llamada)" ;;
esac

# 5b) Emisoras: tarjetas, selector por lugar y cambio de emisora (capturas para revisión visual)
adb shell am force-stop "$PKG"
adb shell am start -n "$ACT" --ei screen 1 >/dev/null 2>&1; sleep 6
if volcar | grep -q "^Emisoras de La Habana|"; then
  sz=$(tamano); sw=${sz%x*}; sh=${sz#*x}
  captura "$OUT/emisora_0_api$API.png"
  for K in 1 2 3; do
    adb shell input swipe $((sw*3/4)) $((sh*28/100)) $((sw/4)) $((sh*28/100)) 300; sleep 3
    captura "$OUT/emisora_${K}_api$API.png"
  done
  if tocar "Emisoras de México"; then
    sleep 3; captura "$OUT/lugar_mexico_api$API.png"
  else
    aviso "no se encontró el selector de México"
  fi
  if tocar "Emisoras de España"; then
    sleep 3; captura "$OUT/lugar_espana_api$API.png"
  else
    aviso "no se encontró el selector de España"
  fi
  if hay_crash; then fallo "cierre inesperado al cambiar de emisora"; else ok "cambio de emisora y selector por lugar sin cierres"; fi
else
  aviso "no se pudo abrir Escuchar para probar las emisoras"
fi

# 6) Pantalla pequeña y densa: todo debe seguir siendo accesible
adb shell wm size 480x800 >/dev/null 2>&1; adb shell wm density 240 >/dev/null 2>&1
adb shell am force-stop "$PKG"; adb shell am start -n "$ACT" >/dev/null 2>&1; sleep 3
volcar > /dev/null
if python3 tools/verificar_inicio.py "$OUT/ui.xml" | tee -a "$LOG"; then ok "Inicio completo en pantalla pequeña"; else fallo "Inicio pequeño recortado"; fi
adb shell settings put system font_scale 1.3 >/dev/null 2>&1
adb shell am force-stop "$PKG"; adb shell am start -n "$ACT" >/dev/null 2>&1; sleep 3
captura "$OUT/inicio_texto_grande_api$API.png"
volcar > /dev/null
if python3 tools/verificar_inicio.py "$OUT/ui.xml" | tee -a "$LOG"; then ok "Inicio completo con letra ampliada"; else fallo "Inicio con letra ampliada recortado"; fi
adb shell settings put system font_scale 1 >/dev/null 2>&1
adb shell am force-stop "$PKG"; adb shell am start -n "$ACT" >/dev/null 2>&1; sleep 3
inventario "$OUT/mapa_pequena.txt" pequena
PEQ=$(cut -d'|' -f2 "$OUT/mapa_pequena.txt" | sort -u)
[ "$(echo "$PEQ" | grep -c '^Abrir ')" -eq 9 ] && ok "pantalla pequeña (480x800): los 9 enlaces accesibles" || fallo "pantalla pequeña: faltan enlaces"
adb shell wm size reset >/dev/null 2>&1; adb shell wm density reset >/dev/null 2>&1

# 7) Animaciones: con las animaciones del sistema activadas, la pantalla debe cambiar sola
adb shell settings put global animator_duration_scale 1 >/dev/null 2>&1
adb shell am force-stop "$PKG"; adb shell am start -n "$ACT" >/dev/null 2>&1; sleep 8
# El emulador de CI puede ser lento: se compara una captura con otras posteriores (hasta 6 intentos)
captura "$OUT/animada_a_api$API.png"
MOVIDA=0
for i in 1 2 3 4 5 6; do
  sleep 2; captura "$OUT/animada_b_api$API.png"
  if ! cmp -s "$OUT/animada_a_api$API.png" "$OUT/animada_b_api$API.png"; then MOVIDA=1; break; fi
done
if [ "$MOVIDA" -eq 1 ]; then ok "las animaciones están en marcha (la pantalla cambia sola)"; else fallo "la pantalla no se mueve (animaciones sin efecto)"; fi
adb shell settings put global animator_duration_scale 0 >/dev/null 2>&1

# 8) Estabilidad general
if hay_crash; then fallo "se registró un cierre inesperado"; adb logcat -d -b crash | grep -A12 "Process: $PKG" | head -30 | tee -a "$LOG"; else ok "sin cierres inesperados durante toda la prueba"; fi
adb shell svc wifi enable >/dev/null 2>&1; adb shell svc data enable >/dev/null 2>&1

log ""
if [ "$FALLOS" -eq 0 ]; then log "RESULTADO API $API: todas las comprobaciones obligatorias pasaron."; else log "RESULTADO API $API: $FALLOS fallo(s)."; fi
exit "$FALLOS"
