#!/usr/bin/env python3
"""32x32 inventory icons for every Steelstorm weapon, legendary and item.

Run from the repository root:  python3 tools/gen_icons.py   (needs Pillow)
"""
import math
import random

from texlib import (DIAG, PERP, R, TIERS, TIER_BLADE, TIER_GEM, TIER_GRIP, TIER_TRIM, Canvas, Ramp, along,
                    bar_test, blade_shader, clamp, cylinder_shader, disc_test, gem, hexc, metal_shader, mix,
                    noise_dither, norm, poly_test, save, sphere_shader)

S = 32


def P(p0, s, side=0.0, u=DIAG, n=PERP):
    return (p0[0] + u[0] * s + n[0] * side, p0[1] + u[1] * s + n[1] * side)


def crossguard(cv, center, half, hw, ramp, u=DIAG, n=PERP, curl=0.0, flare=0.0):
    pts = [(center[0] - n[0] * half + u[0] * curl, center[1] - n[1] * half + u[1] * curl),
           center,
           (center[0] + n[0] * half + u[0] * curl, center[1] + n[1] * half + u[1] * curl)]
    cv.fill(bar_test(pts, lambda t: hw + flare * abs(t - 0.5) * 2, metal_shader(0.6), True, True), ramp)


def pommel(cv, c, r, ramp, gem_ramp=None):
    cv.fill(disc_test(c[0], c[1], r, sphere_shader(0.58)), ramp)
    if gem_ramp:
        cv.dot(int(c[0]), int(c[1]), R[gem_ramp].c[5])


# ----------------------------------------------------------------------------- weapon types

def longsword(blade, trim, grip, gem_r, deco=None):
    cv = Canvas()
    p0 = (4.2, 27.8)
    pommel(cv, p0, 2.3, R[trim], gem_r)
    cv.fill(bar_test([P(p0, 1.4), P(p0, 8.3)], lambda t: 1.35, cylinder_shader(0.5, 0.28, 2.5)), R[grip])
    pts = [P(p0, 9.3), P(p0, 31.0), P(p0, 36.4)]
    cv.fill(bar_test(pts, lambda t: 2.5 - 0.4 * t if t < 0.8 else 2.18 * (1 - (t - 0.8) / 0.2) ** 0.85,
                     blade_shader(fuller=0.22)), R[blade])
    crossguard(cv, P(p0, 8.9), 6.7, 1.15, R[trim], curl=0.9, flare=0.35)
    if gem_r:
        gem(cv, *P(p0, 8.9), gem_r, 1.25)
    if deco:
        deco(cv, p0)
    return cv


def greatsword(blade, trim, grip, gem_r, deco=None):
    cv = Canvas()
    p0 = (3.6, 28.4)
    pommel(cv, p0, 2.7, R[trim], gem_r)
    cv.fill(bar_test([P(p0, 1.6), P(p0, 9.4)], lambda t: 1.5, cylinder_shader(0.5, 0.28, 2.6)), R[grip])
    cv.fill(bar_test([P(p0, 4.8), P(p0, 6.2)], lambda t: 1.75, metal_shader(0.6)), R[trim])
    pts = [P(p0, 10.6), P(p0, 32.5), P(p0, 38.2)]

    def hw(t):
        if t < 0.08:
            return 2.7
        if t < 0.82:
            return 3.9 - 0.5 * t
        return 3.5 * (1 - (t - 0.82) / 0.18) ** 0.8
    cv.fill(bar_test(pts, hw, blade_shader(fuller=0.3, base=0.55)), R[blade])
    crossguard(cv, P(p0, 10.2), 8.3, 1.35, R[trim], curl=2.0, flare=0.6)
    if gem_r:
        gem(cv, *P(p0, 10.2), gem_r, 1.5)
    if deco:
        deco(cv, p0)
    return cv


