"""Animated GUI icons: turns a static 32x32 icon into a looping strip of frames plus its .mcmeta.

Every weapon gets a metallic glint sweeping up the blade. The coloured tiers and the legendaries
also get a pulsing coloured aura, a breathing inlay and their own particles: golden twinkles,
diamond frost, netherite embers rising off the blade, Stormsteel lightning arcs.
"""
import json
import math
import os
import random

from PIL import Image

from texlib import ROOT, hexc, mix

FRAMES = 20
FRAMETIME = 2

STYLES = {
    "stone": {"aura": None, "spark": None},
    "iron": {"aura": None, "spark": None},
    "golden": {"aura": "#ffc21a", "spark": "#fff4b0", "fx": "twinkle"},
    "diamond": {"aura": "#3fe8ff", "spark": "#f0ffff", "fx": "frost"},
    "netherite": {"aura": "#ff5a10", "spark": "#ffb347", "fx": "embers"},
    "stormsteel": {"aura": "#3aa8ff", "spark": "#e8ffff", "fx": "lightning"},
    "lightning": {"aura": "#ffd92e", "spark": "#fffbe0", "fx": "lightning"},
    "frost": {"aura": "#7fe6ff", "spark": "#ffffff", "fx": "frost"},
    "void": {"aura": "#a033ff", "spark": "#f4ddff", "fx": "twinkle"},
    "ember": {"aura": "#ff7a14", "spark": "#fff1c9", "fx": "embers"},
    "blood": {"aura": "#e01b35", "spark": "#ffd0d4", "fx": "embers"},
    "sky": {"aura": "#9fd8ff", "spark": "#ffffff", "fx": "twinkle"},
    "moon": {"aura": "#9cc4ff", "spark": "#ffffff", "fx": "frost"},
    "royal": {"aura": "#ffb800", "spark": "#fff6c2", "fx": "twinkle"},
}


def _opaque(img, x, y):
    return 0 <= x < 32 and 0 <= y < 32 and img.getpixel((x, y))[3] > 0


def _put(img, x, y, c, alpha=1.0):
    if 0 <= x < 32 and 0 <= y < 32:
        base = img.getpixel((x, y))
        if base[3] == 0:
            img.putpixel((x, y), (c[0], c[1], c[2], int(255 * alpha)))
        else:
            img.putpixel((x, y), mix(base, (c[0], c[1], c[2], 255), alpha))


def animate(base, style, seed=0, inlay=None):
    """Returns a 32 x (32 * FRAMES) strip."""
    st = STYLES[style]
    rng = random.Random(seed)
    pixels = [(x, y) for y in range(32) for x in range(32) if base.getpixel((x, y))[3] > 0]
    edge = [(x, y) for y in range(32) for x in range(32) if base.getpixel((x, y))[3] == 0
            and any(_opaque(base, x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    inlay_px = [p for p in pixels if inlay and base.getpixel(p)[:3] == inlay[:3]]
    aura = hexc(st["aura"]) if st["aura"] else None
    spark = hexc(st["spark"]) if st["spark"] else None
    fx = st.get("fx")
    # Particle events: (start frame, x, y).
    events = [(rng.randrange(FRAMES), *rng.choice(pixels)) for _ in range(6)] if pixels else []
    embers = [(rng.randrange(FRAMES), *rng.choice(pixels)) for _ in range(7)] if pixels else []
    strip = Image.new("RGBA", (32, 32 * FRAMES))
    for f in range(FRAMES):
        img = base.copy()
        phase = math.sin(2 * math.pi * f / FRAMES)
        if aura:
            a = 0.42 + 0.25 * phase
            for (x, y) in edge:
                _put(img, x, y, aura, a)
        if inlay_px:
            k = 0.5 + 0.5 * math.sin(2 * math.pi * f / FRAMES * 2)
            for p in inlay_px:
                img.putpixel(p, mix(mix(inlay, aura or inlay, 0.5), (255, 255, 255, 255), k * 0.7))
        # The glint runs across lines of constant (x - y), i.e. perpendicular to a diagonal blade.
        if f < 10:
            pos = -30 + f * 6.5
            for (x, y) in pixels:
                d = abs((x - y) - pos)
                if d < 2.2:
                    _put(img, x, y, (255, 255, 255, 255), 0.6 * (1 - d / 2.2))
        if fx in ("twinkle", "frost"):
            for (s, x, y) in events:
                age = (f - s) % FRAMES
                if age < 4:
                    r = [0, 1, 2, 1][age]
                    _put(img, x, y, spark, 1.0)
                    for k in range(1, r + 1):
                        a = 0.9 if k == 1 else 0.5
                        for dx, dy in ((k, 0), (-k, 0), (0, k), (0, -k)):
                            _put(img, x + dx, y + dy, spark if fx == "twinkle" else aura, a)
        elif fx == "embers":
            for (s, x, y) in embers:
                age = (f - s) % FRAMES
                if age < 7:
                    c = mix(spark, aura, age / 6)
                    _put(img, x + (1 if age > 3 and (x + s) % 2 else 0), y - age, c, 1.0 - age / 9)
        elif fx == "lightning" and f in (3, 4, 12, 13, 17):
            arc = random.Random(seed * 31 + f // 2)
            x, y = arc.choice(pixels)
            for _ in range(12):
                x += arc.choice((-1, 0, 1, 1))
                y += arc.choice((-1, -1, 0, 1))
                _put(img, x, y, spark, 1.0)
                _put(img, x + 1, y, aura, 0.55)
        strip.paste(img, (0, 32 * f))
    return strip


def save_animated(base, style, *path, seed=0, inlay=None):
    full = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    animate(base, style, seed, inlay).save(full)
    with open(full + ".mcmeta", "w") as f:
        json.dump({"animation": {"frametime": FRAMETIME}}, f)
