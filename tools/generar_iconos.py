#!/usr/bin/env python3
"""
Genera los iconos de los botones (PNG) y la silueta de La Habana.
Uso:  python3 tools/generar_iconos.py
Requiere Pillow. Los PNG resultantes ya están incluidos en el proyecto;
este script solo sirve para regenerarlos o modificarlos.
"""
import math
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable-nodpi")
S = 4            # supersampling
N = 192          # tamaño final del icono (px)
W = N * S


def canvas():
    return Image.new("RGBA", (W, W), (0, 0, 0, 0))


def rounded_mask(radius=0.22):
    m = Image.new("L", (W, W), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * radius), fill=255)
    return m


def gradient(c1, c2, angle_deg=90):
    """Degradado lineal c1 -> c2. angle 90 = de arriba hacia abajo."""
    g = Image.new("RGBA", (W, W))
    px = g.load()
    a = math.radians(angle_deg)
    dx, dy = math.cos(a), math.sin(a)
    for y in range(W):
        for x in range(W):
            t = ((x / W - .5) * dx + (y / W - .5) * dy) + .5
            t = max(0.0, min(1.0, t))
            px[x, y] = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3)) + (255,)
    return g


def tile(fill):
    """Cuadrado redondeado con relleno sólido o degradado."""
    base = fill if isinstance(fill, Image.Image) else Image.new("RGBA", (W, W), fill + (255,))
    out = canvas()
    out.paste(base, (0, 0), rounded_mask())
    return out


def p(*pts):
    return [(x * W, y * W) for x, y in pts]


def save(img, name):
    img.resize((N, N), Image.LANCZOS).save(os.path.join(OUT, name), optimize=True)


WHITE = (255, 255, 255, 255)


def icon_web():
    im = tile(gradient((30, 136, 229), (0, 172, 193), 60))
    d = ImageDraw.Draw(im)
    lw = int(W * 0.055)
    cx = cy = W // 2
    r = int(W * 0.29)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=WHITE, width=lw)
    d.ellipse([cx - int(r * .5), cy - r, cx + int(r * .5), cy + r], outline=WHITE, width=int(lw * .8))
    d.line([cx - r, cy, cx + r, cy], fill=WHITE, width=int(lw * .8))
    d.line([cx, cy - r, cx, cy + r], fill=WHITE, width=int(lw * .8))
    for k in (-0.5, 0.5):
        yy = cy + int(r * k)
        half = int(math.sqrt(max(r * r - (r * k) ** 2, 0)) - lw * .45)
        d.line([cx - half, yy, cx + half, yy], fill=WHITE, width=int(lw * .7))
    return im


def icon_audio():
    im = tile(gradient((233, 30, 140), (142, 36, 170), 70))
    d = ImageDraw.Draw(im)
    lw = int(W * 0.065)
    # arco de la diadema
    box = [int(W * .22), int(W * .20), int(W * .78), int(W * .76)]
    d.arc(box, 180, 360, fill=WHITE, width=lw)
    # almohadillas
    d.rounded_rectangle([int(W * .17), int(W * .46), int(W * .36), int(W * .76)], radius=int(W * .06), fill=WHITE)
    d.rounded_rectangle([int(W * .64), int(W * .46), int(W * .83), int(W * .76)], radius=int(W * .06), fill=WHITE)
    # ondas de sonido
    for k, a in ((0.14, 255), (0.21, 150)):
        d.arc([int(W * (.5 - k)), int(W * (.62 - k)), int(W * (.5 + k)), int(W * (.62 + k))],
              200, 340, fill=(255, 255, 255, a), width=int(W * .028))
    return im


