#!/usr/bin/env python3
"""
Genera el arte del nuevo diseño de Radio Enciclopedia (skyline nocturno de La Habana, Capitolio,
micrófono dorado, anillo, bandera, iconos dorados) como SVG y lo renderiza a WebP/PNG con Chromium.

Uso:  python3 tools/generar_arte.py [--vista]
Requiere: playwright (Chromium) y Pillow. Los archivos resultantes ya están en el proyecto;
este script solo sirve para regenerarlos o modificarlos.
"""
import os
import random
import sys
from PIL import Image
from playwright.sync_api import sync_playwright

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, "..", "app", "src", "main", "res", "drawable-nodpi")
PREV = os.environ.get("ARTE_PREVIEW", "/tmp/arte_prev")
os.makedirs(RES, exist_ok=True)
os.makedirs(PREV, exist_ok=True)

LIT = []   # ventanas encendidas (se dibujan dos veces: nítidas y con resplandor)


# ----------------------------------------------------------------------------- utilidades
def ventanas(rng, x0, y0, x1, y1, sx=24, sy=32, ww=10, wh=16, p=0.6, arco=False):
    oscuras = []
    y = y0
    while y + wh <= y1:
        x = x0
        while x + ww <= x1:
            if rng.random() < p:
                col = rng.choice(["#ffd27a", "#ffc15e", "#ffe3a0", "#ffb457"])
                r = 'rx="5"' if arco else 'rx="2"'
                LIT.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{ww}" height="{wh}" {r} fill="{col}"/>')
            else:
                oscuras.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{ww}" height="{wh}" rx="2" fill="#3a1d58"/>')
            x += sx
        y += sy
    return "".join(oscuras)


def palma(x, base, alto, inclina, esc=1.0, color="#12062b"):
    tx, ty = x + inclina, base - alto
    tronco = (f'<path d="M{x-5*esc} {base} C{x+inclina*0.2-4*esc} {base-alto*0.4} '
              f'{x+inclina*0.8-3*esc} {base-alto*0.75} {tx} {ty} '
              f'C{x+inclina*0.8+3*esc} {base-alto*0.75} {x+inclina*0.2+5*esc} {base-alto*0.4} {x+5*esc} {base} Z" '
              f'fill="{color}"/>')
    hojas = ""
    for ang, largo in [(-165, 1.0), (-140, 1.1), (-112, 1.0), (-85, 0.8), (-60, 1.0),
                       (-32, 1.1), (-8, 1.0), (14, 0.85), (-190, 0.85), (35, 0.7)]:
        L = 120 * esc * largo
        hojas += (f'<g transform="translate({tx} {ty}) rotate({ang})">'
                  f'<path d="M0 0 C{L*0.25} {-L*0.22} {L*0.7} {-L*0.26} {L} {L*0.12} '
                  f'C{L*0.7} {-L*0.02} {L*0.3} {L*0.05} 0 0 Z" fill="{color}"/></g>')
    return tronco + hojas


