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
import os

from gen_models import MODELS, TYPE_MODELS, Model, blade_column, tex_grip, write_textures
from texlib import R, TIERS, TIER_BLADE, TIER_CLOTH, TIER_GEM, TIER_GLOW, TIER_GRIP, TIER_TRIM, save

# ----------------------------------------------------------------------------- shared parts


def tassel(m, x, y, z, length=4.5):
    """A cloth tassel hanging below a pommel."""
    m.box((x - 0.35, y - 1.0, z - 0.35), (x + 0.35, y, z + 0.35), "trim")
    m.box((x - 0.55, y - 1.0 - length, z - 0.55), (x + 0.55, y - 1.0, z + 0.55), "cloth")
    m.box((x - 0.7, y - 1.6 - length, z - 0.7), (x + 0.7, y - 1.0 - length, z + 0.7), "cloth")


def inlay(m, tier, x0, x1, y0, y1, z0=7.45, z1=8.55):
    """A glowing strip set into a blade (zig-zagging for lightning)."""
    if not TIER_GLOW[tier]:
        return
    if tier == "stormsteel":
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


# ----------------------------------------------------------------------------- weapon types

def longsword(tier):
    m = Model()
    wide = {"stone": 1.9, "iron": 1.6, "golden": 1.55, "diamond": 1.5, "netherite": 1.75, "stormsteel": 1.45}[tier]
    top = {"stone": 24.0, "iron": 26.0, "golden": 26.0, "diamond": 26.5, "netherite": 25.5, "stormsteel": 27.0}[tier]
    tip = {"stone": 3.2, "iron": 4.6, "golden": 4.4, "diamond": 5.0, "netherite": 4.0, "stormsteel": 4.8}[tier]
    sword_pommel(m, tier, 0.5)
    m.box((7.25, 0.5, 7.25), (8.75, 8, 8.75), "grip")
    sword_guard(m, tier, 8, 5.0)
    m.box((8 - wide - 0.2, 9.5, 7.6), (8 + wide + 0.2, 11, 8.4), "blade", "edge", "edge")
    blade_column(m, 8 - wide, 8 + wide, 7.6 if tier != "stone" else 7.45, 8.4 if tier != "stone" else 8.55, 11, top, tip)
    if tier in ("iron", "golden"):
        m.box((7.6, 11.5, 7.5), (8.4, top - 2, 8.5), "fuller")
    inlay(m, tier, 7.65, 8.35, 11.5, top - 2)
    if tier == "netherite":
        notches(m, 8 - wide, 8 + wide, 13, top - 3)
    if tier == "golden":
        m.box((8 - wide - 0.4, 11.5, 7.5), (8 + wide + 0.4, 12.3, 8.5), "trim")
    return m


def greatsword(tier):
    m = Model()
    wide = {"stone": 3.1, "iron": 2.75, "golden": 2.6, "diamond": 2.5, "netherite": 3.0, "stormsteel": 2.45}[tier]
    top = {"stone": 25.0, "iron": 27.4, "golden": 27.0, "diamond": 27.8, "netherite": 27.0, "stormsteel": 27.8}[tier]
    sword_pommel(m, tier, -1.5, 1.35)
    m.box((7, -1.5, 7), (9, 9, 9), "grip")
    m.box((6.7, 3.5, 6.7), (9.3, 4.5, 9.3), "trim")
    sword_guard(m, tier, 9, 6.6, th=2.0, depth=(6.8, 9.2))
    m.box((8 - wide - 0.6, 11, 6.8), (8 + wide + 0.6, 12.2, 9.2), "trim")
    blade_column(m, 8 - wide, 8 + wide, 7.4, 8.6, 12.2, top, 4.0)
    if tier in ("iron", "golden"):
        m.box((7.3, 12.8, 7.3), (8.7, top - 1.5, 8.7), "fuller")
    inlay(m, tier, 7.4, 8.6, 13, top - 1.5, 7.3, 8.7)
    if tier == "netherite":
        notches(m, 8 - wide, 8 + wide, 14, top - 3, 3.0)
    if tier == "diamond":
        for sgn in (-1, 1):
            m.box((8 + sgn * wide - 0.5, 15, 7.5), (8 + sgn * wide + 0.5, 19, 8.5), "edge", rot=((8 + sgn * wide, 15, 8), "z", -22.5 * sgn))
    if tier == "stone":
        m.box((8 - wide, 18.5, 7.35), (8 - wide + 1.2, 20.0, 8.65), "trim")
    return m


