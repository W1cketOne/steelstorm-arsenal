#!/usr/bin/env python3
"""3D in-hand weapon models (Blockbench-style JSON) and the 16x16 material textures they use.

Run from the repository root:  python3 tools/gen_models.py   (needs Pillow)

Each weapon type gets one parent model in models/item/3d/ with texture variables
(#blade, #edge, #fuller, #metal, #trim, #gem, #grip, #shaft); every tier's item model
(written by datagen) points at it with its own materials. Legendary weapons get standalone
models with extra parts, some of which glow (NeoForge "neoforge_data" light emission).
"""
import json
import math
import os
import random

from PIL import Image

from texlib import R, TIERS, TIER_BLADE, TIER_GEM, TIER_GRIP, TIER_TRIM, clamp, hexc, mix, mul, save

MODELS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "steelstorm", "models", "item", "3d")

# ----------------------------------------------------------------------------- material textures


def tex_blade(ramp, seed, stone=False):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x <= 1:
                l = 0.98 if x == 0 else 0.84
            elif x >= 14:
                l = 0.14 if x == 15 else 0.3
            else:
                l = 0.72 - (x - 2) / 12 * 0.42
                if 6 <= x <= 9:
                    l -= 0.1
            l += 0.04 * math.sin(y * 0.7 + x * 0.3)
            if rng.random() < (0.16 if stone else 0.05):
                l += rng.choice([-0.18, 0.15])
            img.putpixel((x, y), ramp.at(l))
    return img


