"""High-resolution material textures for the 3D weapon models (32 px, blades 32x64).

The models map every face onto the whole texture, so extra resolution shows up as finer
detail: engraved filigree on trims, stitched leather wraps, faceted gems, brushed steel with
rivets, and etched gradient blades with a bright honed edge.
"""
import math
import random

from PIL import Image

from texlib import R, mix, save

N = 32
WHITE = (255, 255, 255, 255)
BLACK = (0, 0, 0, 255)


def _bevel(x, y, n=N, w=2):
    """+1 on the lit top-left rim, -1 on the shadowed bottom-right rim, 0 inside."""
    if x < w or y < w:
        return 1 if x + y < n else -1
    if x >= n - w or y >= n - w:
        return -1
    return 0


def trim(ramp, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            l = 0.62 - 0.012 * (x + y)
            b = _bevel(x, y)
            if b:
                l = 0.95 if b > 0 else 0.18
            else:
                # Engraved scrollwork: two interleaved sine vines with curled dots.
                v1 = abs(y - (N / 2 + 6 * math.sin(x * 0.42)))
                v2 = abs(y - (N / 2 - 6 * math.sin(x * 0.42 + 1.4)))
                if v1 < 0.9 or v2 < 0.9:
                    l = 0.3
                elif v1 < 1.8 or v2 < 1.8:
                    l += 0.22
                if (x % 8 == 4 and abs(y - N / 2) > 9):
                    l = 0.88
                l += rng.uniform(-0.03, 0.03)
            img.putpixel((x, y), ramp.at(l))
    return img


def grip(ramp, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            cyl = 0.7 - 0.55 * abs(x - N * 0.38) / N * 2  # rounded across the grip
            band = (y + x * 0.5) % 7
            if band < 1.0:
                l = 0.12  # gap between wraps
            else:
                l = cyl * (0.75 + 0.25 * math.sin(band / 7 * math.pi))
                if band > 5.6:
                    l -= 0.12
                if int(band) == 3 and x % 4 == 1:
                    l = 0.95  # stitching
            l += rng.uniform(-0.04, 0.04)
            img.putpixel((x, y), ramp.at(l))
    return img


def gem(ramp):
    img = Image.new("RGBA", (N, N))
    c = (N - 1) / 2
    for y in range(N):
        for x in range(N):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy) / c
            a = math.atan2(dy, dx)
            facet = int(((a + math.pi) / (2 * math.pi)) * 8) % 8
            if r < 0.38:
                l = 0.9 - 0.25 * r  # the table
            else:
                l = 0.35 + 0.08 * facet - 0.2 * (dx + dy) / N
            if r > 0.95:
                l = 0.05
            elif r > 0.85:
                l = 0.3
            img.putpixel((x, y), ramp.at(l))
    for (x, y, s) in ((8, 8, 3), (21, 11, 1)):
        for k in range(-s, s + 1):
            img.putpixel((x + k, y), WHITE)
            img.putpixel((x, y + k), WHITE)
    return img


def metal(ramp, seed, rough=False):
    rng = random.Random(seed)
    lines = [rng.uniform(-0.08, 0.08) for _ in range(N)]
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            b = _bevel(x, y)
            if b:
                l = 0.92 if b > 0 else 0.15
            else:
                l = 0.62 - 0.25 * y / N + lines[y]  # horizontally brushed
                if rough and rng.random() < 0.15:
                    l += rng.choice([-0.18, 0.12])
            img.putpixel((x, y), ramp.at(l))
    for (x, y) in ((5, 5), (26, 5), (5, 26), (26, 26)):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                img.putpixel((x + dx, y + dy), ramp.at(0.35))
        img.putpixel((x - 1, y - 1), ramp.at(1.0))
        img.putpixel((x, y - 1), ramp.at(0.9))
    return img


PROFILE = [1.45, 1.32, 1.15, 0.92, 0.84, 0.8, 0.82, 0.86, 0.9, 0.95, 1.0, 1.06, 1.14, 1.24, 1.34, 1.3,
           1.22, 1.12, 1.04, 0.98, 0.93, 0.89, 0.85, 0.81, 0.78, 0.75, 0.72, 0.69, 0.65, 0.6, 0.55, 0.48]


def _stops(colors, t):
    from texlib import hexc
    cs = [hexc(c) for c in colors]
    t = max(0.0, min(1.0, t)) * (len(cs) - 1)
    i = min(int(t), len(cs) - 2)
    return mix(cs[i], cs[i + 1], t - i)


def _shade(c, f):
    return mix(c, WHITE, min(1.0, (f - 1) * 1.6)) if f >= 1 else mix(c, BLACK, (1 - f) * 1.2)


def blade(colors, seed):
    rng = random.Random(seed)
    w, h = 32, 64
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        base = _stops(colors, 1 - y / (h - 1))
        for x in range(w):
            f = PROFILE[x]
            # Etched runes running up the fuller beside the ridge.
            if 9 <= x <= 11 and (y % 9) in (2, 3, 5) and 6 < y < h - 6:
                f *= 0.72
            # Faint damascus ripples.
            f += 0.045 * math.sin(x * 0.9 + y * 0.35 + math.sin(y * 0.2) * 2)
            if rng.random() < 0.02:
                f += 0.3
            img.putpixel((x, y), _shade(base, f))
    return img


def edge(colors):
    img = Image.new("RGBA", (32, 64))
    for y in range(64):
        base = _stops(colors, 1 - y / 63)
        for x in range(32):
            img.putpixel((x, y), _shade(base, 1.45 - 0.3 * x / 31 + (0.12 if (x + y) % 11 == 0 else 0)))
    return img


def write(gradients):
    for name in ("wood", "bronze", "gold", "brass", "silver", "black_iron", "obsidian", "cloth_white"):
        save(trim(R[name], len(name)), "item", "3d", f"trim_{name}.png")
    for name in ("ruby", "sapphire", "emerald", "amethyst", "topaz", "aqua"):
        save(gem(R[name]), "item", "3d", f"gem_{name}.png")
    for name in ("leather", "dark_leather", "red_wrap", "blue_wrap", "violet_wrap", "ember_wrap", "green_wrap", "teal_wrap",
                 "straw"):
        save(grip(R[name], len(name)), "item", "3d", f"grip_{name}.png")
    for name in ("stone", "iron", "gold", "diamond", "netherite", "stormsteel", "tempest", "ice", "void", "earth", "blood",
                 "sky", "moon", "obsidian"):
        save(metal(R[name], len(name), rough=name in ("stone", "earth")), "item", "3d", f"metal_{name}.png")
    for i, (name, colors) in enumerate(gradients.items()):
        save(blade(colors, i), "item", "3d", f"grad_{name}.png")
        save(edge(colors), "item", "3d", f"gedge_{name}.png")