def katana(tier):
    m = Model()
    sword_pommel(m, tier, -2, 0.8) if tier in ("golden", "diamond", "stormsteel", "stone") else \
        m.box((7.2, -3, 7.2), (8.8, -2, 8.8), "trim")
    m.box((7.25, -2, 7.25), (8.75, 8, 8.75), "grip")
    # The tsuba (hand guard).
    if tier == "stone":
        m.box((5.8, 8, 5.8), (10.2, 8.8, 10.2), "trim")
    elif tier == "iron":
        m.box((5.4, 8, 5.4), (10.6, 8.6, 10.6), "trim")
        m.box((5.4, 8, 5.4), (10.6, 8.6, 10.6), "trim", rot=((8, 8.3, 8), "y", 45))
    elif tier == "golden":
        for a in (0, 22.5, 45, 67.5):
            m.box((5.6, 8, 5.6), (10.4, 8.6, 10.4), "trim", rot=((8, 8.3, 8), "y", a if a <= 45 else -22.5))
        m.box((7.1, 7.9, 4.8), (8.9, 8.7, 5.6), "gem")
    elif tier == "diamond":
        m.box((5.2, 8, 7.0), (10.8, 8.7, 9.0), "trim")
        m.box((7.0, 8, 5.2), (9.0, 8.7, 10.8), "trim")
        for (x, z) in ((5.2, 8), (10.8, 8), (8, 5.2), (8, 10.8)):
            m.box((x - 0.5, 8.1, z - 0.5), (x + 0.5, 9.3, z + 0.5), "gem", glow=True)
    elif tier == "netherite":
        m.box((5.6, 8, 5.6), (10.4, 8.6, 10.4), "metal", rot=((8, 8.3, 8), "y", 45))
        for (x, z) in ((4.6, 8), (11.4, 8), (8, 4.6), (8, 11.4)):
            m.box((x - 0.4, 8.0, z - 0.4), (x + 0.4, 8.6, z + 0.4), "metal")
        m.box((7.4, 7.9, 5.9), (8.6, 8.7, 10.1), "glow", glow=True)
    else:
        m.box((5.4, 8, 5.4), (10.6, 8.6, 10.6), "trim", rot=((8, 8.3, 8), "y", 45))
        m.box((6.4, 8.6, 6.4), (9.6, 8.9, 9.6), "glow", glow=True, rot=((8, 8.75, 8), "y", 45))
    m.box((7.1, 8.6, 7.4), (8.9, 9.6, 8.6), "collar")
    curve = {"stone": 0.6, "iron": 1.0, "golden": 1.1, "diamond": 0.8, "netherite": 1.3, "stormsteel": 1.2}[tier]
    segs = [(9.6, 16, 0.0, 1.6), (16, 21, -0.2, 1.55), (21, 25, -0.45, 1.5), (25, 28, -0.75, 1.4)]
    for (y0, y1, dx, w) in segs:
        m.box((8 - w / 2 + dx * curve, y0, 7.75), (8 + w / 2 + dx * curve, y1, 8.25), "blade", "edge", "edge")
        if TIER_GLOW[tier]:
            m.box((8 + w / 2 + dx * curve - 0.3, y0, 7.7), (8 + w / 2 + dx * curve + 0.05, y1, 8.3), "glow", glow=True)
    m.box((8 - 0.55 - 1.1 * curve, 28, 7.78), (8 + 0.55 - 1.1 * curve, 29.6, 8.22), "blade", "edge", "edge")
    m.box((8 - 0.3 - 1.45 * curve, 29.6, 7.8), (8 + 0.3 - 1.45 * curve, 30.6, 8.2), "edge")
    return m