def tex_edge(ramp, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            l = 0.9 - 0.12 * (x / 15) + (0.1 if rng.random() < 0.08 else 0)
            img.putpixel((x, y), ramp.at(l))
    return img


def tex_fuller(ramp, seed):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            l = 0.24 + (0.22 if 6 <= x <= 8 else 0) - (0.1 if x in (0, 15) else 0)
            img.putpixel((x, y), ramp.at(l))
    return img


def tex_metal(ramp, seed, stone=False):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x == 0 or y == 0:
                l = 0.92
            elif x == 15 or y == 15:
                l = 0.12
            elif x == 1 or y == 1:
                l = 0.75
            elif x == 14 or y == 14:
                l = 0.28
            else:
                l = 0.56 - 0.012 * (x + y) + 0.05 * math.sin(x * 1.3 + y * 0.4)
                if rng.random() < (0.18 if stone else 0.06):
                    l += rng.choice([-0.16, 0.14])
            img.putpixel((x, y), ramp.at(l))
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((x, y), ramp.at(1.0))
        img.putpixel((x + 1, y + 1), ramp.at(0.15))
    return img


def tex_trim(ramp, seed):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            l = 0.62 - 0.025 * (x + y) * 0.6
            if y in (0, 15) or x in (0, 15):
                l = 0.25
            if y in (1, 14) or x in (1, 14):
                l = 0.9
            if (x + y) % 6 == 0 and 2 < x < 13 and 2 < y < 13:
                l += 0.18
            img.putpixel((x, y), ramp.at(l))
    return img


def tex_gem(ramp):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            facet = (abs(dx) + abs(dy)) / 10.0
            l = 0.75 - 0.35 * (dx + dy) / 8 - 0.25 * facet
            if r > 7:
                l = 0.12
            img.putpixel((x, y), ramp.at(l))
    for (x, y) in ((4, 4), (5, 4), (4, 5)):
        img.putpixel((x, y), ramp.c[6])
    return img


def tex_grip(ramp, seed):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            band = ((y + x // 3) % 4) == 0
            l = 0.62 - 0.35 * (x / 15) - (0.3 if band else 0)
            img.putpixel((x, y), ramp.at(l))
    return img


def tex_wrap_diamond(ramp, seed):
    img = Image.new("RGBA", (16, 16))
    white = R["cloth_white"]
    for y in range(16):
        for x in range(16):
            diamond = abs((x % 8) - 3.5) + abs((y % 8) - 3.5) < 3.2
            if diamond:
                img.putpixel((x, y), white.at(0.75 - 0.3 * (x / 15)))
            else:
                img.putpixel((x, y), ramp.at(0.45 - 0.25 * (x / 15)))
    return img


def tex_shaft(ramp, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    grain = [rng.uniform(-0.12, 0.12) for _ in range(16)]
    for y in range(16):
        for x in range(16):
            l = 0.62 - 0.3 * (x / 15) + grain[x] + 0.05 * math.sin(y * 0.9 + x)
            img.putpixel((x, y), ramp.at(l))
    return img


def tex_glow(core, outer):
    img = Image.new("RGBA", (16, 16))
    a, b = hexc(core), hexc(outer)
    for y in range(16):
        for x in range(16):
            t = abs(x - 7.5) / 7.5
            img.putpixel((x, y), mix(a, b, t ** 1.5))
    return img


def write_textures():
    blade_ramps = {"stone": "stone", "iron": "iron", "gold": "gold", "diamond": "diamond", "netherite": "netherite",
                   "stormsteel": "stormsteel", "tempest": "tempest", "ice": "ice", "void": "void", "earth": "earth",
                   "blood": "blood", "sky": "sky", "moon": "moon", "obsidian": "obsidian"}
    for name, ramp in blade_ramps.items():
        r = R[ramp]
        save(tex_blade(r, len(name), stone=name in ("stone", "earth")), "item", "3d", f"blade_{name}.png")
        save(tex_edge(r, len(name) + 1), "item", "3d", f"edge_{name}.png")
        save(tex_fuller(r, 0), "item", "3d", f"fuller_{name}.png")
        save(tex_metal(r, len(name) + 2, stone=name in ("stone", "earth")), "item", "3d", f"metal_{name}.png")
    for name in ("wood", "bronze", "gold", "brass", "silver", "black_iron", "obsidian", "cloth_white"):
        save(tex_trim(R[name], 3), "item", "3d", f"trim_{name}.png")
    for name in ("ruby", "sapphire", "emerald", "amethyst", "topaz", "aqua"):
        save(tex_gem(R[name]), "item", "3d", f"gem_{name}.png")
    for name in ("leather", "dark_leather", "red_wrap", "blue_wrap"):
        save(tex_grip(R[name], 5), "item", "3d", f"grip_{name}.png")
    save(tex_wrap_diamond(R["black_iron"], 1), "item", "3d", "grip_ito.png")
    for name in ("wood", "birch", "black_iron", "obsidian"):
        save(tex_shaft(R[name], 7), "item", "3d", f"shaft_{name}.png")
    for name, core, outer in (("lightning", "#fffbe0", "#ffd92e"), ("frost", "#ffffff", "#7fe6ff"),
                              ("void", "#f4ddff", "#a033ff"), ("ember", "#fff1c9", "#ff7a14"),
                              ("blood", "#ffd0d4", "#e01b35"), ("sky", "#ffffff", "#9fd8ff"),
                              ("moon", "#ffffff", "#9cc4ff"), ("royal", "#fff6c2", "#ffb800")):
        save(tex_glow(core, outer), "item", "3d", f"glow_{name}.png")


# ----------------------------------------------------------------------------- model building

FACES = ("north", "south", "east", "west", "up", "down")


class Model:
    def __init__(self):
        self.elements = []

    def box(self, frm, to, tex, side=None, ends=None, rot=None, glow=False, faces=FACES, uvy=None):
        """Adds a cuboid. `tex` covers north/south, `side` east/west, `ends` up/down.

        `uvy=(y_bottom, y_top)` maps the texture's height onto that span of model y instead of
        stretching it over each face, so a gradient runs smoothly along a blade made of many boxes.
        """
        side = side or tex
        ends = ends or side
        face_tex = {"north": tex, "south": tex, "east": side, "west": side, "up": ends, "down": ends}
        for v in list(frm) + list(to):
            if not -16 <= v <= 32:
                raise ValueError(f"Model coordinate {v} outside Minecraft's -16..32 range: {frm} -> {to}")
        el = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to],
              "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#" + face_tex[f]} for f in faces}}
        if uvy:
            ya, yb = uvy

            def v(y):
                return round(max(0.0, min(16.0, 16 * (yb - y) / (yb - ya))), 3)
            for f in ("north", "south", "east", "west"):
                if f in el["faces"]:
                    el["faces"][f]["uv"] = [0, v(to[1]), 16, v(frm[1])]
        if rot:
            origin, axis, angle = rot
            el["rotation"] = {"origin": [round(v, 3) for v in origin], "axis": axis, "angle": angle}
        if glow:
            el["neoforge_data"] = {"block_light": 15, "sky_light": 15}
        self.elements.append(el)
        return self

    def json(self, textures, display, particle):
        tex = dict(textures)
        tex["particle"] = particle
        return {"credit": "Steelstorm Arsenal (generated)", "texture_size": [16, 16], "textures": tex,
                "elements": self.elements, "display": display}


