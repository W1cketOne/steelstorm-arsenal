#!/usr/bin/env python3
"""The weapon models: every type in every tier, plus the legendaries.

Run from the repository root:  python3 tools/gen_pro_models.py

Builds each model from cuboids and paints a texture sheet for it (see pro_model.py). Models
keep the grip positions of the old ones, so the hand, first-person arm and animations line up.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_models import display  # noqa: E402
from pro_model import MODEL_DIR, Mat, ProModel, glow_ramp  # noqa: E402

S2 = math.sqrt(2)

# ----------------------------------------------------------------------------- tiers

GLOWS = {"lightning": ("#fffbe0", "#ffc61a"), "frost": ("#ffffff", "#5fd8ff"), "void": ("#f4ddff", "#9a2cff"),
         "ember": ("#fff1c9", "#ff6a00"), "blood": ("#ffd0d4", "#e01b35"), "sky": ("#ffffff", "#7cc8ff"),
         "moon": ("#ffffff", "#8db7ff"), "royal": ("#fff6c2", "#ffb000"), "aqua": ("#effffd", "#22e0c8"),
         "solar": ("#fffbe8", "#ff9d00"), "abyss": ("#c9fff4", "#00b39a")}

TIERS = {
    "stone": dict(blade="stone", metal="stone", trim="wood", grip="leather", shaft="wood", gem=None, glow=None, rough=0.22),
    "iron": dict(blade="iron", metal="iron", trim="black_iron", grip="leather", shaft="wood", gem=None, glow=None),
    "golden": dict(blade="gold", metal="gold", trim="gold", grip="red_wrap", shaft="wood", gem="ruby", glow=None),
    "diamond": dict(blade="diamond", metal="silver", trim="silver", grip="blue_wrap", shaft="birch", gem="aqua", glow="aqua"),
    "netherite": dict(blade="netherite", metal="netherite", trim="black_iron", grip="dark_leather", shaft="black_iron",
                      gem="ruby", glow="ember"),
    "stormsteel": dict(blade="stormsteel", metal="stormsteel", trim="gold", grip="teal_wrap", shaft="black_iron",
                       gem="aqua", glow="lightning"),
}

LEGENDS = {
    "tempest_edge": ("longsword", dict(blade="tempest", metal="tempest", trim="gold", grip="dark_leather", shaft="black_iron",
                                       gem="aqua", glow="lightning", sil="stormsteel")),
    "rimecleaver": ("greatsword", dict(blade="ice", metal="silver", trim="silver", grip="blue_wrap", shaft="birch", gem="aqua",
                                       glow="frost", sil="diamond")),
    "voidreaver": ("scythe", dict(blade="void", metal="obsidian", trim="obsidian", grip="dark_leather", shaft="obsidian",
                                  gem="amethyst", glow="void", sil="netherite")),
    "earthshaker": ("warhammer", dict(blade="earth", metal="earth", trim="gold", grip="dark_leather", shaft="black_iron",
                                      gem="topaz", glow="ember", sil="netherite")),
    "bloodfang": ("dual_daggers", dict(blade="blood", metal="black_iron", trim="black_iron", grip="dark_leather",
                                       shaft="black_iron", gem="ruby", glow="blood", sil="netherite")),
    "skypiercer": ("spear", dict(blade="sky", metal="gold", trim="gold", grip="blue_wrap", shaft="birch", gem="sapphire",
                                 glow="sky", sil="golden")),
    "moonveil": ("katana", dict(blade="moon", metal="silver", trim="silver", grip="blue_wrap", shaft="black_iron",
                                gem="sapphire", glow="moon", sil="diamond")),
    "kingsbane": ("battleaxe", dict(blade="obsidian", metal="obsidian", trim="gold", grip="red_wrap", shaft="black_iron",
                                    gem="ruby", glow="royal", sil="golden")),
    # Mythic weapons: forged from the legendaries, glowing all over.
    "solaris": ("longsword", dict(blade="gold", metal="brass", trim="gold", grip="red_wrap", shaft="black_iron", gem="topaz",
                                  glow="solar", sil="stormsteel", mythic=True)),
    "worldsplitter": ("greatsword", dict(blade="aqua", metal="black_iron", trim="silver", grip="teal_wrap", shaft="black_iron",
                                         gem="aqua", glow="abyss", sil="netherite", mythic=True)),
    "eclipse": ("katana", dict(blade="obsidian", metal="obsidian", trim="amethyst", grip="violet_wrap", shaft="black_iron",
                               gem="amethyst", glow="void", sil="netherite", mythic=True)),
    "starfall": ("warhammer", dict(blade="netherite", metal="netherite", trim="gold", grip="ember_wrap", shaft="black_iron",
                                   gem="topaz", glow="ember", sil="netherite", mythic=True)),
    "soulreaper": ("scythe", dict(blade="emerald", metal="black_iron", trim="silver", grip="green_wrap", shaft="obsidian",
                                  gem="emerald", glow="aqua", sil="netherite", mythic=True)),
    "venomfang": ("dual_daggers", dict(blade="emerald", metal="black_iron", trim="black_iron", grip="green_wrap",
                                       shaft="black_iron", gem="emerald", glow="aqua", sil="netherite", mythic=True)),
    "dragonspine": ("spear", dict(blade="ruby", metal="black_iron", trim="obsidian", grip="red_wrap", shaft="black_iron",
                                  gem="ruby", glow="ember", sil="stormsteel", mythic=True)),
    "titanbreaker": ("battleaxe", dict(blade="gold", metal="brass", trim="gold", grip="red_wrap", shaft="black_iron",
                                       gem="topaz", glow="solar", sil="netherite", mythic=True)),
}


class Kit:
    """The materials one weapon is made of."""

    def __init__(self, spec, sil):
        self.sil = sil
        self.spec = spec
        rough = spec.get("rough", 0.0)
        self.metal = Mat("metal", spec["metal"], rough=rough)
        self.trim = Mat("wood" if spec["trim"] == "wood" else "trim", spec["trim"], rough=rough)
        self.dark = Mat("metal", "black_iron" if spec["trim"] != "black_iron" else "netherite")
        self.grip = Mat("grip", spec["grip"])
        self.ito = Mat("ito", "black_iron", ramp2="cloth_white" if spec["grip"] in ("leather", "dark_leather") else spec["grip"])
        self.shaft = Mat("wood", spec["shaft"]) if spec["shaft"] in ("wood", "birch") else Mat("metal", spec["shaft"])
        self.gem = Mat("gem", spec["gem"], bevel=False, glow=bool(spec.get("glow"))) if spec.get("gem") else self.trim
        g = spec.get("glow")
        self.glow = Mat("glow", glow_ramp(*GLOWS[g]), glow=True, bevel=False, noise=0.0) if g else None
        self.cloth = Mat("cloth", {"golden": "red_wrap", "stone": "straw"}.get(sil, spec["grip"]))
        self.blade_ramp = spec["blade"]
        self.rough = rough

    def blade(self, **p):
        p.setdefault("hw", lambda y: 1.5 + 0 * y)
        return Mat("blade", self.blade_ramp, noise=0.02 + self.rough * 0.6, **p)

    def edge(self):
        return Mat("metal", self.blade_ramp, bevel=False, noise=0.02).with_(rough=0)


# ----------------------------------------------------------------------------- parts

def octagon(m, mat, cx, y0, y1, cz, r, side=None):
    """An eight-sided block (two squares, one turned 45 degrees), e.g. a pommel or tsuba."""
    a = r / (1 + S2 / 2) if False else r * 0.83
    m.box((cx - a, y0, cz - a), (cx + a, y1, cz + a), mat, side=side)
    m.box((cx - a, y0 + 0.01, cz - a), (cx + a, y1 - 0.01, cz + a), mat, side=side, rot=((cx, (y0 + y1) / 2, cz), "y", 45))


def wrap(m, mat, trim, y0, y1, half, cz=8.0, cx=8.0, ferrules=True):
    m.box((cx - half, y0, cz - half), (cx + half, y1, cz + half), mat)
    if ferrules:
        f = half + 0.18
        m.box((cx - f, y0 - 0.2, cz - f), (cx + f, y0 + 0.5, cz + f), trim)
        m.box((cx - f, y1 - 0.5, cz - f), (cx + f, y1 + 0.2, cz + f), trim)


def straight_blade(m, k, cx, y0, y1, w0, w1, th=0.9, ridge=True, fuller=None, tip=1.2, steps=4, mat=None, single=None,
                   cz=8.0, hamon=False, z_ridge=0.28):
    """A tapering two-edged blade with a raised centre ridge and a sharp pointed tip.

    The tip is two half-width plates leaned in 22.5 degrees from their outer corners so they meet
    in a point; `tip` scales its length relative to the blade's width there.
    """
    def hw(y):
        f = (y - y0) / max(1e-3, (y1 - y0))
        base = w0 / 2 + (w1 / 2 - w0 / 2) * f
        import numpy as np
        over = np.clip((y - y1) / (w1 * 1.115 * tip), 0, 1)
        return base * (1 - over) if hasattr(over, "shape") else base
    mat = mat or k.blade(hw=lambda y: hw_np(y), span=(y0, y1 + w1), fuller=fuller, single=single, hamon=hamon, cx=cx)

    def hw_np(y):
        import numpy as np
        f = np.clip((y - y0) / max(1e-3, (y1 - y0)), 0, 1)
        base = w0 / 2 + (w1 / 2 - w0 / 2) * f
        over = np.clip((y - y1) / (w1 * 1.115 * tip), 0, 1)
        return base * (1 - over * 0.98)
    seg = (y1 - y0) / steps
    for i in range(steps):
        ya, yb = y0 + i * seg, y0 + (i + 1) * seg
        w = w0 + (w1 - w0) * (i + 0.5) / steps
        m.box((cx - w / 2, ya, cz - th / 2), (cx + w / 2, yb, cz + th / 2), mat)
        if ridge:
            rw = max(0.6, w * 0.32)
            m.box((cx - rw / 2, ya, cz - th / 2 - z_ridge), (cx + rw / 2, yb, cz + th / 2 + z_ridge), mat)
    # Tip: two leaning half-plates (the right one slightly thinner so their faces never z-fight).
    h = w1 * 1.207 * tip
    for sgn, thin in ((-1, 0.0), (1, 0.06)):
        if single and sgn != single:
            # Spine side of a single-edged blade: a short straight shoulder.
            m.box((cx + min(0, sgn * w1 / 2), y1, cz - th / 2 + 0.02), (cx + max(0, sgn * w1 / 2), y1 + h * 0.25, cz + th / 2 - 0.02), mat)
            continue
        ox = cx + sgn * w1 / 2
        x0, x1 = sorted((ox, ox - sgn * w1 / 2))
        m.box((x0, y1 - 0.01, cz - th / 2 + thin), (x1, y1 + h, cz + th / 2 - thin), mat,
              rot=((ox, y1, cz), "z", 22.5 * sgn))
    return mat


def gem(m, k, cx, cy, cz, r, depth=None, rot45=True):
    d = depth if depth is not None else r
    m.box((cx - r, cy - r, cz - d), (cx + r, cy + r, cz + d), k.gem, rot=((cx, cy, cz), "z", 45) if rot45 else None)


# ----------------------------------------------------------------------------- guards

def crossguard(m, k, y, half, th=1.4, depth=1.9, sil=None):
    """A crossguard centred on x = 8 at height y, styled by silhouette."""
    sil = sil or k.sil
    z0, z1 = 8 - depth / 2, 8 + depth / 2
    if sil == "stone":
        m.box((8 - half * 0.8, y, z0 - 0.2), (8 + half * 0.8, y + th + 0.3, z1 + 0.2), k.metal)
        m.box((8 - 1.4, y - 0.4, z0 - 0.3), (8 + 1.4, y + th + 0.6, z1 + 0.3), k.trim)
        return
    # Centre block, slightly proud of the bar, with a gem or boss on both faces.
    m.box((8 - 1.5, y - 0.6, z0 - 0.25), (8 + 1.5, y + th + 0.6, z1 + 0.25), k.trim)
    m.box((8 - half, y, z0), (8 + half, y + th, z1), k.trim)
    if k.spec.get("gem"):
        gem(m, k, 8, y + th / 2, 8, 0.75, depth=depth / 2 + 0.4)
    if sil == "iron":
        for sgn in (-1, 1):
            x = 8 + sgn * half
            xa, xb = sorted((x, x - sgn * 1.2))
            m.box((xa, y - 0.3, z0 + 0.1), (xb, y + th + 0.9, z1 - 0.1), k.trim)
    elif sil == "golden":
        for sgn in (-1, 1):
            x = 8 + sgn * (half - 0.3)
            m.box((x - 0.55, y + 0.4, z0 + 0.15), (x + 0.55, y + 3.6, z1 - 0.15), k.trim, rot=((x, y + 0.4, 8), "z", -22.5 * sgn))
            gem(m, k, x + sgn * 1.22, y + 3.6, 8, 0.6, depth=0.55)
        m.box((8 - 1.2, y - 2.0, z0 - 0.1), (8 + 1.2, y, z1 + 0.1), k.trim)
        m.box((8 - 0.6, y - 2.6, z0 + 0.2), (8 + 0.6, y - 2.0, z1 - 0.2), k.trim)
    elif sil == "diamond":
        for sgn in (-1, 1):
            x = 8 + sgn * (half - 0.4)
            m.box((x - 0.5, y + 0.3, z0 + 0.35), (x + 0.5, y + 4.2, z1 - 0.35), k.edge(), rot=((x, y + 0.3, 8), "z", -45 * sgn))
            m.box((x - 0.4, y - 2.4, z0 + 0.45), (x + 0.4, y + 0.2, z1 - 0.45), k.edge(), rot=((x, y + 0.2, 8), "z", 22.5 * sgn))
    elif sil == "netherite":
        for sgn in (-1, 1):
            x = 8 + sgn * half
            m.box((x - 0.5, y - 2.8, z0 + 0.3), (x + 0.5, y + 0.6, z1 - 0.3), k.metal, rot=((x, y + 0.6, 8), "z", 22.5 * sgn))
            m.box((x - 0.4, y + th - 0.2, z0 + 0.4), (x + 0.4, y + th + 2.0, z1 - 0.4), k.metal, rot=((x, y + th, 8), "z", -45 * sgn))
        if k.glow:
            m.box((8 - half + 1, y + th / 2 - 0.2, z0 - 0.05), (8 + half - 1, y + th / 2 + 0.2, z1 + 0.05), k.glow)
    elif sil == "stormsteel":
        for sgn in (-1, 1):
            # Swept wings, stepped like feathers.
            for i, (dx, dy, ln) in enumerate(((half - 0.6, 0.2, 3.6), (half - 2.0, 0.4, 2.8), (half - 3.3, 0.6, 2.0))):
                x = 8 + sgn * dx
                m.box((x - 0.45, y + dy, z0 + 0.2 + i * 0.1), (x + 0.45, y + dy + ln, z1 - 0.2 - i * 0.1), k.trim,
                      rot=((x, y + dy, 8), "z", -45 * sgn if i == 0 else -22.5 * sgn))
        if k.glow:
            m.box((8 - 0.35, y - 1.6, 7.7), (8 + 0.35, y, 8.3), k.glow)


def pommel(m, k, y_top, size=1.3, sil=None):
    sil = sil or k.sil
    if sil in ("stone",):
        m.box((8 - size, y_top - 2.0, 8 - size), (8 + size, y_top, 8 + size), k.metal)
        return y_top - 2.0
    if sil == "golden":
        octagon(m, k.trim, 8, y_top - 1.8, y_top, 8, size)
        gem(m, k, 8, y_top - 0.9, 8, 0.6, depth=size + 0.15)
        m.box((8 - 0.5, y_top - 2.6, 8 - 0.5), (8 + 0.5, y_top - 1.8, 8 + 0.5), k.trim)
        return y_top - 2.6
    if sil == "netherite":
        octagon(m, k.metal, 8, y_top - 1.6, y_top, 8, size)
        m.box((8 - 0.4, y_top - 3.2, 8 - 0.4), (8 + 0.4, y_top - 1.6, 8 + 0.4), k.metal)
        return y_top - 3.2
    if sil == "diamond":
        m.box((8 - size * 0.8, y_top - 2.2, 8 - size * 0.8), (8 + size * 0.8, y_top, 8 + size * 0.8), k.gem,
              rot=((8, y_top - 1.1, 8), "y", 45))
        m.box((8 - size, y_top - 0.6, 8 - size), (8 + size, y_top, 8 + size), k.trim)
        return y_top - 2.2
    octagon(m, k.trim, 8, y_top - 1.8, y_top, 8, size)
    m.box((8 - size * 0.5, y_top - 2.3, 8 - size * 0.5), (8 + size * 0.5, y_top - 1.8, 8 + size * 0.5), k.trim)
    if k.glow and sil == "stormsteel":
        m.box((8 - 0.5, y_top - 1.3, 8 - size - 0.05), (8 + 0.5, y_top - 0.5, 8 + size + 0.05), k.glow)
    return y_top - 2.3


def inlay(m, k, cx, y0, y1, w=0.5, cz=8.0, th=1.62, zig=False):
    """A glowing strip set down the middle of a blade (zig-zagging lightning for stormsteel)."""
    if not k.glow:
        return
    if zig:
        n = max(3, int((y1 - y0) / 3))
        h = (y1 - y0) / n
        for i in range(n):
            dx = 0.3 if i % 2 == 0 else -0.3
            m.box((cx + dx - w / 2, y0 + i * h, cz - th / 2), (cx + dx + w / 2, y0 + (i + 1) * h + 0.2, cz + th / 2), k.glow)
    else:
        m.box((cx - w / 2, y0, cz - th / 2), (cx + w / 2, y1, cz + th / 2), k.glow)


# ----------------------------------------------------------------------------- weapons

def longsword(name, k):
    m = ProModel(name)
    sil = k.sil
    pommel(m, k, -9.0, 1.25)
    grip_mat = k.ito if False else k.grip
    wrap(m, grip_mat, k.trim, -9.0, -1.0, 0.78)
    crossguard(m, k, -1.0, 6.0 if sil != "stone" else 4.8)
    # Ricasso: the blunt, thicker root of the blade above the guard.
    m.box((8 - 1.45, 0.5, 7.45), (8 + 1.45, 2.4, 8.55), k.metal if sil != "stone" else k.blade())
    fuller = None if sil in ("stone", "netherite", "stormsteel") or k.spec.get("legend") else (3.0, 20.0, 0.42)
    straight_blade(m, k, 8, 2.4, 27.0, 3.4, 2.6, fuller=fuller, tip=1.0, steps=5)
    if sil == "netherite":
        inlay(m, k, 8, 3.0, 24.0, w=0.45)
        for y in (8, 13, 18):
            for sgn in (-1, 1):
                x = 8 + sgn * 1.7
                m.box((x - 0.35, y, 7.65), (x + 0.35, y + 1.1, 8.35), k.blade(hw=lambda yy: 1.7 + 0 * yy), rot=((x, y, 8), "z", 45))
    if sil == "stormsteel":
        inlay(m, k, 8, 3.0, 24.0, w=0.42, zig=True)
    if sil == "diamond":
        for sgn in (-1, 1):
            m.box((8 + sgn * 1.0 - 0.35, 2.4, 7.6), (8 + sgn * 1.0 + 0.35, 5.5, 8.4), k.gem, rot=((8 + sgn * 1.0, 2.4, 8), "z", -22.5 * sgn))
    if k.spec.get("legend") == "tempest_edge":
        for sgn in (-1, 1):
            m.box((8 + sgn * 6.4 - 0.5, 0.5, 7.4), (8 + sgn * 6.4 + 0.5, 4.5, 8.6), k.trim, rot=((8 + sgn * 6.4, 0.5, 8), "z", -22.5 * sgn))
    return m


def greatsword(name, k):
    m = ProModel(name)
    sil = k.sil
    pommel(m, k, -9.5, 1.5)
    wrap(m, k.grip, k.trim, -9.5, 1.8, 0.9)
    m.box((8 - 1.1, -4.4, 8 - 1.1), (8 + 1.1, -3.6, 8 + 1.1), k.trim)
    crossguard(m, k, 1.8, 7.4 if sil != "stone" else 5.6, th=1.6, depth=2.2)
    # Long ricasso with parrying lugs, the classic zweihander profile.
    m.box((8 - 1.6, 3.8, 7.35), (8 + 1.6, 8.4, 8.65), k.metal)
    m.box((8 - 1.7, 4.6, 7.4), (8 + 1.7, 7.6, 8.6), k.grip)
    for sgn in (-1, 1):
        x = 8 + sgn * 2.4
        m.box((x - 0.8, 8.0, 7.5), (x + 0.8, 9.2, 8.5), k.metal, rot=((x, 8.6, 8), "z", 45))
    fuller = None if sil in ("stone", "netherite", "stormsteel") or k.spec.get("legend") else (9.5, 24.0, 0.55)
    straight_blade(m, k, 8, 8.4, 27.6, 5.4, 4.2, fuller=fuller, tip=0.62, steps=6, th=1.0)
    if sil == "netherite":
        inlay(m, k, 8, 9.0, 25.0, w=0.55)
        for y in (11, 16, 21):
            for sgn in (-1, 1):
                x = 8 + sgn * 2.55
                m.box((x - 0.45, y, 7.62), (x + 0.45, y + 1.3, 8.38), k.edge(), rot=((x, y, 8), "z", 45))
    if sil == "stormsteel":
        inlay(m, k, 8, 9.0, 25.0, w=0.5, zig=True)
    if sil == "diamond" or k.spec.get("legend") == "rimecleaver":
        for sgn in (-1, 1):
            for (dx, y, ln) in ((2.9, 10, 3.2), (2.8, 15.5, 2.6)):
                x = 8 + sgn * dx
                m.box((x - 0.4, y, 7.55), (x + 0.4, y + ln, 8.45), k.edge(), rot=((x, y, 8), "z", -45 * sgn))
    if k.spec.get("legend") == "rimecleaver":
        inlay(m, k, 8, 9.0, 25.5, w=0.55)
    return m


def katana(name, k):
    m = ProModel(name)
    leg = k.spec.get("legend")
    # Kashira (end cap), tsuka with ito wrap, tsuba, habaki.
    m.box((8 - 0.95, -10.2, 8 - 0.95), (8 + 0.95, -9.0, 8 + 0.95), k.trim)
    m.box((7.3, -9.0, 7.3), (8.7, 3.0, 8.7), k.ito)
    for y in (-6.0, -0.5):
        m.box((7.2, y, 7.75), (8.8, y + 1.2, 8.25), k.trim)  # menuki ornaments
    octagon(m, k.trim, 8, 3.0, 3.7, 8, 2.6)
    m.box((7.15, 3.7, 7.35), (8.85, 5.2, 8.65), Mat("trim", "gold"))
    # Curved, single-edged blade: segments step toward the spine (-x) more and more near the tip.
    curve = [(5.2, 11, 0.0), (11, 16, -0.12), (16, 20.5, -0.3), (20.5, 24.5, -0.55), (24.5, 28, -0.85)]
    import numpy as np
    xs = np.array([c[0] for c in curve] + [28.0])
    off = np.array([c[2] for c in curve] + [-1.15])

    def cx(y):
        return 8 + np.interp(y, xs, off)

    def hw(y):
        return 0.82 - 0.1 * np.clip((y - 5) / 23, 0, 1)
    bmat = k.blade(hw=hw, cx=cx, span=(5.2, 30.5), single=1, hamon=True)
    for (y0, y1, dx) in curve:
        m.box((8 + dx - 0.82, y0, 7.72), (8 + dx + 0.82, y1, 8.28), bmat)
        m.box((8 + dx - 0.82, y0, 7.62), (8 + dx - 0.2, y1, 8.38), bmat)  # thick spine (mune)
    # Kissaki: the edge sweeps up to the point.
    tip_x = 8 - 1.15
    m.box((tip_x - 0.75, 28, 7.75), (tip_x + 0.8, 29.2, 8.25), bmat)
    m.box((tip_x + 0.05, 28.2, 7.78), (tip_x + 0.8, 31.0, 8.22), bmat, rot=((tip_x + 0.8, 28.2, 8), "z", 22.5))
    if (leg == "moonveil" or k.spec.get("mythic")) and k.glow:
        for (y0, y1, dx) in curve:
            m.box((8 + dx + 0.55, y0, 7.68), (8 + dx + 0.88, y1, 8.32), k.glow)
    elif k.sil == "netherite" and k.glow:
        for (y0, y1, dx) in curve[:4]:
            m.box((8 + dx + 0.6, y0 + 0.3, 7.69), (8 + dx + 0.86, y1 - 0.3, 8.31), k.glow)
    elif k.sil == "stormsteel" and k.glow:
        for (y0, y1, dx) in curve[:4]:
            m.box((8 + dx - 0.15, y0 + 0.5, 7.66), (8 + dx + 0.15, y1 - 0.5, 8.34), k.glow)
    # Sageo cord hanging from the scabbard knot, for flavour.
    if k.sil in ("golden", "stormsteel") or leg:
        m.box((8.75, -2.0, 7.8), (9.25, 1.5, 8.2), k.cloth)
        m.box((8.65, -4.0, 7.7), (9.35, -2.0, 8.3), k.cloth)
    return m


def dagger(name, k):
    m = ProModel(name)
    sil = k.sil
    m.box((8 - 0.9, -3.5, 8 - 0.9), (8 + 0.9, -2.2, 8 + 0.9), k.trim)
    wrap(m, k.grip, k.trim, -2.2, 2.8, 0.65)
    # Guard: swept quillons, one up, one down (a parrying dagger).
    m.box((8 - 1.2, 2.8, 7.2), (8 + 1.2, 4.0, 8.8), k.trim)
    m.box((4.4, 3.0, 7.45), (11.6, 3.8, 8.55), k.trim)
    m.box((4.4, 3.2, 7.5), (5.2, 5.8, 8.5), k.trim, rot=((4.4, 3.2, 8), "z", -22.5))
    m.box((10.8, 3.2, 7.5), (11.6, 5.8, 8.5), k.trim, rot=((11.6, 3.2, 8), "z", 22.5))
    if k.spec.get("gem"):
        gem(m, k, 8, 3.4, 8, 0.55, depth=1.0)
    straight_blade(m, k, 8, 4.0, 13.8, 2.6, 1.9, tip=1.25, steps=3, th=0.8, fuller=(4.6, 10.5, 0.3) if sil == "iron" else None)
    if k.spec.get("legend") == "bloodfang" or sil == "netherite":
        for y in (6.0, 8.0, 10.0, 12.0):
            m.box((9.0, y, 7.72), (9.8, y + 0.8, 8.28), k.edge(), rot=((9.0, y, 8), "z", 45))
        inlay(m, k, 8, 4.5, 12.5, w=0.35)
    elif sil == "stormsteel":
        inlay(m, k, 8, 4.5, 12.5, w=0.32, zig=True)
    return m


def spear(name, k):
    m = ProModel(name)
    sil = k.sil
    # Butt spike, shaft with grip wrap and bindings.
    m.box((7.4, -15.0, 7.4), (8.6, -13.0, 8.6), k.metal, rot=((8, -14, 8), "y", 45))
    m.box((7.25, -13.0, 7.25), (8.75, -12.2, 8.75), k.trim)
    m.box((7.4, -12.2, 7.4), (8.6, 18.6, 8.6), k.shaft)
    wrap(m, k.grip, k.trim, 2.0, 10.0, 0.72)
    for y in (-6.0, 15.5):
        m.box((7.3, y, 7.3), (8.7, y + 0.8, 8.7), k.trim)
    # Socket with lugs, then a leaf-shaped head.
    m.box((7.15, 18.0, 7.15), (8.85, 21.4, 8.85), k.metal)
    m.box((7.05, 18.8, 7.05), (8.95, 19.4, 8.95), k.trim)
    m.box((7.05, 20.0, 7.05), (8.95, 20.6, 8.95), k.trim)
    lug = 2.6 if sil in ("stormsteel", "golden") else 1.8
    for sgn in (-1, 1):
        x = 8 + sgn * 0.9
        m.box((x - 0.35, 20.6, 7.6), (x + 0.35, 20.6 + lug, 8.4), k.trim, rot=((x, 20.6, 8), "z", -45 * sgn))
    import numpy as np

    def hw(y):
        y = np.asarray(y, dtype=float)
        a = np.interp(y, [21.4, 22.6, 24.4, 26.4, 28.4], [0.8, 1.7, 2.1, 1.8, 1.2])
        over = np.clip((y - 28.4) / 2.9, 0, 1)
        return a * (1 - over * 0.98)
    bm = k.blade(hw=hw, span=(21.4, 31.3))
    for (ya, yb, w) in ((21.4, 22.4, 2.2), (22.4, 23.4, 3.4), (23.4, 25.4, 4.2), (25.4, 27.0, 3.7), (27.0, 28.4, 2.9)):
        m.box((8 - w / 2, ya, 7.6), (8 + w / 2, yb, 8.4), bm)
        m.box((8 - 0.4, ya, 7.35), (8 + 0.4, yb, 8.65), bm)
    straight_blade(m, k, 8, 28.4, 28.4001, 2.4, 2.4, tip=1.0, steps=1, th=0.8, mat=bm, ridge=False)
    if sil == "stormsteel" or k.spec.get("legend") == "skypiercer":
        inlay(m, k, 8, 22.0, 28.6, w=0.3, th=1.4)
    if sil in ("golden",) or k.spec.get("legend") == "skypiercer":
        m.box((8.85, 15.6, 7.8), (9.35, 19.2, 8.2), k.cloth)
        m.box((8.75, 13.6, 7.7), (9.45, 15.6, 8.3), k.cloth)
    return m


def warhammer(name, k):
    m = ProModel(name)
    sil = k.sil
    m.box((7.1, -11.5, 7.1), (8.9, -10.4, 8.9), k.trim)
    m.box((7.3, -10.4, 7.3), (8.7, 18.0, 8.7), k.shaft)
    wrap(m, k.grip, k.trim, -8.0, 2.0, 0.85)
    # Langets: iron strips running down the haft from the head.
    for z0, z1 in ((7.1, 7.35), (8.65, 8.9)):
        m.box((7.6, 12.0, z0), (8.4, 18.0, z1), k.metal)
    for y in (12.6, 15.6):
        m.box((7.2, y, 7.05), (8.8, y + 0.5, 8.95), k.trim)
    # Head: core block, a broad striking face on +x with a raised rim, a pick on -x.
    m.box((5.6, 17.0, 6.0), (10.4, 24.0, 10.0), k.metal)
    m.box((5.4, 17.6, 5.8), (10.6, 23.4, 10.2), k.trim)
    m.box((10.4, 16.4, 5.4), (13.4, 24.6, 10.6), k.metal)
    m.box((13.4, 16.8, 5.8), (14.4, 24.2, 10.2), k.edge())
    m.box((13.6, 18.4, 7.2), (14.6, 22.6, 8.8), k.metal)
    import numpy as np
    pick = k.blade(hw=lambda x: 1.3 - 0.06 * (8 - np.asarray(x)) * 1.0, axis="x", cx=20.5, span=(5.6, 0.6), flip_g=True)
    m.box((2.6, 19.2, 7.2), (5.6, 21.8, 8.8), pick)
    m.box((0.2, 19.6, 7.4), (2.6, 21.4, 8.6), pick)
    m.box((-0.6, 19.1, 7.55), (0.9, 20.6, 8.45), pick, rot=((0.15, 19.85, 8), "z", 45))
    # Top spike.
    m.box((7.3, 24.0, 7.3), (8.7, 25.6, 8.7), k.metal)
    m.box((7.55, 25.2, 7.55), (8.45, 26.7, 8.45), k.metal, rot=((8, 25.2, 8), "y", 45))
    if k.spec.get("gem"):
        gem(m, k, 8, 20.5, 8, 0.9, depth=2.25)
    if sil == "netherite" or k.spec.get("legend") == "earthshaker":
        for y in (17.4, 23.4):
            m.box((10.5, y, 5.3), (13.3, y + 0.25, 10.7), k.glow or k.trim)
        m.box((14.45, 19.2, 7.6), (14.65, 21.8, 8.4), k.glow or k.trim)
    if sil == "stormsteel":
        m.box((10.5, 20.3, 5.3), (13.3, 20.7, 10.7), k.glow)
    if sil == "golden":
        for sgn in (-1, 1):
            m.box((7.0, 16.4, 8 + sgn * 2.2 - 0.3), (9.0, 16.9, 8 + sgn * 2.2 + 0.3), k.trim)
    return m


def scythe(name, k):
    """Snath with two grips; the blade curves out toward +x from the top, edge facing down."""
    m = ProModel(name)
    sil = k.sil
    m.box((7.25, -14.0, 7.25), (8.75, -12.8, 8.75), k.trim)
    m.box((7.4, -12.8, 7.4), (8.6, 24.0, 8.6), k.shaft)
    wrap(m, k.grip, k.trim, -12.0, -4.0, 0.72)
    # Upper hand peg (the nib), sticking out sideways.
    m.box((7.4, 7.8, 8.6), (8.6, 9.2, 11.4), k.grip)
    m.box((7.3, 7.7, 11.4), (8.7, 9.3, 11.9), k.trim)
    m.box((7.0, 22.0, 7.0), (9.0, 25.4, 9.0), k.metal)
    m.box((6.9, 22.6, 6.9), (9.1, 23.2, 9.1), k.trim)
    import numpy as np
    # Blade segments (pre-rotation, local coordinates): each along x, sharpened on its lower side.
    segs = [  # x0, x1, y_top, height, angle, pivot (x, y)
        (8.6, 15.0, 25.4, 3.8, 0.0, (8.6, 25.4)),
        (14.6, 20.2, 25.4, 3.0, -22.5, (14.6, 25.4)),
        (19.4, 23.4, 23.4, 2.1, -45.0, (19.4, 23.4)),
        (23.0, 26.0, 23.4, 1.1, -45.0, (19.4, 23.4)),
    ]
    for (x0, x1, yt, h, ang, (px, py)) in segs:
        cy = yt - h / 2
        mat = k.blade(axis="x", local=True, cx=cy, hw=lambda xx, h=h: h / 2 + 0 * np.asarray(xx), single=-1, span=(x0, x1 + 4))
        rot = ((px, py, 8), "z", ang) if ang else None
        m.box((x0, yt - h, 7.7), (x1, yt, 8.3), mat, rot=rot)
        m.box((x0, yt - min(1.0, h * 0.45), 7.55), (x1, yt, 8.45), mat, rot=rot)  # thickened back
    # Tip point.
    m.box((25.6, 22.7, 7.75), (27.0, 23.4, 8.25), k.blade(axis="x", local=True, cx=23.05, hw=lambda xx: 0.35 + 0 * np.asarray(xx), single=-1),
          rot=((19.4, 23.4, 8), "z", -45.0))
    # Beard: a short back-hook behind the shaft.
    m.box((5.4, 23.4, 7.7), (7.0, 25.0, 8.3), k.metal)
    m.box((4.6, 23.6, 7.75), (5.6, 25.8, 8.25), k.metal, rot=((5.6, 23.6, 8), "z", 22.5))
    if k.glow:
        for (x0, x1, yt, h, ang, (px, py)) in segs[:3]:
            rot = ((px, py, 8), "z", ang) if ang else None
            m.box((x0 + 0.2, yt - 0.75, 7.5), (x1 - 0.2, yt - 0.45, 8.5), k.glow, rot=rot)
    if sil == "golden":
        m.box((7.5, 20.0, 8.6), (8.0, 22.0, 9.0), k.cloth)
    return m


def battleaxe(name, k):
    m = ProModel(name)
    sil = k.sil
    m.box((7.1, -9.5, 7.1), (8.9, -8.4, 8.9), k.trim)
    m.box((7.3, -8.4, 7.3), (8.7, 22.0, 8.7), k.shaft)
    wrap(m, k.grip, k.trim, -6.0, 3.0, 0.85)
    m.box((7.2, 10.0, 7.2), (8.8, 10.6, 8.8), k.trim)
    # Socket.
    m.box((6.7, 14.0, 6.9), (9.3, 23.0, 9.1), k.metal)
    m.box((6.6, 14.0, 6.8), (9.4, 14.7, 9.2), k.trim)
    m.box((6.6, 22.3, 6.8), (9.4, 23.0, 9.2), k.trim)
    import numpy as np
    for sgn in (-1, 1):
        # Neck, then a crescent bit with upper and lower horns and a bright honed edge.
        nx0, nx1 = sorted((8 + sgn * 1.3, 8 + sgn * 3.6))
        m.box((nx0, 16.4, 7.45), (nx1, 20.6, 8.55), k.metal)
        bx0, bx1 = sorted((8 + sgn * 3.6, 8 + sgn * 6.2))
        m.box((bx0, 14.6, 7.55), (bx1, 22.4, 8.45), k.metal)
        ex0, ex1 = sorted((8 + sgn * 6.2, 8 + sgn * 7.4))
        edge = Mat("blade", k.blade_ramp, single=sgn, hw=lambda y: 99 + 0 * np.asarray(y)) if False else k.edge()
        m.box((ex0, 14.2, 7.65), (ex1, 22.8, 8.35), edge)
        for (y0, y1, piv, ang) in ((22.4, 26.0, 22.4, -22.5 * sgn), (11.0, 14.6, 14.6, 22.5 * sgn)):
            m.box((bx0, y0, 7.6), (bx1, y1, 8.4), k.metal, rot=((8 + sgn * 4.9, piv, 8), "z", ang))
            m.box((ex0, y0, 7.68), (ex1, y1, 8.32), edge, rot=((8 + sgn * 4.9, piv, 8), "z", ang))
        for y in (16.0, 21.0):
            rx = 8 + sgn * 2.5
            m.box((rx - 0.35, y - 0.35, 7.3), (rx + 0.35, y + 0.35, 8.7), k.trim)
        if k.glow and sil in ("netherite", "stormsteel") or k.spec.get("legend") == "kingsbane":
            gx0, gx1 = sorted((8 + sgn * 5.9, 8 + sgn * 6.25))
            m.box((gx0, 15.0, 7.5), (gx1, 22.0, 8.5), k.glow)
    # Top spike.
    m.box((7.4, 23.0, 7.4), (8.6, 25.0, 8.6), k.metal)
    m.box((7.6, 24.6, 7.6), (8.4, 26.0, 8.4), k.metal, rot=((8, 24.6, 8), "y", 45))
    if k.spec.get("gem"):
        gem(m, k, 8, 18.5, 8, 0.8, depth=1.35)
    if sil == "golden":
        m.box((8.7, 9.0, 7.8), (9.2, 13.0, 8.2), k.cloth)
    return m


BUILDERS = {"longsword": longsword, "greatsword": greatsword, "katana": katana, "dual_daggers": dagger, "spear": spear,
            "warhammer": warhammer, "scythe": scythe, "battleaxe": battleaxe}

DISPLAYS = {
    "longsword": display(0.86, 0.66, -5.0, 10),
    "greatsword": display(0.74, 0.6, -3.0, 15),
    "katana": display(0.78, 0.6, -2.5, 10),
    "dual_daggers": display(0.85, 0.75, 0.5, 0),
    "spear": display(0.7, 0.56, 6.0, 5),
    "warhammer": display(0.64, 0.58, -4.0, 15),
    "scythe": display(0.62, 0.55, -8.0, 20, fp_tilt=-25, fp_yaw=120),
    "battleaxe": display(0.72, 0.58, -2.5, 15),
}


BLADE_SPANS = {"longsword": (2.4, 27.0, 3.4, 2.6, 0.9), "greatsword": (8.4, 27.6, 5.4, 4.2, 1.0),
               "dual_daggers": (4.0, 13.8, 2.6, 1.9, 0.8)}


def mythic_extras(m, k, kind):
    """Mythic weapons glow: lit cutting edges, glowing bands down the haft and a blazing core gem."""
    g = k.glow
    if kind in BLADE_SPANS:
        y0, y1, w0, w1, th = BLADE_SPANS[kind]
        n = 5
        for i in range(n):
            ya, yb = y0 + (y1 - y0) * i / n, y0 + (y1 - y0) * (i + 1) / n
            w = w0 + (w1 - w0) * (i + 0.5) / n
            for sgn in (-1, 1):
                x = 8 + sgn * (w / 2 - 0.22)
                m.box((x - 0.24, ya, 8 - th / 2 - 0.06), (x + 0.24, yb, 8 + th / 2 + 0.06), g)
        gy = {"longsword": -0.3, "greatsword": 2.6, "dual_daggers": 3.4}[kind]
        m.box((8 - 0.9, gy - 0.9, 6.6), (8 + 0.9, gy + 0.9, 9.4), g, rot=((8, gy, 8), "z", 45))
        # A crown of glowing prongs over the guard.
        for sgn in (-1, 1):
            x = 8 + sgn * 2.2
            m.box((x - 0.3, gy + 0.6, 7.7), (x + 0.3, gy + 3.0, 8.3), g, rot=((x, gy + 0.6, 8), "z", -22.5 * sgn))
    elif kind == "katana":
        pass  # the katana builder already lights its edge when mythic
    else:
        rings = {"spear": (-6.0, 15.5, 0.72), "warhammer": (-9.6, 12.6, 0.82), "scythe": (-2.0, 14.0, 0.72),
                 "battleaxe": (-7.8, 10.0, 0.82)}[kind]
        lo, hi, r = rings
        for i in range(5):
            y = lo + (hi - lo) * i / 4
            m.box((8 - r, y, 8 - r), (8 + r, y + 0.45, 8 + r), g)
        if kind == "warhammer":
            m.box((5.3, 19.9, 5.7), (10.7, 21.1, 10.3), g)
        if kind == "battleaxe":
            for sgn in (-1, 1):
                ex0, ex1 = sorted((8 + sgn * 7.35, 8 + sgn * 7.6))
                m.box((ex0, 14.6, 7.6), (ex1, 22.4, 8.4), g)
        if kind == "spear":
            m.box((7.6, 21.4, 7.2), (8.4, 28.0, 8.8), g)


def build(kind, name, spec, sil):
    k = Kit(spec, sil)
    m = BUILDERS[kind](name, k)
    if spec.get("mythic"):
        mythic_extras(m, k, kind)
    return m.bake(DISPLAYS[kind])


def main(only=None):
    count = 0
    for kind in BUILDERS:
        for tier, spec in TIERS.items():
            name = f"{tier}_{kind}"
            if only and name not in only:
                continue
            build(kind, name, spec, tier)
            count += 1
    for name, (kind, spec) in LEGENDS.items():
        if only and name not in only:
            continue
        s = dict(spec)
        s["legend"] = name
        build(kind, name, s, spec["sil"])
        count += 1
    print(f"{count} models written")


if __name__ == "__main__":
    main(set(sys.argv[1:]) or None)
