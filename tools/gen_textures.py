#!/usr/bin/env python3
"""Generates every Steelstorm Arsenal texture as original pixel art.

Run from the repository root:  python3 tools/gen_textures.py
Requires Pillow (pip install pillow). Output goes to src/main/resources/assets/steelstorm/textures.

Weapons are drawn with a tiny vector rasterizer (bars, discs, polygons) at 16x16, shaded by
which side of the blade a pixel sits on, then outlined automatically. Entity skins are painted
part by part onto the standard 64x64 humanoid layout.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "steelstorm", "textures")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def darken(c, f):
    return (int(c[0] * f), int(c[1] * f), int(c[2] * f), c[3])


def save(img, *path):
    full = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


# ---------------------------------------------------------------------------
# Palettes: (dark, base, light)
# ---------------------------------------------------------------------------
TIERS = {
    "stone": ("#4a4a4a", "#808080", "#ababab"),
    "iron": ("#6b6b6b", "#d6d6d6", "#ffffff"),
    "golden": ("#9c6f0c", "#f2cf3c", "#fff7ad"),
    "diamond": ("#127f78", "#47dccf", "#d2fff9"),
    "netherite": ("#231d1f", "#4c4245", "#7f7174"),
    "stormsteel": ("#22356b", "#5885dd", "#cfe1ff"),
}
GUARD = {
    "stone": ("#3b3b3b", "#5c5c5c", "#7a7a7a"),
    "iron": ("#5a4a2c", "#8c7448", "#b8a070"),
    "golden": ("#7a5208", "#c9971a", "#f5d36a"),
    "diamond": ("#6b5310", "#d6a922", "#ffe27a"),
    "netherite": ("#3a1818", "#6e2a2a", "#9c4a3a"),
    "stormsteel": ("#7a6a10", "#e8c22c", "#fff27a"),
}
HANDLE = ("#3a2312", "#62401f", "#8a5d32")
WOOD = ("#4a2f17", "#7a5230", "#9f7248")
DARK_HANDLE = ("#1c1416", "#33282b", "#4a3c40")


class Canvas:
    """A 16x16 grid of (palette, shade) entries; shade -1 light, 0 base, 1 dark."""

    def __init__(self, size=16):
        self.size = size
        self.px = [[None] * size for _ in range(size)]

    def put(self, x, y, pal, shade=0):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[y][x] = (pal, shade)

    def bar(self, a, b, hw_a, pal, hw_b=None, shaded=True, stripes=False, shade=None):
        hw_b = hw_a if hw_b is None else hw_b
        ax, ay = a
        bx, by = b
        dx, dy = bx - ax, by - ay
        length = math.hypot(dx, dy) or 1e-6
        ux, uy = dx / length, dy / length
        nx, ny = -uy, ux
        for y in range(self.size):
            for x in range(self.size):
                cx, cy = x + 0.5, y + 0.5
                t = ((cx - ax) * ux + (cy - ay) * uy) / length
                if t < -0.02 or t > 1.02:
                    continue
                d = (cx - ax) * nx + (cy - ay) * ny
                hw = hw_a + (hw_b - hw_a) * min(max(t, 0), 1)
                if abs(d) <= hw + 0.05:
                    if shade is not None:
                        s = shade
                    elif stripes:
                        s = -1 if int(t * length * 1.0) % 2 == 0 else 1
                    elif not shaded:
                        s = 0
                    elif d < -hw * 0.3:
                        s = -1
                    elif d > hw * 0.4:
                        s = 1
                    else:
                        s = 0
                    self.put(x, y, pal, s)

    def disc(self, c, r, pal, shade=None):
        for y in range(self.size):
            for x in range(self.size):
                dx, dy = x + 0.5 - c[0], y + 0.5 - c[1]
                if dx * dx + dy * dy <= r * r:
                    s = shade if shade is not None else (-1 if dx + dy < -r * 0.5 else (1 if dx + dy > r * 0.6 else 0))
                    self.put(x, y, pal, s)

    def ring(self, c, r_out, r_in, pal):
        for y in range(self.size):
            for x in range(self.size):
                dx, dy = x + 0.5 - c[0], y + 0.5 - c[1]
                d = math.hypot(dx, dy)
                if r_in <= d <= r_out:
                    s = -1 if (dx + dy) < -2 else (1 if (dx + dy) > 3 else 0)
                    if d > r_out - 0.8:
                        s = min(s, 0) if dx + dy < 0 else 1
                    self.put(x, y, pal, s)

    def poly(self, pts, pal, shade_fn=None):
        for y in range(self.size):
            for x in range(self.size):
                if point_in_poly(x + 0.5, y + 0.5, pts):
                    self.put(x, y, pal, shade_fn(x, y) if shade_fn else 0)

    def render(self, outline=True):
        img = Image.new("RGBA", (self.size, self.size), (0, 0, 0, 0))
        for y in range(self.size):
            for x in range(self.size):
                e = self.px[y][x]
                if e:
                    pal, s = e
                    img.putpixel((x, y), hexc(pal[{-1: 2, 0: 1, 1: 0}[s]]))
        if outline:
            src = img.copy()
            for y in range(self.size):
                for x in range(self.size):
                    if src.getpixel((x, y))[3]:
                        continue
                    for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + ox, y + oy
                        if 0 <= nx < self.size and 0 <= ny < self.size and src.getpixel((nx, ny))[3]:
                            e = self.px[ny][nx]
                            img.putpixel((x, y), darken(hexc(e[0][0]), 0.45))
                            break
        return img


def point_in_poly(x, y, pts):
    inside = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
            inside = not inside
        j = i
    return inside


DIAG = (1 / math.sqrt(2), -1 / math.sqrt(2))


def along(p, u, s):
    return (p[0] + u[0] * s, p[1] + u[1] * s)


def sword(cv, p0, u, blade, guard, handle, handle_len, guard_half, guard_hw, blade_len, blade_hw,
          tip_len, pommel_r=0.9, fuller=False, curve=0.0, tsuba=False, blade_hw_end=None):
    n = (-u[1], u[0])
    cv.bar(p0, along(p0, u, handle_len), 0.7, handle, stripes=True)
    cv.disc(p0, pommel_r, guard)
    g = along(p0, u, handle_len)
    if tsuba:
        cv.disc(g, guard_half, guard)
    else:
        cv.bar(along(g, n, -guard_half), along(g, n, guard_half), guard_hw, guard)
    start = handle_len + 0.35
    end_hw = blade_hw if blade_hw_end is None else blade_hw_end
    if curve == 0:
        a = along(p0, u, start)
        b = along(p0, u, start + blade_len)
        cv.bar(a, b, blade_hw, blade, hw_b=end_hw)
        cv.bar(b, along(b, u, tip_len), end_hw, blade, hw_b=0.15)
        if fuller:
            cv.bar(along(a, u, 1.0), along(a, u, blade_len - 1.5), 0.32, blade, shade=1)
    else:
        steps = 14
        total = blade_len + tip_len
        prev = None
        for i in range(steps + 1):
            t = i / steps
            s = start + total * t
            off = curve * math.sin(math.pi * t * 0.85)
            pt = along(along(p0, u, s), n, -off)
            if prev is not None:
                hw = blade_hw if s - start < blade_len else blade_hw * max(0.15, 1 - (s - start - blade_len) / tip_len)
                cv.bar(prev, pt, hw, blade)
            prev = pt


def weapon_texture(kind, blade, guard, handle):
    cv = Canvas()
    if kind == "longsword":
        sword(cv, (2.0, 14.0), DIAG, blade, guard, handle, 3.2, 2.7, 0.8, 10.6, 1.1, 2.4)
    elif kind == "greatsword":
        sword(cv, (1.8, 14.2), DIAG, blade, guard, handle, 3.0, 3.6, 0.95, 10.8, 1.95, 2.6, pommel_r=1.0,
              fuller=True, blade_hw_end=1.7)
    elif kind == "katana":
        sword(cv, (1.6, 14.4), DIAG, blade, guard, handle, 4.2, 1.35, 0.8, 10.4, 0.95, 2.4, pommel_r=0.7,
              curve=1.2, tsuba=True)
    elif kind == "dual_daggers":
        sword(cv, (13.6, 13.6), (-1 / math.sqrt(2), -1 / math.sqrt(2)), blade, guard, handle, 2.4, 1.9, 0.7, 7.6, 1.0, 2.0, pommel_r=0.8)
        sword(cv, (2.4, 13.6), DIAG, blade, guard, handle, 2.4, 1.9, 0.7, 7.6, 1.0, 2.0, pommel_r=0.8)
    elif kind == "spear":
        p0 = (1.4, 14.6)
        cv.bar(p0, along(p0, DIAG, 12.4), 0.55, WOOD if handle is HANDLE else handle)
        cv.bar(along(p0, DIAG, 5.0), along(p0, DIAG, 6.6), 0.62, handle, stripes=True)
        g = along(p0, DIAG, 11.4)
        n = (-DIAG[1], DIAG[0])
        cv.bar(along(g, n, -1.0), along(g, n, 1.0), 0.55, guard)
        h0 = along(p0, DIAG, 11.8)
        h1 = along(p0, DIAG, 13.8)
        cv.bar(h0, h1, 1.0, blade, hw_b=1.9)
        cv.bar(h1, along(h1, DIAG, 3.0), 1.9, blade, hw_b=0.1)
    elif kind == "warhammer":
        p0 = (2.0, 14.0)
        cv.bar(p0, along(p0, DIAG, 11.6), 0.62, WOOD if handle is HANDLE else handle)
        cv.bar(along(p0, DIAG, 1.0), along(p0, DIAG, 4.0), 0.68, handle, stripes=True)
        cv.disc(p0, 0.85, guard)
        c = along(p0, DIAG, 11.0)
        n = (-DIAG[1], DIAG[0])
        cv.bar(along(c, n, -4.3), along(c, n, 4.3), 2.1, blade)
        cv.bar(along(c, n, -4.3), along(c, n, -3.3), 2.3, blade, shade=1)
        cv.bar(along(c, n, -0.9), along(c, n, 0.9), 2.25, guard, shaded=False)
        top = along(c, DIAG, 2.0)
        cv.bar(top, along(top, DIAG, 1.8), 0.7, blade, hw_b=0.1)
    elif kind == "scythe":
        cv.bar((2.6, 14.8), (12.6, 3.0), 0.55, WOOD if handle is HANDLE else handle)
        cv.bar((5.0, 12.0), (6.8, 9.9), 0.62, handle, stripes=True)
        pal = blade

        def blade_shade(x, y):
            d1 = math.hypot(x + 0.5 - 8, y + 0.5 - 10)
            return -1 if d1 > 7.1 else (1 if d1 < 6.0 else 0)

        for y in range(16):
            for x in range(16):
                cx, cy = x + 0.5, y + 0.5
                if 1.2 <= cx <= 12.6 and cy <= 7.2 and math.hypot(cx - 8, cy - 10) <= 8.0 and math.hypot(cx - 7, cy - 12) >= 8.6:
                    cv.put(x, y, pal, blade_shade(x, y))
        cv.disc((12.4, 3.4), 0.9, guard)
    elif kind == "battleaxe":
        p0 = (2.0, 14.0)
        cv.bar(p0, along(p0, DIAG, 12.8), 0.6, WOOD if handle is HANDLE else handle)
        cv.bar(along(p0, DIAG, 1.0), along(p0, DIAG, 4.2), 0.66, handle, stripes=True)
        cv.disc(p0, 0.8, guard)
        c = along(p0, DIAG, 10.6)
        n = (-DIAG[1], DIAG[0])
        u = DIAG
        for sign in (1, -1):
            local = [(-0.9, 0.5), (0.9, 0.5), (2.6, 4.4), (0, 4.9), (-2.6, 4.4)]
            pts = [(c[0] + u[0] * a + n[0] * b * sign, c[1] + u[1] * a + n[1] * b * sign) for a, b in local]

            def shade_fn(x, y, sign=sign):
                b = ((x + 0.5 - c[0]) * n[0] + (y + 0.5 - c[1]) * n[1]) * sign
                return -1 if b > 3.5 else (1 if b < 1.4 else 0)

            cv.poly(pts, blade, shade_fn)
        cv.bar(along(c, u, -0.9), along(c, u, 0.9), 0.75, guard)
        top = along(c, u, 1.0)
        cv.bar(top, along(top, u, 2.2), 0.6, blade, hw_b=0.1)
    elif kind == "chakram":
        cv.ring((8, 8), 6.4, 4.4, blade)
        for i in range(8):
            ang = i * math.pi / 4 + math.pi / 8
            a = (8 + math.cos(ang) * 6.0, 8 + math.sin(ang) * 6.0)
            b = (8 + math.cos(ang + 0.3) * 7.9, 8 + math.sin(ang + 0.3) * 7.9)
            cv.bar(a, b, 0.7, blade, hw_b=0.1)
        cv.ring((8, 8), 4.4, 3.5, guard)
    elif kind == "throwing_knives":
        for p0 in ((1.6, 11.0), (5.0, 14.4)):
            cv.ring(p0, 1.1, 0.4, guard)
            cv.bar(along(p0, DIAG, 0.8), along(p0, DIAG, 3.0), 0.6, handle, stripes=True)
            a = along(p0, DIAG, 3.2)
            b = along(p0, DIAG, 7.6)
            cv.bar(a, b, 0.85, blade)
            cv.bar(b, along(b, DIAG, 2.0), 0.85, blade, hw_b=0.1)
    else:
        raise ValueError(kind)
    return cv.render()


def sparkle(img, points, color):
    for x, y in points:
        if 0 <= x < img.width and 0 <= y < img.height:
            img.putpixel((x, y), hexc(color))


TIERED = ["longsword", "greatsword", "katana", "dual_daggers", "spear", "warhammer", "scythe", "battleaxe"]


def gen_weapons():
    for tier, blade in TIERS.items():
        guard = GUARD[tier]
        handle = DARK_HANDLE if tier == "netherite" else HANDLE
        for kind in TIERED:
            img = weapon_texture(kind, blade, guard, handle)
            if tier == "stormsteel":
                sparkle(img, [(13, 1), (14, 3)], "#fff7a0")
            save(img, "item", f"{tier}_{kind}.png")
    save(weapon_texture("chakram", TIERS["iron"], GUARD["golden"], HANDLE), "item", "chakram.png")
    save(weapon_texture("throwing_knives", TIERS["iron"], GUARD["iron"], HANDLE), "item", "throwing_knife.png")

    tempest = weapon_texture("longsword", ("#26307a", "#6c7cff", "#eef5ff"), ("#8a6a10", "#f2c62c", "#fff3a0"), HANDLE)
    sparkle(tempest, [(14, 0), (12, 2), (15, 2), (10, 4), (13, 5)], "#fff86a")
    save(tempest, "item", "tempest_edge.png")
    rime = weapon_texture("greatsword", ("#2a6f8f", "#8fe0ff", "#f4ffff"), ("#4a6a8a", "#9ab8d6", "#e0f0ff"), ("#1f3247", "#35506e", "#4f7396"))
    sparkle(rime, [(15, 0), (11, 2), (13, 6), (8, 5)], "#ffffff")
    save(rime, "item", "rimecleaver.png")
    void = weapon_texture("scythe", ("#1a0b2e", "#6a2bb3", "#c991ff"), ("#2a0f40", "#4a1d73", "#8a4fd0"), ("#120a18", "#261833", "#3d2a52"))
    sparkle(void, [(3, 3), (6, 1), (1, 6)], "#f0c8ff")
    save(void, "item", "voidreaver.png")


# ---------------------------------------------------------------------------
# Materials, blocks and misc items
# ---------------------------------------------------------------------------
def gen_materials():
    pal = TIERS["stormsteel"]
    cv = Canvas()
    cv.poly([(2, 10), (5, 6), (14, 6), (11, 10)], pal, lambda x, y: -1 if y <= 6 else 0)
    cv.poly([(2, 10), (11, 10), (11, 12.5), (2, 12.5)], pal, lambda x, y: 1)
    cv.poly([(11, 10), (14, 6), (14, 8.5), (11, 12.5)], pal, lambda x, y: 1 if x > 12 else 0)
    img = cv.render()
    sparkle(img, [(6, 7), (7, 7), (9, 8)], "#ffffff")
    sparkle(img, [(12, 7)], "#fff7a0")
    save(img, "item", "stormsteel_ingot.png")

    cv = Canvas()
    for c, r in (((6, 9), 3.3), ((10, 7), 3.0), ((9.5, 11), 2.4), ((5, 5.5), 1.8)):
        cv.disc(c, r, ("#2b3550", "#5c6f9e", "#a9bce8"))
    img = cv.render()
    sparkle(img, [(5, 8), (10, 6), (9, 11)], "#cfe1ff")
    save(img, "item", "raw_stormsteel.png")


def noise_tile(base_colors, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            img.putpixel((x, y), hexc(rng.choice(base_colors)))
    return img


def add_ore(img, seed, colors):
    rng = random.Random(seed)
    clusters = [(3, 4), (10, 3), (6, 10), (12, 11), (2, 13)]
    for cx, cy in clusters:
        for _ in range(rng.randint(3, 5)):
            x = min(15, max(0, cx + rng.randint(-1, 2)))
            y = min(15, max(0, cy + rng.randint(-1, 1)))
            img.putpixel((x, y), hexc(rng.choice(colors)))
        img.putpixel((cx, cy), hexc(colors[-1]))
    return img


def gen_blocks():
    stone = ["#7f7f7f", "#7a7a7a", "#858585", "#8a8a8a", "#747474", "#7f7f7f", "#6e6e6e"]
    deep = ["#4d4d52", "#47474c", "#525257", "#3f3f44", "#4a4a4f", "#56565b"]
    ore = ["#2f4a8f", "#4f7ad4", "#8fb3ff", "#e8f0ff"]
    save(add_ore(noise_tile(stone, 1), 11, ore), "block", "stormsteel_ore.png")
    save(add_ore(noise_tile(deep, 2), 12, ore), "block", "deepslate_stormsteel_ore.png")

    # Storage block: riveted plates with a lightning bolt.
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            seam = x == 7 or y == 7
            c = "#22356b" if edge else ("#3c5aa6" if seam else ("#6b93e6" if (x + y) % 7 else "#5885dd"))
            img.putpixel((x, y), hexc(c))
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        img.putpixel((x, y), hexc("#cfe1ff"))
    for x, y in ((9, 2), (8, 3), (8, 4), (7, 5), (9, 5), (8, 6), (7, 8), (6, 9), (6, 10), (5, 11), (7, 10), (5, 12)):
        img.putpixel((x, y), hexc("#fff27a"))
    save(img, "block", "stormsteel_block.png")

    img = noise_tile(["#3a4666", "#5c6f9e", "#4d5d88", "#6a7fb3", "#2b3550"], 3)
    save(add_ore(img, 13, ["#a9bce8", "#cfe1ff"]), "block", "raw_stormsteel_block.png")

    # Weapon rack: dark spruce boards and iron pegs.
    rng = random.Random(4)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            band = y // 4
            c = ["#5a3d22", "#6b4a2a", "#614226", "#70502e"][band]
            if y % 4 == 3:
                c = "#3a2614"
            elif rng.random() < 0.12:
                c = "#4c331c"
            img.putpixel((x, y), hexc(c))
    save(img, "block", "weapon_rack.png")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), hexc("#5e5e5e" if (x + y) % 5 else "#8c8c8c"))
    save(img, "block", "weapon_rack_peg.png")


def gen_dummy_item():
    cv = Canvas()
    straw = ("#8a6a26", "#d9b44a", "#f2d77a")
    wood = WOOD
    cv.bar((8, 15.5), (8, 9), 0.7, wood)
    cv.bar((3, 7), (13, 7), 0.9, wood)
    cv.bar((8, 6), (8, 12), 2.6, straw)
    cv.disc((8, 3.6), 2.6, straw)
    img = cv.render()
    # Stitched eyes and a red target ring on the chest.
    sparkle(img, [(6, 3), (10, 3)], "#3a2412")
    sparkle(img, [(6, 9), (7, 10), (8, 9), (9, 10), (10, 9)], "#a3392c")
    save(img, "item", "target_dummy.png")


def gen_effects():
    def icon(draw):
        cv = Canvas(18)
        draw(cv)
        return cv.render()

    def bleed(cv):
        cv.disc((9, 11), 4.2, ("#5c0a0a", "#c21d1d", "#ff7a6a"))
        cv.poly([(9, 2), (5.6, 9.5), (12.4, 9.5)], ("#5c0a0a", "#c21d1d", "#ff7a6a"), lambda x, y: -1 if x < 8 else 0)

    def stagger(cv):
        for i in range(3):
            ang = i * 2 * math.pi / 3
            c = (9 + math.cos(ang) * 5, 9 + math.sin(ang) * 3)
            cv.disc(c, 2.0, ("#8a6a08", "#f2cf3c", "#fff7ad"))
        cv.ring((9, 9), 7.5, 6.8, ("#6b6b6b", "#bdbdbd", "#ffffff"))

    def armor_break(cv):
        pal = ("#3a3a3a", "#8a8a8a", "#cfcfcf")
        cv.poly([(3, 3), (15, 3), (15, 9), (9, 16), (3, 9)], pal, lambda x, y: -1 if x < 7 else 0)

    img = icon(bleed)
    save(img, "mob_effect", "bleed.png")
    img = icon(stagger)
    save(img, "mob_effect", "stagger.png")
    img = icon(armor_break)
    for x, y in ((9, 3), (8, 5), (10, 7), (8, 9), (9, 11), (10, 13)):
        img.putpixel((x, y), (0, 0, 0, 0))
    save(img, "mob_effect", "armor_break.png")


# ---------------------------------------------------------------------------
# Entity skins (64x64 humanoid layout, left limbs mirror the right ones)
# ---------------------------------------------------------------------------
class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.rng = random.Random(seed)

    def rect(self, x0, y0, w, h, colors):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                c = colors(x - x0, y - y0) if callable(colors) else self.rng.choice(colors)
                if c is not None:
                    self.img.putpixel((x, y), hexc(c) if isinstance(c, str) else c)

    # Box faces: (u, v) origin, box size (w, h, d)
    def box(self, u, v, w, h, d, side, front=None, top=None, bottom=None, back=None):
        front = front or side
        back = back or side
        top = top or side
        bottom = bottom or side
        self.rect(u + d, v, w, d, top)
        self.rect(u + d + w, v, w, d, bottom)
        self.rect(u, v + d, d, h, side)
        self.rect(u + d, v + d, w, h, front)
        self.rect(u + d + w, v + d, d, h, side)
        self.rect(u + d + w + d, v + d, w, h, back)

    def head(self, side, front, top=None, back=None):
        self.box(0, 0, 8, 8, 8, side, front, top, top, back)

    def hat(self, side, front, top=None, back=None):
        self.box(32, 0, 8, 8, 8, side, front, top, top, back)

    def body(self, side, front, back=None):
        self.box(16, 16, 8, 12, 4, side, front, side, side, back)

    def arm(self, side, front=None):
        self.box(40, 16, 4, 12, 4, side, front)

    def leg(self, side, front=None):
        self.box(0, 16, 4, 12, 4, side, front)


def rows(spec):
    """spec: list of (row_limit, colors) -> function(x, y) choosing colors by row."""
    rng = random.Random(len(spec))

    def f(x, y):
        for limit, cols in spec:
            if y < limit:
                return rng.choice(cols) if isinstance(cols, list) else (cols(x, y) if callable(cols) else cols)
        return None

    return f


def gen_skins():
    # Bandit Duelist: red bandana, leather vest, tan skin.
    s = Skin(10)
    skin = ["#c69c6d", "#bf9465", "#c9a074"]
    hair = ["#2b1d12", "#33231a"]
    s.head(rows([(2, hair), (8, skin)]),
           lambda x, y: (hair[0] if y < 2 else ("#f2f2f2" if y == 4 and x in (1, 6) else ("#2a1a10" if y == 4 and x in (2, 5) else
                         ("#a31f1f" if y >= 5 else ("#3b2616" if y == 3 and x in (1, 2, 5, 6) else skin[0]))))),
           top=hair, back=rows([(7, hair), (8, "#a31f1f")]))
    s.hat(lambda x, y: "#8f1a1a" if y == 5 else None, lambda x, y: "#b52525" if y in (5, 6) else None,
          top=lambda x, y: None, back=lambda x, y: "#8f1a1a" if y in (5, 6) or (y == 7 and x in (3, 4)) else None)
    vest = ["#5a3a1e", "#664425", "#52341a"]
    shirt = ["#a32a2a", "#9a2525"]
    s.body(rows([(9, vest), (10, "#2a1a0e"), (12, ["#3a2a1e", "#3f2e22"])]),
           lambda x, y: ("#2a1a0e" if y == 9 else ("#c9a227" if y == 9 and x == 4 else
                         (shirt[(x + y) % 2] if 2 <= x <= 5 and y < 9 else (vest[0] if y < 9 else "#3a2a1e")))),
           back=rows([(9, vest), (10, "#2a1a0e"), (12, "#3a2a1e")]))
    s.arm(rows([(4, shirt), (10, skin), (12, ["#3a2414"])]))
    s.leg(rows([(9, ["#3a2a1e", "#41301f"]), (12, ["#1e1a18", "#24201c"])]))
    save(s.img, "entity", "bandit_duelist.png")

    # Bandit Archer: green hood, tunic, quiver strap.
    s = Skin(11)
    hood = ["#2f4a22", "#35522a", "#2a4220"]
    s.head(rows([(8, skin)]),
           lambda x, y: ("#1c2a14" if y < 2 else ("#f2f2f2" if y == 4 and x in (1, 6) else ("#24331a" if y == 4 and x in (2, 5) else
                         ("#5a4a3a" if y == 6 and 2 <= x <= 5 else skin[0])))), top=hood, back=hood)
    s.hat(rows([(8, hood)]), lambda x, y: hood[0] if (y < 2 or x in (0, 7)) else None, top=hood, back=hood)
    tunic = ["#3d5c2c", "#456633", "#38552a"]
    s.body(rows([(12, tunic)]),
           lambda x, y: "#5a3a1e" if x == y or x == y - 1 else ("#2a1a0e" if y == 8 else tunic[(x * 3 + y) % 3]),
           back=lambda x, y: "#6b4a2a" if 2 <= x <= 4 and y < 8 else ("#e8e0d0" if 2 <= x <= 4 and y == 0 else tunic[(x + y) % 3]))
    s.arm(rows([(5, tunic), (9, skin), (12, ["#4a321c"])]))
    s.leg(rows([(8, ["#5a4630", "#624d35"]), (12, ["#3a2a1a", "#33251a"])]))
    save(s.img, "entity", "bandit_archer.png")

    # Iron Revenant: battered plate armour, dark visor with red eyes.
    s = Skin(12)
    iron = ["#8a8a8a", "#7d7d7d", "#949494", "#6e6e6e", "#8a7a6a"]
    s.head(rows([(8, iron)]),
           lambda x, y: ("#1a1a1a" if y in (3, 4) and 1 <= x <= 6 and not (y == 3 and x in (2, 5)) else
                         ("#ff3a2a" if y == 3 and x in (2, 5) else iron[(x + y) % 5])), top=iron)
    s.hat(lambda x, y: None, lambda x, y: "#5a5a5a" if y == 0 else None, top=lambda x, y: "#6e6e6e" if x in (3, 4) else None)
    s.body(rows([(12, iron)]),
           lambda x, y: ("#4a4a4a" if y == 8 else ("#a35a3a" if (x * 7 + y * 3) % 11 == 0 else iron[(x + 2 * y) % 5])))
    s.arm(rows([(4, ["#9a9a9a", "#a5a5a5"]), (12, iron)]))
    s.leg(rows([(12, iron)]))
    save(s.img, "entity", "iron_revenant.png")

    # Fallen Warlord: blackened crimson plate with gold trim and a horned helm.
    s = Skin(13)
    plate = ["#2a1416", "#33181b", "#24110f"]
    crimson = ["#6e1a1f", "#7a1f24"]
    gold = "#c9971a"
    s.head(rows([(8, plate)]),
           lambda x, y: ("#ff8a1a" if y == 3 and x in (2, 5) else ("#0c0606" if y in (3, 4) and 1 <= x <= 6 else
                         (gold if y == 0 or (x in (3, 4) and y >= 5) else plate[(x + y) % 3]))), top=plate)
    s.hat(lambda x, y: gold if y == 7 else (plate[0] if y < 2 else None),
          lambda x, y: ("#e8dcc0" if (y < 3 and x in (0, 7)) else (gold if y == 7 else None)),
          top=lambda x, y: "#e8dcc0" if x in (0, 7) and y in (3, 4) else None,
          back=lambda x, y: plate[0] if y < 3 else None)
    s.body(rows([(12, plate)]),
           lambda x, y: (gold if y in (0, 8) or x in (0, 7) else (crimson[(x + y) % 2] if 2 <= x <= 5 and 2 <= y <= 6 else plate[(x + y) % 3])),
           back=lambda x, y: crimson[(x + y) % 2] if y < 11 else gold)
    s.arm(rows([(4, [gold, "#a37a14"]), (12, plate)]))
    s.leg(rows([(10, plate), (12, [gold])]))
    save(s.img, "entity", "fallen_warlord.png")

    # Target dummy: burlap sack with stitches on a wooden post.
    s = Skin(14)
    burlap = ["#c9a95a", "#bf9f52", "#d4b466", "#b39248"]
    s.head(rows([(8, burlap)]),
           lambda x, y: ("#3a2412" if (y, x) in ((2, 1), (3, 2), (4, 1), (2, 3), (4, 3), (2, 4), (3, 5), (4, 4), (2, 6), (4, 6)) else
                         ("#a3392c" if y == 6 and 2 <= x <= 5 else burlap[(x * 3 + y) % 4])))
    s.body(rows([(12, burlap)]),
           lambda x, y: ("#a3392c" if (x - 3.5) ** 2 + (y - 5) ** 2 < 5 and (x - 3.5) ** 2 + (y - 5) ** 2 > 2 else
                         ("#e8e0c0" if (x - 3.5) ** 2 + (y - 5) ** 2 <= 2 else ("#5a3a1e" if y == 10 else burlap[(x + y) % 4]))))
    s.arm(rows([(12, burlap)]))
    s.leg(rows([(12, ["#7a5230", "#6b4a2a", "#84593a"])]))
    save(s.img, "entity", "target_dummy.png")


if __name__ == "__main__":
    # Item icons, particles and effect icons now come from gen_icons.py / gen_fx.py.
    gen_blocks()
    gen_skins()
    print("Textures written to", os.path.abspath(ROOT))
