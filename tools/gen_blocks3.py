"""Block models and textures for the interactive blocks (whetstone, rune forge, arena gong,
champion's coffer, bandit vault, sarcophagus, legendary pedestal, storm altar, signal brazier).

Textures are 16x16 pixel art painted procedurally; models are JSON element models written to
src/main/resources/assets/steelstorm/models/block. Glowing parts use NeoForge's per-element
light override, so they stay bright in the dark.

Run: python3 tools/gen_blocks3.py
"""
import json
import math
import os
import random

from PIL import Image

from texlib import ROOT, clamp, hexc, mix, mul, save

MODELS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "steelstorm", "models")


# ----------------------------------------------------------------------------- painting helpers

def palette(*hexes):
    return [hexc(h) for h in hexes]


def shade(pal, t):
    """Pick from a dark-to-light palette by brightness 0..1."""
    t = clamp(t)
    return pal[min(len(pal) - 1, int(t * len(pal)))]


def new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def noise_grid(seed, size=16, scale=4):
    rng = random.Random(seed)
    g = [[rng.random() for _ in range(size // scale + 2)] for _ in range(size // scale + 2)]

    def at(x, y):
        fx, fy = x / scale, y / scale
        x0, y0 = int(fx), int(fy)
        tx, ty = fx - x0, fy - y0
        a = g[y0][x0] * (1 - tx) + g[y0][x0 + 1] * tx
        b = g[y0 + 1][x0] * (1 - tx) + g[y0 + 1][x0 + 1] * tx
        return a * (1 - ty) + b * ty

    return at


def stone(pal, seed, rough=0.35, base=0.55):
    img = new()
    n1 = noise_grid(seed, scale=4)
    n2 = noise_grid(seed + 1, scale=2)
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            v = base + (n1(x, y) - 0.5) * rough + (n2(x, y) - 0.5) * rough * 0.6 + (rng.random() - 0.5) * 0.08
            img.putpixel((x, y), shade(pal, v))
    return img


def bricks(pal, mortar, seed, rows=4, base=0.55):
    """Offset brick courses with a lit top edge and a shadowed bottom edge on each brick."""
    img = new()
    rng = random.Random(seed)
    h = 16 // rows
    n = noise_grid(seed, scale=3)
    for y in range(16):
        row = y // h
        yy = y % h
        offset = (row % 2) * 4
        for x in range(16):
            xx = (x + offset) % 8
            if yy == h - 1 or xx == 7:
                img.putpixel((x, y), mortar)
                continue
            v = base + (n(x, y) - 0.5) * 0.3 + (rng.random() - 0.5) * 0.08
            if yy == 0:
                v += 0.15
            elif yy == h - 2:
                v -= 0.12
            if xx == 0:
                v += 0.06
            img.putpixel((x, y), shade(pal, v))
    return img


def planks(pal, seed, vertical=False, boards=4):
    img = new()
    rng = random.Random(seed)
    w = 16 // boards
    for y in range(16):
        for x in range(16):
            a, b = (x, y) if vertical else (y, x)
            board = a // w
            within = a % w
            grain = math.sin((b + board * 5) * 0.9 + rng.random() * 0.3) * 0.08
            v = 0.55 + grain + (board % 2) * 0.05
            if within == 0:
                v += 0.12
            if within == w - 1:
                v = 0.12
            img.putpixel((x, y), shade(pal, v))
    return img


def metal(pal, seed, base=0.55, brushed=True):
    img = new()
    rng = random.Random(seed)
    for y in range(16):
        streak = rng.random() * 0.1 if brushed else 0
        for x in range(16):
            v = base + streak + (rng.random() - 0.5) * 0.06 - y * 0.012
            img.putpixel((x, y), shade(pal, v))
    return img


def border(img, light, dark, width=1):
    """Bevelled frame: light top/left edges, dark bottom/right edges."""
    for i in range(width):
        for k in range(16):
            img.putpixel((k, i), light)
            img.putpixel((i, k), light)
            img.putpixel((k, 15 - i), dark)
            img.putpixel((15 - i, k), dark)
    return img


def rivets(img, points, light, dark):
    for (x, y) in points:
        img.putpixel((x, y), light)
        if x + 1 < 16 and y + 1 < 16:
            img.putpixel((x + 1, y + 1), dark)


def glyphs(color, seed, density=1.0, transparent=True, bg=None):
    """Carved rune strokes (for glowing overlays): a few small angular symbols."""
    img = new() if transparent else bg.copy()
    rng = random.Random(seed)
    shapes = [
        [(0, 0), (0, 4), (2, 2), (4, 4), (4, 0)],
        [(2, 0), (2, 4), (0, 2), (4, 2)],
        [(0, 0), (4, 0), (2, 4), (0, 0)],
        [(0, 4), (2, 0), (4, 4), (1, 2), (3, 2)],
        [(0, 0), (4, 4), (0, 4), (4, 0)],
        [(2, 0), (0, 2), (2, 4), (4, 2), (2, 0)],
    ]
    slots = [(1, 2), (6, 2), (11, 2), (3, 9), (9, 9)]
    for (ox, oy) in slots:
        if rng.random() > density:
            continue
        pts = rng.choice(shapes)
        for i in range(len(pts) - 1):
            line(img, ox + pts[i][0], oy + pts[i][1], ox + pts[i + 1][0], oy + pts[i + 1][1], color)
    return img


def line(img, x0, y0, x1, y1, color):
    steps = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(steps + 1):
        x = round(x0 + (x1 - x0) * i / steps)
        y = round(y0 + (y1 - y0) * i / steps)
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), color)


def glow_pixels(img, color, alpha_light=True):
    """Soft halo around existing opaque pixels (one pixel wide, half-transparent)."""
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3] > 200:
                        out.putpixel((x, y), (color[0], color[1], color[2], 110))
                        break
    return out