def capitolio(cx, base, esc=1.0, ids=""):
    """Capitolio de La Habana: cuerpo con columnata, frontón, tambor, cúpula dorada y linterna."""
    g = [f'<g transform="translate({cx} {base}) scale({esc}) translate({-cx} {-base})">']
    # plataforma y cuerpo
    g.append(f'<rect x="{cx-240}" y="{base-40}" width="480" height="48" fill="url(#piedra)"/>')
    g.append(f'<rect x="{cx-200}" y="{base-125}" width="400" height="90" fill="url(#piedra)"/>')
    # columnata luminosa
    for i in range(15):
        x = cx - 190 + i * 27
        g.append(f'<rect x="{x}" y="{base-118}" width="9" height="78" fill="#ffd9a0" opacity="0.92"/>')
    g.append(f'<rect x="{cx-205}" y="{base-130}" width="410" height="9" fill="#f7c978"/>')
    # frontón
    g.append(f'<polygon points="{cx-95},{base-130} {cx},{base-162} {cx+95},{base-130}" fill="#6a3a7c"/>')
    g.append(f'<polygon points="{cx-80},{base-134} {cx},{base-158} {cx+80},{base-134}" fill="#3a1b4f"/>')
    # segundo cuerpo
    g.append(f'<rect x="{cx-110}" y="{base-195}" width="220" height="42" fill="url(#piedra)"/>')
    for i in range(9):
        g.append(f'<rect x="{cx-100+i*24}" y="{base-188}" width="8" height="30" fill="#ffd9a0" opacity="0.85"/>')
    g.append(f'<rect x="{cx-118}" y="{base-200}" width="236" height="8" fill="#f7c978"/>')
    # tambor con columnas
    g.append(f'<rect x="{cx-82}" y="{base-262}" width="164" height="64" fill="url(#piedra)"/>')
    for i in range(13):
        g.append(f'<rect x="{cx-76+i*12.5}" y="{base-256}" width="5" height="50" fill="#ffd9a0" opacity="0.9"/>')
    g.append(f'<rect x="{cx-90}" y="{base-268}" width="180" height="9" fill="#f7c978"/>')
    # cúpula (dorada, iluminada)
    cupula = (f'M{cx-82} {base-268} C{cx-82} {base-360} {cx-40} {base-410} {cx} {base-410} '
              f'C{cx+40} {base-410} {cx+82} {base-360} {cx+82} {base-268} Z')
    g.append(f'<path d="{cupula}" fill="#ffc45a" opacity="0.55" filter="url(#brillo)"/>')
    g.append(f'<path d="{cupula}" fill="url(#oro)"/>')
    for k in range(-3, 4):
        g.append(f'<path d="M{cx+k*24} {base-268} Q{cx+k*14} {base-360} {cx} {base-408}" '
                 f'stroke="#7a3d12" stroke-opacity="0.35" stroke-width="2.2" fill="none"/>')
    g.append(f'<path d="M{cx-70} {base-300} Q{cx} {base-318} {cx+70} {base-300}" stroke="#fff4cc" '
             f'stroke-opacity="0.5" stroke-width="3" fill="none"/>')
    # linterna y aguja
    g.append(f'<rect x="{cx-13}" y="{base-446}" width="26" height="40" fill="url(#oro)"/>')
    for i in range(4):
        g.append(f'<rect x="{cx-10+i*6.5}" y="{base-440}" width="3" height="28" fill="#fff0c0"/>')
    g.append(f'<path d="M{cx-15} {base-446} C{cx-15} {base-472} {cx+15} {base-472} {cx+15} {base-446} Z" fill="url(#oro)"/>')
    g.append(f'<rect x="{cx-1.3}" y="{base-505}" width="2.6" height="38" fill="#f7c978"/>')
    g.append(f'<circle cx="{cx}" cy="{base-507}" r="4" fill="#fff4cc"/>')
    g.append("</g>")
    return "".join(g)


DEFS_COMUNES = """
<linearGradient id="piedra" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#7a4a86"/><stop offset="0.5" stop-color="#4b2663"/><stop offset="1" stop-color="#2d1445"/>
</linearGradient>
<linearGradient id="oro" x1="0.15" y1="0" x2="0.85" y2="1">
  <stop offset="0" stop-color="#fff3c4"/><stop offset="0.45" stop-color="#f5c04a"/><stop offset="1" stop-color="#b8741a"/>
</linearGradient>
<linearGradient id="edif" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#5d3270"/><stop offset="1" stop-color="#26113c"/>
</linearGradient>
<filter id="brillo" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="16"/></filter>
<filter id="brillo2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="5"/></filter>
"""