def blade_column(m, x0, x1, z0, z1, y0, y1, tip_len, steps=3, mat="blade", edge="edge", glow=False):
    """A straight blade with a stepped, tapering tip."""
    m.box((x0, y0, z0), (x1, y1, z1), mat, edge, edge, glow=glow)
    w = x1 - x0
    cx = (x0 + x1) / 2
    zc = (z0 + z1) / 2
    th = z1 - z0
    y = y1
    for k in range(steps):
        frac = 1 - (k + 1) / (steps + 0.6)
        hw = w / 2 * frac
        h = tip_len / steps
        m.box((cx - hw, y, zc - th / 2 * (0.9 - 0.15 * k)), (cx + hw, y + h, zc + th / 2 * (0.9 - 0.15 * k)), mat, edge, edge,
              glow=glow)
        y += h


def longsword_model(legend=None):
    m = Model()
    m.box((7, -1.5, 7), (9, 0.5, 9), "trim")
    m.box((7.5, -1, 6.8), (8.5, 0, 9.2), "gem")
    m.box((7.25, 0.5, 7.25), (8.75, 8, 8.75), "grip")
    m.box((3, 8, 7), (13, 9.5, 9), "trim")
    m.box((2.2, 8.4, 7.2), (3, 10.6, 8.8), "trim")
    m.box((13, 8.4, 7.2), (13.8, 10.6, 8.8), "trim")
    m.box((7.3, 8.2, 6.7), (8.7, 9.3, 9.3), "gem")
    m.box((6.6, 9.5, 7.6), (9.4, 11, 8.4), "blade", "edge", "edge")
    blade_column(m, 6.4, 9.6, 7.6, 8.4, 11, 26, 4.6)
    m.box((7.6, 11.5, 7.5), (8.4, 24, 8.5), "fuller", glow=bool(legend))
    if legend == "tempest_edge":
        for sgn in (-1, 1):
            m.box((8 + sgn * 6.2 - 0.6, 9.5, 7.3), (8 + sgn * 6.2 + 0.6, 13.5, 8.7), "trim",
                  rot=((8 + sgn * 6.2, 9.5, 8), "z", -22.5 * sgn))
    return m


def greatsword_model(legend=None):
    m = Model()
    m.box((6.5, -4.5, 6.5), (9.5, -1.5, 9.5), "trim")
    m.box((7.3, -3.7, 6.2), (8.7, -2.3, 9.8), "gem")
    m.box((7, -1.5, 7), (9, 9, 9), "grip")
    m.box((6.7, 3.5, 6.7), (9.3, 4.5, 9.3), "trim")
    m.box((1, 9, 6.8), (15, 11, 9.2), "trim")
    m.box((0, 10, 7), (1.5, 13, 9), "trim", rot=((0.75, 10, 8), "z", 22.5))
    m.box((14.5, 10, 7), (16, 13, 9), "trim", rot=((15.25, 10, 8), "z", -22.5))
    m.box((6, 8.5, 6.5), (10, 12, 9.5), "trim")
    m.box((7, 9.5, 6.3), (9, 11.5, 9.7), "gem")
    blade_column(m, 5.25, 10.75, 7.4, 8.6, 12, 27.4, 4.2)
    m.box((7.3, 12.8, 7.3), (8.7, 26, 8.7), "fuller", glow=bool(legend))
    if legend == "rimecleaver":
        for sgn in (-1, 1):
            m.box((8 + sgn * 7 - 0.7, 11, 7.2), (8 + sgn * 7 + 0.7, 16, 8.8), "edge", rot=((8 + sgn * 7, 11, 8), "z", -22.5 * sgn))
            m.box((8 + sgn * 5 - 0.5, 11, 7.4), (8 + sgn * 5 + 0.5, 14, 8.6), "edge", rot=((8 + sgn * 5, 11, 8), "z", -45 * sgn))
    return m


