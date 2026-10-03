#!/usr/bin/env python3
"""A distinct 3D in-hand model for every weapon type in every tier.

Run from the repository root after gen_models.py:  python3 tools/gen_tier_models.py

Each tier has its own silhouette, not just its own colours:
  stone      crude and chunky: thick blade, plain bar guard, twine grip
  iron       the classic soldier's pattern
  golden     ornate: swept guards, ring pommels, a cloth tassel
  diamond    crystalline: faceted prongs and shards, a glowing frost core
  netherite  brutal: spikes, notched edges and a smouldering ember inlay
  stormsteel elegant: winged guards and a crackling lightning inlay
Models are written to models/item/3d/<tier>_<type>.json with their textures filled in.
"""
import json
import math
import os

from gen_models import LEGENDARY_MODELS, MODELS, TYPE_MODELS, Model, tex_grip, write_textures
from PIL import Image

from texlib import R, TIERS, TIER_BLADE, TIER_CLOTH, TIER_GEM, TIER_GLOW, TIER_GRIP, TIER_TRIM, hexc, mix, save

# ----------------------------------------------------------------------------- shared parts


def tassel(m, x, y, z, length=4.5):
    """A cloth tassel hanging below a pommel (shortened to stay inside the model bounds)."""
    length = min(length, y - 1.6 + 15.9)
    m.box((x - 0.35, y - 1.0, z - 0.35), (x + 0.35, y, z + 0.35), "trim")
    m.box((x - 0.55, y - 1.0 - length, z - 0.55), (x + 0.55, y - 1.0, z + 0.55), "cloth")
    m.box((x - 0.7, y - 1.6 - length, z - 0.7), (x + 0.7, y - 1.0 - length, z + 0.7), "cloth")


def inlay(m, s, x0, x1, y0, y1, z0=7.45, z1=8.55):
    """A glowing strip set into a blade (zig-zagging for lightning)."""
    if not s["glow"]:
        return
    if s["glow"] in ("lightning", "royal") and s["sil"] == "stormsteel":
        h = (y1 - y0) / 4
        for k in range(4):
            dx = 0.35 if k % 2 == 0 else -0.35
            m.box((x0 + dx, y0 + k * h, z0), (x1 + dx, y0 + (k + 1) * h, z1), "glow", glow=True)
    else:
        m.box((x0, y0, z0), (x1, y1, z1), "glow", glow=True)


def sword_guard(m, tier, y, half, th=1.5, depth=(7, 9)):
    """A crossguard centred on x = 8 at height y, styled per tier."""
    z0, z1 = depth
    if tier == "stone":
        m.box((8 - half * 0.75, y, z0 - 0.2), (8 + half * 0.75, y + th + 0.4, z1 + 0.2), "trim")
        return
    m.box((8 - half, y, z0), (8 + half, y + th, z1), "trim")
    if tier == "iron":
        for sgn in (-1, 1):
            x = 8 + sgn * half
            m.box((min(x, x + sgn * 0.8), y + 0.4, z0 + 0.2), (max(x, x + sgn * 0.8), y + th + 1.6, z1 - 0.2), "trim")
    elif tier == "golden":
        for sgn in (-1, 1):
            x = 8 + sgn * (half - 0.4)
            m.box((x - 0.6, y + 0.5, z0 + 0.1), (x + 0.6, y + 3.4, z1 - 0.1), "trim", rot=((x, y + 0.5, 8), "z", -22.5 * sgn))
            m.box((x - 0.9, y + 2.6, z0 - 0.2), (x + 0.9, y + 4.0, z1 + 0.2), "gem", rot=((x, y + 3.3, 8), "z", 45))
        m.box((8 - 1.6, y - 1.2, z0 - 0.3), (8 + 1.6, y, z1 + 0.3), "trim")
    elif tier == "diamond":
        for sgn in (-1, 1):
            x = 8 + sgn * (half - 0.2)
            m.box((x - 0.55, y + 0.3, z0 + 0.3), (x + 0.55, y + 4.6, z1 - 0.3), "edge", rot=((x, y + 0.3, 8), "z", -45 * sgn))
            m.box((x - 0.5, y - 2.2, z0 + 0.4), (x + 0.5, y + 0.2, z1 - 0.4), "edge", rot=((x, y + 0.2, 8), "z", 22.5 * sgn))
        m.box((8 - 1.1, y - 0.4, z0 - 0.6), (8 + 1.1, y + th + 0.4, z1 + 0.6), "gem", glow=True)
    elif tier == "netherite":
        for sgn in (-1, 1):
            x = 8 + sgn * half
            m.box((x - 0.5, y - 2.6, z0 + 0.3), (x + 0.5, y + 0.6, z1 - 0.3), "metal", rot=((x, y + 0.6, 8), "z", 22.5 * sgn))
            m.box((x - 0.4, y + th - 0.2, z0 + 0.4), (x + 0.4, y + th + 1.8, z1 - 0.4), "metal",
                  rot=((x, y + th, 8), "z", -45 * sgn))
        m.box((8 - 0.5, y - 1.8, z0 - 0.4), (8 + 0.5, y, z1 + 0.4), "glow", glow=True)
    elif tier == "stormsteel":
        for sgn in (-1, 1):
            x = 8 + sgn * (half - 0.3)
            m.box((x - 0.45, y + 0.2, z0 + 0.5), (x + 0.45, y + 5.2, z1 - 0.5), "trim", rot=((x, y + 0.2, 8), "z", -22.5 * sgn))
            m.box((x - 0.35, y + 0.2, z0 + 0.6), (x + 0.35, y + 3.8, z1 - 0.6), "glow", glow=True,
                  rot=((x, y + 0.2, 8), "z", -45 * sgn))