# ----------------------------------------------------------------------------- skyline nocturno
def skyline_noche():
    LIT.clear()
    rng = random.Random(7)
    W, H = 1440, 900
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs>{DEFS_COMUNES}']
    s.append("""
<linearGradient id="cielo" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#1b1466" stop-opacity="0"/>
  <stop offset="0.22" stop-color="#2a2796" stop-opacity="0.5"/>
  <stop offset="0.5" stop-color="#7d3bb0" stop-opacity="0.85"/>
  <stop offset="0.7" stop-color="#e2569f" stop-opacity="0.95"/>
  <stop offset="0.84" stop-color="#ff9a5c"/>
  <stop offset="1" stop-color="#ffbb74"/>
</linearGradient>
<radialGradient id="horizonte" cx="0.5" cy="0.5" r="0.5">
  <stop offset="0" stop-color="#ffd08a" stop-opacity="0.85"/><stop offset="1" stop-color="#ffd08a" stop-opacity="0"/>
</radialGradient>
<linearGradient id="fundido" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#fff"/><stop offset="0.86" stop-color="#fff"/><stop offset="1" stop-color="#000"/>
</linearGradient>
<mask id="m"><rect width="1440" height="900" fill="url(#fundido)"/></mask>
</defs><g mask="url(#m)">""")
    s.append('<rect width="1440" height="840" fill="url(#cielo)"/>')
    s.append('<ellipse cx="720" cy="800" rx="900" ry="230" fill="url(#horizonte)"/>')

    # capa lejana
    lej = []
    x = -20
    while x < 1460:
        w = rng.randint(40, 90)
        h = rng.randint(50, 130)
        lej.append(f'<rect x="{x}" y="{790-h}" width="{w}" height="{h+60}" fill="#4a2370" opacity="0.65"/>')
        x += w + rng.randint(0, 14)
    s.append("".join(lej))

    # cuerpo medio: edificios variados
    med = []
    x = 540
    while x < 900:
        w = rng.randint(56, 96)
        h = rng.randint(80, 150)
        y0 = 800 - h
        med.append(f'<rect x="{x}" y="{y0}" width="{w}" height="{h+40}" fill="url(#edif)"/>')
        med.append(f'<rect x="{x-3}" y="{y0-5}" width="{w+6}" height="7" fill="#7a4a86"/>')
        med.append(ventanas(rng, x + 8, y0 + 12, x + w - 8, 790, sx=22, sy=30, p=0.6))
        x += w + rng.randint(2, 10)
    s.append("".join(med))

    # iglesia con torre y cúpula
    s.append('<rect x="610" y="560" width="74" height="270" fill="url(#edif)"/>')
    s.append('<rect x="604" y="552" width="86" height="12" fill="#8a5a96"/>')
    s.append(ventanas(rng, 624, 590, 672, 760, sx=24, sy=44, ww=14, wh=24, p=0.7, arco=True))
    s.append('<path d="M612 552 C612 505 647 488 647 488 C647 488 682 505 682 552 Z" fill="url(#oro)" opacity="0.95"/>')
    s.append('<rect x="645" y="460" width="4" height="32" fill="#f7c978"/><rect x="636" y="468" width="22" height="4" fill="#f7c978"/>')

    # teatro (izquierda): cuerpo con dos torres
    s.append('<rect x="60" y="690" width="520" height="150" fill="url(#edif)"/>')
    s.append('<rect x="50" y="680" width="540" height="12" fill="#9a6aa6"/>')
    s.append('<rect x="50" y="670" width="540" height="6" fill="#7a4a86"/>')
    for i in range(16):
        s.append(f'<rect x="{90+i*29}" y="664" width="6" height="14" fill="#8a5a96"/>')   # balaustrada
    s.append(ventanas(rng, 160, 706, 480, 820, sx=32, sy=48, ww=16, wh=30, p=0.72, arco=True))
    for tx in (70, 470):
        s.append(f'<rect x="{tx}" y="590" width="100" height="250" fill="url(#edif)"/>')
        s.append(f'<rect x="{tx-6}" y="580" width="112" height="14" fill="#9a6aa6"/>')
        s.append(ventanas(rng, tx + 18, 616, tx + 84, 800, sx=34, sy=50, ww=16, wh=30, p=0.75, arco=True))
        s.append(f'<path d="M{tx-2} 580 C{tx-2} 520 {tx+50} 500 {tx+50} 500 C{tx+50} 500 {tx+102} 520 {tx+102} 580 Z" fill="url(#oro)" opacity="0.95"/>')
        s.append(f'<rect x="{tx+48}" y="468" width="4" height="36" fill="#f7c978"/><circle cx="{tx+50}" cy="465" r="5" fill="#fff0c0"/>')

    # Capitolio
    s.append(capitolio(1140, 830, 1.0))
    # bloques laterales junto al Capitolio
    s.append('<rect x="900" y="735" width="120" height="105" fill="url(#edif)"/>')
    s.append(ventanas(rng, 912, 745, 1010, 830, sx=24, sy=34, p=0.65))
    s.append('<rect x="1330" y="740" width="120" height="100" fill="url(#edif)"/>')
    s.append(ventanas(rng, 1342, 750, 1440, 830, sx=24, sy=34, p=0.65))

    # ventanas luminosas: nítidas + resplandor
    lit = "".join(LIT)
    s.append(f'<g filter="url(#brillo2)" opacity="0.8">{lit}</g>')
    s.append(f'<g>{lit}</g>')

    # suelo oscuro y árboles
    s.append('<rect x="0" y="812" width="1440" height="120" fill="#16072e"/>')
    s.append('<path d="M0 820 Q180 800 360 820 T720 818 T1080 820 T1440 812 L1440 900 L0 900 Z" fill="#1a0a36"/>')
    # luces de farola
    for fx in (210, 470, 760, 980, 1290):
        s.append(f'<circle cx="{fx}" cy="826" r="26" fill="#ffc15e" opacity="0.35" filter="url(#brillo2)"/>')
        s.append(f'<circle cx="{fx}" cy="826" r="4.5" fill="#fff0c0"/>')
    # palmeras
    s.append(palma(40, 840, 360, 38, 1.15))
    s.append(palma(150, 846, 250, -26, 0.9))
    s.append(palma(1010, 846, 230, 30, 0.8))
    s.append(palma(1395, 846, 400, -44, 1.2))
    s.append(palma(1300, 850, 270, 28, 0.9))
    s.append("</g></svg>")
    return "".join(s), W, H


