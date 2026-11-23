#!/usr/bin/env bash
# Verifica el APK release: manifiesto, firma, alineación y tamaño.
# Uso: tools/verificar_apk.sh [ruta_apk]     (requiere ANDROID_HOME o ANDROID_SDK_ROOT)
set -u
APK="${1:-dist/Radio_Enciclopedia.apk}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
FALLOS=0
ok()    { echo "[OK]    $*"; }
fallo() { echo "[FALLO] $*"; FALLOS=$((FALLOS+1)); }

[ -f "$APK" ] || { echo "No existe $APK"; exit 2; }
[ -n "$SDK" ] || { echo "Define ANDROID_HOME"; exit 2; }
BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)
[ -n "$BT" ] || { echo "No hay build-tools en $SDK"; exit 2; }
echo "Herramientas: $BT"
echo "APK: $APK  ($(du -h "$APK" | cut -f1), $(stat -c%s "$APK") bytes)"
echo "SHA-256: $(sha256sum "$APK" | cut -d' ' -f1)"
echo

BADGING=$("$BT/aapt2" dump badging "$APK" 2>&1)
echo "$BADGING" | grep -E "^package:|^sdkVersion|^minSdkVersion|^targetSdkVersion|^launchable-activity|^application-label:" 
echo

echo "$BADGING" | grep -q "name='cu.radioenciclopedia.app'" && ok "identificador de paquete cu.radioenciclopedia.app" || fallo "paquete inesperado"
echo "$BADGING" | grep -qE "^(min)?[sS]dkVersion:'28'"      && ok "minSdk = 28 (Android 9)"                         || fallo "minSdk distinto de 28"
echo "$BADGING" | grep -q "^targetSdkVersion:'35'"          && ok "targetSdk = 35"                                  || fallo "targetSdk distinto de 35"
echo "$BADGING" | grep -q "launchable-activity: name='cu.radioenciclopedia.app.MainActivity'" && ok "actividad principal MainActivity" || fallo "actividad principal no encontrada"
PERMISOS=$(echo "$BADGING" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p" | sort)
ESPERADOS=$(cat <<'PERMISSION_LIST'
android.permission.FOREGROUND_SERVICE
android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
android.permission.INTERNET
android.permission.POST_NOTIFICATIONS
android.permission.WAKE_LOCK
PERMISSION_LIST
)
[ "$PERMISOS" = "$ESPERADOS" ] && ok "permisos de reproducción multimedia y notificación correctos" || fallo "permisos distintos de los previstos para el reproductor: $PERMISOS"
if echo "$BADGING" | grep -q "native-code"; then echo "[INFO]  incluye código nativo (inesperado)"; else ok "sin código nativo (universal para cualquier ABI)"; fi

SIG=$("$BT/apksigner" verify --verbose --print-certs "$APK" 2>&1)
echo "$SIG" | head -12
echo "$SIG" | grep -q "Verifies" && ok "firma válida" || fallo "firma NO válida"
# Con minSdk 28 (Android 9) basta la firma v3; v2 es opcional.
echo "$SIG" | grep -qE "APK Signature Scheme v(2|3)\): true" && ok "firma v2/v3 presente" || fallo "falta firma v2/v3"
echo "$SIG" | grep -qi "Android Debug" && fallo "firmado con la clave de DEPURACIÓN" || ok "no usa la clave de depuración"

"$BT/zipalign" -c 4 "$APK" && ok "alineación zip (4 bytes)" || fallo "APK mal alineado"

echo
[ "$FALLOS" -eq 0 ] && echo "RESULTADO: todas las comprobaciones pasaron." || echo "RESULTADO: $FALLOS comprobación(es) fallida(s)."
exit "$FALLOS"