def sword_pommel(m, tier, y, size=1.0):
    """A pommel whose top sits at height y."""
    s = size
    if tier == "stone":
        m.box((8 - 1.3 * s, y - 2.2 * s, 8 - 1.3 * s), (8 + 1.3 * s, y, 8 + 1.3 * s), "trim")
        tassel(m, 8, y - 2.2 * s, 8, 3.0)
    elif tier == "iron":
        m.box((8 - s, y - 2 * s, 8 - s), (8 + s, y, 8 + s), "trim")
        m.box((8 - 0.5 * s, y - 1.5 * s, 8 - 1.2 * s), (8 + 0.5 * s, y - 0.5 * s, 8 + 1.2 * s), "gem")
    elif tier == "golden":
        m.box((8 - 1.4 * s, y - 2.6 * s, 7.6), (8 + 1.4 * s, y, 8.4), "trim")
        m.box((8 - 0.8 * s, y - 2.0 * s, 7.2), (8 + 0.8 * s, y - 0.6 * s, 8.8), "gem")
        tassel(m, 8, y - 2.6 * s, 8, 5.0)
    elif tier == "diamond":
        m.box((8 - 0.9 * s, y - 2.4 * s, 8 - 0.9 * s), (8 + 0.9 * s, y, 8 + 0.9 * s), "gem", glow=True,
              rot=((8, y - 1.2 * s, 8), "y", 45))
        m.box((8 - 0.4 * s, y - 3.6 * s, 8 - 0.4 * s), (8 + 0.4 * s, y - 2.4 * s, 8 + 0.4 * s), "edge")
        tassel(m, 8, y - 3.6 * s, 8, 3.5)
    elif tier == "netherite":
        m.box((8 - 1.2 * s, y - 1.8 * s, 8 - 1.2 * s), (8 + 1.2 * s, y, 8 + 1.2 * s), "metal")
        m.box((8 - 0.5 * s, y - 4.0 * s, 8 - 0.5 * s), (8 + 0.5 * s, y - 1.8 * s, 8 + 0.5 * s), "metal")
        m.box((8 - 1.25 * s, y - 1.2 * s, 8 - 0.3), (8 + 1.25 * s, y - 0.6 * s, 8 + 0.3), "glow", glow=True)
    else:
        m.box((8 - 1.1 * s, y - 2.0 * s, 8 - 1.1 * s), (8 + 1.1 * s, y, 8 + 1.1 * s), "trim", rot=((8, y - s, 8), "y", 45))
        m.box((8 - 0.6 * s, y - 1.7 * s, 6.9), (8 + 0.6 * s, y - 0.3 * s, 9.1), "glow", glow=True)
        tassel(m, 8, y - 2.0 * s, 8, 4.0)


def notches(m, x0, x1, y0, y1, step=2.6, z0=7.65, z1=8.35):
    """Small spikes along both edges of a blade (netherite)."""
    y = y0
    while y < y1:
        for x, sgn in ((x0, -1), (x1, 1)):
            m.box((x - 0.5, y, z0), (x + 0.5, y + 1.0, z1), "edge", rot=((x, y, 8), "z", 45 * sgn))
        y += step


# ----------------------------------------------------------------------------- styles
#
# A style is a tier (or a legendary weapon): `sil` picks the silhouette branch the builders use,
# `grad` is the blade gradient from hilt to tip, and `emissive` makes the whole blade glow.

GRADIENTS = {
    "stone": ["#4a4a4a", "#7a7a7a", "#b0b0b0"],
    "iron": ["#5d6773", "#a9b3bf", "#f2f6fa"],
    "golden": ["#8a5a0c", "#e2b23a", "#fff3b8"],
    "diamond": ["#0e5f66", "#36d6c6", "#e0fffb"],
    "netherite": ["#231a1f", "#4a3b42", "#b2502a"],
    "stormsteel": ["#1a2a8a", "#3d7cff", "#7fe8ff", "#f4ffff"],
    "tempest_edge": ["#1d2a73", "#4f86ff", "#b9e6ff", "#fff6a0"],
    "rimecleaver": ["#1b5f80", "#6fd4ff", "#ffffff"],
    "voidreaver": ["#2a0f45", "#8a3cff", "#ff9cf2"],
    "earthshaker": ["#4f4031", "#8a6a3a", "#ffb347"],
    "bloodfang": ["#45060e", "#c4182e", "#ff9a9a"],
    "skypiercer": ["#a99245", "#fff1a8", "#ffffff"],
    "moonveil": ["#33405e", "#9cb8ff", "#ffffff"],
    "kingsbane": ["#1a1424", "#5a2d80", "#ffcc33"],
}

LEGEND_SIL = {"tempest_edge": "stormsteel", "rimecleaver": "diamond", "voidreaver": "netherite", "earthshaker": "golden",
              "bloodfang": "netherite", "skypiercer": "golden", "moonveil": "stormsteel", "kingsbane": "golden"}


def tier_style(tier):
    return {"name": tier, "sil": tier, "legend": None, "glow": TIER_GLOW[tier], "emissive": tier == "stormsteel"}


def legend_style(name, spec):
    return {"name": name, "sil": LEGEND_SIL[name], "legend": name, "glow": spec["glow"], "emissive": True}


def _stops(colors, t):
    cs = [hexc(c) for c in colors]
    t = max(0.0, min(1.0, t)) * (len(cs) - 1)
    i = min(int(t), len(cs) - 2)
    return mix(cs[i], cs[i + 1], t - i)


def _shade(c, f):
    if f >= 1:
        return mix(c, (255, 255, 255, 255), min(1.0, (f - 1) * 1.6))
    return mix(c, (0, 0, 0, 255), (1 - f) * 1.2)