def overlay(base, top):
    out = base.copy()
    out.alpha_composite(top)
    return out


# ----------------------------------------------------------------------------- palettes

BLACKSTONE = palette("#141018", "#1e1a24", "#2a2532", "#363040", "#433b4f", "#524861", "#64597a")
DARK_STONE = palette("#1f2126", "#2b2e35", "#383c44", "#464b54", "#555b65", "#666d78", "#7b828d")
CRYPT = palette("#22252b", "#2f333b", "#3c414b", "#4a505b", "#59606c", "#69717e", "#7c8492")
MARBLE = palette("#8f8a80", "#a8a397", "#bdb8ac", "#cfcabf", "#dedad0", "#ebe8e0", "#f7f5f0")
SANDSTONE = palette("#5a5246", "#6f6657", "#857a68", "#998e7a", "#ada18c", "#c0b49e", "#d3c8b3")
GRIND = palette("#4b4a47", "#5d5b57", "#6f6d68", "#817f79", "#94918b", "#a8a59e", "#bcb9b2")
WOOD = palette("#2a1709", "#3a2210", "#4d2f17", "#61401f", "#755029", "#8a6134", "#a07541")
DARK_WOOD = palette("#140b06", "#1f120a", "#2b1a0f", "#382415", "#462e1b", "#553a23", "#66472c")
RED_WOOD = palette("#2a0b0b", "#3d1111", "#521919", "#672222", "#7d2b2b", "#933838", "#a94848")
IRON = palette("#2a2e33", "#3d434a", "#525a63", "#69727d", "#818b97", "#9ba5b1", "#b8c1cb")
DARK_IRON = palette("#121417", "#1b1e22", "#25292e", "#30353b", "#3c424a", "#4a5159", "#5b636d")
GOLD = palette("#5a3606", "#7a4c09", "#9d650e", "#c2851a", "#dca826", "#f0c845", "#ffe98c")
BRONZE = palette("#3a210c", "#53311a", "#6e4423", "#8a592e", "#a8703b", "#c48a4c", "#dca866")
STORM_STONE = palette("#14192b", "#1c2339", "#252e49", "#2f3a5a", "#3a476c", "#475680", "#556697")
STORMSTEEL = palette("#1c326e", "#2c4fa1", "#3a62b8", "#4474cf", "#5a8ae0", "#6c9ff0", "#a6cbff")
COAL = palette("#0b0b0c", "#141415", "#1c1c1e", "#252528", "#2f2f33", "#3a3a3f", "#47474d")


# ----------------------------------------------------------------------------- textures