def katana_model(legend=None):
    m = Model()
    m.box((7.2, -3, 7.2), (8.8, -2, 8.8), "trim")
    m.box((7.25, -2, 7.25), (8.75, 8, 8.75), "grip")
    m.box((5.4, 8, 5.4), (10.6, 8.6, 10.6), "trim")
    m.box((5.4, 8, 5.4), (10.6, 8.6, 10.6), "trim", rot=((8, 8.3, 8), "y", 45))
    m.box((7.1, 8.6, 7.4), (8.9, 9.6, 8.6), "collar")
    segs = [(9.6, 16, 0.0, 1.6), (16, 21, -0.2, 1.55), (21, 25, -0.45, 1.5), (25, 28, -0.75, 1.4)]
    for (y0, y1, dx, w) in segs:
        m.box((8 - w / 2 + dx, y0, 7.75), (8 + w / 2 + dx, y1, 8.25), "blade", "edge", "edge")
    m.box((8 - 0.55 - 1.1, 28, 7.78), (8 + 0.55 - 1.1, 29.6, 8.22), "blade", "edge", "edge")
    m.box((8 - 0.3 - 1.45, 29.6, 7.8), (8 + 0.3 - 1.45, 30.6, 8.2), "edge")
    if legend == "moonveil":
        for (y0, y1, dx, w) in segs:
            m.box((8 + w / 2 + dx - 0.35, y0, 7.7), (8 + w / 2 + dx, y1, 8.3), "glow", glow=True)
        m.box((4.6, 8.05, 7.5), (6.4, 8.55, 8.5), "trim", rot=((5.5, 8.3, 8), "y", 45))
        m.box((9.6, 8.05, 7.5), (11.4, 8.55, 8.5), "trim", rot=((10.5, 8.3, 8), "y", 45))
    return m


def dagger_model(legend=None):
    m = Model()
    m.box((7.2, -1.5, 7.6), (8.8, 0, 8.4), "trim")
    m.box((7.4, 0, 7.4), (8.6, 5, 8.6), "grip")
    m.box((5, 5, 7.2), (11, 6, 8.8), "trim")
    m.box((4.5, 5.5, 7.4), (5, 7, 8.6), "trim")
    m.box((11, 5.5, 7.4), (11.5, 7, 8.6), "trim")
    m.box((7.4, 5.1, 7.0), (8.6, 5.9, 9.0), "gem", glow=legend == "bloodfang")
    blade_column(m, 6.9, 9.1, 7.65, 8.35, 6, 15, 2.8)
    if legend == "bloodfang":
        for y in (7.5, 9.5, 11.5, 13.5):
            m.box((9.0, y, 7.75), (9.8, y + 1, 8.25), "edge", rot=((9.0, y, 8), "z", 45))
    return m