def katana(blade, trim, grip, gem_r, deco=None, tsuba="black_iron"):
    cv = Canvas()
    p0 = (3.0, 29.0)

    def curve(s, total=38.0):
        return P(p0, s, -1.7 * math.sin(math.pi * clamp(s / total)))
    cv.fill(disc_test(*curve(0), 1.35, sphere_shader(0.45)), R["black_iron"])
    grip_pts = [curve(s) for s in (0.6, 5.0, 10.2)]

    def ito(t, v, s):
        diamond = (int(s * 0.9 + v * 1.4) + int(s * 0.9 - v * 1.4)) % 2 == 0
        return (0.85 if diamond and abs(v) < 0.75 else 0.32) - 0.25 * v
    cv.fill(bar_test(grip_pts, lambda t: 1.35, ito), R[grip])
    blade_pts = [curve(s) for s in (12.2, 18.0, 24.0, 30.0, 34.5, 37.6)]

    def hw(t):
        return 1.6 - 0.25 * t if t < 0.86 else 1.38 * (1 - (t - 0.86) / 0.14) ** 0.7

    def hamon(t, v, s):
        wave = -0.2 + 0.18 * math.sin(s * 1.4)
        if v < -0.7:
            return 1.0
        if v < wave:
            return 0.88
        return 0.45 - 0.2 * v
    cv.fill(bar_test(blade_pts, hw, hamon), R[blade])
    cv.fill(bar_test([curve(11.0), curve(12.6)], lambda t: 1.75, metal_shader(0.7)), R["gold"])
    cv.fill(disc_test(*curve(10.6), 2.9, lambda nx, ny, r: (0.85 if r > 0.78 else 0.4 - 0.3 * (nx + ny))), R[tsuba])
    if gem_r:
        gem(cv, *curve(10.6), gem_r, 0.9)
    if deco:
        deco(cv, curve)
    return cv


def dagger(cv, p0, u, blade, trim, grip, gem_r, length=12.5, serrated=False):
    n = (u[1] * -1, u[0]) if u[0] * u[1] < 0 else (-u[1], u[0])
    n = norm(n)
    cv.fill(disc_test(*p0, 1.55, lambda nx, ny, r: 0.65 - 0.4 * (nx + ny), inner=0.6), R[trim])
    cv.fill(bar_test([along(p0, u, 1.3), along(p0, u, 6.0)], lambda t: 1.15, cylinder_shader(0.5, 0.3, 2.2)), R[grip])
    pts = [along(p0, u, 7.0), along(p0, u, 7.0 + length), along(p0, u, 11.0 + length)]

    def hw(t):
        return 1.75 - 0.35 * t if t < 0.75 else 1.5 * (1 - (t - 0.75) / 0.25) ** 0.9

    def sh(t, v, s):
        if serrated and v > 0.45 and int(s * 1.3) % 2 == 0 and 0.1 < t < 0.7:
            return None
        return blade_shader(fuller=0.0)(t, v, s)
    cv.fill(bar_test(pts, hw, sh), R[blade])
    c = along(p0, u, 6.6)
    cv.fill(bar_test([(c[0] - n[0] * 3.6, c[1] - n[1] * 3.6), (c[0] + n[0] * 3.6, c[1] + n[1] * 3.6)],
                     lambda t: 0.95 + 0.4 * abs(t - 0.5), metal_shader(0.6), True, True), R[trim])
    if gem_r:
        cv.dot(int(c[0]), int(c[1]), R[gem_r].c[5])


def dual_daggers(blade, trim, grip, gem_r, deco=None, serrated=False):
    cv = Canvas()
    dagger(cv, (27.6, 27.8), norm((-1, -1)), blade, trim, grip, gem_r, serrated=serrated)
    dagger(cv, (4.4, 27.8), DIAG, blade, trim, grip, gem_r, serrated=serrated)
    if deco:
        deco(cv, None)
    return cv


def spear(blade, trim, grip, gem_r, deco=None, shaft="wood", wings=None):
    cv = Canvas()
    p0 = (2.6, 29.4)
    cv.fill(bar_test([P(p0, 0.0), P(p0, 27.0)], lambda t: 0.95, cylinder_shader(0.52)), R[shaft])
    cv.fill(bar_test([P(p0, -0.6), P(p0, 1.4)], lambda t: 1.2, metal_shader(0.55)), R[trim])
    cv.fill(bar_test([P(p0, 11.0), P(p0, 16.5)], lambda t: 1.15, cylinder_shader(0.5, 0.3, 2.0)), R[grip])
    if wings:
        for side in (-1, 1):
            cv.fill(bar_test([P(p0, 26.5, 0), P(p0, 25.0, side * 4.2), P(p0, 22.8, side * 5.4)],
                             lambda t: 1.1 - 0.7 * t, metal_shader(0.7), False, False), R[wings])
    cv.fill(bar_test([P(p0, 25.6), P(p0, 28.8)], lambda t: 1.35, metal_shader(0.62)), R[trim])
    cv.fill(bar_test([P(p0, 28.4, -3.0), P(p0, 28.4, 3.0)], lambda t: 0.75, metal_shader(0.55), True, True), R[trim])

    def leaf(t):
        return 3.3 * math.sin(math.pi * clamp(t * 0.9 + 0.08)) ** 0.85 * (1 - t) ** 0.25

    def ridge(t, v, s):
        if abs(v) < 0.16:
            return 0.95
        return 0.62 - 0.4 * v if v < 0 else 0.42 - 0.3 * v
    cv.fill(bar_test([P(p0, 28.9), P(p0, 40.5)], leaf, ridge), R[blade])
    if gem_r:
        cv.dot(*map(int, P(p0, 27.2)), R[gem_r].c[5])
    if deco:
        deco(cv, p0)
    return cv


