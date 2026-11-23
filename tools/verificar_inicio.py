#!/usr/bin/env python3
"""Comprueba los accesos de Inicio dentro del área visible y encima de la navegación."""
import sys
import re
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
nodes = list(root.iter('node'))
nav_names = ['Ir a Inicio', 'Ir a Escuchar', 'Ir a Programas', 'Ir a Contacto']
home_names = [
    'Abrir menú', 'Reproductor de Radio Enciclopedia',
    'Ir a la sección Programas', 'Ver frecuencias nacionales',
    'Abrir VIDEOS', 'Ir a la sección Comunidad',
    'Abrir PODCAST', 'Abrir SITIO WEB', 'Abrir SITIO WEB EN INGLÉS',
    'Abrir FACEBOOK', 'Abrir INSTAGRAM', 'Abrir TELEGRAM', 'Abrir X',
    'Enviar un correo a radio.enciclo.62@gmail.com',
]

def bounds(desc):
    matches = [n for n in nodes if n.get('content-desc') == desc]
    if not matches:
        raise AssertionError('No está visible: ' + desc)
    rect = list(map(int, re.findall(r'\d+', matches[0].get('bounds', ''))))
    assert len(rect) == 4, ('Sin límites', desc)
    assert rect[2] > rect[0] and rect[3] > rect[1], ('Área vacía', desc)
    return rect

assert not any(n.get('class') == 'android.widget.ScrollView' for n in nodes), 'Inicio contiene un ScrollView'
assert not any('Ser culto es el único modo de ser libre' in n.get('text', '') or n.get('content-desc') == 'Retrato de José Martí' for n in nodes), 'La cita y el retrato de Martí deben estar en Comunidad'
nav_top = min(bounds(n)[1] for n in nav_names)
for name in home_names:
    rect = bounds(name)
    assert rect[1] >= 0 and rect[3] <= nav_top, ('Acceso recortado por la navegación', name, rect, nav_top)
print(f'Inicio: los {len(home_names)} accesos y las 4 pestañas están visibles, sin desplazamiento ni solapamiento.')