# Cross-section of a bevelled blade: bright cutting edge, dark bevel, lit central ridge, shadowed far side.
PROFILE = [1.42, 1.22, 0.86, 0.8, 0.86, 0.96, 1.12, 1.3, 1.24, 1.06, 0.92, 0.84, 0.76, 0.7, 0.6, 0.5]


def tex_grad_blade(colors, seed):
    import random
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        base = _stops(colors, 1 - y / 15)
        for x in range(16):
            f = PROFILE[x] + 0.035 * ((x * 7 + y * 3) % 5 - 2) / 2
            if rng.random() < 0.035:
                f += 0.25
            img.putpixel((x, y), _shade(base, f))
    return img


def tex_grad_edge(colors):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        base = _stops(colors, 1 - y / 15)
        for x in range(16):
            img.putpixel((x, y), _shade(base, 1.38 - 0.25 * x / 15))
    return img


# ----------------------------------------------------------------------------- ornate parts


def grip(m, y0, y1, r=0.75, every=2.0):
    """A wrapped grip with thin bands."""
    m.box((8 - r, y0, 8 - r), (8 + r, y1, 8 + r), "grip")
    y = y0 + every * 0.6
    while y < y1 - 0.5:
        m.box((8 - r - 0.1, y, 8 - r - 0.1), (8 + r + 0.1, y + 0.3, 8 + r + 0.1), "trim")
        y += every


def ornate_guard(m, s, y, half, th=1.4, depth=(7, 9)):
    """A crossguard with a jewelled centre block and a langet climbing onto the blade."""
    sil = s["sil"]
    sword_guard(m, sil, y, half, th, depth)
    z0, z1 = depth
    glow = bool(s["glow"])
    m.box((8 - 1.8, y - 0.7, z0 - 0.35), (8 + 1.8, y + th + 0.9, z1 + 0.35), "trim")
    m.box((8 - 0.9, y - 0.25, z0 - 0.6), (8 + 0.9, y + th + 0.45, z1 + 0.6), "gem", glow=glow)
    m.box((8 - 1.15, y + th + 0.9, 8 - 0.62), (8 + 1.15, y + th + 2.1, 8 + 0.62), "trim")
    m.box((8 - 0.6, y + th + 2.1, 8 - 0.58), (8 + 0.6, y + th + 2.9, 8 + 0.58), "trim", rot=((8, y + th + 2.1, 8), "z", 0))
    if sil != "stone":
        for sgn in (-1, 1):
            x = 8 + sgn * (half + 0.1)
            m.box((x - 0.55, y + 0.2, z0 - 0.25), (x + 0.55, y + th - 0.2, z1 + 0.25), "gem", glow=glow)


def blade(m, s, hw, y0, y1, tip, th=0.42, steps=4, ridge=True):
    """A bevelled blade that narrows in stages to a true point; returns the tip height.

    The point is two stacked squares turned 45 degrees (the Blockbench way of getting a clean
    diagonal), so the tip reads as a sharp spear point instead of a stepped tower.
    """
    top = min(31.9, y1 + tip)
    g = (y0, top)
    em = s["emissive"]
    hw *= 0.88
    wl = hw * 0.88 + 0.4  # half-width at the base of the point, edges included
    base = top - wl * 1.3
    n = 3
    ys = [y0 + (base - y0) * k / n for k in range(n + 1)]
    t = th
    for k in range(n):
        w = hw * (1 - 0.06 * k)
        t = th * (1 - 0.08 * k)
        m.box((8 - w, ys[k], 8 - t), (8 + w, ys[k + 1], 8 + t), "blade", "edge", "edge", uvy=g, glow=em)
        for sgn in (-1, 1):
            xa, xb = sorted((8 + sgn * w, 8 + sgn * (w + 0.4)))
            m.box((xa, ys[k] + (0.6 if k == 0 else 0), 8 - t * 0.45), (xb, ys[k + 1], 8 + t * 0.45), "edge", uvy=g, glow=em)
    h = wl / 1.4142
    m.box((8 - h, base - h, 8 - t * 0.9), (8 + h, base + h, 8 + t * 0.9), "blade", "edge", "edge", uvy=g, glow=em,
          rot=((8, base, 8), "z", 45))
    c2, h2 = base + wl * 0.72, h * 0.6
    m.box((8 - h2, c2 - h2, 8 - t * 0.7), (8 + h2, c2 + h2, 8 + t * 0.7), "edge", uvy=g, glow=em, rot=((8, c2, 8), "z", 45))
    if s["glow"]:
        inlay(m, s, 7.7, 8.3, y0 + 1.0, base + wl * 0.3, 8 - th - 0.1, 8 + th + 0.1)
    elif ridge:
        m.box((7.7, y0 + 0.6, 8 - th - 0.08), (8.3, base + wl * 0.3, 8 + th + 0.08), "fuller", uvy=g)
    return top


def mirror_x(m):
    """Mirrors a model across x = 8 (used to turn a blade to face the other way)."""
    for el in m.elements:
        a, b = el["from"][0], el["to"][0]
        el["from"][0], el["to"][0] = round(16 - b, 3), round(16 - a, 3)
        f = el["faces"]
        if "east" in f and "west" in f:
            f["east"], f["west"] = f["west"], f["east"]
        r = el.get("rotation")
        if r:
            r["origin"][0] = round(16 - r["origin"][0], 3)
            if r["axis"] in ("y", "z"):
                r["angle"] = -r["angle"]
    return m


# ----------------------------------------------------------------------------- weapon types