def warhammer(blade, trim, grip, gem_r, deco=None, shaft="wood"):
    cv = Canvas()
    p0 = (4.4, 27.6)
    cv.fill(bar_test([P(p0, -1.4), P(p0, 24.5)], lambda t: 1.2, cylinder_shader(0.5)), R[shaft])
    cv.fill(bar_test([P(p0, 0.3), P(p0, 7.4)], lambda t: 1.35, cylinder_shader(0.5, 0.3, 2.3)), R[grip])
    cv.fill(disc_test(*P(p0, -1.6), 1.6, sphere_shader(0.55)), R[trim])
    c = P(p0, 21.6)

    def head(px, py):
        rel = (px - c[0], py - c[1])
        s = rel[0] * DIAG[0] + rel[1] * DIAG[1]
        d = rel[0] * PERP[0] + rel[1] * PERP[1]
        if abs(s) > 4.3 or abs(d) > 7.6:
            return None
        if abs(s) > 3.4 and abs(d) > 6.6:
            return None
        if abs(d) > 6.1:
            return 0.92 if d < 0 else 0.18
        if s > 3.3:
            return 0.85
        if s < -3.3:
            return 0.2
        return 0.6 - 0.07 * d - 0.04 * s
    cv.fill(head, R[blade])
    cv.fill(bar_test([P(c, 0, -6.2), P(c, 0, 6.2)], lambda t: 1.1, metal_shader(0.6), False, False), R[trim])
    for sd in (-4.5, 4.5):
        for ss in (-2.5, 2.5):
            x, y = P(c, ss, sd)
            cv.dot(int(x), int(y), R[trim].c[6])
    cv.fill(bar_test([P(c, 4.2), P(c, 8.4)], lambda t: 1.3 * (1 - t) + 0.1, blade_shader(edge_glint=False)), R[blade])
    if gem_r:
        gem(cv, *c, gem_r, 1.2)
    if deco:
        deco(cv, c)
    return cv


def scythe(blade, trim, grip, gem_r, deco=None, shaft="wood"):
    cv = Canvas()
    shaft_pts = [(3.2, 30.0), (9.0, 22.0), (15.5, 13.5), (21.6, 5.4)]
    cv.fill(bar_test(shaft_pts, lambda t: 0.98, cylinder_shader(0.5)), R[shaft])
    cv.fill(bar_test(shaft_pts[:2], lambda t: 1.15, cylinder_shader(0.5, 0.3, 2.0)), R[grip])
    cv.fill(bar_test([(12.2, 15.8), (15.6, 19.4)], lambda t: 0.8, cylinder_shader(0.55), True, True), R[grip])
    arc = []
    for k in range(9):
        a = math.radians(-58 - k * 15.5)
        arc.append((15.6 + 13.4 * math.cos(a) * 1.05, 18.6 + 13.4 * math.sin(a) * 0.98))

    def hw(t):
        return 2.9 * (1 - t) ** 0.7 + 0.25

    def sh(t, v, s):
        if v > 0.55:
            return 0.98
        if v < -0.7:
            return 0.2
        return 0.55 + 0.25 * (-v) * 0.6 + 0.1 * t
    cv.fill(bar_test(arc, hw, sh), R[blade])
    cv.fill(disc_test(21.4, 5.8, 1.9, sphere_shader(0.6)), R[trim])
    if gem_r:
        cv.dot(21, 5, R[gem_r].c[5])
    if deco:
        deco(cv, arc)
    return cv