def spear_model(legend=None):
    m = Model()
    m.box((7.2, -15, 7.2), (8.8, -13, 8.8), "trim")
    m.box((7.4, -13, 7.4), (8.6, 22, 8.6), "shaft", "shaft", "trim")
    m.box((7.25, 2, 7.25), (8.75, 10, 8.75), "grip")
    m.box((7.1, 21, 7.1), (8.9, 23.5, 8.9), "trim")
    m.box((5, 22.5, 7.6), (11, 23.2, 8.4), "trim")
    m.box((6.6, 23.5, 7.6), (9.4, 26, 8.4), "blade", "edge", "edge")
    m.box((6.2, 26, 7.62), (9.8, 28, 8.38), "blade", "edge", "edge")
    m.box((6.7, 28, 7.65), (9.3, 29.8, 8.35), "blade", "edge", "edge")
    m.box((7.2, 29.8, 7.7), (8.8, 31, 8.3), "blade", "edge", "edge")
    m.box((7.65, 31, 7.75), (8.35, 32, 8.25), "edge")
    m.box((7.7, 23.5, 7.5), (8.3, 30.5, 8.5), "edge", glow=bool(legend))
    m.box((7.4, 21.8, 6.9), (8.6, 22.8, 9.1), "gem")
    if legend == "skypiercer":
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.5 - 1.6, 19, 7.9), (8 + sgn * 2.5 + 1.6, 24, 8.1), "wing", rot=((8 + sgn * 1, 22, 8), "z", -22.5 * sgn))
            m.box((8 + sgn * 3.8 - 1.2, 17.5, 7.95), (8 + sgn * 3.8 + 1.2, 22, 8.05), "wing", rot=((8 + sgn * 2, 21, 8), "z", -45 * sgn))
    return m


def warhammer_model(legend=None):
    m = Model()
    m.box((7, -11.5, 7), (9, -9.5, 9), "trim")
    m.box((7.3, -9.5, 7.3), (8.7, 17, 8.7), "shaft", "shaft", "trim")
    m.box((7.15, -8, 7.15), (8.85, 2, 8.85), "grip")
    m.box((2, 15.5, 5), (14, 22.5, 11), "metal")
    m.box((1, 16, 5.5), (2, 22, 10.5), "metal")
    m.box((14, 16, 5.5), (15, 22, 10.5), "metal")
    m.box((6.5, 15, 4.6), (9.5, 23, 11.4), "trim")
    m.box((7.3, 17.8, 4.3), (8.7, 20.2, 11.7), "gem", glow=bool(legend))
    m.box((7.2, 22.5, 7.2), (8.8, 25, 8.8), "metal")
    m.box((7.6, 25, 7.6), (8.4, 26.6, 8.4), "metal")
    if legend == "earthshaker":
        for x0, y0 in ((3, 17), (4.5, 19.5), (11, 16.5), (10, 19.8)):
            m.box((x0, y0, 4.85), (x0 + 1.6, y0 + 0.6, 11.15), "glow", glow=True)
        m.box((2.6, 18.7, 4.85), (5.2, 19.2, 11.15), "glow", glow=True)
        m.box((10.6, 18, 4.85), (13.4, 18.5, 11.15), "glow", glow=True)
    return m


def scythe_model(legend=None):
    m = Model()
    m.box((7.4, -14, 7.4), (8.6, 26, 8.6), "shaft", "shaft", "trim")
    m.box((7.25, -12, 7.25), (8.75, -4, 8.75), "grip")
    m.box((8.6, 8, 7.5), (12, 9, 8.5), "grip")
    m.box((7, 23, 7), (9, 26.5, 9), "trim")
    m.box((7.3, 24, 6.8), (8.7, 25.4, 9.2), "gem", glow=bool(legend))
    steps = [(2.0, 8.0, 23.4, 27.0), (-3.0, 2.0, 22.0, 25.6), (-7.0, -3.0, 20.2, 23.6), (-10.0, -7.0, 18.4, 21.2),
             (-12.0, -10.0, 16.8, 18.9), (-13.4, -12.0, 15.8, 17.2)]
    for (x0, x1, y0, y1) in steps:
        m.box((x0, y0, 7.75), (x1, y1, 8.25), "blade", "edge", "edge")
        m.box((x0, y0 - 0.3, 7.8), (x1, y0 + 0.35, 8.2), "edge" if not legend else "glow", glow=bool(legend))
    if legend == "voidreaver":
        m.box((8.6, 24.5, 7.75), (11.5, 26, 8.25), "blade", "edge", "edge")
        m.box((11.5, 22.5, 7.8), (12.8, 26, 8.2), "blade", "edge", "edge")
    return m