def dagger(tier):
    m = Model()
    length = {"stone": 13.0, "iron": 15.0, "golden": 14.5, "diamond": 16.0, "netherite": 14.0, "stormsteel": 16.0}[tier]
    m.box((7.2, -1.5, 7.6), (8.8, 0, 8.4), "trim")
    if tier in ("golden", "stormsteel"):
        tassel(m, 8, -1.5, 8, 2.5)
    m.box((7.4, 0, 7.4), (8.6, 5, 8.6), "grip")
    sword_guard(m, tier, 5, 3.0, th=1.0, depth=(7.2, 8.8))
    wide = 1.25 if tier != "stone" else 1.5
    blade_column(m, 8 - wide, 8 + wide, 7.65, 8.35, 6, length - 2.8, 2.8)
    inlay(m, tier, 7.75, 8.25, 6.5, length - 3.5, 7.6, 8.4)
    if tier == "netherite":
        m.box((8 + wide - 0.2, 8, 7.7), (8 + wide + 1.6, 9.0, 8.3), "edge", rot=((8 + wide, 8, 8), "z", 45))
        notches(m, 8 - wide, 8 + wide, 9, length - 4, 2.2)
    if tier == "diamond":
        m.box((8 - 0.9, length - 4, 7.55), (8 + 0.9, length - 2.5, 8.45), "gem", glow=True, rot=((8, length - 3.2, 8), "z", 45))
    return m


def spear(tier):
    m = Model()
    m.box((7.2, -15, 7.2), (8.8, -13, 8.8), "trim")
    m.box((7.4, -13, 7.4), (8.6, 22, 8.6), "shaft", "shaft", "trim")
    m.box((7.25, 2, 7.25), (8.75, 10, 8.75), "grip")
    m.box((7.1, 21, 7.1), (8.9, 23.5, 8.9), "trim")
    if tier == "stone":
        # A knapped flint point lashed on with twine.
        m.box((7.2, 20.5, 7.2), (8.8, 22.5, 8.8), "cloth")
        m.box((6.4, 23.5, 7.55), (9.6, 26, 8.45), "blade", "edge", "edge")
        m.box((6.9, 26, 7.6), (9.1, 28, 8.4), "blade", "edge", "edge")
        m.box((7.4, 28, 7.65), (8.6, 29.5, 8.35), "edge")
        return m
    m.box((7.4, 21.8, 6.9), (8.6, 22.8, 9.1), "gem")
    reach = {"iron": 0, "golden": 0, "diamond": 0.8, "netherite": 0, "stormsteel": 0.6}[tier]
    m.box((6.6, 23.5, 7.6), (9.4, 26, 8.4), "blade", "edge", "edge")
    m.box((6.2, 26, 7.62), (9.8, 28 + reach * 0.5, 8.38), "blade", "edge", "edge")
    m.box((6.7, 28 + reach * 0.5, 7.65), (9.3, 29.8 + reach, 8.35), "blade", "edge", "edge")
    m.box((7.2, 29.8 + reach, 7.7), (8.8, 31 + reach, 8.3), "blade", "edge", "edge")
    m.box((7.65, 31 + reach, 7.75), (8.35, 32, 8.25), "edge")
    inlay(m, tier, 7.75, 8.25, 24, 30 + reach, 7.55, 8.45)
    if tier == "iron":
        m.box((5, 22.5, 7.6), (11, 23.2, 8.4), "trim")
    elif tier == "golden":
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.4 - 0.8, 21.5, 7.6), (8 + sgn * 2.4 + 0.8, 25.0, 8.4), "trim", rot=((8 + sgn * 1.5, 22, 8), "z", -22.5 * sgn))
        m.box((7.3, 17.5, 7.3), (8.7, 21, 8.7), "cloth")
        m.box((7.0, 15.5, 7.0), (9.0, 17.5, 9.0), "cloth")
    elif tier == "diamond":
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.0 - 0.45, 23, 7.65), (8 + sgn * 2.0 + 0.45, 27.5, 8.35), "edge", rot=((8 + sgn * 2, 23, 8), "z", -22.5 * sgn))
    elif tier == "netherite":
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.2 - 0.5, 22, 7.65), (8 + sgn * 2.2 + 0.5, 26.0, 8.35), "metal", rot=((8 + sgn * 2.2, 26, 8), "z", 22.5 * sgn))
            m.box((8 + sgn * 3.4 - 0.4, 21, 7.7), (8 + sgn * 3.4 + 0.4, 23.5, 8.3), "edge", rot=((8 + sgn * 3.4, 23.5, 8), "z", 45 * sgn))
    else:
        for sgn in (-1, 1):
            m.box((8 + sgn * 2.6 - 0.35, 21.5, 7.75), (8 + sgn * 2.6 + 0.35, 26.5, 8.25), "trim", rot=((8 + sgn * 1.2, 22, 8), "z", -45 * sgn))
        m.box((7.5, 15, 7.95), (8.5, 21, 8.05), "cloth", rot=((8, 21, 8), "x", 22.5))
    return m