def textures():
    t = {}
    # Whetstone
    wheel = stone(GRIND, 31, rough=0.25, base=0.55)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if abs(r - 3.5) < 0.5 or abs(r - 6.0) < 0.5:
                wheel.putpixel((x, y), mul(wheel.getpixel((x, y)), 0.82))
            if r < 1.5:
                wheel.putpixel((x, y), IRON[3] if r < 1 else IRON[1])
    t["whetstone_wheel"] = wheel
    rim = stone(GRIND, 32, rough=0.2, base=0.5)
    for y in range(16):
        for x in range(0, 16, 3):
            rim.putpixel((x, y), mul(rim.getpixel((x, y)), 0.8))
    t["whetstone_rim"] = rim
    t["whetstone_wood"] = border(planks(WOOD, 33, vertical=True), WOOD[6], WOOD[0])
    t["whetstone_iron"] = metal(IRON, 34)

    # Rune forge
    forge_side = bricks(BLACKSTONE, BLACKSTONE[0], 41, rows=4, base=0.45)
    t["rune_forge_side"] = forge_side
    top = stone(BLACKSTONE, 42, rough=0.25, base=0.5)
    border(top, BLACKSTONE[6], BLACKSTONE[0])
    for i in range(4, 12):
        top.putpixel((i, 4), hexc("#9d6bff"))
        top.putpixel((i, 11), hexc("#9d6bff"))
        top.putpixel((4, i), hexc("#9d6bff"))
        top.putpixel((11, i), hexc("#9d6bff"))
    t["rune_forge_top"] = top
    t["rune_forge_base"] = border(stone(BLACKSTONE, 43, rough=0.3, base=0.4), BLACKSTONE[4], BLACKSTONE[0])
    t["rune_forge_glyphs"] = glyphs(hexc("#d2b4ff"), 44)
    t["rune_forge_crystal"] = crystal(palette("#3a1366", "#5b21a8", "#7c3aed", "#9d6bff", "#bf9bff", "#dcc6ff", "#ffffff"), 45)

    # Arena gong
    disc = new()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            v = 0.72 - r * 0.03 + 0.14 * math.cos(r * 1.6) - (x - y) * 0.012
            if r < 2.2:
                v = 0.95 - r * 0.15
            disc.putpixel((x, y), shade(GOLD if r < 2.2 else BRONZE, v))
    for a in range(0, 360, 45):
        x = round(7.5 + math.cos(math.radians(a)) * 5)
        y = round(7.5 + math.sin(math.radians(a)) * 5)
        disc.putpixel((x, y), GOLD[6])
    t["gong_disc"] = disc
    t["gong_edge"] = metal(BRONZE, 51, base=0.4)
    t["gong_wood"] = border(planks(RED_WOOD, 52, vertical=True, boards=2), RED_WOOD[5], RED_WOOD[0])
    t["gong_gold"] = metal(GOLD, 53, base=0.6)
    t["gong_rope"] = rope()

    # Champion's coffer
    coffer = planks(RED_WOOD, 61, boards=4)
    band(coffer, GOLD, [0, 15])
    band(coffer, GOLD, [], cols=[0, 15])
    rivets(coffer, [(2, 2), (13, 2), (2, 13), (13, 13)], GOLD[6], GOLD[1])
    t["coffer_side"] = coffer
    lid = planks(RED_WOOD, 62, boards=4, vertical=True)
    band(lid, GOLD, [0, 15, 7, 8])
    t["coffer_top"] = lid
    front = coffer.copy()
    t["coffer_front"] = front
    t["coffer_gold"] = metal(GOLD, 63, base=0.65)
    t["coffer_chain"] = chain()
    lock = metal(GOLD, 64, base=0.7)
    for y in range(6, 11):
        lock.putpixel((7, y), GOLD[0])
        lock.putpixel((8, y), GOLD[0])
    lock.putpixel((7, 5), GOLD[0])
    lock.putpixel((8, 5), GOLD[0])
    t["coffer_lock"] = lock

    # Bandit vault
    vault = planks(WOOD, 71, boards=4)
    band(vault, DARK_IRON, [1, 2, 13, 14])
    rivets(vault, [(1, 4), (14, 4), (1, 10), (14, 10)], IRON[6], IRON[0])
    t["vault_side"] = vault
    vtop = planks(WOOD, 72, boards=4, vertical=True)
    band(vtop, DARK_IRON, [], cols=[1, 2, 13, 14])
    t["vault_top"] = vtop
    t["vault_iron"] = metal(DARK_IRON, 73, base=0.6)
    plock = metal(IRON, 74, base=0.55)
    plock.putpixel((7, 8), IRON[0])
    plock.putpixel((8, 8), IRON[0])
    plock.putpixel((7, 9), IRON[0])
    t["vault_lock"] = plock

    # Sarcophagus
    side = bricks(CRYPT, CRYPT[0], 81, rows=2, base=0.5)
    t["sarcophagus_side"] = side
    lidtex = stone(CRYPT, 82, rough=0.3, base=0.55)
    border(lidtex, CRYPT[6], CRYPT[0])
    t["sarcophagus_lid"] = lidtex
    relief = stone(CRYPT, 83, rough=0.25, base=0.6)
    for y in range(2, 15):
        relief.putpixel((7, y), CRYPT[6])
        relief.putpixel((8, y), CRYPT[1])
    for x in range(4, 12):
        relief.putpixel((x, 5), CRYPT[6])
        relief.putpixel((x, 6), CRYPT[1])
    t["sarcophagus_relief"] = relief
    inside = stone(palette("#050507", "#0a0a0d", "#0f1013", "#15161a", "#1b1c21", "#222329", "#2a2b32"), 84, base=0.4)
    t["sarcophagus_inside"] = inside
    t["sarcophagus_glyphs"] = glyphs(hexc("#a9c4ff"), 85, density=0.7)

    # Legendary pedestal
    mside = new()
    for y in range(16):
        for x in range(16):
            flute = 0.5 + 0.25 * math.cos(x * math.pi / 2)
            mside.putpixel((x, y), shade(MARBLE, flute - y * 0.01))
    t["pedestal_column"] = mside
    mtop = stone(MARBLE, 91, rough=0.18, base=0.65)
    border(mtop, GOLD[5], GOLD[1])
    for i in range(16):
        if i % 4 == 1:
            mtop.putpixel((i, 1), GOLD[6])
    t["pedestal_top"] = mtop
    mbase = stone(MARBLE, 92, rough=0.2, base=0.58)
    for x in range(16):
        mbase.putpixel((x, 0), GOLD[5])
        mbase.putpixel((x, 15), GOLD[2])
    t["pedestal_base"] = mbase
    t["pedestal_gem"] = crystal(palette("#5a3606", "#9d650e", "#dca826", "#f0c845", "#ffe98c", "#fff4c2", "#ffffff"), 93)

    # Storm altar
    t["altar_side"] = overlay(bricks(STORM_STONE, STORM_STONE[0], 101, rows=4, base=0.5), bolt_glyph(hexc("#3a5f9a")))
    atop = stone(STORM_STONE, 102, rough=0.25, base=0.55)
    border(atop, STORMSTEEL[5], STORMSTEEL[0])
    t["altar_top"] = atop
    t["altar_base"] = border(stone(STORM_STONE, 103, rough=0.3, base=0.42), STORM_STONE[5], STORM_STONE[0])
    t["altar_socket"] = metal(DARK_IRON, 104, base=0.35)
    t["altar_crystal"] = crystal(palette("#0a1a3a", "#12357a", "#1f5bc4", "#3f8bff", "#7fbfff", "#c4e6ff", "#ffffff"), 105)
    t["altar_glyphs"] = bolt_glyph(hexc("#c4f0ff"))

    # Signal brazier
    t["brazier_iron"] = metal(DARK_IRON, 111, base=0.6)
    bowl = metal(IRON, 112, base=0.45)
    for x in range(16):
        bowl.putpixel((x, 0), IRON[6])
        bowl.putpixel((x, 1), IRON[4])
    rivets(bowl, [(2, 6), (7, 6), (12, 6)], IRON[6], IRON[0])
    t["brazier_bowl"] = bowl
    coal = stone(COAL, 113, rough=0.4, base=0.45)
    t["brazier_coal"] = coal
    embers = coal.copy()
    rng = random.Random(114)
    for _ in range(40):
        x, y = rng.randrange(16), rng.randrange(16)
        embers.putpixel((x, y), rng.choice([hexc("#ff7a1a"), hexc("#ffb03a"), hexc("#ff4a0a"), hexc("#ffd36a")]))
    t["brazier_embers"] = embers
    return t