# ----------------------------------------------------------------------------- banner atardecer
def banner_atardecer():
    LIT.clear()
    rng = random.Random(11)
    W, H = 1440, 600
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}"><defs>{DEFS_COMUNES}']
    s.append("""
<linearGradient id="cielo" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#2a0f5a"/><stop offset="0.35" stop-color="#7a2fa8"/>
  <stop offset="0.62" stop-color="#d94a9e"/><stop offset="0.82" stop-color="#ff8a5a"/><stop offset="1" stop-color="#ffc06e"/>
</linearGradient>
<radialGradient id="sol" cx="0.5" cy="0.5" r="0.5">
  <stop offset="0" stop-color="#ffe2a0" stop-opacity="0.95"/><stop offset="1" stop-color="#ffb060" stop-opacity="0"/>
</radialGradient>
</defs>""")
    s.append(f'<rect width="{W}" height="{H}" fill="url(#cielo)"/>')
    s.append('<ellipse cx="520" cy="470" rx="640" ry="210" fill="url(#sol)"/>')
    # estrellas fijas
    for _ in range(26):
        s.append(f'<circle cx="{rng.randint(10,1430)}" cy="{rng.randint(10,230)}" r="{rng.choice([1.2,1.6,2.0])}" fill="#fff" opacity="{rng.uniform(.3,.8):.2f}"/>')
    # fondo de edificios bajos
    x = -10
    while x < 1450:
        w = rng.randint(50, 110)
        h = rng.randint(40, 90)
        s.append(f'<rect x="{x}" y="{480-h}" width="{w}" height="{h+80}" fill="#4a1e6c" opacity="0.8"/>')
        x += w + rng.randint(0, 10)
    s.append(capitolio(480, 520, 0.95))
    LIT_HTML = "".join(LIT)
    s.append(f'<g>{LIT_HTML}</g>')
    s.append('<path d="M0 500 Q300 484 600 500 T1200 498 T1440 490 L1440 600 L0 600 Z" fill="#1c0a38"/>')
    s.append(palma(60, 520, 300, 34, 1.1))
    s.append(palma(190, 526, 210, -22, 0.85))
    s.append(palma(1010, 526, 230, 26, 0.9))
    s.append(palma(1140, 530, 170, -18, 0.75))
    s.append("</svg>")
    return "".join(s), W, H


