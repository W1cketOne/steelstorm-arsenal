#!/usr/bin/env python3
"""Particle sprites, ability icons, HUD sprites and status-effect icons.

Run from the repository root:  python3 tools/gen_fx.py   (needs Pillow)
Icons are drawn at 4x with anti-aliased vector shapes, then downsampled.
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter

from texlib import hexc, mix, save

# ----------------------------------------------------------------------------- particles


def radial(size, power=2.0, core=0.0):
    img = Image.new("RGBA", (size, size))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            r = math.hypot(x - c, y - c) / (size / 2)
            a = max(0.0, 1 - r) ** power
            if r < core:
                a = 1.0
            img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    return img


def slash_frame(k, size=64):
    img = Image.new("RGBA", (size, size))
    c = size / 2
    thick = [0.34, 0.28, 0.2, 0.12][k]
    fade = [1.0, 0.85, 0.6, 0.35][k]
    for y in range(size):
        for x in range(size):
            dx, dy = (x + 0.5 - c) / c, (y + 0.5 - c) / c
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            # arc spanning the upper half, thickest in the middle
            if not (-math.pi * 0.95 < ang < -math.pi * 0.05):
                continue
            t = (ang + math.pi * 0.95) / (math.pi * 0.9)
            w = thick * math.sin(math.pi * t) ** 0.8
            inner = 0.92 - w
            if inner < r < 0.92:
                edge = (r - inner) / max(w, 1e-3)
                a = (edge ** 1.6) * fade * math.sin(math.pi * t) ** 0.5
                img.putpixel((x, y), (255, 255, 255, int(255 * min(1, a * 1.3))))
    return img


def ring_tex(size=64):
    img = Image.new("RGBA", (size, size))
    c = size / 2
    for y in range(size):
        for x in range(size):
            r = math.hypot(x + 0.5 - c, y + 0.5 - c) / c
            a = max(0.0, 1 - abs(r - 0.86) / 0.12) ** 1.4
            img.putpixel((x, y), (255, 255, 255, int(255 * a)))
    return img


def spark_tex():
    img = Image.new("RGBA", (8, 8))
    for (x, y, a) in ((3, 3, 255), (4, 3, 255), (3, 4, 255), (4, 4, 255), (2, 3, 140), (5, 4, 140), (3, 2, 140), (4, 5, 140),
                      (1, 3, 60), (6, 4, 60)):
        img.putpixel((x, y), (255, 255, 255, a))
    return img


def impact_frame(k):
    img = Image.new("RGBA", (32, 32))
    d = ImageDraw.Draw(img)
    rays = 8
    length = [10, 14, 15][k]
    width = [3, 2, 1][k]
    alpha = [255, 200, 110][k]
    for i in range(rays):
        a = i * math.pi * 2 / rays + (0.2 if i % 2 else 0)
        l = length if i % 2 == 0 else length * 0.6
        d.line([(16, 16), (16 + math.cos(a) * l, 16 + math.sin(a) * l)], fill=(255, 255, 255, alpha), width=width)
    d.ellipse([16 - 4 + k, 16 - 4 + k, 16 + 4 - k, 16 + 4 - k], fill=(255, 255, 255, alpha))
    return img.filter(ImageFilter.GaussianBlur(0.4))


def smoke_frame(k, seed):
    """A soft puff of smoke, growing and thinning over its four frames. Fully clear at the edges."""
    rng = random.Random(seed + k)
    n = 32
    alpha = [[0.0] * n for _ in range(n)]
    for _ in range(6):
        cx, cy = rng.uniform(14, 18), rng.uniform(14, 18)
        r = rng.uniform(9.5, 11.5) * (1 + k * 0.05)
        for y in range(n):
            for x in range(n):
                dist = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r
                if dist < 1:
                    alpha[y][x] += (1 - dist * dist) ** 1.5 * 0.4
    img = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            edge = math.hypot(x + 0.5 - n / 2, y + 0.5 - n / 2) / (n / 2)
            a = min(1.0, alpha[y][x]) * (1 - k * 0.18) * max(0.0, 1 - edge ** 4)
            shade = int(250 - 50 * (y / n))
            img.putpixel((x, y), (shade, shade, shade, int(255 * a)))
    return img


def petal(k):
    img = Image.new("RGBA", (8, 8))
    pts = [(1, 4), (3, 1), (6, 2), (7, 5), (4, 7)] if k == 0 else [(2, 6), (1, 3), (4, 1), (7, 3), (5, 6)]
    d = ImageDraw.Draw(img)
    d.polygon(pts, fill=hexc("#ffb7d5"))
    d.line([pts[0], pts[2]], fill=hexc("#ff8fbf"))
    img.putpixel((3, 3), hexc("#ffe6f1"))
    return img


def blood():
    img = Image.new("RGBA", (8, 8))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 3, 6, 7], fill=hexc("#a3121f"))
    d.polygon([(4, 0), (2, 4), (6, 4)], fill=hexc("#a3121f"))
    img.putpixel((3, 4), hexc("#e0455a"))
    return img


RUNE_STROKES = [
    [((8, 2), (8, 14)), ((3, 5), (13, 11)), ((13, 5), (3, 11))],
    [((3, 13), (8, 3)), ((8, 3), (13, 13)), ((5, 9), (11, 9))],
    [((4, 3), (12, 3)), ((8, 3), (8, 13)), ((4, 13), (12, 8))],
    [((3, 3), (13, 13)), ((13, 3), (8, 8)), ((4, 13), (8, 9))],
    [((8, 2), (3, 8)), ((3, 8), (8, 14)), ((8, 14), (13, 8)), ((13, 8), (8, 2))],
    [((3, 4), (13, 4)), ((3, 8), (13, 8)), ((3, 12), (13, 12)), ((8, 2), (8, 14))],
]


def rune_tex(k):
    img = Image.new("RGBA", (64, 64))
    d = ImageDraw.Draw(img)
    for (a, b) in RUNE_STROKES[k]:
        d.line([(a[0] * 4, a[1] * 4), (b[0] * 4, b[1] * 4)], fill=(255, 255, 255, 255), width=7)
    return img.resize((16, 16), Image.LANCZOS)


def frost():
    img = Image.new("RGBA", (32, 32))
    d = ImageDraw.Draw(img)
    for i in range(3):
        a = i * math.pi / 3
        d.line([(16 - math.cos(a) * 13, 16 - math.sin(a) * 13), (16 + math.cos(a) * 13, 16 + math.sin(a) * 13)], fill="white", width=3)
    return img.resize((8, 8), Image.LANCZOS)


def write_particles():
    save(radial(16, 2.2), "particle", "glow.png")
    save(spark_tex(), "particle", "spark.png")
    for k in range(4):
        save(slash_frame(k), "particle", f"slash_{k}.png")
        save(smoke_frame(k, 5), "particle", f"smoke_{k}.png")
    save(ring_tex(), "particle", "shockwave.png")
    for k in range(3):
        save(impact_frame(k), "particle", f"impact_{k}.png")
    for k in range(2):
        save(petal(k), "particle", f"petal_{k}.png")
    save(blood(), "particle", "blood.png")
    for k in range(len(RUNE_STROKES)):
        save(rune_tex(k), "particle", f"rune_{k}.png")
    save(frost(), "particle", "frost.png")


# ----------------------------------------------------------------------------- ability icons

TYPE_COLORS = {
    "longsword": ("#2e4a7a", "#7aa7e8"), "greatsword": ("#5a3b1c", "#e0a050"), "katana": ("#6b1f3c", "#ff7fb0"),
    "dual_daggers": ("#2a2440", "#a78bfa"), "spear": ("#1f4d3a", "#6ee7b7"), "warhammer": ("#5c2a12", "#ff9a3c"),
    "scythe": ("#2b1240", "#c084fc"), "battleaxe": ("#5a1414", "#ff6b6b"), "chakram": ("#1d4450", "#67e8f9"),
    "throwing_knife": ("#3a3a3a", "#d4d4d8"), "legendary": ("#3d2a05", "#ffd166"),
}

W = (255, 255, 255, 255)
BIG = 128


def arc(d, box, start, end, width, fill=W):
    d.arc(box, start, end, fill=fill, width=width)


def blade(d, x0, y0, x1, y1, w=10, fill=W):
    ang = math.atan2(y1 - y0, x1 - x0)
    nx, ny = -math.sin(ang) * w / 2, math.cos(ang) * w / 2
    tipx, tipy = x1 + math.cos(ang) * w, y1 + math.sin(ang) * w
    d.polygon([(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (tipx, tipy), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)], fill=fill)
    gx, gy = x0 + math.cos(ang) * 4, y0 + math.sin(ang) * 4
    d.line([(gx - nx * 2.2, gy - ny * 2.2), (gx + nx * 2.2, gy + ny * 2.2)], fill=fill, width=int(w * 0.7))
    d.line([(x0, y0), (x0 - math.cos(ang) * w * 1.6, y0 - math.sin(ang) * w * 1.6)], fill=fill, width=int(w * 0.6))


def arrow(d, x0, y0, x1, y1, w=8, fill=W):
    d.line([(x0, y0), (x1, y1)], fill=fill, width=w)
    ang = math.atan2(y1 - y0, x1 - x0)
    for s in (-1, 1):
        d.line([(x1, y1), (x1 - math.cos(ang + s * 0.6) * 20, y1 - math.sin(ang + s * 0.6) * 20)], fill=fill, width=w)


def star(d, cx, cy, r, points=8, inner=0.4, fill=W):
    pts = []
    for i in range(points * 2):
        a = i * math.pi / points - math.pi / 2
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=fill)


def ring(d, cx, cy, r, w=7, fill=W):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=fill, width=w)


def hammer(d, cx, cy, s=1.0, fill=W):
    d.line([(cx - 26 * s, cy + 30 * s), (cx + 8 * s, cy - 4 * s)], fill=fill, width=int(9 * s))
    d.polygon([(cx - 6 * s, cy - 30 * s), (cx + 30 * s, cy + 6 * s), (cx + 18 * s, cy + 18 * s), (cx - 18 * s, cy - 18 * s)], fill=fill)


def spiral(d, cx, cy, r, turns=1.6, w=7, fill=W):
    pts = []
    for i in range(80):
        t = i / 79
        a = t * turns * 2 * math.pi
        rr = r * (0.2 + 0.8 * t)
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.line(pts, fill=fill, width=w, joint="curve")


def glyph(name, d):
    c = BIG / 2
    if name == "up_slash":
        arc(d, [22, 30, 106, 114], 200, 330, 12); arrow(d, 64, 96, 64, 26, 9)
    elif name == "stance":
        blade(d, 30, 98, 90, 34, 10); blade(d, 98, 98, 38, 34, 10)
        d.polygon([(64, 70), (84, 80), (80, 104), (64, 114), (48, 104), (44, 80)], fill=W)
    elif name == "dash":
        blade(d, 44, 64, 104, 64, 12)
        for y, l in ((44, 30), (64, 22), (84, 30)):
            d.line([(14, y), (14 + l, y)], fill=W, width=6)
    elif name == "judgment":
        blade(d, 64, 18, 64, 92, 14)
        for i in range(8):
            a = i * math.pi / 4
            d.line([(64 + math.cos(a) * 30, 104 + math.sin(a) * 12), (64 + math.cos(a) * 52, 104 + math.sin(a) * 20)], fill=W, width=5)
    elif name == "cleave":
        blade(d, 30, 30, 70, 70, 14)
        for (x0, y0, x1, y1) in ((70, 80, 112, 70), (68, 88, 100, 110), (60, 92, 70, 120)):
            d.line([(x0, y0), (x1, y1)], fill=W, width=6)
    elif name == "crescent":
        d.ellipse([18, 18, 110, 110], fill=W); d.ellipse([34, 6, 126, 98], fill=(0, 0, 0, 0))
    elif name in ("cyclone", "whirlwind"):
        spiral(d, c, c, 50, 1.8, 9)
        if name == "whirlwind":
            arrow(d, 96, 40, 104, 60, 7)
    elif name == "colossus":
        blade(d, 64, 12, 64, 76, 18); star(d, 64, 100, 30, 10, 0.45)
    elif name == "flash":
        d.line([(16, 90), (50, 70), (44, 58), (112, 34)], fill=W, width=9, joint="curve")
        for k in range(3):
            d.line([(16 + k * 10, 104 + k * 6), (40 + k * 10, 94 + k * 6)], fill=(255, 255, 255, 150 - k * 40), width=5)
    elif name == "iaido":
        d.rounded_rectangle([18, 74, 92, 90], 6, fill=W); blade(d, 70, 74, 112, 20, 9)
        d.line([(16, 40), (110, 110)], fill=W, width=4)
    elif name == "petals":
        for i in range(5):
            a = i * 2 * math.pi / 5
            px, py = c + math.cos(a) * 32, c + math.sin(a) * 32
            d.ellipse([px - 14, py - 9, px + 14, py + 9], fill=W)
        d.ellipse([c - 8, c - 8, c + 8, c + 8], fill=(255, 220, 230, 255))
    elif name == "thousand_cuts":
        for i in range(7):
            a = i * math.pi / 7
            d.line([(c - math.cos(a) * 54, c - math.sin(a) * 54), (c + math.cos(a) * 54, c + math.sin(a) * 54)], fill=W, width=5)
    elif name == "flurry":
        for k in range(4):
            d.line([(20 + k * 18, 100), (60 + k * 18, 24)], fill=W, width=8)
    elif name == "shadowstep":
        d.ellipse([28, 40, 100, 88], outline=W, width=8); d.ellipse([52, 52, 76, 76], fill=W)
        for k in range(4):
            d.arc([10 + k * 6, 84, 120 - k * 6, 124], 200, 340, fill=(255, 255, 255, 160), width=4)
    elif name == "fan_knives":
        for i in range(5):
            a = math.radians(-150 + i * 30)
            blade(d, 64 + math.cos(a) * 10, 100 + math.sin(a) * 10, 64 + math.cos(a) * 66, 100 + math.sin(a) * 66, 8)
    elif name == "death_blossom":
        for i in range(6):
            a = i * math.pi / 3
            blade(d, c, c, c + math.cos(a) * 46, c + math.sin(a) * 46, 9)
        d.ellipse([c - 12, c - 12, c + 12, c + 12], fill=(255, 80, 120, 255))
    elif name == "impale":
        blade(d, 64, 10, 64, 84, 10)
        d.line([(24, 104), (104, 104)], fill=W, width=8); d.line([(40, 92), (52, 104)], fill=W, width=5)
    elif name == "vault":
        d.arc([10, 24, 118, 140], 200, 330, fill=W, width=9); arrow(d, 100, 60, 112, 80, 8)
        d.line([(18, 112), (40, 70)], fill=W, width=8)
    elif name == "sweep":
        d.arc([14, 20, 114, 120], 180, 360, fill=W, width=12); blade(d, 64, 110, 64, 36, 8)
    elif name == "dragon_dive":
        d.polygon([(20, 30), (64, 50), (108, 30), (90, 56), (64, 64), (38, 56)], fill=W)
        blade(d, 64, 40, 64, 106, 10)
    elif name == "earthquake":
        for r in (18, 34, 50):
            d.ellipse([c - r, 92 - r * 0.45, c + r, 92 + r * 0.45], outline=W, width=6)
        hammer(d, 64, 40, 0.8)
    elif name == "hammer_throw":
        hammer(d, 56, 60, 0.85); d.arc([10, 10, 118, 118], 280, 40, fill=W, width=7); arrow(d, 108, 72, 104, 84, 6)
    elif name == "fissure":
        d.line([(30, 120), (46, 92), (38, 72), (64, 48), (56, 30), (90, 6)], fill=W, width=10, joint="curve")
        for (x, y) in ((70, 60), (32, 100), (84, 24)):
            d.rectangle([x, y, x + 10, y + 10], fill=W)
    elif name == "cataclysm":
        star(d, c, c, 58, 12, 0.5); d.ellipse([c - 18, c - 18, c + 18, c + 18], fill=(255, 210, 120, 255))
    elif name == "reap":
        d.arc([16, 16, 112, 112], 0, 300, fill=W, width=12); arrow(d, 112, 56, 108, 70, 7)
    elif name == "soul_harvest":
        for a in range(0, 360, 60):
            r = math.radians(a)
            arrow(d, c + math.cos(r) * 56, c + math.sin(r) * 56, c + math.cos(r) * 22, c + math.sin(r) * 22, 6)
        d.ellipse([c - 10, c - 10, c + 10, c + 10], fill=W)
    elif name == "deaths_crescent":
        d.pieslice([14, 14, 114, 114], 200, 340, fill=W); d.pieslice([30, 34, 98, 102], 190, 350, fill=(0, 0, 0, 0))
        spiral(d, c, 84, 24, 1.0, 5)
    elif name == "grim_eclipse":
        d.ellipse([20, 20, 108, 108], fill=W); d.ellipse([30, 30, 98, 98], fill=(20, 0, 30, 255))
        for i in range(12):
            a = i * math.pi / 6
            d.line([(c + math.cos(a) * 50, c + math.sin(a) * 50), (c + math.cos(a) * 62, c + math.sin(a) * 62)], fill=W, width=4)
    elif name == "cleaving_leap":
        d.arc([10, 30, 118, 150], 200, 330, fill=W, width=8)
        d.polygon([(80, 70), (112, 60), (118, 96), (86, 92)], fill=W); d.line([(60, 120), (96, 80)], fill=W, width=8)
    elif name == "war_cry":
        d.ellipse([20, 40, 64, 88], fill=W)
        for r in (22, 38, 54):
            d.arc([64 - r, 64 - r, 64 + r, 64 + r], 300, 60, fill=W, width=6)
    elif name == "berserk":
        d.polygon([(64, 10), (86, 52), (104, 40), (98, 92), (64, 118), (30, 92), (24, 40), (42, 52)], fill=W)
        d.polygon([(64, 50), (78, 76), (64, 100), (50, 76)], fill=(200, 30, 40, 255))
    elif name == "sawblade":
        star(d, c, c, 54, 12, 0.72); d.ellipse([c - 18, c - 18, c + 18, c + 18], fill=(0, 0, 0, 0))
    elif name == "twin_throw":
        ring(d, 44, 64, 26, 10); ring(d, 88, 56, 22, 9)
    elif name == "guard_ring":
        ring(d, c, c, 52, 8); d.polygon([(64, 36), (88, 46), (84, 80), (64, 94), (44, 80), (40, 46)], fill=W)
    elif name == "blade_tempest":
        for i in range(6):
            a = i * math.pi / 3
            ring(d, c + math.cos(a) * 40, c + math.sin(a) * 40, 12, 6)
        d.ellipse([c - 12, c - 12, c + 12, c + 12], fill=W)
    elif name == "volley":
        for i in range(5):
            a = math.radians(-60 + i * 30)
            arrow(d, 30, 98, 30 + math.cos(a) * 80, 98 + math.sin(a) * 80 - 20, 6)
    elif name == "blink":
        blade(d, 70, 60, 112, 18, 9)
        for k in range(5):
            d.ellipse([18 + k * 11, 104 - k * 9, 26 + k * 11, 112 - k * 9], fill=W)
    elif name == "poison":
        d.ellipse([34, 48, 94, 108], fill=W); d.polygon([(64, 10), (36, 70), (92, 70)], fill=W)
        d.ellipse([50, 66, 66, 82], fill=(60, 200, 80, 255))
    elif name == "knife_storm":
        for (x, y) in ((24, 20), (56, 8), (88, 22), (40, 52), (76, 48), (104, 54)):
            blade(d, x, y, x, y + 34, 6)
        d.line([(10, 116), (118, 116)], fill=W, width=6)
    elif name == "bolt":
        d.polygon([(76, 6), (30, 70), (60, 70), (46, 122), (100, 50), (68, 50)], fill=W)
    elif name == "snowflake":
        for i in range(3):
            a = i * math.pi / 3
            d.line([(c - math.cos(a) * 54, c - math.sin(a) * 54), (c + math.cos(a) * 54, c + math.sin(a) * 54)], fill=W, width=9)
        for i in range(6):
            a = i * math.pi / 3
            px, py = c + math.cos(a) * 36, c + math.sin(a) * 36
            d.line([(px, py), (px + math.cos(a + 0.8) * 14, py + math.sin(a + 0.8) * 14)], fill=W, width=6)
            d.line([(px, py), (px + math.cos(a - 0.8) * 14, py + math.sin(a - 0.8) * 14)], fill=W, width=6)
    elif name == "black_hole":
        spiral(d, c, c, 56, 2.2, 7); d.ellipse([c - 16, c - 16, c + 16, c + 16], fill=(10, 0, 20, 255))
    elif name == "world_crack":
        hammer(d, 70, 36, 0.7)
        d.line([(10, 100), (40, 86), (58, 104), (84, 84), (118, 98)], fill=W, width=9, joint="curve")
    elif name == "crimson_frenzy":
        for x in (40, 64, 88):
            d.polygon([(x - 10, 24), (x + 10, 24), (x, 104)], fill=W)
        d.ellipse([52, 92, 76, 116], fill=(230, 40, 60, 255))
    elif name == "heaven":
        d.polygon([(10, 44), (64, 26), (118, 44), (92, 50), (64, 44), (36, 50)], fill=W)
        blade(d, 64, 36, 64, 110, 10)
    elif name == "moon":
        d.ellipse([16, 16, 112, 112], fill=W); d.ellipse([40, 6, 126, 92], fill=(0, 0, 0, 0))
        star(d, 96, 92, 12, 4, 0.4)
    elif name == "crown":
        d.polygon([(16, 96), (24, 36), (46, 70), (64, 24), (82, 70), (104, 36), (112, 96)], fill=W)
        d.rectangle([16, 98, 112, 112], fill=W)
    elif name == "titan_guard":
        d.polygon([(64, 14), (104, 30), (98, 84), (64, 116), (30, 84), (24, 30)], fill=W)
        d.polygon([(64, 30), (88, 40), (84, 78), (64, 98), (44, 78), (40, 40)], fill=(0, 0, 0, 0))
        blade(d, 64, 22, 64, 104, 9)
    elif name == "sanctum":
        blade(d, 64, 10, 64, 92, 14)
        d.ellipse([16, 90, 112, 122], outline=W, width=7)
        for x in (28, 100):
            d.line([(x, 84), (x, 60)], fill=W, width=5)
    elif name == "stone_prison":
        for (x0, y0, x1, y1) in ((20, 20, 60, 60), (68, 20, 108, 60), (20, 68, 60, 108), (68, 68, 108, 108)):
            d.rounded_rectangle([x0, y0, x1, y1], 6, fill=W)
        d.line([(64, 14), (60, 50), (70, 70), (62, 114)], fill=(0, 0, 0, 0), width=6)
    elif name == "wind_scar":
        blade(d, 18, 18, 110, 110, 11); blade(d, 110, 18, 18, 110, 11)
        d.arc([30, 30, 98, 98], 30, 150, fill=W, width=5)
    elif name == "chain_hook":
        for k in range(5):
            x, y = 20 + k * 16, 104 - k * 16
            d.ellipse([x - 8, y - 6, x + 8, y + 6], outline=W, width=5)
        d.arc([78, 10, 118, 50], 300, 200, fill=W, width=9)
        d.polygon([(96, 44), (112, 52), (100, 60)], fill=W)
    elif name == "death_mark":
        d.ellipse([28, 18, 100, 86], fill=W)
        d.ellipse([42, 42, 58, 58], fill=(0, 0, 0, 0)); d.ellipse([70, 42, 86, 58], fill=(0, 0, 0, 0))
        d.rectangle([44, 80, 84, 104], fill=W)
        for x in (52, 64, 76):
            d.line([(x, 84), (x, 104)], fill=(0, 0, 0, 0), width=4)
    elif name == "hemorrhage":
        star(d, c, c, 56, 9, 0.45)
        for (x, y) in ((24, 24), (104, 30), (30, 104), (100, 100)):
            d.ellipse([x - 8, y - 8, x + 8, y + 8], fill=W)
        d.ellipse([c - 14, c - 14, c + 14, c + 14], fill=(230, 40, 60, 255))
    elif name == "tectonic":
        for i in range(6):
            a = i * math.pi / 3
            pts = [(c + math.cos(a + t * 0.5) * (12 + t * 48), c + math.sin(a + t * 0.5) * (12 + t * 48)) for t in (0, 0.33, 0.66, 1)]
            d.line(pts, fill=W, width=7, joint="curve")
        d.ellipse([c - 12, c - 12, c + 12, c + 12], fill=W)
    elif name == "updraft":
        for k, (w, y) in enumerate(((50, 20), (40, 40), (32, 60), (24, 80), (16, 98))):
            d.arc([c - w, y - 8, c + w, y + 8], 0, 360, fill=W, width=6)
        arrow(d, 64, 120, 64, 8, 6)
    else:
        raise ValueError(name)


ABILITY_ICONS = {
    # longsword
    "longsword_rising_slash": ("longsword", "up_slash"), "longsword_riposte": ("longsword", "stance"),
    "longsword_blade_dash": ("longsword", "dash"), "longsword_judgment": ("longsword", "judgment"),
    "greatsword_titans_guard": ("greatsword", "titan_guard"), "greatsword_crescent_wave": ("greatsword", "crescent"),
    "greatsword_sword_sanctum": ("greatsword", "sanctum"), "greatsword_colossus_strike": ("greatsword", "colossus"),
    "katana_flash_step": ("katana", "flash"), "katana_iaido": ("katana", "iaido"),
    "katana_wind_scar": ("katana", "wind_scar"), "katana_thousand_cuts": ("katana", "thousand_cuts"),
    "dual_daggers_flurry": ("dual_daggers", "flurry"), "dual_daggers_shadowstep": ("dual_daggers", "shadowstep"),
    "dual_daggers_fan_of_knives": ("dual_daggers", "fan_knives"), "dual_daggers_death_blossom": ("dual_daggers", "death_blossom"),
    "spear_impale": ("spear", "impale"), "spear_vault_leap": ("spear", "vault"),
    "spear_sweeping_arc": ("spear", "sweep"), "spear_dragon_dive": ("spear", "dragon_dive"),
    "warhammer_earthquake": ("warhammer", "earthquake"), "warhammer_hammer_throw": ("warhammer", "hammer_throw"),
    "warhammer_stone_prison": ("warhammer", "stone_prison"), "warhammer_cataclysm": ("warhammer", "cataclysm"),
    "scythe_reap": ("scythe", "reap"), "scythe_soul_harvest": ("scythe", "soul_harvest"),
    "scythe_deaths_crescent": ("scythe", "deaths_crescent"), "scythe_death_mark": ("scythe", "death_mark"),
    "battleaxe_whirlwind": ("battleaxe", "whirlwind"), "battleaxe_chain_hook": ("battleaxe", "chain_hook"),
    "battleaxe_war_cry": ("battleaxe", "war_cry"), "battleaxe_berserker_rage": ("battleaxe", "berserk"),
    "chakram_sawblade": ("chakram", "sawblade"), "chakram_twin_throw": ("chakram", "twin_throw"),
    "chakram_guard_ring": ("chakram", "guard_ring"), "chakram_blade_tempest": ("chakram", "blade_tempest"),
    "throwing_knife_volley": ("throwing_knife", "volley"), "throwing_knife_blink": ("throwing_knife", "blink"),
    "throwing_knife_venom": ("throwing_knife", "poison"), "throwing_knife_knife_storm": ("throwing_knife", "knife_storm"),
    "tempest_edge_thunder_verdict": ("legendary", "bolt"), "rimecleaver_absolute_zero": ("legendary", "snowflake"),
    "voidreaver_event_horizon": ("legendary", "black_hole"), "earthshaker_tectonic_spiral": ("legendary", "tectonic"),
    "bloodfang_hemorrhage": ("legendary", "hemorrhage"), "skypiercer_updraft": ("legendary", "updraft"),
    "moonveil_moonfall": ("legendary", "moon"), "kingsbane_regicide": ("legendary", "crown"),
}
ULTIMATE_SUFFIXES = ("judgment", "colossus_strike", "thousand_cuts", "death_blossom", "dragon_dive", "cataclysm", "death_mark",
                     "berserker_rage", "blade_tempest", "knife_storm")


def ability_icon(key):
    kind, g = ABILITY_ICONS[key]
    ultimate = kind == "legendary" or key.endswith(ULTIMATE_SUFFIXES)
    dark, light = (hexc(c) for c in TYPE_COLORS[kind])
    img = Image.new("RGBA", (BIG, BIG))
    # background: diagonal gradient rounded square
    bg = Image.new("RGBA", (BIG, BIG))
    for y in range(BIG):
        for x in range(BIG):
            t = (x + y) / (2 * BIG)
            bg.putpixel((x, y), mix(light, dark, 0.35 + 0.65 * t))
    mask = Image.new("L", (BIG, BIG))
    ImageDraw.Draw(mask).rounded_rectangle([4, 4, BIG - 5, BIG - 5], 18, fill=255)
    img.paste(bg, (0, 0), mask)
    d = ImageDraw.Draw(img)
    border = hexc("#ffd166") if ultimate else mix(dark, (0, 0, 0, 255), 0.5)
    d.rounded_rectangle([4, 4, BIG - 5, BIG - 5], 18, outline=border, width=8 if ultimate else 6)
    # glyph with a soft dark shadow
    glyph_layer = Image.new("RGBA", (BIG, BIG))
    glyph(g, ImageDraw.Draw(glyph_layer))
    shadow = Image.new("RGBA", (BIG, BIG), (0, 0, 0, 0))
    alpha = glyph_layer.split()[3]
    shadow.paste((0, 0, 0, 170), (4, 5), alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(3))
    img.alpha_composite(shadow)
    tint = Image.new("RGBA", (BIG, BIG), hexc("#fff4d6") if ultimate else (255, 255, 255, 255))
    img.paste(tint, (0, 0), alpha)
    return img.resize((32, 32), Image.LANCZOS)


# ----------------------------------------------------------------------------- HUD and effects


def hud():
    img = Image.new("RGBA", (128, 64))
    d = ImageDraw.Draw(img)
    # ability slot frame 22x22 at (0,0)
    d.rectangle([0, 0, 21, 21], fill=hexc("#1b1d22", 210), outline=hexc("#08090b"))
    d.line([(1, 1), (20, 1)], fill=hexc("#5b616d")); d.line([(1, 1), (1, 20)], fill=hexc("#5b616d"))
    d.line([(1, 20), (20, 20)], fill=hexc("#2a2d33")); d.line([(20, 1), (20, 20)], fill=hexc("#2a2d33"))
    # ultimate frame 26x26 at (24,0)
    d.rectangle([24, 0, 49, 25], fill=hexc("#22180a", 220), outline=hexc("#140c02"))
    d.rectangle([25, 1, 48, 24], outline=hexc("#c9971a"))
    d.rectangle([26, 2, 47, 23], outline=hexc("#7a5a0e"))
    for (x, y) in ((25, 1), (48, 1), (25, 24), (48, 24)):
        d.point((x, y), fill=hexc("#fff0b0"))
    # stamina frame 83x7 at (0,32), fills 81x5 at (0,40) blue, (0,46) orange, (0,52) gold
    d.rectangle([0, 32, 82, 38], fill=hexc("#0d1014", 230), outline=hexc("#050608"))
    d.line([(1, 33), (81, 33)], fill=hexc("#2c323b"))
    for row, (top, bot) in enumerate((("#9fdcff", "#2f7fd8"), ("#ffd27a", "#d0701a"), ("#fff1a8", "#d9a01a"))):
        for y in range(5):
            c = mix(hexc(top), hexc(bot), y / 4)
            d.line([(0, 40 + row * 6 + y), (80, 40 + row * 6 + y)], fill=c)
    return img


def effect_icon(kind):
    img = Image.new("RGBA", (72, 72))
    d = ImageDraw.Draw(img)
    if kind == "bleed":
        d.ellipse([18, 30, 54, 66], fill=hexc("#b3121f")); d.polygon([(36, 4), (18, 44), (54, 44)], fill=hexc("#b3121f"))
        d.ellipse([26, 40, 36, 50], fill=hexc("#ff7a85"))
    elif kind == "stagger":
        for i in range(3):
            a = i * 2 * math.pi / 3
            star(d, 36 + math.cos(a) * 20, 36 + math.sin(a) * 12, 12, 5, 0.45, hexc("#ffd84a"))
        d.ellipse([6, 22, 66, 50], outline=hexc("#d9d9d9"), width=4)
    elif kind == "armor_break":
        d.polygon([(10, 10), (62, 10), (62, 36), (36, 66), (10, 36)], fill=hexc("#8f969e"))
        d.line([(36, 10), (30, 26), (40, 38), (32, 56)], fill=(0, 0, 0, 0), width=6)
    elif kind == "berserk":
        d.polygon([(36, 4), (48, 28), (60, 20), (56, 52), (36, 68), (16, 52), (12, 20), (24, 28)], fill=hexc("#e0302f"))
        d.polygon([(36, 30), (44, 46), (36, 60), (28, 46)], fill=hexc("#ffd166"))
    elif kind == "riposte":
        d.polygon([(36, 6), (62, 16), (58, 44), (36, 66), (14, 44), (10, 16)], fill=hexc("#7aa7e8"))
        d.line([(20, 52), (52, 20)], fill="white", width=6); d.line([(20, 20), (52, 52)], fill="white", width=6)
    elif kind == "shadow_veil":
        d.ellipse([10, 20, 62, 52], outline=hexc("#a78bfa"), width=6); d.ellipse([28, 28, 44, 44], fill=hexc("#a78bfa"))
    elif kind == "sharpened":
        blade(d, 14, 58, 54, 18, 10, hexc("#e8eef5"))
        for (x, y) in ((56, 40), (48, 52), (62, 28)):
            d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=hexc("#fff59a"))
    elif kind == "frozen":
        glyph("snowflake", d) if False else None
        for i in range(3):
            a = i * math.pi / 3
            d.line([(36 - math.cos(a) * 30, 36 - math.sin(a) * 30), (36 + math.cos(a) * 30, 36 + math.sin(a) * 30)],
                   fill=hexc("#aef3ff"), width=7)
    elif kind == "marked":
        d.ellipse([10, 10, 62, 62], outline=hexc("#c084fc"), width=6); d.ellipse([28, 28, 44, 44], fill=hexc("#c084fc"))
        d.line([(36, 2), (36, 70)], fill=hexc("#c084fc"), width=4); d.line([(2, 36), (70, 36)], fill=hexc("#c084fc"), width=4)
    return img.resize((18, 18), Image.LANCZOS)


# ----------------------------------------------------------------------------- entity effect textures
# Drawn with additive blending, so these are greyscale brightness maps; the game tints them.


def smoothstep(e0, e1, x):
    t = min(1.0, max(0.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)


def slash_wave_texture():
    """Crescent ribbon: u runs tip to tip along the arc, v across the band (bright middle line)."""
    w, h = 64, 32
    img = Image.new("RGBA", (w, h))
    px = img.load()
    rnd = random.Random(7)
    streak = [0.8 + 0.2 * rnd.random() for _ in range(h)]
    for x in range(w):
        u = (x + 0.5) / w
        fade = smoothstep(0.0, 0.2, u) * smoothstep(0.0, 0.2, 1 - u)
        for y in range(h):
            v = (y + 0.5) / h
            core = math.exp(-((v - 0.5) / 0.13) ** 2)
            halo = 0.45 * math.exp(-((v - 0.5) / 0.3) ** 2)
            b = min(1.0, (core + halo) * fade * streak[y] * (0.9 + 0.1 * math.sin(u * 37 + y)))
            c = int(255 * b)
            px[x, y] = (c, c, c, c)
    return img


def vortex_ring_texture():
    """Accretion ring: u around the ring (tiles), v from the outer edge (0) to the inner edge (1)."""
    w, h = 64, 32
    img = Image.new("RGBA", (w, h))
    px = img.load()
    rnd = random.Random(11)
    phases = [rnd.random() * math.tau for _ in range(6)]
    for x in range(w):
        u = x / w
        for y in range(h):
            v = (y + 0.5) / h
            radial = (v ** 1.6) * (1 - 0.6 * smoothstep(0.92, 1.0, v)) + 0.15 * math.exp(-((v - 0.35) / 0.08) ** 2)
            swirl = 0.55
            for k, ph in enumerate(phases):
                swirl += 0.09 * math.sin(math.tau * (k + 1) * u + ph + v * (3 + k))
            b = max(0.0, min(1.0, radial * swirl * 1.4))
            c = int(255 * b)
            px[x, y] = (c, c, c, c)
    return img


def glow_texture():
    """Soft round glow for halos and beams."""
    n = 32
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for x in range(n):
        for y in range(n):
            r = math.hypot((x + 0.5) / n - 0.5, (y + 0.5) / n - 0.5) * 2
            b = max(0.0, math.exp(-(r / 0.42) ** 2) - 0.0035) * (1 - smoothstep(0.85, 1.0, r))
            c = int(255 * min(1.0, b))
            px[x, y] = (c, c, c, c)
    return img


def main():
    write_particles()
    save(slash_wave_texture(), "entity", "slash_wave.png")
    save(vortex_ring_texture(), "entity", "vortex_ring.png")
    save(glow_texture(), "entity", "glow.png")
    for key in ABILITY_ICONS:
        save(ability_icon(key), "gui", "ability", f"{key}.png")
    save(hud(), "gui", "hud.png")
    for kind in ("bleed", "stagger", "armor_break", "berserk", "riposte", "shadow_veil", "sharpened", "frozen", "marked"):
        save(effect_icon(kind), "mob_effect", f"{kind}.png")
    print(f"Particles, {len(ABILITY_ICONS)} ability icons, HUD and effect icons written")


if __name__ == "__main__":
    main()