def battleaxe(blade, trim, grip, gem_r, deco=None, shaft="wood"):
    cv = Canvas()
    p0 = (4.2, 27.8)
    cv.fill(bar_test([P(p0, -0.8), P(p0, 27.0)], lambda t: 1.15, cylinder_shader(0.5)), R[shaft])
    cv.fill(bar_test([P(p0, 0.2), P(p0, 7.0)], lambda t: 1.3, cylinder_shader(0.5, 0.3, 2.2)), R[grip])
    c = P(p0, 20.0)

    def bits(px, py):
        rel = (px - c[0], py - c[1])
        s = rel[0] * DIAG[0] + rel[1] * DIAG[1]
        d = rel[0] * PERP[0] + rel[1] * PERP[1]
        ad = abs(d)
        if ad < 1.4 or ad > 10.0:
            return None
        width = 1.7 + (ad - 1.4) / 8.6 * 5.0
        edge = 10.0 - 0.07 * s * s
        if abs(s) > width or ad > edge:
            return None
        if ad > edge - 1.2:
            return 0.98 if s > -width + 1 else 0.7
        return 0.62 - 0.25 * (s / width) - 0.15 * (ad / 10.0)
    cv.fill(bits, R[blade])
    cv.fill(bar_test([P(c, -2.6), P(c, 2.6)], lambda t: 1.6, metal_shader(0.6)), R[trim])
    cv.fill(bar_test([P(c, 2.8), P(c, 7.2)], lambda t: 1.25 * (1 - t) + 0.15, blade_shader(edge_glint=False)), R[blade])
    if gem_r:
        gem(cv, *c, gem_r, 1.1)
    if deco:
        deco(cv, c)
    return cv


WEAPONS = {
    "longsword": longsword, "greatsword": greatsword, "katana": katana, "dual_daggers": dual_daggers,
    "spear": spear, "warhammer": warhammer, "scythe": scythe, "battleaxe": battleaxe,
}


def tier_icon(kind, tier):
    blade = TIER_BLADE[tier]
    trim = TIER_TRIM[tier]
    grip = TIER_GRIP[tier]
    gem_r = TIER_GEM[tier]
    kwargs = {}
    if kind in ("spear", "warhammer", "scythe", "battleaxe"):
        kwargs["shaft"] = "black_iron" if tier == "netherite" else ("birch" if tier == "stormsteel" else "wood")
    cv = WEAPONS[kind](blade, trim, grip, gem_r, **kwargs)
    inlay = INLAY_COLOR.get(tier)
    if inlay and kind in INLAY_PATH:
        glow_line(cv, INLAY_PATH[kind], hexc(inlay))
    if tier == "stone":
        noise_dither(cv, ["stone"], 0.18, hash(kind) & 0xFFFF)
    img = cv.render()
    if tier == "stormsteel":
        sparks(img, kind)
    return img


# A bright inlay down the middle of the blade (or head) for the richer tiers.
INLAY_COLOR = {"golden": "#fff2a8", "diamond": "#e6fbff", "netherite": "#ff8a2a", "stormsteel": "#a6f6ff"}
INLAY_PATH = {
    "longsword": [P((4.2, 27.8), 11.0), P((4.2, 27.8), 29.0)],
    "greatsword": [P((3.6, 28.4), 12.5), P((3.6, 28.4), 31.0)],
    "katana": [(13.5, 19.5), (20.5, 11.5), (26.5, 5.0)],
    "spear": [P((2.6, 29.4), 29.5), P((2.6, 29.4), 37.0)],
    "scythe": [(15.6 + 12.3 * 1.05 * math.cos(math.radians(a)), 18.6 + 12.3 * 0.98 * math.sin(math.radians(a)))
               for a in range(-68, -160, -8)],
}


def sparks(img, kind):
    rng = random.Random(len(kind))
    for _ in range(3):
        x, y = rng.randint(18, 30), rng.randint(1, 13)
        if img.getpixel((x, y))[3] == 0:
            img.putpixel((x, y), hexc("#bff6ff"))


def glow_line(cv, pts, color, step=0.5):
    total = sum(math.hypot(pts[i + 1][0] - pts[i][0], pts[i + 1][1] - pts[i][1]) for i in range(len(pts) - 1))
    s = 0.0
    while s <= total:
        acc = 0.0
        for i in range(len(pts) - 1):
            l = math.hypot(pts[i + 1][0] - pts[i][0], pts[i + 1][1] - pts[i][1])
            if acc + l >= s:
                t = (s - acc) / l
                x = pts[i][0] + (pts[i + 1][0] - pts[i][0]) * t
                y = pts[i][1] + (pts[i + 1][1] - pts[i][1]) * t
                if cv.get(int(x), int(y)):
                    cv.dot(int(x), int(y), color)
                break
            acc += l
        s += step


# ----------------------------------------------------------------------------- legendaries

def tempest_edge():
    def deco(cv, p0):
        pts = [P(p0, 11, 0), P(p0, 14, -0.8), P(p0, 17, 0.8), P(p0, 20, -0.8), P(p0, 23, 0.8), P(p0, 26, -0.6), P(p0, 29, 0.4)]
        glow_line(cv, pts, "#fff27a")
    cv = longsword("tempest", "gold", "dark_leather", "aqua", deco)
    img = cv.render()
    for x, y in ((29, 1), (31, 4), (26, 2), (30, 7)):
        img.putpixel((x, y), hexc("#fff59a"))
    return img