def battleaxe_model(legend=None):
    m = Model()
    m.box((7, -9.5, 7), (9, -8, 9), "trim")
    m.box((7.3, -8, 7.3), (8.7, 22, 8.7), "shaft", "shaft", "trim")
    m.box((7.15, -6, 7.15), (8.85, 3, 8.85), "grip")
    m.box((6.6, 13, 6.6), (9.4, 21, 9.4), "trim")
    m.box((7.4, 16, 6.3), (8.6, 18, 9.7), "gem", glow=legend == "kingsbane")
    for sgn in (-1, 1):
        def X(a, b):
            lo, hi = 8 + sgn * a, 8 + sgn * b
            return (min(lo, hi), max(lo, hi))
        x0, x1 = X(1.4, 4.0)
        m.box((x0, 14, 7.6), (x1, 20, 8.4), "metal")
        x0, x1 = X(4.0, 6.5)
        m.box((x0, 12.5, 7.65), (x1, 21.5, 8.35), "metal")
        x0, x1 = X(6.5, 8.0)
        m.box((x0, 11, 7.7), (x1, 23, 8.3), "edge" if legend != "kingsbane" else "glow", glow=legend == "kingsbane")
    m.box((7.4, 21, 7.4), (8.6, 24.5, 8.6), "metal")
    m.box((7.7, 24.5, 7.7), (8.3, 26, 8.3), "metal")
    if legend == "kingsbane":
        for x in (6.2, 8.0, 9.8):
            m.box((x - 0.4, 21, 7.6), (x + 0.4, 23.5 if x != 8.0 else 27, 8.4), "trim")
    return m


