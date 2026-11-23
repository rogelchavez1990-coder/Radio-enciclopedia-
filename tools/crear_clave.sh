#!/usr/bin/env bash
# Crea UNA SOLA VEZ la clave de firma release de Radio Enciclopedia.
# Requiere JDK (keytool). Guarda el archivo .jks y la contraseña en un lugar seguro:
# si los pierdes, no podrás publicar actualizaciones que se instalen sobre la app existente.
set -euo pipefail

ALIAS="radioenciclopedia"
KS="radio-release.jks"

if [ -e "$KS" ]; then
  echo "Ya existe $KS. No se sobrescribe (una clave nueva rompería las actualizaciones)." >&2
  exit 1
fi

read -r -s -p "Contraseña para la clave (mínimo 8 caracteres): " PASS; echo
read -r -s -p "Repite la contraseña: " PASS2; echo
[ "$PASS" = "$PASS2" ] || { echo "Las contraseñas no coinciden." >&2; exit 1; }
[ "${#PASS}" -ge 8 ] || { echo "Mínimo 8 caracteres." >&2; exit 1; }

keytool -genkeypair -v \
  -keystore "$KS" -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 36500 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=Radio Enciclopedia, O=Radio Enciclopedia, L=La Habana, C=CU"

# Para compilar en tu PC (keystore.properties está en .gitignore)
cat > keystore.properties <<EOP
storeFile=$KS
storePassword=$PASS
keyAlias=$ALIAS
keyPassword=$PASS
EOP

# Para GitHub Actions: el contenido de este archivo va en el secreto KEYSTORE_BASE64
base64 -w0 "$KS" > "$KS.b64.txt" 2>/dev/null || base64 "$KS" | tr -d '\n' > "$KS.b64.txt"

echo
echo "Listo. Se crearon: $KS, keystore.properties y $KS.b64.txt"
echo "Secretos de GitHub a crear: KEYSTORE_BASE64 (contenido de $KS.b64.txt), KEYSTORE_PASSWORD, KEY_ALIAS=$ALIAS, KEY_PASSWORD"
echo "GUARDA UNA COPIA de $KS y de la contraseña fuera del repositorio."