def rimecleaver():
    def deco(cv, p0):
        for side in (-1, 1):
            base = P(p0, 10.6, side * 8.0)
            cv.fill(bar_test([base, P(p0, 13.8, side * 9.6)], lambda t: 1.1 * (1 - t) + 0.1, blade_shader()), R["ice"])
        glow_line(cv, [P(p0, 12.5), P(p0, 30.0)], "#e6fdff", 0.7)
    cv = greatsword("ice", "silver", "blue_wrap", "aqua", deco)
    img = cv.render()
    for x, y in ((27, 3), (31, 6), (24, 1)):
        img.putpixel((x, y), hexc("#ffffff"))
    return img


def voidreaver():
    def deco(cv, arc):
        rng = random.Random(9)
        for _ in range(9):
            x, y = rng.randint(2, 22), rng.randint(2, 14)
            e = cv.get(x, y)
            if e and e[0] is R["void"]:
                cv.dot(x, y, "#f0c8ff")
    cv = scythe("void", "obsidian", "dark_leather", "amethyst", deco, shaft="obsidian")
    return cv.render()


def earthshaker():
    def deco(cv, c):
        glow_line(cv, [P(c, -2.5, -5), P(c, -0.5, -2.5), P(c, 1.0, -3.6), P(c, 2.4, -1.0)], "#ffa23a")
        glow_line(cv, [P(c, -2.2, 3.2), P(c, 0.4, 2.0), P(c, 1.8, 4.6)], "#ff8a1e")
    cv = warhammer("earth", "gold", "dark_leather", "topaz", deco, shaft="black_iron")
    return cv.render()


def bloodfang():
    cv = dual_daggers("blood", "black_iron", "dark_leather", "ruby", serrated=True)
    return cv.render()


def skypiercer():
    def deco(cv, p0):
        glow_line(cv, [P(p0, 30.0), P(p0, 38.5)], "#ffffff", 0.6)
    cv = spear("sky", "gold", "blue_wrap", "sapphire", deco, shaft="birch", wings="cloth_white")
    return cv.render()


def moonveil():
    def deco(cv, curve):
        glow_line(cv, [curve(s) for s in (13, 18, 24, 30, 35)], "#c9f4ff", 0.6)
    cv = katana("moon", "silver", "blue_wrap", "sapphire", deco, tsuba="silver")
    return cv.render()


def kingsbane():
    def deco(cv, c):
        for sd in (-1, 1):
            glow_line(cv, [P(c, -5.5, sd * 9.4), P(c, 5.5, sd * 9.4)], "#ffd75a", 0.5)
    cv = battleaxe("obsidian", "gold", "red_wrap", "ruby", deco, shaft="black_iron")
    return cv.render()


LEGENDARIES = {
    "tempest_edge": tempest_edge, "rimecleaver": rimecleaver, "voidreaver": voidreaver, "earthshaker": earthshaker,
    "bloodfang": bloodfang, "skypiercer": skypiercer, "moonveil": moonveil, "kingsbane": kingsbane,
}


# ----------------------------------------------------------------------------- thrown weapons and misc items

def chakram():
    cv = Canvas()
    c = (16.0, 16.0)

    def ring(px, py):
        dx, dy = px - c[0], py - c[1]
        r = math.hypot(dx, dy)
        if r < 7.6 or r > 13.0:
            return None
        light = 0.55 - 0.3 * (dx + dy) / r
        if r > 12.0:
            light = 0.98 if dx + dy < 4 else 0.55
        if r < 8.6:
            light -= 0.2
        return light
    for k in range(8):
        a = math.radians(k * 45 + 10)
        a2 = a + math.radians(26)
        cv.fill(poly_test([(c[0] + math.cos(a) * 12.0, c[1] + math.sin(a) * 12.0),
                           (c[0] + math.cos(a2) * 15.6, c[1] + math.sin(a2) * 15.6),
                           (c[0] + math.cos(a2 + 0.35) * 12.0, c[1] + math.sin(a2 + 0.35) * 12.0)],
                          lambda x, y: 0.8), R["iron"], separate=False)
    cv.fill(ring, R["iron"], separate=False)
    cv.fill(disc_test(c[0], c[1], 7.7, lambda nx, ny, r: 0.6 - 0.35 * (nx + ny), inner=6.4), R["gold"])
    cv.fill(bar_test([(9.8, 16.0), (22.2, 16.0)], lambda t: 1.3, cylinder_shader(0.5, 0.3, 2.0)), R["leather"])
    return cv.render()