def crystal(pal, seed):
    img = new()
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            facet = ((x + y) // 4) % 2
            v = 0.45 + facet * 0.2 + (1 - y / 16) * 0.25 + (rng.random() - 0.5) * 0.08
            if (x + y) % 7 == 0:
                v += 0.25
            img.putpixel((x, y), shade(pal, v))
    return img


def bolt_glyph(color):
    img = new()
    pts = [(9, 1), (5, 8), (8, 8), (6, 14), (11, 6), (8, 6), (10, 1)]
    for i in range(len(pts) - 1):
        line(img, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], color)
    return img


def band(img, pal, rows, cols=()):
    for y in rows:
        for x in range(16):
            img.putpixel((x, y), shade(pal, 0.75 if y % 2 == 0 else 0.45))
    for x in cols:
        for y in range(16):
            img.putpixel((x, y), shade(pal, 0.75 if x % 2 == 0 else 0.45))


def chain():
    img = new()
    for y in range(16):
        link = y % 4
        for x in range(4, 12):
            if link in (0, 3):
                if x in (6, 7, 8, 9):
                    img.putpixel((x, y), IRON[5] if link == 0 else IRON[2])
            else:
                if x in (5, 10):
                    img.putpixel((x, y), IRON[4])
                elif x in (6, 9):
                    img.putpixel((x, y), IRON[1])
    return img