def longsword(s):
    m = Model()
    sil = s["sil"]
    hw = {"stone": 1.9, "iron": 1.55, "golden": 1.5, "diamond": 1.45, "netherite": 1.7, "stormsteel": 1.45}[sil]
    sword_pommel(m, sil, -4.5)
    grip(m, -4.5, 2.5)
    ornate_guard(m, s, 2.5, 5.8)
    blade(m, s, hw, 3.9, 28.0, 4.0)
    if sil == "netherite":
        notches(m, 8 - hw - 0.45, 8 + hw + 0.45, 9, 26, 2.8)
    if sil == "golden":
        m.box((8 - hw - 0.5, 8.0, 7.45), (8 + hw + 0.5, 8.8, 8.55), "trim")
    if s["legend"] == "tempest_edge":
        for sgn in (-1, 1):
            m.box((8 + sgn * 6.6 - 0.6, 3.0, 7.3), (8 + sgn * 6.6 + 0.6, 8.0, 8.7), "trim", rot=((8 + sgn * 6.6, 3.0, 8), "z", -22.5 * sgn))
            m.box((8 + sgn * 6.6 - 0.35, 3.5, 7.2), (8 + sgn * 6.6 + 0.35, 6.5, 8.8), "glow", glow=True,
                  rot=((8 + sgn * 6.6, 3.0, 8), "z", -45 * sgn))
    return m


def greatsword(s):
    m = Model()
    sil = s["sil"]
    hw = {"stone": 3.0, "iron": 2.65, "golden": 2.55, "diamond": 2.45, "netherite": 2.9, "stormsteel": 2.4}[sil]
    sword_pommel(m, sil, -9.5, 1.3)
    grip(m, -9.5, 3, 1.0, 2.4)
    m.box((6.8, -3.4, 6.8), (9.2, -2.6, 9.2), "trim")
    ornate_guard(m, s, 3, 7.2, th=2.0, depth=(6.8, 9.2))
    m.box((8 - hw - 0.8, 5.0, 7.0), (8 + hw + 0.8, 6.4, 9.0), "trim")
    blade(m, s, hw, 6.4, 27.5, 4.5, th=0.6)
    if sil == "netherite":
        notches(m, 8 - hw - 0.45, 8 + hw + 0.45, 10, 26, 3.0)
    if sil == "diamond":
        for sgn in (-1, 1):
            m.box((8 + sgn * hw - 0.5, 9, 7.5), (8 + sgn * hw + 0.5, 13, 8.5), "edge", rot=((8 + sgn * hw, 9, 8), "z", -22.5 * sgn))
    if s["legend"] == "rimecleaver":
        for sgn in (-1, 1):
            m.box((8 + sgn * 7.4 - 0.7, 4, 7.2), (8 + sgn * 7.4 + 0.7, 9, 8.8), "edge", glow=True, rot=((8 + sgn * 7.4, 4, 8), "z", -22.5 * sgn))
            m.box((8 + sgn * 5.2 - 0.5, 4.5, 7.4), (8 + sgn * 5.2 + 0.5, 7.5, 8.6), "edge", glow=True,
                  rot=((8 + sgn * 5.2, 4.5, 8), "z", -45 * sgn))
    return m


def katana(s):
    m = Model()
    sil = s["sil"]
    m.box((7.15, -10.2, 7.15), (8.85, -9, 8.85), "trim")
    m.box((7.25, -9, 7.25), (8.75, 3, 8.75), "grip")
    for y in (-7.5, -4.5, -1.5, 1.2):
        m.box((7.1, y, 7.1), (8.9, y + 0.5, 8.9), "cloth")
    y = 3
    if sil == "stone":
        m.box((5.6, y, 5.6), (10.4, y + 0.8, 10.4), "trim")
    elif sil == "iron":
        m.box((5.3, y, 5.3), (10.7, y + 0.6, 10.7), "trim")
        m.box((5.3, y, 5.3), (10.7, y + 0.6, 10.7), "trim", rot=((8, y + 0.3, 8), "y", 45))
    elif sil == "golden":
        for a in (0, 22.5, 45, -22.5):
            m.box((5.4, y, 5.4), (10.6, y + 0.6, 10.6), "trim", rot=((8, y + 0.3, 8), "y", a))
        m.box((7.1, y - 0.1, 4.7), (8.9, y + 0.7, 5.5), "gem")
    elif sil == "diamond":
        m.box((5.0, y, 7.0), (11.0, y + 0.7, 9.0), "trim")
        m.box((7.0, y, 5.0), (9.0, y + 0.7, 11.0), "trim")
        for (x, z) in ((5.0, 8), (11.0, 8), (8, 5.0), (8, 11.0)):
            m.box((x - 0.5, y + 0.1, z - 0.5), (x + 0.5, y + 1.3, z + 0.5), "gem", glow=True)
    elif sil == "netherite":
        m.box((5.4, y, 5.4), (10.6, y + 0.6, 10.6), "metal", rot=((8, y + 0.3, 8), "y", 45))
        for (x, z) in ((4.4, 8), (11.6, 8), (8, 4.4), (8, 11.6)):
            m.box((x - 0.4, y, z - 0.4), (x + 0.4, y + 0.6, z + 0.4), "metal")
        m.box((7.4, y - 0.1, 5.8), (8.6, y + 0.7, 10.2), "glow", glow=True)
    else:
        m.box((5.3, y, 5.3), (10.7, y + 0.6, 10.7), "trim", rot=((8, y + 0.3, 8), "y", 45))
        m.box((6.3, y + 0.6, 6.3), (9.7, y + 0.9, 9.7), "glow", glow=True, rot=((8, y + 0.75, 8), "y", 45))
    m.box((7.05, 3.6, 7.35), (8.95, 4.8, 8.65), "collar")
    curve = {"stone": 0.6, "iron": 1.0, "golden": 1.1, "diamond": 0.8, "netherite": 1.3, "stormsteel": 1.2}[sil]
    g = (4.8, 32)
    em = s["emissive"]
    segs = [(4.8, 11, 0.0, 1.75), (11, 17, -0.2, 1.7), (17, 22, -0.45, 1.62), (22, 26, -0.8, 1.52), (26, 29, -1.2, 1.38)]
    for (y0, y1, dx, w) in segs:
        x0 = 8 - w / 2 + dx * curve
        m.box((x0, y0, 7.72), (x0 + w, y1, 8.28), "blade", "edge", "edge", uvy=g, glow=em)
        # The hamon: a bright tempered line along the cutting edge.
        m.box((x0 + w - 0.4, y0, 7.68), (x0 + w + 0.08, y1, 8.32), "glow" if s["glow"] else "edge", uvy=g,
              glow=bool(s["glow"]))
        m.box((x0 - 0.12, y0, 7.85), (x0 + 0.3, y1, 8.15), "fuller", uvy=g)
    m.box((8 - 0.6 - 1.65 * curve, 29, 7.76), (8 + 0.6 - 1.65 * curve, 30.8, 8.24), "blade", "edge", "edge", uvy=g, glow=em)
    m.box((8 - 0.3 - 2.0 * curve, 30.8, 7.8), (8 + 0.3 - 2.0 * curve, 32, 8.2), "edge", uvy=g, glow=em)
    if s["legend"] == "moonveil":
        m.box((4.4, 3.05, 7.5), (6.2, 3.55, 8.5), "trim", rot=((5.3, 3.3, 8), "y", 45))
        m.box((9.8, 3.05, 7.5), (11.6, 3.55, 8.5), "trim", rot=((10.7, 3.3, 8), "y", 45))
    return m