def throwing_knives():
    cv = Canvas()
    for ang, base in ((-20, (6.5, 27.0)), (-45, (4.5, 27.5)), (-70, (4.0, 25.5))):
        u = (math.cos(math.radians(ang)), math.sin(math.radians(ang)))
        cv.fill(disc_test(*base, 1.6, lambda nx, ny, r: 0.6 - 0.4 * (nx + ny), inner=0.7), R["iron"])
        cv.fill(bar_test([along(base, u, 1.4), along(base, u, 6.4)], lambda t: 0.95, cylinder_shader(0.5, 0.35, 1.8)),
                R["dark_leather"])

        def leaf(t):
            return 2.0 * math.sin(math.pi * clamp(t * 0.85 + 0.12)) ** 0.9
        cv.fill(bar_test([along(base, u, 6.6), along(base, u, 17.5)], leaf, blade_shader()), R["iron"])
    return cv.render()


def ingot():
    cv = Canvas()
    top = [(6, 14), (12, 9), (27, 9), (21, 14)]
    front = [(6, 14), (21, 14), (21, 22), (6, 22)]
    side = [(21, 14), (27, 9), (27, 17), (21, 22)]
    cv.fill(poly_test(front, lambda x, y: 0.45 + 0.1 * (y - 14) / -8), R["stormsteel"])
    cv.fill(poly_test(side, lambda x, y: 0.22), R["stormsteel"])
    cv.fill(poly_test(top, lambda x, y: 0.82 if (x + y) > 22 else 0.95), R["stormsteel"])
    img = cv.render()
    for x, y in ((9, 16), (10, 16), (11, 17), (14, 12), (15, 12)):
        img.putpixel((x, y), hexc("#e8f6ff"))
    for x, y in ((16, 17), (15, 18), (16, 18), (15, 19)):
        img.putpixel((x, y), hexc("#bff3ff"))
    return img


def raw_chunk():
    cv = Canvas()
    for (cx, cy, r) in ((13, 17, 7.5), (20, 13, 6.0), (19, 21, 5.5), (10, 11, 4.0)):
        cv.fill(disc_test(cx, cy, r, lambda nx, ny, rr: 0.5 - 0.35 * (nx + ny) + (0.1 if rr > 0.8 else 0)), R["stone"])
    noise_dither(cv, ["stone"], 0.25, 3)
    for (cx, cy) in ((12, 15), (19, 12), (18, 20), (9, 11), (15, 19)):
        cv.fill(poly_test([(cx, cy - 2.5), (cx + 1.6, cy), (cx, cy + 2.5), (cx - 1.6, cy)],
                          lambda x, y, cx=cx: 0.95 if x < cx else 0.5), R["stormsteel"], separate=False)
    return cv.render()


RUNE_GLYPHS = {
    "ember": ("topaz", "#ff9a2e"), "frost": ("aqua", "#9ff3ff"), "storm": ("sapphire", "#fff36b"),
    "venom": ("emerald", "#8dff6b"), "vampiric": ("ruby", "#ff4d6a"), "gale": ("silver", "#f2fbff"),
}


def rune(name):
    gem_r, glow = RUNE_GLYPHS[name]
    cv = Canvas()

    def tablet(px, py):
        if not (6 <= px <= 26 and 4 <= py <= 28):
            return None
        if (px < 8 or px > 24) and (py < 6 or py > 26):
            return None
        edge = min(px - 6, 26 - px, py - 4, 28 - py)
        if edge < 1.2:
            return 0.8 if (px + py) < 30 else 0.2
        return 0.45 + 0.08 * math.sin(px * 1.7 + py * 0.9)
    cv.fill(tablet, R["earth"])
    noise_dither(cv, ["earth"], 0.15, len(name))
    img = cv.render()
    glyphs = {
        "ember": [(16, 8), (15, 10), (17, 10), (14, 12), (18, 12), (13, 14), (19, 14), (14, 17), (18, 17), (15, 19), (17, 19),
                  (16, 21), (16, 14), (16, 16), (15, 15), (17, 15), (16, 24)],
        "frost": [(16, y) for y in range(8, 25)] + [(x, 16) for x in range(9, 24)] + [(12, 12), (20, 12), (12, 20), (20, 20),
                                                                                       (13, 13), (19, 13), (13, 19), (19, 19)],
        "storm": [(19, 7), (18, 9), (17, 11), (16, 13), (15, 15), (19, 15), (18, 17), (17, 19), (16, 21), (15, 23), (14, 25),
                  (16, 15), (17, 15), (18, 15), (14, 15)],
        "venom": [(16, 8), (15, 10), (17, 10), (14, 12), (18, 12), (13, 15), (19, 15), (13, 18), (19, 18), (14, 20), (18, 20),
                  (15, 22), (16, 22), (17, 22), (16, 17)],
        "vampiric": [(12, 9), (13, 11), (14, 13), (15, 15), (16, 17), (20, 9), (19, 11), (18, 13), (17, 15), (16, 19), (16, 21),
                     (12, 8), (20, 8)],
        "gale": [(10, 12), (12, 11), (14, 11), (16, 12), (17, 14), (16, 16), (14, 16), (13, 15), (10, 19), (13, 19), (16, 19),
                 (19, 18), (21, 16), (22, 14), (10, 22), (14, 23), (18, 22)],
    }
    for x, y in glyphs[name]:
        img.putpixel((x, y), hexc(glow))
        for ox, oy in ((1, 0), (0, 1)):
            if img.getpixel((x + ox, y + oy))[3] and img.getpixel((x + ox, y + oy)) != hexc(glow):
                img.putpixel((x + ox, y + oy), mix(img.getpixel((x + ox, y + oy)), hexc(glow), 0.35))
    return img