def warhammer(tier):
    m = Model()
    m.box((7, -11.5, 7), (9, -9.5, 9), "trim")
    m.box((7.3, -9.5, 7.3), (8.7, 17, 8.7), "shaft", "shaft", "trim")
    m.box((7.15, -8, 7.15), (8.85, 2, 8.85), "grip")
    if tier == "stone":
        m.box((2.5, 15, 4.8), (13.5, 22.5, 11.2), "metal")
        m.box((7, 14, 7), (9, 15, 9), "cloth")
        m.box((7.6, 22.5, 7.6), (8.4, 23.5, 8.4), "metal")
        return m
    if tier == "iron":
        m.box((2, 15.5, 5), (14, 22.5, 11), "metal")
        m.box((1, 16, 5.5), (2, 22, 10.5), "metal")
        m.box((14, 16, 5.5), (15, 22, 10.5), "metal")
        m.box((6.5, 15, 4.6), (9.5, 23, 11.4), "trim")
        m.box((7.3, 17.8, 4.3), (8.7, 20.2, 11.7), "gem")
        m.box((7.2, 22.5, 7.2), (8.8, 25, 8.8), "metal")
        return m
    if tier == "golden":
        m.box((3, 15.5, 5.5), (13, 22.5, 10.5), "metal")
        for x in (2.2, 13.0):
            m.box((x, 15, 5), (x + 0.8, 23, 11), "trim")
        m.box((6.5, 14.5, 4.8), (9.5, 23.5, 11.2), "trim")
        m.box((7.1, 17.6, 4.4), (8.9, 20.4, 11.6), "gem")
        m.box((7.3, 23.5, 7.3), (8.7, 27.5, 8.7), "trim")
        m.box((7.0, 27.5, 7.0), (9.0, 29.0, 9.0), "gem")
        tassel(m, 8, 14.5, 8, 3.0)
        return m
    if tier == "diamond":
        m.box((3, 15.5, 5), (13, 22.5, 11), "metal")
        for sgn in (-1, 1):
            x = 8 + sgn * 5.0
            m.box((x - 1.6, 16.5, 6.4), (x + 1.6, 21.5, 9.6), "gem", glow=True, rot=((x, 19, 8), "x", 45))
        m.box((6.5, 15, 4.6), (9.5, 23, 11.4), "trim")
        m.box((7.2, 22.5, 7.2), (8.8, 27, 8.8), "edge", rot=((8, 22.5, 8), "y", 45))
        return m
    if tier == "netherite":
        m.box((2.5, 15.5, 5), (13.5, 22.5, 11), "metal")
        for (y, z) in ((16.5, 4.2), (20.5, 4.2), (16.5, 11.0), (20.5, 11.0)):
            m.box((1.0, y, z), (2.5, y + 1.2, z + 0.8), "edge")
            m.box((13.5, y, z), (15.0, y + 1.2, z + 0.8), "edge")
        m.box((7.4, 15.4, 4.7), (8.6, 22.6, 11.3), "glow", glow=True)
        m.box((7.4, 22.5, 7.4), (8.6, 27.5, 8.6), "metal")
        m.box((7.7, 27.5, 7.7), (8.3, 29, 8.3), "edge")
        return m
    # stormsteel: a head like a storm-cloud anvil with a glowing core and lightning-rod prongs
    m.box((2, 15.5, 5.2), (14, 22.5, 10.8), "metal")
    m.box((1, 17, 6), (2, 21, 10), "trim")
    m.box((14, 17, 6), (15, 21, 10), "trim")
    m.box((4.5, 17.5, 4.8), (11.5, 20.5, 11.2), "glow", glow=True)
    for x in (5.5, 10.5):
        m.box((x - 0.35, 22.5, 7.65), (x + 0.35, 26.5, 8.35), "trim")
    m.box((7.6, 22.5, 7.6), (8.4, 28, 8.4), "trim")
    return m


