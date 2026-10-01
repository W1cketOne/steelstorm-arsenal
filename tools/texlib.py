"""Shared pixel-art helpers for the Steelstorm Arsenal texture generators.

Everything is drawn procedurally: parts are filled with a per-pixel test, shaded through a
7-step colour ramp (outline, 3 shadows, mid, 2 lights + specular), then outlined.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "steelstorm", "textures")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def mul(c, f):
    return (int(clamp(c[0] * f, 0, 255)), int(clamp(c[1] * f, 0, 255)), int(clamp(c[2] * f, 0, 255)), c[3])


def save(img, *path):
    full = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


class Ramp:
    """Seven colours: [outline, shadow3, shadow2, shadow1, mid, light, specular]."""

    def __init__(self, *colors):
        assert len(colors) == 7, colors
        self.c = [hexc(c) if isinstance(c, str) else c for c in colors]

    def at(self, light):
        return self.c[1 + int(round(clamp(light) * 5))]

    @property
    def outline(self):
        return self.c[0]

    def shifted(self, f):
        return Ramp(*[mul(c, f) for c in self.c])


R = {
    # blades / metals
    "stone": Ramp("#191919", "#3a3a3a", "#525252", "#6b6b6b", "#878787", "#a7a7a7", "#cbcbcb"),
    "iron": Ramp("#1b1f24", "#47505b", "#69737f", "#8f99a5", "#b8c1cb", "#dce3ea", "#ffffff"),
    "gold": Ramp("#3a2104", "#7a4c09", "#ad7a11", "#dca826", "#f5cf47", "#ffe98c", "#fffbe3"),
    "diamond": Ramp("#082a2e", "#11585a", "#1b928e", "#2fc9bd", "#66eedd", "#b0fff3", "#f3fffd"),
    "netherite": Ramp("#100b0e", "#271f24", "#3a3036", "#51444b", "#6a5c63", "#887980", "#ada0a7"),
    "stormsteel": Ramp("#0a1330", "#1c326e", "#2c4fa1", "#4474cf", "#6c9ff0", "#a6cbff", "#effaff"),
    # handles / trims
    "leather": Ramp("#170c05", "#38200f", "#573317", "#764923", "#925e30", "#ac763f", "#c59055"),
    "dark_leather": Ramp("#0a0809", "#1a1517", "#292225", "#393034", "#4a3f44", "#5c5056", "#73666c"),
    "red_wrap": Ramp("#1f060a", "#470c13", "#73131e", "#9e1d2a", "#c4303d", "#de5660", "#f0898f"),
    "blue_wrap": Ramp("#060a1f", "#0d1747", "#152573", "#1f369e", "#3050c4", "#5677de", "#8aa6f0"),
    "wood": Ramp("#1a0f07", "#3a2311", "#58381b", "#784e28", "#966735", "#b48045", "#cd9d5f"),
    "birch": Ramp("#2a241c", "#5c5446", "#8a8170", "#b3aa96", "#d2cab6", "#e8e2d2", "#fbf8f0"),
    "bronze": Ramp("#211208", "#4a2b12", "#77461d", "#a3652a", "#c6873d", "#deaa5a", "#f4d08a"),
    "brass": Ramp("#2b1d06", "#5e420f", "#8c6619", "#b88d27", "#d8b240", "#efd477", "#fff2bf"),
    "silver": Ramp("#22262b", "#565d66", "#7d8590", "#a5adb7", "#c9cfd6", "#e6eaee", "#ffffff"),
    "black_iron": Ramp("#07080a", "#16181c", "#24272d", "#33373f", "#454a54", "#5c626d", "#7c838f"),
    # gems
    "ruby": Ramp("#28040a", "#650b17", "#a2142a", "#da2540", "#ff586b", "#ff9aa5", "#ffffff"),
    "sapphire": Ramp("#04102a", "#0b2a6b", "#1546a8", "#2669e0", "#58a0ff", "#9ccaff", "#ffffff"),
    "emerald": Ramp("#03200f", "#0b5228", "#138a42", "#1fc05e", "#55e88d", "#a1ffc5", "#ffffff"),
    "amethyst": Ramp("#1a0729", "#43136b", "#6b21a8", "#9536e0", "#bb6bff", "#dcaaff", "#ffffff"),
    "topaz": Ramp("#2a1704", "#6b3e0b", "#a86515", "#e09528", "#ffc04e", "#ffe19a", "#ffffff"),
    "aqua": Ramp("#03221f", "#0a5650", "#118f84", "#1fd0bd", "#5ffbe7", "#acfff5", "#ffffff"),
    # legendary blades
    "tempest": Ramp("#0b1033", "#1d2a73", "#3149a8", "#5574d6", "#86a6f2", "#c3d8ff", "#ffffff"),
    "ice": Ramp("#0b2b3d", "#1b5f80", "#2f8fb5", "#59bfdf", "#93e3f7", "#cdf6ff", "#ffffff"),
    "void": Ramp("#07020d", "#170828", "#2a0f45", "#421a68", "#61278f", "#8a45c0", "#c99bff"),
    "earth": Ramp("#14100c", "#33291f", "#4f4031", "#6b5844", "#88725a", "#a68f74", "#c9b59a"),
    "blood": Ramp("#1a0205", "#45060e", "#740c18", "#a3162a", "#cf2b3f", "#ec5f6c", "#ffb3b9"),
    "sky": Ramp("#2b2410", "#6b5c2b", "#a99245", "#d8c26a", "#f2e39b", "#fff6d1", "#ffffff"),
    "moon": Ramp("#141a2b", "#33405e", "#55688f", "#7e93ba", "#a9bbdb", "#d4e0f2", "#ffffff"),
    "obsidian": Ramp("#030205", "#0e0a14", "#1a1424", "#271f34", "#372d47", "#4a3e5e", "#6d5f87"),
    "straw": Ramp("#2e1f06", "#6b4c12", "#9c741f", "#c99d34", "#e3bf55", "#f2d983", "#fff1c2"),
    "cloth_white": Ramp("#2b2b2b", "#6e6a64", "#9e988e", "#c4beb2", "#ddd8cc", "#efebe2", "#ffffff"),
}

TIER_BLADE = {"stone": "stone", "iron": "iron", "golden": "gold", "diamond": "diamond",
              "netherite": "netherite", "stormsteel": "stormsteel"}
TIER_TRIM = {"stone": "wood", "iron": "bronze", "golden": "gold", "diamond": "brass",
             "netherite": "bronze", "stormsteel": "brass"}
TIER_GEM = {"stone": None, "iron": "ruby", "golden": "emerald", "diamond": "aqua",
            "netherite": "amethyst", "stormsteel": "sapphire"}
TIER_GRIP = {"stone": "leather", "iron": "leather", "golden": "red_wrap", "diamond": "blue_wrap",
             "netherite": "dark_leather", "stormsteel": "dark_leather"}
TIERS = ["stone", "iron", "golden", "diamond", "netherite", "stormsteel"]


class Canvas:
    """A grid of (ramp, light, part) entries rendered to RGBA with automatic outlines."""

    def __init__(self, w=32, h=32):
        self.w = w
        self.h = h
        self.px = [[None] * w for _ in range(h)]
        self.part_counter = 0
        self.overrides = {}

    def new_part(self):
        self.part_counter += 1
        return self.part_counter

    def set(self, x, y, ramp, light, part=0, color=None):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = (ramp, light, part, color)

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return None

    def fill(self, test, ramp, separate=True):
        """test(cx, cy) -> light (0..1) or None. Draws over earlier parts with a dark seam."""
        part = self.new_part()
        hits = []
        for y in range(self.h):
            for x in range(self.w):
                l = test(x + 0.5, y + 0.5)
                if l is not None:
                    hits.append((x, y, l))
        hit_set = {(x, y) for x, y, _ in hits}
        for x, y, l in hits:
            old = self.px[y][x]
            self.px[y][x] = (ramp, l, part, None)
            if separate and old is not None and old[2] != part:
                # Seam where this part covers another: darken its border pixels.
                for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if (x + ox, y + oy) not in hit_set:
                        nb = self.get(x + ox, y + oy)
                        if nb is not None and nb[2] != part and nb[0] is not ramp:
                            self.px[y][x] = (ramp, min(l, 0.12), part, None)
                            break
        return part

    def dot(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            e = self.px[y][x]
            ramp = e[0] if e else R["silver"]
            part = e[2] if e else 0
            self.px[y][x] = (ramp, 1.0, part, hexc(color) if isinstance(color, str) else color)

    def render(self, outline=True, corner_outline=False):
        img = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        for y in range(self.h):
            for x in range(self.w):
                e = self.px[y][x]
                if e:
                    ramp, l, part, color = e
                    img.putpixel((x, y), color if color else ramp.at(l))
        if outline:
            src = [[self.px[y][x] for x in range(self.w)] for y in range(self.h)]
            dirs = ((1, 0), (-1, 0), (0, 1), (0, -1))
            if corner_outline:
                dirs = dirs + ((1, 1), (-1, -1), (1, -1), (-1, 1))
            for y in range(self.h):
                for x in range(self.w):
                    if src[y][x] is not None:
                        continue
                    for ox, oy in dirs:
                        nx, ny = x + ox, y + oy
                        if 0 <= nx < self.w and 0 <= ny < self.h and src[ny][nx] is not None:
                            img.putpixel((x, y), src[ny][nx][0].outline)
                            break
        return img


# ----------------------------------------------------------------------------- geometry

def polyline_coords(px, py, pts):
    """Nearest point on a polyline: returns (s, d, total_length, seg_dir) with s = arc length
    from the first point and d = signed distance (positive to the left of travel when y is down)."""
    best = None
    acc = 0.0
    total = 0.0
    lens = []
    for i in range(len(pts) - 1):
        l = math.hypot(pts[i + 1][0] - pts[i][0], pts[i + 1][1] - pts[i][1])
        lens.append(l)
        total += l
    for i in range(len(pts) - 1):
        ax, ay = pts[i]
        bx, by = pts[i + 1]
        l = lens[i] or 1e-6
        ux, uy = (bx - ax) / l, (by - ay) / l
        t = (px - ax) * ux + (py - ay) * uy
        tc = clamp(t, 0, l)
        cx, cy = ax + ux * tc, ay + uy * tc
        dist = math.hypot(px - cx, py - cy)
        # signed: normal = (-uy, ux) rotated so that it points "down-right" for an up-right line
        nx, ny = -uy, ux
        sd = (px - cx) * nx + (py - cy) * ny
        if i == 0 and t < 0:
            s = t
        elif i == len(pts) - 2 and t > l:
            s = acc + t
        else:
            s = acc + tc
        if best is None or dist < best[0]:
            best = (dist, s, sd if abs(sd) > 1e-9 else 0.0, (ux, uy))
        acc += l
    return best[1], best[2], total, best[3]


def bar_test(pts, hw, shader, cap_start=False, cap_end=False):
    """Filled stroke along a polyline. hw(t) gives the half width at t = 0..1 along the line."""
    def test(px, py):
        s, d, total, _ = polyline_coords(px, py, pts)
        if s < (-0.01 if not cap_start else -hw(0.0)) or s > total + (0.01 if not cap_end else hw(1.0)):
            return None
        t = clamp(s / total)
        w = hw(t)
        if w <= 0.05:
            return None
        if cap_start and s < 0:
            if math.hypot(s, d) > w:
                return None
        if cap_end and s > total:
            if math.hypot(s - total, d) > w:
                return None
        if abs(d) > w + 0.08:
            return None
        return shader(t, clamp(d / w, -1, 1), s)
    return test


def disc_test(cx, cy, r, shader, inner=0.0):
    def test(px, py):
        dx, dy = px - cx, py - cy
        dist = math.hypot(dx, dy)
        if dist > r + 0.05 or dist < inner:
            return None
        return shader(dx / r, dy / r, dist / r)
    return test


def poly_test(pts, shader):
    def inside(x, y):
        c = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-12) + xi:
                c = not c
            j = i
        return c

    def test(px, py):
        return shader(px, py) if inside(px, py) else None
    return test


def along(p, u, s):
    return (p[0] + u[0] * s, p[1] + u[1] * s)


def norm(v):
    l = math.hypot(v[0], v[1])
    return (v[0] / l, v[1] / l)


DIAG = norm((1, -1))
PERP = norm((1, 1))   # points down-right: the shadow side of a diagonal weapon


# ----------------------------------------------------------------------------- shaders

def blade_shader(fuller=0.0, edge_glint=True, base=0.58, grad=0.12, tip_spec=0.78):
    """Bevelled blade: up-left edge catches the light, down-right edge falls into shadow."""
    def sh(t, v, s):
        if v < -0.62:
            return 0.98 if edge_glint else 0.82
        if v > 0.66:
            return 0.12
        l = base + grad * t - 0.32 * v
        if fuller and abs(v) < fuller and 0.06 < t < 0.85:
            l -= 0.24
        if abs(t - tip_spec) < 0.025 and v < -0.2:
            l = 1.0
        return l
    return sh


def cylinder_shader(base=0.55, wrap=0.0, wrap_len=3.0, wrap_offset=0.0):
    def sh(t, v, s):
        l = base - 0.36 * v
        if wrap and ((s + wrap_offset + v * 1.2) % wrap_len) < 1.0:
            l -= wrap
        return l
    return sh


def metal_shader(base=0.62):
    def sh(t, v, s):
        l = base - 0.4 * v
        if v < -0.5:
            l += 0.15
        return l
    return sh


def sphere_shader(base=0.55):
    def sh(nx, ny, r):
        l = base - 0.45 * (nx + ny) * 0.7
        if r > 0.85:
            l -= 0.1
        return l
    return sh


def gem(cv, cx, cy, ramp_name, r=1.4):
    cv.fill(disc_test(cx, cy, r, lambda nx, ny, rr: 0.55 - 0.5 * (nx + ny) * 0.7), R[ramp_name])
    cv.dot(int(cx - r * 0.4), int(cy - r * 0.4), R[ramp_name].c[6])


def noise_dither(cv, ramp_names, amount, seed):
    """Light speckle on parts using the given ramps (stone, worn metal)."""
    rng = random.Random(seed)
    names = [R[n] for n in ramp_names]
    for y in range(cv.h):
        for x in range(cv.w):
            e = cv.px[y][x]
            if e and e[0] in names and e[3] is None and rng.random() < amount:
                cv.px[y][x] = (e[0], clamp(e[1] + rng.choice([-0.2, 0.2])), e[2], None)