def vault_key():
    cv = Canvas()
    cv.fill(disc_test(9, 9, 5.8, lambda nx, ny, r: 0.65 - 0.4 * (nx + ny), inner=2.6), R["gold"])
    cv.fill(bar_test([(12.5, 12.5), (26, 26)], lambda t: 1.5, metal_shader(0.6)), R["black_iron"])
    for s in (20.5, 24.2):
        a = along((12.5, 12.5), norm((1, 1)), s - 12.5 * 1.414)
        cv.fill(bar_test([a, (a[0] + 3.2, a[1] - 3.2)], lambda t: 1.0, metal_shader(0.6), False, True), R["black_iron"])
    gem(cv, 9, 4.2, "ruby", 1.2)
    return cv.render()


def target_dummy():
    cv = Canvas()
    cv.fill(bar_test([(16, 30), (16, 17)], lambda t: 1.4, cylinder_shader(0.5)), R["wood"])
    cv.fill(bar_test([(5, 14), (27, 14)], lambda t: 1.3, cylinder_shader(0.5), True, True), R["wood"])
    cv.fill(bar_test([(16, 11), (16, 24)], lambda t: 5.2 - 1.2 * t, lambda t, v, s: 0.55 - 0.3 * v + 0.05 * math.sin(s * 3)), R["straw"])
    cv.fill(disc_test(16, 7.5, 4.6, sphere_shader(0.6)), R["straw"])
    noise_dither(cv, ["straw"], 0.25, 4)
    img = cv.render()
    for x, y in ((14, 7), (18, 7), (13, 6), (15, 8), (17, 6), (19, 8)):
        img.putpixel((x, y), hexc("#3a2412"))
    for x, y in ((14, 16), (15, 15), (16, 15), (17, 15), (18, 16), (18, 17), (17, 18), (16, 18), (15, 18), (14, 17), (16, 16), (16, 17)):
        img.putpixel((x, y), hexc("#b3312a") if (x, y) not in ((16, 16), (16, 17)) else hexc("#f2e6c8"))
    return img


def armor_piece(kind):
    cv = Canvas()
    st = "stormsteel"
    if kind == "helmet":
        cv.fill(lambda x, y: (0.6 - 0.04 * (x - 16) - 0.03 * (y - 12)) if ((x - 16) ** 2 / 110 + (y - 15) ** 2 / 90 <= 1 and y < 23
                                                                       and not (10 < x < 22 and 15 < y < 20)) else None, R[st])
        cv.fill(bar_test([(16, 6), (16, 20)], lambda t: 1.2, metal_shader(0.7)), R["brass"])
    elif kind == "chestplate":
        def chest(x, y):
            if not (5 <= x <= 27 and 6 <= y <= 27):
                return None
            if (x < 9 or x > 23) and y > 15:
                return None
            if 12 <= x <= 20 and y < 9:
                return None
            return 0.58 - 0.03 * (x - 16) - 0.015 * (y - 15)
        cv.fill(chest, R[st])
        cv.fill(bar_test([(16, 9), (16, 26)], lambda t: 1.0, metal_shader(0.7)), R["brass"])
    elif kind == "leggings":
        def legs(x, y):
            if not (8 <= x <= 24 and 6 <= y <= 28):
                return None
            if 14 <= x <= 18 and y > 12:
                return None
            return 0.55 - 0.03 * (x - 16)
        cv.fill(legs, R[st])
        cv.fill(bar_test([(8.5, 7.5), (23.5, 7.5)], lambda t: 1.1, metal_shader(0.7)), R["brass"])
    else:
        for x0 in (6, 18):
            cv.fill(lambda x, y, x0=x0: (0.6 - 0.04 * (x - x0 - 4)) if (x0 <= x <= x0 + 8 and 12 <= y <= 26 and
                                                                         not (x > x0 + 4 and y < 19)) else None, R[st])
            cv.fill(bar_test([(x0 + 0.5, 25), (x0 + 8.5, 25)], lambda t: 1.0, metal_shader(0.6)), R["brass"])
    img = cv.render()
    for x, y in ((12, 10), (20, 10), (16, 14)):
        if img.getpixel((x, y))[3]:
            img.putpixel((x, y), hexc("#bff3ff"))
    return img