def scythe(tier):
    m = Model()
    m.box((7.4, -14, 7.4), (8.6, 26, 8.6), "shaft", "shaft", "trim")
    m.box((7.25, -12, 7.25), (8.75, -4, 8.75), "grip")
    m.box((8.6, 8, 7.5), (12, 9, 8.5), "grip")
    m.box((7, 23, 7), (9, 26.5, 9), "trim")
    m.box((7.3, 24, 6.8), (8.7, 25.4, 9.2), "gem", glow=tier in ("diamond", "stormsteel"))
    reach = {"stone": 0.7, "iron": 1.0, "golden": 1.05, "diamond": 1.0, "netherite": 1.0, "stormsteel": 1.1}[tier]
    steps = [(2.0, 8.0, 23.4, 27.0), (-3.0, 2.0, 22.0, 25.6), (-7.0, -3.0, 20.2, 23.6), (-10.0, -7.0, 18.4, 21.2),
             (-12.0, -10.0, 16.8, 18.9), (-13.4, -12.0, 15.8, 17.2)]
    if reach < 1:
        steps = steps[:4]
    glowing = TIER_GLOW[tier] is not None
    for (x0, x1, y0, y1) in steps:
        m.box((x0, y0, 7.75), (x1, y1, 8.25), "blade", "edge", "edge")
        m.box((x0, y0 - 0.3, 7.8), (x1, y0 + 0.35, 8.2), "glow" if glowing else "edge", glow=glowing)
    if tier == "golden":
        m.box((8.6, 24.5, 7.75), (11.5, 26, 8.25), "trim")
        tassel(m, 12, 9, 8, 3.5)
    elif tier == "netherite":
        # A second, smaller blade on the back of the head.
        for (x0, x1, y0, y1) in ((9.0, 12.0, 23.0, 25.5), (12.0, 14.5, 21.0, 24.0), (14.5, 15.8, 19.5, 22.0)):
            m.box((x0, y0, 7.78), (x1, y1, 8.22), "blade", "edge", "edge")
        m.box((7.2, 26.5, 7.2), (8.8, 29.5, 8.8), "metal")
    elif tier == "diamond":
        for x in (-4.0, -9.0):
            m.box((x - 0.6, 24.0 - abs(x) * 0.35, 7.7), (x + 0.6, 26.0 - abs(x) * 0.35, 8.3), "edge", rot=((x, 24, 8), "z", 45))
    elif tier == "stormsteel":
        m.box((7.5, 26.5, 7.5), (8.5, 30.0, 8.5), "trim")
        m.box((7.65, 30.0, 7.65), (8.35, 31.5, 8.35), "glow", glow=True)
    return m


