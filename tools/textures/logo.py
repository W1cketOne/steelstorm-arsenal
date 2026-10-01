"""Mod-list logo: crossed Stormsteel blades over a storm sky, with a pixel-font title."""
import math

from PIL import Image

import weapons
from pixelart import hex_rgb

GLYPHS = {
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    " ": ["000", "000", "000", "000", "000", "000", "000"],
}


def text(img, msg, x, y, scale, fill, shadow):
    px = img.load()
    cursor = x
    for ch in msg:
        glyph = GLYPHS[ch]
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            X = cursor + gx * scale + sx
                            Y = y + gy * scale + sy
                            px[X + scale, Y + scale] = shadow
                            t = gy / 6.0
                            px[X, Y] = tuple(int(fill[0][i] + (fill[1][i] - fill[0][i]) * t) for i in range(3)) + (255,)
        cursor += (len(glyph[0]) + 1) * scale
    return cursor


def make_logo():
    w, h = 320, 112
    img = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    px = img.load()
    top, bottom = hex_rgb("#1b2440"), hex_rgb("#0b0e18")
    for y in range(h):
        for x in range(w):
            t = y / (h - 1)
            c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
            # A soft glow behind the blades.
            d = math.hypot(x - 56, y - 56) / 60.0
            glow = max(0.0, 1.0 - d) ** 2 * 0.55
            c = tuple(min(255, int(c[i] + (hex_rgb("#4fd8ff")[i] - c[i]) * glow)) for i in range(3))
            px[x, y] = c + (255,)
    # Lightning bolt.
    bolt = [(64, 0), (58, 20), (66, 22), (54, 50), (62, 52), (50, 84)]
    for (x0, y0), (x1, y1) in zip(bolt, bolt[1:]):
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(steps + 1):
            x = round(x0 + (x1 - x0) * i / steps)
            y = round(y0 + (y1 - y0) * i / steps)
            for dx in (-1, 0, 1):
                if 0 <= x + dx < w:
                    px[x + dx, y] = (230, 250, 255, 255) if dx == 0 else (120, 220, 255, 255)
    sword = weapons.longsword("stormsteel").resize((96, 96), Image.NEAREST)
    great = weapons.greatsword("netherite").transpose(Image.FLIP_LEFT_RIGHT).resize((96, 96), Image.NEAREST)
    img.alpha_composite(great, (8, 8))
    img.alpha_composite(sword, (8, 8))
    end = text(img, "STEELSTORM", 118, 30, 3, (hex_rgb("#e6f0ff"), hex_rgb("#8fb1f4")), (10, 14, 28, 255))
    text(img, "ARSENAL", 118 + (end - 118 - 7 * 6 * 3) // 2, 62, 3, (hex_rgb("#f7dc66"), hex_rgb("#b47d18")), (10, 14, 28, 255))
    return img