def dagger(s):
    m = Model()
    sil = s["sil"]
    length = {"stone": 15.5, "iron": 17.0, "golden": 16.5, "diamond": 18.0, "netherite": 16.5, "stormsteel": 18.0}[sil]
    m.box((7.1, -3.5, 7.5), (8.9, -2, 8.5), "trim")
    m.box((7.5, -3.2, 7.2), (8.5, -2.3, 8.8), "gem", glow=bool(s["glow"]))
    if sil in ("golden", "stormsteel"):
        tassel(m, 8, -3.5, 8, 2.5)
    grip(m, -2, 3, 0.62, 1.6)
    ornate_guard(m, s, 3, 3.3, th=1.0, depth=(7.2, 8.8))
    hw = 1.2 if sil != "stone" else 1.45
    blade(m, s, hw, 4.0, length - 3.0, 3.0, th=0.36, steps=3)
    if sil == "netherite":
        notches(m, 8 - hw - 0.45, 8 + hw + 0.45, 7, length - 4, 2.2)
    if s["legend"] == "bloodfang":
        for y in (6.5, 8.5, 10.5, 12.5):
            m.box((9.6, y, 7.75), (10.4, y + 1, 8.25), "edge", glow=True, rot=((9.6, y, 8), "z", 45))
    return m


def spear(s):
    m = Model()
    sil = s["sil"]
    m.box((7.2, -15, 7.2), (8.8, -13, 8.8), "trim")
    m.box((7.4, -13, 7.4), (8.6, 22, 8.6), "shaft", "shaft", "trim")
    m.box((7.25, 2, 7.25), (8.75, 10, 8.75), "grip")
    for y in (-6, 12, 17):
        m.box((7.25, y, 7.25), (8.75, y + 0.6, 8.75), "trim")
    m.box((7.1, 21, 7.1), (8.9, 23.5, 8.9), "trim")
    g = (23.5, 32)
    em = s["emissive"]
    if sil == "stone":
        m.box((7.2, 20.5, 7.2), (8.8, 22.5, 8.8), "cloth")
        m.box((6.4, 23.5, 7.55), (9.6, 26, 8.45), "blade", "edge", "edge", uvy=g)
        m.box((6.9, 26, 7.6), (9.1, 28, 8.4), "blade", "edge", "edge", uvy=g)
        m.box((7.4, 28, 7.65), (8.6, 29.5, 8.35), "edge", uvy=g)
        return m
    m.box((7.4, 21.8, 6.9), (8.6, 22.8, 9.1), "gem", glow=bool(s["glow"]))
    # A leaf-shaped head: swells out of the socket, then narrows to a long point.
    m.box((7.0, 23.2, 7.62), (9.0, 24.4, 8.38), "blade", "edge", "edge", uvy=g, glow=em)
    blade(m, s, 1.75, 24.4, 28.2, 3.7, th=0.38)
    if sil == "iron":
        m.box((5, 22.5, 7.6), (11, 23.2, 8.4), "trim")
    elif sil == "golden":
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.4 - 0.8, 21.5, 7.6), (8 + sgn * 2.4 + 0.8, 25.0, 8.4), "trim", rot=((8 + sgn * 1.5, 22, 8), "z", -22.5 * sgn))
        m.box((7.3, 17.5, 7.3), (8.7, 21, 8.7), "cloth")
        m.box((7.0, 15.5, 7.0), (9.0, 17.5, 9.0), "cloth")
    elif sil == "diamond":
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.0 - 0.45, 23, 7.65), (8 + sgn * 2.0 + 0.45, 27.5, 8.35), "edge", rot=((8 + sgn * 2, 23, 8), "z", -22.5 * sgn))
    elif sil == "netherite":
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.2 - 0.5, 22, 7.65), (8 + sgn * 2.2 + 0.5, 26.0, 8.35), "metal", rot=((8 + sgn * 2.2, 26, 8), "z", 22.5 * sgn))
            m.box((8 + sgn * 3.4 - 0.4, 21, 7.7), (8 + sgn * 3.4 + 0.4, 23.5, 8.3), "edge", rot=((8 + sgn * 3.4, 23.5, 8), "z", 45 * sgn))
    else:
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.6 - 0.35, 21.5, 7.75), (8 + sgn * 2.6 + 0.35, 26.5, 8.25), "trim", rot=((8 + sgn * 1.2, 22, 8), "z", -45 * sgn))
        m.box((7.5, 15, 7.95), (8.5, 21, 8.05), "cloth", rot=((8, 21, 8), "x", 22.5))
    if s["legend"] == "skypiercer":
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.5 - 1.6, 19, 7.9), (8 + sgn * 2.5 + 1.6, 24, 8.1), "wing", rot=((8 + sgn * 1, 22, 8), "z", -22.5 * sgn))
            m.box((8 + sgn * 3.8 - 1.2, 17.5, 7.95), (8 + sgn * 3.8 + 1.2, 22, 8.05), "wing", rot=((8 + sgn * 2, 21, 8), "z", -45 * sgn))
    return m