# ----------------------------------------------------------------------------- micrófono, anillo, bandera
def microfono():
    s = ["""<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"><defs>
<linearGradient id="oro" x1="0.1" y1="0" x2="0.9" y2="1">
  <stop offset="0" stop-color="#fff6cf"/><stop offset="0.35" stop-color="#f8cd5a"/><stop offset="0.7" stop-color="#d9931f"/><stop offset="1" stop-color="#8f560f"/>
</linearGradient>
<linearGradient id="orov" x1="0" y1="0" x2="1" y2="0">
  <stop offset="0" stop-color="#8f560f"/><stop offset="0.28" stop-color="#ffe9a0"/><stop offset="0.55" stop-color="#e9ad33"/><stop offset="1" stop-color="#8f560f"/>
</linearGradient>
<radialGradient id="aura" cx="0.5" cy="0.45" r="0.5">
  <stop offset="0" stop-color="#ffc85a" stop-opacity="0.55"/><stop offset="1" stop-color="#ffc85a" stop-opacity="0"/>
</radialGradient>
<clipPath id="cap"><rect x="186" y="56" width="140" height="236" rx="70"/></clipPath>
<filter id="bl" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="3"/></filter>
</defs>
<circle cx="256" cy="230" r="230" fill="url(#aura)"/>
<path d="M146 226 C146 352 366 352 366 226" stroke="url(#orov)" stroke-width="20" fill="none" stroke-linecap="round"/>
<rect x="246" y="326" width="20" height="92" fill="url(#orov)"/>
<path d="M170 452 Q256 400 342 452 L342 462 Q256 478 170 462 Z" fill="url(#oro)"/>
<ellipse cx="256" cy="462" rx="92" ry="12" fill="#8f560f" opacity="0.6"/>
<rect x="186" y="56" width="140" height="236" rx="70" fill="url(#oro)"/>
<g clip-path="url(#cap)" opacity="0.55" stroke="#6b3d0a" stroke-width="3">""",
         "".join(f'<line x1="170" y1="{y}" x2="342" y2="{y}"/>' for y in range(76, 300, 17)),
         "".join(f'<line x1="{x}" y1="40" x2="{x}" y2="300"/>' for x in range(196, 330, 17)),
         """</g>
<rect x="176" y="156" width="160" height="26" rx="8" fill="url(#orov)"/>
<rect x="176" y="156" width="160" height="5" rx="3" fill="#fff6cf" opacity="0.7"/>
<path d="M206 84 C206 70 222 62 236 62" stroke="#fff" stroke-opacity="0.8" stroke-width="7" stroke-linecap="round" fill="none" filter="url(#bl)"/>
</svg>"""]
    return "".join(s), 512, 512


def anillo():
    s = """<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"><defs>
<linearGradient id="g" x1="0" y1="1" x2="1" y2="0">
  <stop offset="0" stop-color="#ffd86b"/><stop offset="0.35" stop-color="#ff7ad9"/><stop offset="0.75" stop-color="#9a4dff"/><stop offset="1" stop-color="#5b2fe0" stop-opacity="0.2"/>
</linearGradient>
<filter id="bl" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="6"/></filter>
</defs>
<path id="c" d="M56 330 C30 190 130 52 290 44 C410 40 486 120 470 196 C440 90 330 70 230 104 C130 140 92 240 108 330 C112 420 190 456 276 448 C190 486 80 450 56 330 Z" fill="url(#g)" opacity="0.55" filter="url(#bl)"/>
<path d="M56 330 C30 190 130 52 290 44 C410 40 486 120 470 196 C440 90 330 70 230 104 C130 140 92 240 108 330 C112 420 190 456 276 448 C190 486 80 450 56 330 Z" fill="url(#g)"/>
</svg>"""
    return s, 512, 512


def bandera():
    estrella = "M0 -78 L18.4 -25.3 L74.2 -24 L29.8 9.7 L45.8 63.1 L0 31.3 L-45.8 63.1 L-29.8 9.7 L-74.2 -24 L-18.4 -25.3 Z"
    franjas = "".join(
        f'<rect x="0" y="{i*60}" width="600" height="60" fill="{"#0b2f9f" if i%2==0 else "#ffffff"}"/>' for i in range(5))
    s = f"""<svg xmlns="http://www.w3.org/2000/svg" width="600" height="300" viewBox="0 0 600 300">
{franjas}
<polygon points="0,0 260,150 0,300" fill="#cf142b"/>
<g transform="translate(88 150) scale(0.82)"><path d="{estrella}" fill="#fff"/></g>
</svg>"""
    return s, 600, 300