def chakram_model():
    m = Model()
    r = 6.0
    seg = 2 * r * math.tan(math.radians(22.5)) + 0.5
    for k in range(8):
        a = k * 45
        cx = 8 + r * math.cos(math.radians(a))
        cy = 8 + r * math.sin(math.radians(a))
        if a % 90 == 0:
            if a in (0, 180):
                m.box((cx - 0.8, cy - seg / 2, 7.6), (cx + 0.8, cy + seg / 2, 8.4), "blade", "edge", "edge")
            else:
                m.box((cx - seg / 2, cy - 0.8, 7.6), (cx + seg / 2, cy + 0.8, 8.4), "blade", "edge", "edge")
        else:
            m.box((cx - seg / 2, cy - 0.8, 7.6), (cx + seg / 2, cy + 0.8, 8.4), "blade", "edge", "edge",
                  rot=((cx, cy, 8), "z", 45 if a in (135, 315) else -45))
        tx = 8 + (r + 1.4) * math.cos(math.radians(a + 22.5))
        ty = 8 + (r + 1.4) * math.sin(math.radians(a + 22.5))
        m.box((tx - 0.9, ty - 0.45, 7.75), (tx + 0.9, ty + 0.45, 8.25), "edge",
              rot=((tx, ty, 8), "z", 22.5 if (a // 45) % 2 == 0 else -22.5))
    m.box((3, 7.4, 7.7), (13, 8.6, 8.3), "grip")
    m.box((7.3, 7.3, 7.4), (8.7, 8.7, 8.6), "trim")
    return m


def knife_model():
    m = Model()
    m.box((7.3, -1.2, 7.8), (8.7, 0, 8.2), "trim")
    m.box((7.6, 0, 7.6), (8.4, 4, 8.4), "grip")
    m.box((7.0, 4, 7.75), (9.0, 6, 8.25), "blade", "edge", "edge")
    m.box((6.8, 6, 7.76), (9.2, 8, 8.24), "blade", "edge", "edge")
    m.box((7.2, 8, 7.8), (8.8, 9.5, 8.2), "blade", "edge", "edge")
    m.box((7.6, 9.5, 7.85), (8.4, 10.5, 8.15), "edge")
    return m


# Display transforms for a model built with the blade along +Y, centred on x = z = 8.
# `grip` is the model y where the hand should hold it; the pivot is the model centre (y = 8),
# so the model is slid along its own axis by (8 - grip) to put that point in the hand.
# Hand anchors (in model pixels): where the hold point of a weapon should end up in each view.
HAND_TP = (0.0, 0.4, 1.0)
HAND_FP = (1.13, 2.0, 0.15)


def _offset(d, scale, tilt, yaw):
    """Where model offset (0, d, 0) from the centre lands after rotation [0, yaw, tilt] and scale."""
    t, f = math.radians(tilt), math.radians(yaw)
    x, y = -d * math.sin(t), d * math.cos(t)
    return (x * math.cos(f) * scale, y * scale, -x * math.sin(f) * scale)


def display(scale_tp=0.62, scale_fp=0.46, grip=4.0, tilt=10, fp_tilt=25, fp_yaw=-60):
    """Display transforms for a model built along +Y and centred on x = z = 8.

    `grip` is the model y the hand closes around. The translation is solved so that this point,
    after the tilt and scale, lands exactly on the hand anchor, so long-hafted weapons sit in
    the fist instead of floating beside it however far they are tilted. `fp_yaw` = 90 turns a
    model round in first person (the scythe, whose model is mirrored to face forward in third person).
    """
    d = grip - 8

    def solve(anchor, scale, tilt, yaw):
        o = _offset(d, scale, tilt, yaw)
        return [round(anchor[i] - o[i], 3) for i in range(3)]

    tp = solve(HAND_TP, scale_tp, tilt, 90)
    fp = solve(HAND_FP, scale_fp, fp_tilt, fp_yaw)
    return {
        "thirdperson_righthand": {"rotation": [0, 90, tilt], "translation": tp, "scale": [scale_tp] * 3},
        "thirdperson_lefthand": {"rotation": [0, -90, -tilt], "translation": tp, "scale": [scale_tp] * 3},
        "firstperson_righthand": {"rotation": [0, fp_yaw, fp_tilt], "translation": fp, "scale": [scale_fp] * 3},
        "firstperson_lefthand": {"rotation": [0, -fp_yaw, -fp_tilt], "translation": fp, "scale": [scale_fp] * 3},
        "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "gui": {"rotation": [30, 225, 0], "scale": [0.6] * 3},
        "fixed": {"rotation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    }


TYPE_MODELS = {
    "longsword": (longsword_model, display(0.86, 0.66, -5.0, 10)),
    "greatsword": (greatsword_model, display(0.74, 0.6, -3.0, 15)),
    "katana": (katana_model, display(0.78, 0.6, -2.5, 10)),
    "dual_daggers": (dagger_model, display(0.85, 0.75, 0.5, 0)),
    "spear": (spear_model, display(0.7, 0.56, 6.0, 5)),
    "warhammer": (warhammer_model, display(0.64, 0.58, -4.0, 15)),
    "scythe": (scythe_model, display(0.62, 0.55, -8.0, 20, fp_tilt=-25, fp_yaw=120)),
    "battleaxe": (battleaxe_model, display(0.72, 0.58, -2.5, 15)),
}

TYPE_VARS = ["blade", "edge", "fuller", "metal", "trim", "gem", "grip", "shaft", "collar"]


def tier_textures(kind, tier):
    blade = {"stone": "stone", "iron": "iron", "golden": "gold", "diamond": "diamond", "netherite": "netherite",
             "stormsteel": "stormsteel"}[tier]
    trim = TIER_TRIM[tier]
    gem = TIER_GEM[tier] or "topaz"
    grip = TIER_GRIP[tier]
    shaft = "black_iron" if tier == "netherite" else ("birch" if tier == "stormsteel" else "wood")
    return {
        "blade": f"steelstorm:item/3d/blade_{blade}", "edge": f"steelstorm:item/3d/edge_{blade}",
        "fuller": f"steelstorm:item/3d/fuller_{blade}", "metal": f"steelstorm:item/3d/metal_{blade}",
        "trim": f"steelstorm:item/3d/trim_{trim}", "gem": f"steelstorm:item/3d/gem_{gem}",
        "grip": "steelstorm:item/3d/grip_ito" if kind == "katana" else f"steelstorm:item/3d/grip_{grip}",
        "shaft": f"steelstorm:item/3d/shaft_{shaft}", "collar": "steelstorm:item/3d/trim_gold",
    }


LEGENDARY_MODELS = {
    "tempest_edge": ("longsword", {"blade": "tempest", "trim": "gold", "gem": "aqua", "grip": "dark_leather", "glow": "lightning"}),
    "rimecleaver": ("greatsword", {"blade": "ice", "trim": "silver", "gem": "aqua", "grip": "blue_wrap", "glow": "frost"}),
    "voidreaver": ("scythe", {"blade": "void", "trim": "obsidian", "gem": "amethyst", "grip": "dark_leather", "glow": "void",
                              "shaft": "obsidian"}),
    "earthshaker": ("warhammer", {"blade": "earth", "trim": "gold", "gem": "topaz", "grip": "dark_leather", "glow": "ember",
                                  "shaft": "black_iron"}),
    "bloodfang": ("dual_daggers", {"blade": "blood", "trim": "black_iron", "gem": "ruby", "grip": "dark_leather", "glow": "blood"}),
    "skypiercer": ("spear", {"blade": "sky", "trim": "gold", "gem": "sapphire", "grip": "blue_wrap", "glow": "sky", "shaft": "birch"}),
    "moonveil": ("katana", {"blade": "moon", "trim": "silver", "gem": "sapphire", "grip": "blue_wrap", "glow": "moon"}),
    "kingsbane": ("battleaxe", {"blade": "obsidian", "trim": "gold", "gem": "ruby", "grip": "red_wrap", "glow": "royal",
                                "shaft": "black_iron"}),
}


def legendary_textures(spec, kind):
    b = spec["blade"]
    tex = {
        "blade": f"steelstorm:item/3d/blade_{b}", "edge": f"steelstorm:item/3d/edge_{b}",
        "fuller": f"steelstorm:item/3d/glow_{spec['glow']}", "metal": f"steelstorm:item/3d/metal_{b}",
        "trim": f"steelstorm:item/3d/trim_{spec['trim']}", "gem": f"steelstorm:item/3d/gem_{spec['gem']}",
        "grip": "steelstorm:item/3d/grip_ito" if kind == "katana" else f"steelstorm:item/3d/grip_{spec['grip']}",
        "shaft": f"steelstorm:item/3d/shaft_{spec.get('shaft', 'wood')}", "collar": "steelstorm:item/3d/trim_gold",
        "glow": f"steelstorm:item/3d/glow_{spec['glow']}", "wing": "steelstorm:item/3d/trim_cloth_white",
    }
    return tex


def write_models():
    os.makedirs(MODELS, exist_ok=True)
    for kind, (fn, disp) in TYPE_MODELS.items():
        m = fn()
        tex = {v: "steelstorm:item/3d/blade_iron" for v in TYPE_VARS}
        data = m.json(tex, disp, "#blade")
        data["textures"] = {k: "#" + k if False else v for k, v in data["textures"].items()}
        # Parent models only declare variables; children (datagen) fill them in.
        data["textures"] = {"particle": "#blade"}
        with open(os.path.join(MODELS, f"{kind}.json"), "w") as f:
            json.dump(data, f, indent=1)
    # Legendary models are built by gen_tier_models.py alongside the tier models.
    for name, m, tex in (("chakram", chakram_model(), {"blade": "steelstorm:item/3d/blade_iron", "edge": "steelstorm:item/3d/edge_iron",
                                                           "grip": "steelstorm:item/3d/grip_leather", "trim": "steelstorm:item/3d/trim_gold"}),
                         ("throwing_knife", knife_model(), {"blade": "steelstorm:item/3d/blade_iron", "edge": "steelstorm:item/3d/edge_iron",
                                                             "grip": "steelstorm:item/3d/grip_dark_leather",
                                                             "trim": "steelstorm:item/3d/trim_silver"})):
        disp = display(0.62, 0.5, 4.0 if name == 'chakram' else 2.0)
        data = m.json(tex, disp, tex["blade"])
        with open(os.path.join(MODELS, f"{name}.json"), "w") as f:
            json.dump(data, f, indent=1)


def main():
    write_textures()
    write_models()
    print("3D models and material textures written")


if __name__ == "__main__":
    main()