def battleaxe(tier):
    m = Model()
    m.box((7, -9.5, 7), (9, -8, 9), "trim")
    m.box((7.3, -8, 7.3), (8.7, 22, 8.7), "shaft", "shaft", "trim")
    m.box((7.15, -6, 7.15), (8.85, 3, 8.85), "grip")
    m.box((6.6, 13, 6.6), (9.4, 21, 9.4), "trim")
    m.box((7.4, 16, 6.3), (8.6, 18, 9.7), "gem", glow=tier in ("diamond", "stormsteel"))
    sides = (1,) if tier == "stone" else (-1, 1)
    size = {"stone": 1.1, "iron": 1.0, "golden": 1.15, "diamond": 1.0, "netherite": 1.05, "stormsteel": 1.1}[tier]
    for sgn in sides:
        def X(a, b):
            lo, hi = 8 + sgn * a * size, 8 + sgn * b * size
            return (min(lo, hi), max(lo, hi))
        x0, x1 = X(1.4, 4.0)
        m.box((x0, 14, 7.6), (x1, 20, 8.4), "metal")
        x0, x1 = X(4.0, 6.5)
        m.box((x0, 12.5 - (size - 1) * 6, 7.65), (x1, 21.5 + (size - 1) * 6, 8.35), "metal")
        x0, x1 = X(6.5, 7.6)
        glowing = TIER_GLOW[tier] is not None
        m.box((x0, 11 - (size - 1) * 8, 7.7), (x1, 23 + (size - 1) * 8, 8.3), "glow" if glowing else "edge", glow=glowing)
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


BUILDERS = {"longsword": longsword, "greatsword": greatsword, "katana": katana, "dual_daggers": dagger, "spear": spear,
            "warhammer": warhammer, "scythe": scythe, "battleaxe": battleaxe}

SHAFT = {"stone": "wood", "iron": "wood", "golden": "birch", "diamond": "birch", "netherite": "black_iron", "stormsteel": "black_iron"}


def textures(kind, tier):
    blade = {"golden": "gold"}.get(tier, TIER_BLADE[tier])
    glow = TIER_GLOW[tier] or "ember"
    return {
        "blade": f"steelstorm:item/3d/blade_{blade}", "edge": f"steelstorm:item/3d/edge_{blade}",
        "fuller": f"steelstorm:item/3d/fuller_{blade}", "metal": f"steelstorm:item/3d/metal_{blade}",
        "trim": f"steelstorm:item/3d/trim_{TIER_TRIM[tier]}", "gem": f"steelstorm:item/3d/gem_{TIER_GEM[tier]}",
        "grip": "steelstorm:item/3d/grip_ito" if kind == "katana" and tier == "iron" else f"steelstorm:item/3d/grip_{TIER_GRIP[tier]}",
        "shaft": f"steelstorm:item/3d/shaft_{SHAFT[tier]}", "collar": "steelstorm:item/3d/trim_gold",
        "glow": f"steelstorm:item/3d/glow_{glow}", "cloth": f"steelstorm:item/3d/grip_{TIER_CLOTH[tier]}",
        "particle": f"steelstorm:item/3d/blade_{blade}",
    }


def main():
    write_textures()
    for name in ("violet_wrap", "ember_wrap", "green_wrap", "teal_wrap", "straw", "red_wrap", "blue_wrap"):
        save(tex_grip(R[name], 5), "item", "3d", f"grip_{name}.png")
    count = 0
    for kind, build in BUILDERS.items():
        disp = TYPE_MODELS[kind][1]
        for tier in TIERS:
            m = build(tier)
            data = {"credit": "Steelstorm Arsenal (generated)", "texture_size": [16, 16], "textures": textures(kind, tier),
                    "elements": m.elements, "display": disp}
            with open(os.path.join(MODELS, f"{tier}_{kind}.json"), "w") as f:
                json.dump(data, f, indent=1)
            count += 1
    print(f"{count} tier models written")


if __name__ == "__main__":
    main()