def octagon_x(m, x0, x1, cy, cz, h, tex, side=None, glow=False):
    """An eight-sided prism along x: a box plus the same box turned 45 degrees."""
    for rot in (None, ((x0, cy, cz), "x", 45)):
        m.box((x0, cy - h, cz - h), (x1, cy + h, cz + h), tex, side or tex, side or tex, rot=rot, glow=glow)


def warhammer(s):
    m = Model()
    tier = s["sil"]
    glowing = bool(s["glow"])
    m.box((7, -11.5, 7), (9, -9.5, 9), "trim")
    m.box((7.3, -9.5, 7.3), (8.7, 17, 8.7), "shaft", "shaft", "trim")
    m.box((7.15, -8, 7.15), (8.85, 2, 8.85), "grip")
    cy, h = 19.0, (3.5 if tier == "stone" else 3.1)
    # Two chamfered striking halves, each with a rimmed face.
    for (x0, x1) in ((2.4, 6.6), (9.4, 13.6)):
        octagon_x(m, x0, x1, cy, 8, h, "metal")
    if tier != "stone":
        for (x0, x1) in ((1.6, 2.5), (13.5, 14.4)):
            octagon_x(m, x0, x1, cy, 8, h + 0.35, "trim")
    # The socket where head meets haft, jewelled on both faces.
    octagon_x(m, 6.4, 9.6, cy, 8, h + 0.45, "trim")
    if tier != "stone":
        m.box((7.25, cy - 0.75, 4.0), (8.75, cy + 0.75, 12.0), "gem", glow=glowing, rot=((8, cy, 8), "z", 45))
    # Langets: steel straps riveted down the haft.
    for x0 in (6.95, 8.65):
        m.box((x0, 10.5, 7.55), (x0 + 0.4, 15.6, 8.45), "metal")
    m.box((7.1, 10.0, 7.1), (8.9, 10.6, 8.9), "trim")
    # Top spike.
    m.box((7.25, cy + h, 7.25), (8.75, cy + h + 3.0, 8.75), "metal", rot=((8, cy + h, 8), "y", 45))
    m.box((7.6, cy + h + 3.0, 7.6), (8.4, cy + h + 4.6, 8.4), "edge", rot=((8, cy + h + 3, 8), "y", 45))
    if tier == "stone":
        m.box((7, 14.4, 7), (9, 15.6, 9), "cloth")
    elif tier == "golden":
        for x in (4.5, 11.5):
            octagon_x(m, x - 0.3, x + 0.3, cy, 8, h + 0.15, "trim")
        tassel(m, 8, 14.5, 8, 3.0)
    elif tier == "diamond":
        for sgn in (-1, 1):
            x = 8 + sgn * 6.4
            for dy in (-2.2, 2.2):
                m.box((x - 0.4, cy + dy - 0.4, 7.6), (x + 0.4 + sgn * 1.6, cy + dy + 0.4, 8.4), "edge", glow=True,
                      rot=((x, cy + dy, 8), "z", 22.5 * sgn * (1 if dy > 0 else -1)))
    elif tier == "netherite":
        # The back half becomes a hooked pick; molten seams run round the head.
        m.box((9.4, cy - 1.2, 7.3), (15.4, cy + 1.2, 8.7), "metal", rot=((9.4, cy, 8), "z", -22.5))
        m.box((14.0, cy - 3.6, 7.5), (15.0, cy - 0.6, 8.5), "edge", rot=((14.5, cy - 0.6, 8), "z", -22.5))
        for x in (4.2, 11.6):
            octagon_x(m, x - 0.2, x + 0.2, cy, 8, h + 0.05, "glow", glow=True)
    elif tier == "stormsteel":
        for x in (4.4, 11.6):
            octagon_x(m, x - 0.35, x + 0.35, cy, 8, h + 0.1, "glow", glow=True)
        for x in (5.6, 10.4):
            m.box((x - 0.3, cy + h - 0.2, 7.7), (x + 0.3, cy + h + 3.2, 8.3), "trim", rot=((x, cy + h, 8), "z", 22.5 if x < 8 else -22.5))
    return m


