#!/usr/bin/env python3
"""
Generira ikonu aplikacije MojTijek za Android u svim potrebnim veličinama.
Tirkizna pozadina (#0E9488) sa styliziranim M na bijelo — konzistentno s iOS ikonom.
Izlaz: app/src/main/res/mipmap-*/ic_launcher*.png
"""
import struct, zlib, math, os, sys

def png(w, h, pixels):
    def chunk(tag, data):
        c = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", c)
    raw = b"".join(b"\x00" + bytes([c for px in pixels[y] for c in px]) for y in range(h))
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)) + \
           chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")

def ikona(size, zaobljenje_postotak=0):
    """Tirkizna podloga, bijelo 'M' kao srce brenda."""
    teal = (14, 148, 136)
    bijela = (255, 255, 255)
    r = int(size * zaobljenje_postotak / 100) if zaobljenje_postotak else 0
    p = [[teal]*size for _ in range(size)]

    # Zaobljeni uglovi
    if r > 0:
        prozirna = (0, 0, 0)
        for y in range(size):
            for x in range(size):
                u_uglu = False
                if x < r and y < r:
                    u_uglu = math.sqrt((x-r)**2 + (y-r)**2) > r
                elif x > size-1-r and y < r:
                    u_uglu = math.sqrt((x-(size-1-r))**2 + (y-r)**2) > r
                elif x < r and y > size-1-r:
                    u_uglu = math.sqrt((x-r)**2 + (y-(size-1-r))**2) > r
                elif x > size-1-r and y > size-1-r:
                    u_uglu = math.sqrt((x-(size-1-r))**2 + (y-(size-1-r))**2) > r
                if u_uglu:
                    p[y][x] = prozirna

    # Slovo M: dva stupa + V u sredini, centriran
    debljina = max(2, size // 10)
    visina = size * 55 // 100
    sirina = size * 60 // 100
    ox = (size - sirina) // 2
    oy = (size - visina) // 2

    def tocka(x, y, boja):
        if 0 <= x < size and 0 <= y < size:
            p[y][x] = boja

    def pravokutnik(x1, y1, x2, y2):
        for y in range(max(0,y1), min(size,y2)):
            for x in range(max(0,x1), min(size,x2)):
                p[y][x] = bijela

    # lijevi stup
    pravokutnik(ox, oy, ox + debljina, oy + visina)
    # desni stup
    pravokutnik(ox + sirina - debljina, oy, ox + sirina, oy + visina)
    # lijeva kosa crta V (dolje-lijevo prema sredini)
    sredina_x = ox + sirina // 2
    sredina_y = oy + visina * 55 // 100
    for t in range(100):
        x = ox + debljina // 2 + int((sredina_x - ox - debljina // 2) * t / 100)
        y = oy + int((sredina_y - oy) * t / 100)
        for dy in range(-debljina//2, debljina//2 + 1):
            for dx in range(-debljina//2, debljina//2 + 1):
                tocka(x+dx, y+dy, bijela)
    # desna kosa crta V
    for t in range(100):
        x = ox + sirina - debljina // 2 - int((sirina - debljina) * t / 100)
        y = oy + int((sredina_y - oy) * t / 100)
        for dy in range(-debljina//2, debljina//2 + 1):
            for dx in range(-debljina//2, debljina//2 + 1):
                tocka(x+dx, y+dy, bijela)

    return png(size, size, p)

VELICINE = {
    "mipmap-mdpi":    (48,  48),
    "mipmap-hdpi":    (72,  72),
    "mipmap-xhdpi":   (96,  96),
    "mipmap-xxhdpi":  (144, 144),
    "mipmap-xxxhdpi": (192, 192),
}

base = "app/src/main/res"
for mapa, (w, h) in VELICINE.items():
    d = os.path.join(base, mapa)
    os.makedirs(d, exist_ok=True)
    # ic_launcher (kvadrat, za API < 26)
    with open(os.path.join(d, "ic_launcher.png"), "wb") as f:
        f.write(ikona(w))
    # ic_launcher_round
    with open(os.path.join(d, "ic_launcher_round.png"), "wb") as f:
        f.write(ikona(w, zaobljenje_postotak=50))

print("Ikone generirane:", list(VELICINE.keys()))
