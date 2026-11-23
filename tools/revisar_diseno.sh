#!/usr/bin/env bash
set -euo pipefail
OUT="${2:-diseno-1.6}"
source tools/android_ui.sh
trap ui_restore EXIT
adb install -r "${1:-dist/Radio_Enciclopedia.apk}"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell wm size 720x1600
adb shell wm density 280
adb shell settings put system font_scale 1
adb shell settings put global animator_duration_scale 0
adb logcat -c
ui_open 0
python3 tools/verificar_inicio.py "$OUT/ui.xml"
ui_photo inicio
cp "$OUT/ui.xml" "$OUT/inicio.xml"

# La nueva franja debe verse en movimiento, también con el audio detenido.
adb shell settings put global animator_duration_scale 1
adb shell am force-stop "$PKG"
ui_open 0
sleep 2
ui_photo notas_a
sleep 2
ui_photo notas_b
if timeout 25 adb shell screenrecord --time-limit 6 --bit-rate 1500000 /sdcard/notas-carrusel.mp4; then
  adb pull /sdcard/notas-carrusel.mp4 "$OUT/notas-carrusel.mp4" >/dev/null
else
  echo '[AVISO] El emulador no pudo grabar vídeo; se conservan los fotogramas de la franja.'
fi
adb shell settings put global animator_duration_scale 0

if [ "${RADIO_ONLY_HOME:-0}" != 1 ]; then
ui_touch 'Abrir menú'
ui_photo menu
ui_touch 'Ir a Frecuencias'
ui_photo frecuencias
: > "$OUT/frecuencias-visibles.txt"
for pass in 1 2 3 4 5 6; do
  ui_dump
  python3 tools/ui_nodos.py "$OUT/ui.xml" >> "$OUT/frecuencias-visibles.txt"
  adb shell input swipe 360 1100 360 400 220
done
python3 - "$OUT/frecuencias-visibles.txt" <<'PY'
import sys
s=open(sys.argv[1]).read()
expected={'Pinar del Río':'102,7','La Habana':'94,1','Artemisa':'100,7','Matanzas':'91,1 y 106,3','Cienfuegos':'90,3','Villa Clara':'92,5 y 102,3','Sancti Spíritus':'91,1','Ciego de Ávila':'95,3','Camagüey':'100,5','Las Tunas':'101,1','Holguín':'102,7','Granma':'93,7 y 94,1','Santiago de Cuba':'98,1','Guantánamo':'96,3 y 97,1','Isla de la Juventud':'92,7'}
for province,freq in expected.items():assert province+': '+freq+' MHz' in s,province
print('Las 15 entradas FM y sus frecuencias están accesibles.')
PY
ui_touch 'Ir a Programas'
ui_touch 'Ver lista de horarios'
ui_photo horarios
adb shell input keyevent KEYCODE_BACK
ui_touch 'Mostrar programación: Domingo'
ui_photo domingo
for pass in 1 2 3 4 5; do adb shell input swipe 360 1200 360 350 220; done
ui_dump
ui_photo programas_final
cp "$OUT/ui.xml" "$OUT/programas_final.xml"
python3 - "$OUT/ui.xml" <<'PY'
import sys,xml.etree.ElementTree as E
descs={n.get('content-desc','') for n in E.parse(sys.argv[1]).getroot().iter('node')}
removed={'Abrir PODCAST','Abrir VIDEOS','Abrir SITIO WEB EN INGLÉS','Ver programación oficial'}
assert not descs.intersection(removed), ('Siguen presentes los accesos retirados de Programas',descs.intersection(removed))
assert any(d.startswith('Ver programa:') for d in descs), 'No se muestra la lista de programas'
print('Programas: retirados los cuatro accesos finales de la captura.')
PY
ui_touch 'Ir a Inicio'
ui_touch 'Ir a la sección Comunidad'
ui_photo comunidad
ui_dump
python3 - "$OUT/ui.xml" <<'PY'
import sys,xml.etree.ElementTree as E
s=' '.join((n.get('text','')+' '+n.get('content-desc','')) for n in E.parse(sys.argv[1]).getroot().iter('node'))
assert 'Ser culto es el único modo de ser libre' in s
assert 'Retrato de José Martí' in s
nodes=list(E.parse(sys.argv[1]).getroot().iter('node'))
quote=next(n for n in nodes if 'Ser culto es el único modo de ser libre' in n.get('text',''))
portrait=next(n for n in nodes if n.get('content-desc')=='Retrato de José Martí')
import re
q=list(map(int,re.findall(r'\d+',quote.get('bounds',''))))
p=list(map(int,re.findall(r'\d+',portrait.get('bounds',''))))
assert q[2] <= p[0], ('La cita debe quedar a la izquierda del retrato',q,p)
assert max(q[1],p[1]) < min(q[3],p[3]), ('Texto y retrato deben compartir fila',q,p)
print('Comunidad: cita a la izquierda y retrato de Martí a la derecha.')
PY
ui_touch 'Ir a Contacto'
ui_photo contacto
ui_dump
python3 - "$OUT/ui.xml" <<'PY'
import sys,xml.etree.ElementTree as E
s=' '.join(n.get('text','') for n in E.parse(sys.argv[1]).getroot().iter('node'))
assert 'Edificio ENE, calle N número 266, entre 21 y 23, Vedado, La Habana.' in s
print('Dirección corregida visible.')
PY
fi

adb shell settings put system font_scale 1.3
adb shell am force-stop "$PKG"
ui_open 0
python3 tools/verificar_inicio.py "$OUT/ui.xml"
ui_photo texto_grande
adb shell wm size 480x800
adb shell wm density 240
adb shell am force-stop "$PKG"
ui_open 0
python3 tools/verificar_inicio.py "$OUT/ui.xml"
ui_photo pequeno_texto_grande
adb shell settings put system font_scale 1
adb shell wm size 800x480
adb shell am force-stop "$PKG"
ui_open 0
python3 tools/verificar_inicio.py "$OUT/ui.xml"
ui_photo horizontal
adb logcat -d -b crash > "$OUT/crashes.txt"
if grep -q "Process: $PKG" "$OUT/crashes.txt"; then exit 1; fi
echo 'Diseño: comprobaciones aprobadas.'