def scythe(s):
    m = Model()
    tier = s["sil"]
    m.box((7.4, -14, 7.4), (8.6, 26, 8.6), "shaft", "shaft", "trim")
    m.box((7.25, -12, 7.25), (8.75, -4, 8.75), "grip")
    m.box((8.6, 8, 7.5), (12, 9, 8.5), "grip")
    m.box((7, 23, 7), (9, 26.5, 9), "trim")
    m.box((7.3, 24, 6.8), (8.7, 25.4, 9.2), "gem", glow=bool(s["glow"]))
    # The crescent: segments chained along an arc, each turned a little further down and tapering to the tip.
    glowing = bool(s["glow"])
    segs = [(0, 6.5, 3.1), (22.5, 5.5, 2.7), (45, 4.5, 2.0), (45, 3.0, 1.1)]
    if tier == "stone":
        segs = segs[:3]
    px, py = 7.6, 25.6
    for i, (ang, length, w) in enumerate(segs):
        r = ((px, py, 8), "z", ang)
        m.box((px - length, py - w, 7.74), (px + 0.3, py, 8.26), "blade", "edge", "edge", rot=r)
        m.box((px - length, py - w - 0.3, 7.8), (px + 0.3, py - w + 0.35, 8.2), "glow" if glowing else "edge", glow=glowing, rot=r)
        m.box((px - length, py - 0.35, 7.66), (px + 0.3, py + 0.2, 8.34), "trim" if tier != "stone" else "metal", rot=r)
        if tier != "stone" and i < len(segs) - 1:
            m.box((px - 0.45, py - 0.45 - w * 0.4, 7.6), (px + 0.45, py + 0.45 - w * 0.4, 8.4), "gem", glow=glowing,
                  rot=((px, py - w * 0.4, 8), "z", 45))
        rad = math.radians(ang)
        px, py = px - length * math.cos(rad), py - length * math.sin(rad)
    if tier == "golden":
        m.box((8.6, 24.5, 7.75), (11.5, 26, 8.25), "trim")
        tassel(m, 12, 9, 8, 3.5)
    elif tier == "netherite":
        # A hooked spike on the back of the head.
        m.box((8.6, 24.0, 7.78), (13.5, 25.4, 8.22), "blade", "edge", "edge", rot=((8.6, 25.4, 8), "z", -22.5))
        m.box((12.4, 22.2, 7.8), (13.6, 24.8, 8.2), "edge", rot=((13, 24.8, 8), "z", -45))
        m.box((7.2, 26.5, 7.2), (8.8, 29.5, 8.8), "metal")
    elif tier == "stormsteel":
        m.box((7.5, 26.5, 7.5), (8.5, 30.0, 8.5), "trim")
        m.box((7.65, 30.0, 7.65), (8.35, 31.5, 8.35), "glow", glow=True)
    return m


def battleaxe(s):
    m = Model()
    tier = s["sil"]
    m.box((7, -9.5, 7), (9, -8, 9), "trim")
    m.box((7.3, -8, 7.3), (8.7, 22, 8.7), "shaft", "shaft", "trim")
    m.box((7.15, -6, 7.15), (8.85, 3, 8.85), "grip")
    m.box((6.6, 13, 6.6), (9.4, 21, 9.4), "trim")
    m.box((7.4, 16, 6.3), (8.6, 18, 9.7), "gem", glow=bool(s["glow"]))
    sides = (1,) if tier == "stone" else (-1, 1)
    size = {"stone": 1.1, "iron": 1.0, "golden": 1.15, "diamond": 1.0, "netherite": 1.05, "stormsteel": 1.1}[tier]
    for sgn in sides:
        def X(a, b):
            lo, hi = 8 + sgn * a * size, 8 + sgn * b * size
            return (min(lo, hi), max(lo, hi))
        # A bearded crescent: narrow neck, flaring body, and a curved bit whose horns sweep out.
        glowing = bool(s["glow"])
        k = size
        x0, x1 = X(1.4, 3.6)
        m.box((x0, 15.2, 7.6), (x1, 18.8, 8.4), "metal")
        x0, x1 = X(3.6, 5.6)
        m.box((x0, 13.6, 7.64), (x1, 20.4, 8.36), "metal")
        ex = 8 + sgn * 5.6 * k
        xa, xb = sorted((ex, ex + sgn * 1.4))
        m.box((xa, 12.4, 7.68), (xb, 21.6, 8.32), "metal")
        ea, eb = sorted((ex + sgn * 1.4, ex + sgn * 2.2))
        m.box((ea, 12.6, 7.72), (eb, 21.4, 8.28), "glow" if glowing else "edge", glow=glowing)
        # Horns: the bit's top and bottom swept outward, each with its own honed edge.
        for (y0, y1, piv, ang) in ((21.2, 25.6, 21.2, -22.5 * sgn), (8.4, 12.8, 12.8, 22.5 * sgn)):
            m.box((xa, y0, 7.7), (xb + (0.8 if sgn > 0 else 0) - (0 if sgn > 0 else -0.0), y1, 8.3), "metal",
                  rot=((ex + sgn * 0.7, piv, 8), "z", ang))
            m.box((ea, y0, 7.74), (eb, y1, 8.26), "glow" if glowing else "edge", glow=glowing,
                  rot=((ex + sgn * 0.7, piv, 8), "z", ang))
        # Rivets where the head meets the haft.
        for y in (14.6, 19.4):
            rx = 8 + sgn * 2.6
            m.box((rx - 0.35, y - 0.35, 7.45), (rx + 0.35, y + 0.35, 8.55), "trim")
        if tier == "netherite":
            ex = 8 + sgn * 7.4
            m.box((ex - 0.5, 22.5, 7.72), (ex + 0.5, 25, 8.28), "edge", rot=((ex, 22.5, 8), "z", -22.5 * sgn))
            m.box((ex - 0.5, 8.5, 7.72), (ex + 0.5, 11, 8.28), "edge", rot=((ex, 11, 8), "z", 22.5 * sgn))
        if tier == "diamond":
            ex = 8 + sgn * 5.2
            m.box((ex - 0.9, 16, 7.5), (ex + 0.9, 18, 8.5), "gem", glow=True, rot=((ex, 17, 8), "z", 45))
        if tier == "stormsteel":
            ex = 8 + sgn * 4.6
            m.box((ex - 0.3, 20.5, 7.75), (ex + 0.3, 25.5, 8.25), "trim", rot=((ex, 20.5, 8), "z", -22.5 * sgn))
    if tier == "stone":
        m.box((6.0, 15, 7.6), (7.3, 19, 8.4), "cloth")
    m.box((7.4, 21, 7.4), (8.6, 24.5, 8.6), "metal")
    m.box((7.7, 24.5, 7.7), (8.3, 26, 8.3), "metal" if tier != "golden" else "gem")
    if tier == "golden":
        tassel(m, 8, 13, 8, 3.0)
    return m


