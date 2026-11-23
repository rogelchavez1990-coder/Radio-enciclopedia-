#!/usr/bin/env python3
"""Lee un volcado de uiautomator y escribe: descripcion|x|y|clickable (centro de cada nodo con content-desc)."""
import re
import sys
import xml.etree.ElementTree as ET

try:
    raiz = ET.parse(sys.argv[1]).getroot()
except Exception:
    sys.exit(0)
for n in raiz.iter("node"):
    desc = n.attrib.get("content-desc", "")
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.attrib.get("bounds", ""))
    if desc and m:
        l, t, r, b = map(int, m.groups())
        print(f"{desc}|{(l + r) // 2}|{(t + b) // 2}|{n.attrib.get('clickable', '')}")