def rope():
    img = new()
    for y in range(16):
        for x in range(16):
            v = 0.5 + 0.2 * math.sin((x + y) * 1.6)
            img.putpixel((x, y), shade(palette("#3a2a14", "#5a4220", "#7a5c2e", "#987640", "#b39257", "#cdb072", "#e2cb92"), v))
    return img


# ----------------------------------------------------------------------------- model builder

def auto_uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    if face == "north":
        u = (16 - x1, 16 - x0); v = (16 - y1, 16 - y0)
    elif face == "south":
        u = (x0, x1); v = (16 - y1, 16 - y0)
    elif face == "east":
        u = (16 - z1, 16 - z0); v = (16 - y1, 16 - y0)
    elif face == "west":
        u = (z0, z1); v = (16 - y1, 16 - y0)
    elif face == "up":
        u = (x0, x1); v = (z0, z1)
    else:
        u = (x0, x1); v = (16 - z1, 16 - z0)
    fu, fv = fit(u), fit(v)
    return [fu[0], fv[0], fu[1], fv[1]]


def fit(r):
    """Keep a UV span inside 0..16: shift it in, or squash it if it's longer than 16."""
    a, b = r
    span = b - a
    if span > 16:
        return (0, 16)
    if a < 0:
        a, b = a + 16 * math.ceil(-a / 16), b + 16 * math.ceil(-a / 16)
    if b > 16:
        shift = b - 16
        a, b = a - shift, b - shift
    return (round(max(0, a), 3), round(min(16, b), 3))


def box(f, t, tex, faces=("north", "south", "east", "west", "up", "down"), rot=None, glow=False, shade=True, uv=None,
        tex_by_face=None, cull=None):
    el = {"from": list(f), "to": list(t), "faces": {}}
    for face in faces:
        entry = {"uv": (uv or {}).get(face, auto_uv(face, f, t)),
                 "texture": "#" + ((tex_by_face or {}).get(face, tex))}
        if cull and face in cull:
            entry["cullface"] = face
        el["faces"][face] = entry
    if rot:
        axis, angle, origin = rot
        el["rotation"] = {"angle": angle, "axis": axis, "origin": list(origin)}
    if glow:
        el["neoforge_data"] = {"block_light": 15, "sky_light": 15}
        el["shade"] = False
    if not shade:
        el["shade"] = False
    return el


def disc(cx, cy, z0, z1, r, tex, glow=False, edge_tex=None):
    """A round plate in the XY plane: a plus shape plus a 45-degree square."""
    w = r * 0.62
    els = [
        box((cx - r, cy - w, z0), (cx + r, cy + w, z1), tex, tex_by_face=edge_side(edge_tex, "x")),
        box((cx - w, cy - r, z0), (cx + w, cy + r, z1), tex, tex_by_face=edge_side(edge_tex, "y")),
    ]
    s = r * 0.66
    els.append(box((cx - s, cy - s, z0 + 0.01), (cx + s, cy + s, z1 - 0.01), tex, rot=("z", 45, (cx, cy, (z0 + z1) / 2)),
                   tex_by_face=edge_side(edge_tex, "r")))
    return els


def edge_side(edge_tex, _kind):
    if not edge_tex:
        return None
    return {"east": edge_tex, "west": edge_tex, "up": edge_tex, "down": edge_tex}