def warhammer_legend(s):
    m = warhammer(s)
    if s["legend"] == "earthshaker":
        for x0, y0 in ((3, 17), (4.5, 19.5), (11, 16.5), (10, 19.8)):
            m.box((x0, y0, 4.85), (x0 + 1.6, y0 + 0.6, 11.15), "glow", glow=True)
        m.box((2.6, 18.7, 4.85), (5.2, 19.2, 11.15), "glow", glow=True)
        m.box((10.6, 18, 4.85), (13.4, 18.5, 11.15), "glow", glow=True)
    return m


# The scythe's blade is built pointing toward -x; flip it so it sweeps out in front of the player.
SCYTHE_FLIP = True


def scythe_legend(s):
    m = scythe(s)
    if s["legend"] == "voidreaver":
        m.box((8.6, 24.5, 7.75), (11.5, 26, 8.25), "blade", "edge", "edge")
        m.box((11.5, 22.5, 7.8), (12.8, 26, 8.2), "blade", "edge", "edge")
    for y in (-2, 4, 14):
        m.box((7.25, y, 7.25), (8.75, y + 0.6, 8.75), "trim")
    return mirror_x(m) if SCYTHE_FLIP else m


def battleaxe_legend(s):
    m = battleaxe(s)
    if s["legend"] == "kingsbane":
        for x in (6.2, 8.0, 9.8):
            m.box((x - 0.4, 21, 7.6), (x + 0.4, 23.5 if x != 8.0 else 27, 8.4), "trim")
    for y in (5, 9):
        m.box((7.15, y, 7.15), (8.85, y + 0.6, 8.85), "trim")
    return m


BUILDERS = {"longsword": longsword, "greatsword": greatsword, "katana": katana, "dual_daggers": dagger, "spear": spear,
            "warhammer": warhammer_legend, "scythe": scythe_legend, "battleaxe": battleaxe_legend}

SHAFT = {"stone": "wood", "iron": "wood", "golden": "birch", "diamond": "birch", "netherite": "black_iron", "stormsteel": "black_iron"}


def textures(kind, tier):
    blade = {"golden": "gold"}.get(tier, TIER_BLADE[tier])
    glow = TIER_GLOW[tier] or "ember"
    return {
        "blade": f"steelstorm:item/3d/grad_{tier}", "edge": f"steelstorm:item/3d/gedge_{tier}",
        "fuller": f"steelstorm:item/3d/fuller_{blade}", "metal": f"steelstorm:item/3d/metal_{blade}",
        "trim": f"steelstorm:item/3d/trim_{TIER_TRIM[tier]}", "gem": f"steelstorm:item/3d/gem_{TIER_GEM[tier]}",
        "grip": "steelstorm:item/3d/grip_ito" if kind == "katana" and tier == "iron" else f"steelstorm:item/3d/grip_{TIER_GRIP[tier]}",
        "shaft": f"steelstorm:item/3d/shaft_{SHAFT[tier]}", "collar": "steelstorm:item/3d/trim_gold",
        "glow": f"steelstorm:item/3d/glow_{glow}", "cloth": f"steelstorm:item/3d/grip_{TIER_CLOTH[tier]}",
        "particle": f"steelstorm:item/3d/blade_{blade}",
    }


def legend_textures(kind, name, spec):
    b = spec["blade"]
    return {
        "blade": f"steelstorm:item/3d/grad_{name}", "edge": f"steelstorm:item/3d/gedge_{name}",
        "fuller": f"steelstorm:item/3d/glow_{spec['glow']}", "metal": f"steelstorm:item/3d/metal_{b}",
        "trim": f"steelstorm:item/3d/trim_{spec['trim']}", "gem": f"steelstorm:item/3d/gem_{spec['gem']}",
        "grip": "steelstorm:item/3d/grip_ito" if kind == "katana" else f"steelstorm:item/3d/grip_{spec['grip']}",
        "shaft": f"steelstorm:item/3d/shaft_{spec.get('shaft', 'wood')}", "collar": "steelstorm:item/3d/trim_gold",
        "glow": f"steelstorm:item/3d/glow_{spec['glow']}", "cloth": f"steelstorm:item/3d/grip_{spec['grip']}",
        "wing": "steelstorm:item/3d/trim_cloth_white", "particle": f"steelstorm:item/3d/blade_{b}",
    }


def write(name, kind, m, tex):
    data = {"credit": "Steelstorm Arsenal (generated)", "texture_size": [16, 16], "textures": tex,
            "elements": m.elements, "display": TYPE_MODELS[kind][1]}
    with open(os.path.join(MODELS, f"{name}.json"), "w") as f:
        json.dump(data, f, indent=1)


def main():
    write_textures()
    for name in ("violet_wrap", "ember_wrap", "green_wrap", "teal_wrap", "straw", "red_wrap", "blue_wrap"):
        save(tex_grip(R[name], 5), "item", "3d", f"grip_{name}.png")
    import hd_textures
    hd_textures.write(GRADIENTS)
    count = 0
    for kind, build in BUILDERS.items():
        for tier in TIERS:
            write(f"{tier}_{kind}", kind, build(tier_style(tier)), textures(kind, tier))
            count += 1
    for name, (kind, spec) in LEGENDARY_MODELS.items():
        write(name, kind, BUILDERS[kind](legend_style(name, spec)), legend_textures(kind, name, spec))
        count += 1
    print(f"{count} weapon models written")


if __name__ == "__main__":
    main()