def explorers_compass():
    cv = Canvas()
    cv.fill(disc_test(16, 16, 12.5, sphere_shader(0.55)), R["gold"])
    cv.fill(disc_test(16, 16, 9.8, lambda nx, ny, r: 0.25 + 0.15 * (1 - r)), R["obsidian"])
    for k in range(8):
        a = k * math.pi / 4
        cv.dot(int(16 + math.cos(a) * 8.2), int(16 + math.sin(a) * 8.2), R["gold"].c[5])
    cv.fill(poly_test([(16, 7), (18.2, 16), (16, 17.5), (13.8, 16)], lambda x, y: 0.75 - 0.05 * (x - 16)), R["ruby"])
    cv.fill(poly_test([(16, 25), (18.2, 16), (16, 14.5), (13.8, 16)], lambda x, y: 0.7 - 0.05 * (x - 16)), R["silver"])
    cv.fill(disc_test(16, 16, 1.6, sphere_shader(0.6)), R["amethyst"])
    img = cv.render()
    for x, y in ((9, 8), (23, 10), (24, 23)):
        img.putpixel((x, y), hexc("#fff6c2"))
    return img


LEGEND_GLOW = {"tempest_edge": "lightning", "rimecleaver": "frost", "voidreaver": "void", "earthshaker": "ember",
               "bloodfang": "blood", "skypiercer": "sky", "moonveil": "moon", "kingsbane": "royal"}


def warlord_recolor(img):
    """Stormsteel's blue plate becomes blackened steel, its gold trim glowing ember."""
    import colorsys
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if 0.45 < h < 0.75 and s > 0.2:
                if h < 0.53 and l > 0.68:
                    nr, ng, nb = colorsys.hls_to_rgb(0.07, 0.6, 1.0)  # cyan glow -> molten orange
                else:
                    nr, ng, nb = colorsys.hls_to_rgb(0.97, 0.1 + l * 0.48, 0.1)  # plate -> blackened steel
            elif 0.08 < h < 0.2 and s > 0.3:
                nr, ng, nb = colorsys.hls_to_rgb(0.04, min(0.7, l * 0.9), 0.95)  # gold -> ember
            else:
                nr, ng, nb = colorsys.hls_to_rgb(h, l * 0.7, s * 0.5)
            out.putpixel((x, y), (int(nr * 255), int(ng * 255), int(nb * 255), a))
    return out


def main():
    from icon_anim import save_animated
    for kind in WEAPONS:
        for i, tier in enumerate(TIERS):
            inlay = INLAY_COLOR.get(tier) if kind in INLAY_PATH else None
            save_animated(tier_icon(kind, tier), tier, "item", f"{tier}_{kind}.png", seed=i * 7 + len(kind),
                          inlay=hexc(inlay) if inlay else None)
    for name, fn in LEGENDARIES.items():
        save_animated(fn(), LEGEND_GLOW[name], "item", f"{name}.png", seed=len(name))
    save(chakram(), "item", "chakram.png")
    save(throwing_knives(), "item", "throwing_knife.png")
    save_animated(ingot(), "stormsteel", "item", "stormsteel_ingot.png", seed=3)
    save(raw_chunk(), "item", "raw_stormsteel.png")
    for name in RUNE_GLYPHS:
        save(rune(name), "item", f"{name}_rune.png")
    save(vault_key(), "item", "vault_key.png")
    save(explorers_compass(), "item", "explorers_compass.png")
    save(target_dummy(), "item", "target_dummy.png")
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        save_animated(armor_piece(piece), "stormsteel", "item", f"stormsteel_{piece}.png", seed=len(piece))
        save_animated(warlord_recolor(armor_piece(piece)), "ember", "item", f"warlord_{piece}.png", seed=len(piece) + 3)
    print("Icons written")


if __name__ == "__main__":
    main()