def model(name, textures, elements, parent="block/block", render_type="minecraft:cutout", extra=None):
    m = {"parent": parent, "render_type": render_type, "textures": textures, "elements": elements}
    if extra:
        m.update(extra)
    for el in elements:
        for c in el["from"] + el["to"]:
            assert -16 <= c <= 32, (name, el)
    path = os.path.join(MODELS, "block", name + ".json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(m, fh, indent=2)


def tx(**names):
    out = {k: "steelstorm:block/" + v for k, v in names.items()}
    return out


# ----------------------------------------------------------------------------- models

def models():
    # Whetstone: a grinding wheel on a wooden frame, facing north.
    wheel = []
    wheel += disc(8, 10, 7, 9, 5.5, "wheel", edge_tex="rim")
    model("whetstone", tx(particle="whetstone_wheel", wheel="whetstone_wheel", rim="whetstone_rim", wood="whetstone_wood",
                          iron="whetstone_iron"), [
        box((1, 0, 3), (15, 3, 13), "wood"),
        box((2, 3, 4), (4, 11, 12), "wood"),
        box((12, 3, 4), (14, 11, 12), "wood"),
        box((4, 9.5, 7.5), (12, 10.5, 8.5), "iron"),
        box((1, 9.5, 7.5), (2, 10.5, 8.5), "iron"),
        box((0, 9.5, 7.5), (1, 13.5, 8.5), "iron"),
        box((-1.5, 12.5, 7.5), (0, 13.5, 8.5), "wood"),
        *wheel,
    ])

    # Rune forge: blackstone altar with glowing runes and amethyst crystals.
    crystals = []
    for (x, z) in ((2.5, 2.5), (12, 2.5), (2.5, 12), (12, 12)):
        crystals.append(box((x, 13, z), (x + 1.5, 16.5, z + 1.5), "crystal", rot=("y", 45, (x + 0.75, 13, z + 0.75)), glow=True))
    model("rune_forge", tx(particle="rune_forge_side", side="rune_forge_side", top="rune_forge_top", base="rune_forge_base",
                           glyphs="rune_forge_glyphs", crystal="rune_forge_crystal"), [
        box((0, 0, 0), (16, 4, 16), "base", cull=("down",)),
        box((2, 4, 2), (14, 10, 14), "side"),
        box((1.95, 4.5, 1.95), (14.05, 9.5, 14.05), "glyphs", faces=("north", "south", "east", "west"), glow=True),
        box((1, 10, 1), (15, 13, 15), "base", tex_by_face={"up": "top"}),
        *crystals,
    ])

    # Arena gong: a bronze disc hanging in a red lacquered frame, 1.5 blocks tall.
    model("arena_gong", tx(particle="gong_disc", disc="gong_disc", edge="gong_edge", wood="gong_wood", gold="gong_gold",
                           rope="gong_rope"), [
        box((-1, 0, 4), (3, 2, 12), "wood"),
        box((13, 0, 4), (17, 2, 12), "wood"),
        box((0, 2, 6), (2, 24, 10), "wood"),
        box((14, 2, 6), (16, 24, 10), "wood"),
        box((-1.5, 22, 5.5), (17.5, 25, 10.5), "wood"),
        box((-2, 25, 5.5), (0, 27, 10.5), "gold", rot=("z", 22.5, (-1, 25, 8))),
        box((16, 25, 5.5), (18, 27, 10.5), "gold", rot=("z", -22.5, (17, 25, 8))),
        box((-0.2, 18, 5.8), (2.2, 19, 10.2), "gold"),
        box((13.8, 18, 5.8), (16.2, 19, 10.2), "gold"),
        box((5, 18, 7.75), (5.5, 22, 8.25), "rope"),
        box((10.5, 18, 7.75), (11, 22, 8.25), "rope"),
        *disc(8, 12, 7.5, 8.5, 6.0, "disc", edge_tex="edge"),
        box((6.5, 10.5, 7.2), (9.5, 13.5, 8.8), "gold"),
    ])

    # Champion's coffer: red lacquered chest with gold fittings; the locked one is wrapped in chains.
    coffer_els = [
        box((1, 0, 2), (15, 10, 14), "side", tex_by_face={"up": "top"}),
        box((0.8, 10, 1.8), (15.2, 13, 14.2), "side", tex_by_face={"up": "top", "down": "top"}),
        box((1.5, 13, 2.5), (14.5, 14.5, 13.5), "top"),
        box((0.5, 0, 1.5), (2, 13.5, 3), "gold"),
        box((14, 0, 1.5), (15.5, 13.5, 3), "gold"),
        box((0.5, 0, 13), (2, 13.5, 14.5), "gold"),
        box((14, 0, 13), (15.5, 13.5, 14.5), "gold"),
        box((6.5, 6.5, 1.3), (9.5, 11, 2), "lock"),
    ]
    tex_coffer = tx(particle="coffer_side", side="coffer_side", top="coffer_top", gold="coffer_gold", lock="coffer_lock",
                    chain="coffer_chain")
    model("champions_coffer", tex_coffer, coffer_els)
    chains = [
        box((-1, 6.5, 1.0), (17, 8, 1.6), "chain", rot=("z", 22.5, (8, 7.25, 1.3)), faces=("north", "south", "up", "down")),
        box((-1, 6.5, 1.0), (17, 8, 1.6), "chain", rot=("z", -22.5, (8, 7.25, 1.3)), faces=("north", "south", "up", "down")),
        box((0.6, 7, 1.2), (1.2, 8, 14.8), "chain", faces=("west", "east", "up")),
        box((14.8, 7, 1.2), (15.4, 8, 14.8), "chain", faces=("west", "east", "up")),
        box((5.5, 2.5, 0.6), (10.5, 7.5, 1.4), "lock"),
        box((6.5, 7.5, 0.8), (7.5, 9.5, 1.2), "gold"),
        box((8.5, 7.5, 0.8), (9.5, 9.5, 1.2), "gold"),
        box((6.5, 9.5, 0.8), (9.5, 10.3, 1.2), "gold"),
    ]
    model("champions_coffer_locked", tex_coffer, coffer_els + chains)

    # Bandit vault: iron-banded strongbox; locked ones have a heavy padlock.
    vault_els = [
        box((2, 0, 3), (14, 9, 13), "side", tex_by_face={"up": "top", "down": "top"}),
        box((1.8, 9, 2.8), (14.2, 12, 13.2), "side", tex_by_face={"up": "top", "down": "top"}),
        box((1.5, 0, 2.5), (2.5, 12.5, 3.5), "iron"),
        box((13.5, 0, 2.5), (14.5, 12.5, 3.5), "iron"),
        box((1.5, 0, 12.5), (2.5, 12.5, 13.5), "iron"),
        box((13.5, 0, 12.5), (14.5, 12.5, 13.5), "iron"),
        box((7, 8, 2.3), (9, 10, 2.8), "iron"),
    ]
    tex_vault = tx(particle="vault_side", side="vault_side", top="vault_top", iron="vault_iron", lock="vault_lock")
    model("bandit_vault", tex_vault, vault_els)
    model("bandit_vault_locked", tex_vault, vault_els + [
        box((6, 3.5, 1.5), (10, 7.5, 2.5), "lock"),
        box((6.6, 7.5, 1.8), (7.4, 9.5, 2.2), "iron"),
        box((8.6, 7.5, 1.8), (9.4, 9.5, 2.2), "iron"),
        box((6.6, 9.3, 1.8), (9.4, 10.1, 2.2), "iron"),
    ])

    # Sarcophagus: head and foot halves, closed and open (lid shoved aside).
    tex_s = tx(particle="sarcophagus_side", side="sarcophagus_side", lid="sarcophagus_lid", relief="sarcophagus_relief",
               inside="sarcophagus_inside", glyphs="sarcophagus_glyphs")
    for part in ("head", "foot"):
        end = "north" if part == "head" else "south"
        base_faces = ("north", "south", "east", "west", "up", "down")
        base_faces = tuple(f for f in base_faces if f != ("south" if part == "head" else "north"))
        closed_lid = box((0.5, 9, 0), (15.5, 12, 16), "lid",
                         faces=tuple(f for f in ("north", "south", "east", "west", "up", "down")
                                     if f != ("south" if part == "head" else "north")),
                         tex_by_face={"up": "relief"} if part == "head" else None)
        glyph_el = box((0.95, 2, 0.5 if part == "head" else 0), (15.05, 8, 16 if part == "head" else 15.5), "glyphs",
                       faces=("east", "west"), glow=True)
        base = box((1, 0, 0), (15, 9, 16), "side", faces=base_faces, tex_by_face={"up": "inside", "down": "lid"})
        extra_closed = []
        if part == "head":
            extra_closed.append(box((5, 12, 2), (11, 13.5, 7), "relief"))
        model(f"sarcophagus_{part}", tex_s, [base, glyph_el, closed_lid] + extra_closed)
        # Slid six pixels to the side and tipped over the edge.
        open_lid = box((6.5, 9, 0), (21.5, 12, 16), "lid", rot=("z", -22.5, (6.5, 9, 8)),
                       tex_by_face={"up": "relief"} if part == "head" else None)
        model(f"sarcophagus_{part}_open", tex_s, [base, glyph_el, open_lid])

    # Legendary pedestal: fluted marble column with a gold-rimmed top and four glowing gems.
    gems = [box((x, 14, z), (x + 1, 15, z + 1), "gem", glow=True) for (x, z) in ((2.5, 2.5), (12.5, 2.5), (2.5, 12.5), (12.5, 12.5))]
    model("legendary_pedestal", tx(particle="pedestal_top", column="pedestal_column", top="pedestal_top", base="pedestal_base",
                                   gem="pedestal_gem"), [
        box((1, 0, 1), (15, 3, 15), "base", tex_by_face={"up": "top"}, cull=("down",)),
        box((4, 3, 4), (12, 11, 12), "column"),
        box((2, 11, 2), (14, 14, 14), "base", tex_by_face={"up": "top"}),
        *gems,
    ])

    # Storm altar: four sockets; each charge lights one crystal.
    sockets = [(2, 2), (11, 2), (2, 11), (11, 11)]
    for charge in range(5):
        els = [
            box((0, 0, 0), (16, 4, 16), "base", cull=("down",)),
            box((2, 4, 2), (14, 10, 14), "side"),
            box((1.95, 4.5, 1.95), (14.05, 9.5, 14.05), "glyphs", faces=("north", "south", "east", "west"), glow=charge > 0),
            box((1, 10, 1), (15, 12, 15), "base", tex_by_face={"up": "top"}),
        ]
        for i, (x, z) in enumerate(sockets):
            els.append(box((x, 12, z), (x + 3, 13, z + 3), "socket"))
            if i < charge:
                els.append(box((x + 0.75, 13, z + 0.75), (x + 2.25, 16, z + 2.25), "crystal",
                               rot=("y", 45, (x + 1.5, 13, z + 1.5)), glow=True))
        model(f"storm_altar_{charge}", tx(particle="altar_side", side="altar_side", top="altar_top", base="altar_base",
                                          socket="altar_socket", crystal="altar_crystal", glyphs="altar_glyphs"), els)

    # Signal brazier: an iron bowl of coals on a stand; lit, with fire and glowing embers.
    for lit in (False, True):
        els = [
            box((3, 0, 3), (13, 2, 13), "iron", cull=("down",)),
            box((6.5, 2, 6.5), (9.5, 9, 9.5), "iron"),
            box((2, 9, 2), (14, 11, 14), "bowl", tex_by_face={"up": "coal"}),
            box((2, 11, 2), (14, 15, 3), "bowl"),
            box((2, 11, 13), (14, 15, 14), "bowl"),
            box((2, 11, 3), (3, 15, 13), "bowl"),
            box((13, 11, 3), (14, 15, 13), "bowl"),
            box((3, 11, 3), (13, 13, 13), "embers" if lit else "coal", faces=("up",), glow=lit),
        ]
        textures_b = tx(particle="brazier_bowl", iron="brazier_iron", bowl="brazier_bowl", coal="brazier_coal",
                        embers="brazier_embers")
        if lit:
            textures_b["fire"] = "minecraft:block/fire_0"
            els.append(box((3, 13, 8), (13, 24, 8), "fire", faces=("north", "south"), glow=True,
                           uv={"north": [0, 0, 16, 16], "south": [0, 0, 16, 16]}))
            els.append(box((8, 13, 3), (8, 24, 13), "fire", faces=("east", "west"), glow=True,
                           uv={"east": [0, 0, 16, 16], "west": [0, 0, 16, 16]}))
        model("signal_brazier_lit" if lit else "signal_brazier", textures_b, els)

    # A whole sarcophagus for the item icon.
    whole = []
    for part, dz in (("head", -8), ("foot", 8)):
        whole.append(box((1, 0, dz), (15, 9, dz + 16), "side", tex_by_face={"up": "inside"}))
        whole.append(box((0.5, 9, dz), (15.5, 12, dz + 16), "lid", tex_by_face={"up": "relief"} if part == "head" else None))
    m = {"parent": "block/block", "render_type": "minecraft:cutout", "textures": tex_s, "elements": whole,
         "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.42, 0.42, 0.42]},
                     "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.2, 0.2, 0.2]},
                     "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
                     "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
                     "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}}
    with open(os.path.join(MODELS, "block", "sarcophagus_item.json"), "w") as fh:
        json.dump(m, fh, indent=2)


def main():
    for name, img in textures().items():
        save(img, "block", name + ".png")
    models()
    print("Interactive block textures and models written")


if __name__ == "__main__":
    main()