# ----------------------------------------------------------------------------- iconos dorados
GRAD_ORO = """<defs><linearGradient id="o" x1="0.1" y1="0" x2="0.9" y2="1">
<stop offset="0" stop-color="#fff3c0"/><stop offset="0.5" stop-color="#f6c453"/><stop offset="1" stop-color="#c98616"/></linearGradient></defs>"""


def icono(nombre, cuerpo):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128">'
            f'{GRAD_ORO}{cuerpo}</svg>'), 128, 128


def iconos():
    d = {}
    d["ic_gold_radio"] = icono("radio", """
<line x1="34" y1="36" x2="92" y2="10" stroke="url(#o)" stroke-width="6" stroke-linecap="round"/>
<rect x="10" y="34" width="108" height="76" rx="14" fill="url(#o)"/>
<rect x="18" y="42" width="92" height="60" rx="9" fill="#2a0b4d" opacity="0.9"/>
<circle cx="42" cy="72" r="19" fill="url(#o)"/><circle cx="42" cy="72" r="11" fill="#2a0b4d"/><circle cx="42" cy="72" r="4" fill="url(#o)"/>
<rect x="70" y="52" width="32" height="7" rx="3.5" fill="url(#o)"/>
<rect x="70" y="67" width="32" height="5" rx="2.5" fill="url(#o)" opacity="0.8"/>
<rect x="70" y="79" width="32" height="5" rx="2.5" fill="url(#o)" opacity="0.8"/>
<rect x="70" y="91" width="20" height="5" rx="2.5" fill="url(#o)" opacity="0.8"/>""")
    d["ic_gold_audio"] = icono("audio", """
<path d="M22 78 V64 C22 38 40 20 64 20 C88 20 106 38 106 64 V78" stroke="url(#o)" stroke-width="9" fill="none" stroke-linecap="round"/>
<rect x="14" y="68" width="26" height="42" rx="11" fill="url(#o)"/>
<rect x="88" y="68" width="26" height="42" rx="11" fill="url(#o)"/>
<rect x="20" y="76" width="9" height="26" rx="4.5" fill="#2a0b4d" opacity="0.55"/>
<rect x="99" y="76" width="9" height="26" rx="4.5" fill="#2a0b4d" opacity="0.55"/>""")
    d["ic_gold_comunidad"] = icono("comunidad", """
<circle cx="64" cy="40" r="18" fill="url(#o)"/>
<path d="M30 104 C30 80 46 66 64 66 C82 66 98 80 98 104 Z" fill="url(#o)"/>
<circle cx="28" cy="52" r="13" fill="url(#o)" opacity="0.85"/>
<path d="M4 100 C4 82 14 72 28 72 C32 72 35 73 38 74 C30 80 26 90 26 100 Z" fill="url(#o)" opacity="0.85"/>
<circle cx="100" cy="52" r="13" fill="url(#o)" opacity="0.85"/>
<path d="M124 100 C124 82 114 72 100 72 C96 72 93 73 90 74 C98 80 102 90 102 100 Z" fill="url(#o)" opacity="0.85"/>""")
    d["ic_gold_play"] = icono("play", """
<circle cx="64" cy="64" r="54" fill="none" stroke="url(#o)" stroke-width="8"/>
<polygon points="50,36 50,92 94,64" fill="url(#o)"/>""")
    d["ic_gold_web"] = icono("web", """
<circle cx="64" cy="64" r="48" fill="none" stroke="url(#o)" stroke-width="8"/>
<ellipse cx="64" cy="64" rx="22" ry="48" fill="none" stroke="url(#o)" stroke-width="6"/>
<line x1="16" y1="64" x2="112" y2="64" stroke="url(#o)" stroke-width="6"/>
<path d="M24 38 Q64 52 104 38" stroke="url(#o)" stroke-width="5" fill="none"/>
<path d="M24 90 Q64 76 104 90" stroke="url(#o)" stroke-width="5" fill="none"/>""")
    d["ic_gold_podcast"] = icono("podcast", """
<circle cx="64" cy="38" r="16" fill="url(#o)"/>
<rect x="50" y="60" width="28" height="58" rx="8" fill="url(#o)"/>
<path d="M30 52 C20 66 20 86 30 100" stroke="url(#o)" stroke-width="7" fill="none" stroke-linecap="round" opacity="0.8"/>
<path d="M98 52 C108 66 108 86 98 100" stroke="url(#o)" stroke-width="7" fill="none" stroke-linecap="round" opacity="0.8"/>""")
    return d


