#!/usr/bin/env bash
# Compila el APK release firmado y lo deja en dist/Radio_Enciclopedia.apk
# Requisitos: JDK 17+, Android SDK (ANDROID_HOME) con platforms;android-35 y build-tools;34.0.0,
# y la clave de firma (ver README: tools/crear_clave.sh).
set -euo pipefail
cd "$(dirname "$0")/.."
chmod +x gradlew
./gradlew --no-daemon clean assembleRelease
mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk dist/Radio_Enciclopedia.apk
echo
bash tools/verificar_apk.sh dist/Radio_Enciclopedia.apk