def icon_podcast():
    im = tile((255, 108, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([int(W * .41), int(W * .20), int(W * .59), int(W * .38)], fill=WHITE)
    d.rounded_rectangle([int(W * .41), int(W * .44), int(W * .59), int(W * .82)], radius=int(W * .03), fill=WHITE)
    return im


def icon_video():
    im = tile((255, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon(p((.38, .28), (.38, .72), (.74, .50)), fill=WHITE)
    return im


def icon_facebook():
    im = tile((24, 119, 242))
    d = ImageDraw.Draw(im)
    # tallo y travesaño
    d.rectangle([int(W * .46), int(W * .36), int(W * .60), W], fill=WHITE)
    d.rectangle([int(W * .34), int(W * .43), int(W * .71), int(W * .55)], fill=WHITE)
    # gancho superior
    cx, cy, R, r = .63, .36, .17, .03
    d.pieslice([int((cx - R) * W), int((cy - R) * W), int((cx + R) * W), int((cy + R) * W)], 180, 270, fill=WHITE)
    d.rectangle([int(cx * W), int((cy - R) * W), int(.77 * W), int((cy - R + .14) * W)], fill=WHITE)
    # recorta lo que sobresalga fuera del cuadrado redondeado
    out = canvas()
    out.paste(im, (0, 0), rounded_mask())
    return out


def icon_x():
    im = tile((0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line(p((.27, .24), (.73, .76)), fill=WHITE, width=int(W * .085))
    d.line(p((.73, .24), (.27, .76)), fill=WHITE, width=int(W * .040))
    return im


def icon_telegram():
    im = tile(gradient((55, 174, 226), (30, 150, 200), 90))
    d = ImageDraw.Draw(im)
    d.polygon(p((.80, .27), (.19, .50), (.34, .56), (.40, .74), (.50, .65), (.63, .74)), fill=WHITE)
    d.polygon(p((.34, .56), (.66, .38), (.44, .62), (.40, .74)), fill=(200, 218, 234, 255))
    return im


def icon_instagram():
    # degradado diagonal desde abajo-izquierda
    g = Image.new("RGBA", (W, W))
    px = g.load()
    stops = [(0.0, (254, 218, 117)), (0.25, (250, 126, 30)), (0.5, (214, 41, 118)),
             (0.75, (150, 47, 191)), (1.0, (79, 91, 213))]
    for y in range(W):
        for x in range(W):
            t = ((W - y) / W * .6 + (1 - (W - x) / W) * .0)  # base
            t = (x / W) * 0.45 + ((W - y) / W) * 0.55
            t = 1 - max(0.0, min(1.0, t))
            for i in range(len(stops) - 1):
                if stops[i][0] <= t <= stops[i + 1][0]:
                    u = (t - stops[i][0]) / (stops[i + 1][0] - stops[i][0])
                    c = tuple(int(stops[i][1][k] + (stops[i + 1][1][k] - stops[i][1][k]) * u) for k in range(3))
                    px[x, y] = c + (255,)
                    break
    im = tile(g)
    d = ImageDraw.Draw(im)
    lw = int(W * .058)
    d.rounded_rectangle([int(W * .22), int(W * .22), int(W * .78), int(W * .78)], radius=int(W * .17), outline=WHITE, width=lw)
    d.ellipse([int(W * .35), int(W * .35), int(W * .65), int(W * .65)], outline=WHITE, width=lw)
    d.ellipse([int(W * .655), int(W * .275), int(W * .725), int(W * .345)], fill=WHITE)
    return im


def pequenos_iconos():
    """Iconos del pie (teléfono, correo) como PNG blancos no hacen falta: son vectores XML."""
    pass


def skyline():
    """Silueta estilizada de La Habana (Capitolio, torre FOCSA, hotel, faro, palmeras,
    antena de radio y mar) en 1440x360, color ciruela oscuro, fondo transparente."""
    sx, sy = 3, 3
    w, h = 1440, 360
    im = Image.new("RGBA", (w * sx, h * sy), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    col = (84, 22, 90, 255)       # ciruela
    col2 = (104, 30, 110, 255)    # un tono más claro para planos lejanos

    def R(x0, y0, x1, y1, c=col):
        d.rectangle([x0 * sx, y0 * sy, x1 * sx, y1 * sy], fill=c)

    def P(pts, c=col):
        d.polygon([(x * sx, y * sy) for x, y in pts], fill=c)

    base = 300
    # plano lejano: edificios bajos
    for x0, hh, ww in [(20, 60, 40), (70, 90, 30), (150, 70, 50), (560, 80, 40), (620, 110, 34),
                       (1030, 90, 44), (1090, 70, 50), (1260, 100, 36), (1320, 70, 50)]:
        R(x0, base - hh, x0 + ww, base, col2)
    # Capitolio (centro-izquierda)
    cx = 330
    R(cx - 110, base - 55, cx + 110, base)                      # cuerpo
    R(cx - 95, base - 70, cx + 95, base - 55)                   # friso
    for k in range(-4, 5):                                      # columnas (huecos)
        R(cx + k * 20 - 3, base - 52, cx + k * 20 + 3, base, (0, 0, 0, 0))
    P([(cx - 45, base - 70), (cx + 45, base - 70), (cx + 40, base - 95), (cx - 40, base - 95)])
    d.pieslice([(cx - 40) * sx, (base - 160) * sy, (cx + 40) * sx, (base - 70) * sy], 180, 360, fill=col)  # cúpula
    R(cx - 8, base - 182, cx + 8, base - 150)                   # linterna
    P([(cx - 8, base - 182), (cx + 8, base - 182), (cx, base - 205)])
    R(cx - 1, base - 235, cx + 1, base - 205)                   # aguja
    # FOCSA (alta, con retranqueo)
    R(560, base - 190, 625, base)
    R(572, base - 215, 613, base - 190)
    R(591, base - 245, 594, base - 215)
    # hotel de dos torres
    R(720, base - 120, 775, base)
    R(790, base - 120, 845, base)
    R(775, base - 95, 790, base)
    P([(718, base - 120), (747, base - 140), (777, base - 120)])
    P([(788, base - 120), (817, base - 140), (847, base - 120)])
    # antena de radio con ondas (la emisora)
    ax = 1000
    P([(ax - 14, base), (ax, base - 215), (ax + 14, base)])
    R(ax - 1.5, base - 245, ax + 1.5, base - 215)
    for r in (14, 26, 38):
        d.arc([(ax - r) * sx, (base - 232 - r) * sy, (ax + r) * sx, (base - 232 + r) * sy], 215, 325,
              fill=(255, 155, 228, 255), width=3 * sx // 2)
    # torre alta con ventanas (edificio moderno)
    R(1120, base - 150, 1175, base)
    R(1175, base - 110, 1215, base)
    # faro del Morro
    R(1340, base - 90, 1362, base)
    P([(1336, base - 90), (1366, base - 90), (1357, base - 118), (1345, base - 118)])
    R(1346, base - 128, 1356, base - 118)
    # palmeras
    def palma(x, alt, lean):
        pts = [(x, base)]
        tx = x + lean
        d.line([(x * sx, base * sy), ((x + lean * .3) * sx, (base - alt * .55) * sy), (tx * sx, (base - alt) * sy)],
               fill=col, width=8 * sx // 2)
        for ang in (-170, -140, -110, -70, -40, -10):
            a = math.radians(ang)
            mx = tx + math.cos(a) * 34
            my = base - alt + math.sin(a) * 26
            ex = tx + math.cos(a) * 62
            ey = base - alt + math.sin(a) * 8 + 26
            d.line([(tx * sx, (base - alt) * sy), (mx * sx, my * sy), (ex * sx, ey * sy)],
                   fill=col, width=6 * sx, joint="curve")
    palma(120, 150, 22)
    palma(215, 115, -16)
    palma(1250, 140, 24)
    palma(1400, 110, -18)
    # mar (Malecón): banda con olas
    R(0, base, 1440, 360)
    for k in range(0, 1440, 60):
        d.ellipse([k * sx, (base - 8) * sy, (k + 60) * sx, (base + 8) * sy], fill=col)
    return im.resize((w, h), Image.LANCZOS)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    save(icon_web(), "ic_menu_web.png")
    save(icon_audio(), "ic_menu_audio.png")
    save(icon_podcast(), "ic_menu_podcast.png")
    save(icon_video(), "ic_menu_video.png")
    save(icon_facebook(), "ic_menu_facebook.png")
    save(icon_x(), "ic_menu_x.png")
    save(icon_telegram(), "ic_menu_telegram.png")
    save(icon_instagram(), "ic_menu_instagram.png")
    skyline().save(os.path.join(OUT, "bg_skyline.png"), optimize=True)
    print("Iconos generados en", os.path.abspath(OUT))