# ----------------------------------------------------------------------------- render
def renderizar(pw_page, svg, w, h, escala=1.0):
    html = ("<html><body style='margin:0;background:transparent'>" + svg + "</body></html>")
    pw_page.set_viewport_size({"width": w, "height": h})
    pw_page.set_content(html)
    pw_page.wait_for_timeout(120)
    return pw_page.screenshot(omit_background=True, clip={"x": 0, "y": 0, "width": w, "height": h})


def guardar(datos, nombre, formato="webp", calidad=90, tam=None):
    ruta_tmp = os.path.join(PREV, nombre + ".png")
    with open(ruta_tmp, "wb") as f:
        f.write(datos)
    im = Image.open(ruta_tmp).convert("RGBA")
    if tam:
        im = im.resize(tam, Image.LANCZOS)
    destino = os.path.join(RES, nombre + "." + formato)
    if formato == "webp":
        im.save(destino, "WEBP", quality=calidad, method=6)
    else:
        im.save(destino, "PNG", optimize=True)
    print(f"{nombre}.{formato}: {os.path.getsize(destino)//1024} KB  {im.size}")
    return im


def icono_lanzador(mic, anillo_im):
    """Primer plano adaptativo 432x432 (zona segura central) y versión monocroma."""
    S = 432
    fg = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    an = anillo_im.resize((300, 300), Image.LANCZOS)
    fg.alpha_composite(an, ((S - 300) // 2, (S - 300) // 2 + 6))
    mi = mic.resize((230, 230), Image.LANCZOS)
    fg.alpha_composite(mi, ((S - 230) // 2, (S - 230) // 2 - 4))
    fg.save(os.path.join(RES, "ic_launcher_foreground.png"), optimize=True)
    mono = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sil = Image.new("RGBA", mi.size, (0, 0, 0, 255))
    sil.putalpha(mi.getchannel("A").point(lambda v: 255 if v > 90 else 0))
    mono.alpha_composite(sil, ((S - 230) // 2, (S - 230) // 2 - 4))
    mono.save(os.path.join(RES, "ic_launcher_monochrome.png"), optimize=True)
    print("icono del lanzador regenerado")


def circulares():
    """Versiones circulares de los iconos de marca (para la fila de redes)."""
    from PIL import ImageDraw
    for n in ["facebook", "instagram", "telegram", "x", "video", "web", "podcast", "audio"]:
        im = Image.open(os.path.join(RES, f"ic_menu_{n}.png")).convert("RGBA")
        m = Image.new("L", (im.width * 4, im.height * 4), 0)
        ImageDraw.Draw(m).ellipse([0, 0, m.width - 1, m.height - 1], fill=255)
        m = m.resize(im.size, Image.LANCZOS)
        out = Image.new("RGBA", im.size, (0, 0, 0, 0))
        out.paste(im, (0, 0), m)
        out.save(os.path.join(RES, f"ic_c_{n}.png"), optimize=True)
    print("iconos circulares generados")


if __name__ == "__main__":
    with sync_playwright() as p:
        nav = p.chromium.launch()
        pg = nav.new_page()
        svg, w, h = skyline_noche()
        guardar(renderizar(pg, svg, w, h), "art_skyline_noche", "webp", 88)
        svg, w, h = banner_atardecer()
        guardar(renderizar(pg, svg, w, h), "art_banner_atardecer", "webp", 88)
        svg, w, h = microfono()
        mic = guardar(renderizar(pg, svg, w, h), "art_mic", "webp", 92)
        svg, w, h = anillo()
        ani = guardar(renderizar(pg, svg, w, h), "art_anillo", "webp", 92)
        svg, w, h = bandera()
        guardar(renderizar(pg, svg, w, h), "art_bandera", "webp", 92)
        for nombre, (svg, w, h) in iconos().items():
            guardar(renderizar(pg, svg, w, h), nombre, "png")
        nav.close()
    icono_lanzador(mic, ani)
    circulares()
