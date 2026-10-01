"""Small pixel-art toolkit used by generate_textures.py.

Weapons are described as a list of *parts*. Each part is a shape test evaluated at pixel centres,
either in screen space (x, y) or in "weapon space":

    u = distance along the weapon axis (bottom-left grip end -> top-right tip), in pixels
    v = signed distance across the axis (negative = upper-left side, the lit side)

Rasterising shapes at pixel centres in weapon space produces the clean diagonal staircases that
vanilla tools and swords use. Each part names a palette and a shading style.
"""
import math
from PIL import Image

SQRT2 = math.sqrt(2.0)


def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def palette(*colors):
    """Five tones: outline, shadow, base, light, highlight."""
    tones = [hex_rgb(c) for c in colors]
    assert len(tones) == 5, colors
    return tones


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


class Canvas:
    """A size x size grid of (part, rgba) cells."""

    def __init__(self, size=32):
        self.size = size
        self.part = [[None] * size for _ in range(size)]
        self.color = [[None] * size for _ in range(size)]

    # Weapon space for a diagonal from (x0, y0) (grip end) to (x1, y1) (tip).
    @staticmethod
    def axis(x0, y0, x1, y1):
        dx, dy = x1 - x0, y1 - y0
        length = math.hypot(dx, dy)
        ax, ay = dx / length, dy / length          # along
        # Across: rotate "along" 90 degrees clockwise on screen (y points down), so +v is the
        # lower-right (shadow) side for a bottom-left -> top-right weapon.
        return (x0, y0, ax, ay, -ay, ax, length)

    @staticmethod
    def to_uv(axis, px, py):
        x0, y0, ax, ay, nx, ny, _ = axis
        rx, ry = px - x0, py - y0
        u = rx * ax + ry * ay
        v = rx * nx + ry * ny
        return u, v

    def stamp(self, part, overwrite=False):
        """Rasterise a part. Later parts sit on top unless overwrite is False and the cell is taken."""
        for y in range(self.size):
            for x in range(self.size):
                cx, cy = x + 0.5, y + 0.5
                if part.contains(cx, cy):
                    if self.part[y][x] is None or overwrite or part.on_top:
                        self.part[y][x] = part

    def filled(self, x, y):
        return 0 <= x < self.size and 0 <= y < self.size and self.part[y][x] is not None

    def same(self, x, y, part):
        return 0 <= x < self.size and 0 <= y < self.size and self.part[y][x] is part

    def shade(self):
        for y in range(self.size):
            for x in range(self.size):
                part = self.part[y][x]
                if part is not None:
                    self.color[y][x] = part.shade(self, x, y)

    def image(self):
        img = Image.new("RGBA", (self.size, self.size), (0, 0, 0, 0))
        px = img.load()
        for y in range(self.size):
            for x in range(self.size):
                c = self.color[y][x]
                if c is not None:
                    px[x, y] = c if len(c) == 4 else (c[0], c[1], c[2], 255)
        return img


class Part:
    """A shape plus a palette and shading style.

    styles:
      'blade'  - dark rim, bright bevel on the lit edge, fuller line along the axis
      'metal'  - rim + top-left light / bottom-right shadow (guards, heads, pommels)
      'wrap'   - banded grip wrap
      'wood'   - lit/shadow sides only (thin shafts read better without a full rim)
      'gem'    - glossy jewel
      'glow'   - flat bright colour (energy veins)
      'flat'   - single tone index given by `tone`
    """

    def __init__(self, test, pal, style="metal", axis=None, on_top=False, tone=2, band=1.6, fuller=None, rim=True,
                 speckle=False):
        self.test = test
        self.pal = pal
        self.style = style
        self.axis = axis
        self.on_top = on_top
        self.tone = tone
        self.band = band
        self.fuller = fuller
        self.rim = rim
        self.speckle = speckle

    def uv(self, x, y):
        return Canvas.to_uv(self.axis, x, y) if self.axis else (x, y)

    def contains(self, x, y):
        if self.axis is not None:
            u, v = Canvas.to_uv(self.axis, x, y)
            return self.test(u, v)
        return self.test(x, y)

    # ------------------------------------------------------------------ shading
    def edge(self, canvas, x, y):
        """Neighbour emptiness: returns (upper-left open, lower-right open)."""
        ul = not canvas.filled(x - 1, y) or not canvas.filled(x, y - 1)
        lr = not canvas.filled(x + 1, y) or not canvas.filled(x, y + 1)
        return ul, lr

    def own_edge(self, canvas, x, y):
        ul = not canvas.same(x - 1, y, self) or not canvas.same(x, y - 1, self)
        lr = not canvas.same(x + 1, y, self) or not canvas.same(x, y + 1, self)
        return ul, lr

    def shade(self, canvas, x, y):
        p = self.pal
        cx, cy = x + 0.5, y + 0.5
        ul, lr = self.edge(canvas, x, y)
        oul, olr = self.own_edge(canvas, x, y)
        if self.style == "flat":
            return p[self.tone]
        if self.style == "glow":
            return p[4] if (oul or olr) is False else p[3]
        if self.style == "gem":
            if ul and lr:
                return p[0]
            if oul and not olr:
                return p[4]
            if olr:
                return p[1]
            return p[2]
        if self.style == "wood":
            u, v = self.uv(cx, cy)
            if lr:
                return p[0] if self.rim else p[1]
            if v < -0.25:
                return p[3]
            if v > 0.45:
                return p[1]
            return p[2]
        if self.style == "wrap":
            u, v = self.uv(cx, cy)
            band = int(math.floor(u / self.band)) % 2
            if lr and ul:
                return p[0]
            if lr:
                return p[0] if band else p[1]
            base = p[3] if band == 0 else p[2]
            if v < -0.3 and band == 0:
                base = p[4]
            return base
        if self.style == "blade":
            u, v = self.uv(cx, cy)
            if lr:
                return p[0]
            if ul:
                # Lit edge: the sharpened bevel catches the light.
                return p[4]
            if self.speckle:
                h = (x * 7 + y * 13 + x * y * 3) % 9
                if h == 0:
                    return p[1]
                if h == 4:
                    return p[3]
            if self.fuller is not None and self.fuller(u, v):
                return p[3]
            if v > 0.4:
                return p[1]
            return p[2]
        # metal
        if lr and ul:
            return p[0]
        if lr:
            return p[0]
        if ul:
            return p[3]
        u, v = self.uv(cx, cy)
        return p[2] if v < 0.5 else p[1]


def compose(parts, size=32):
    canvas = Canvas(size)
    # Parts are listed back-to-front; stamp front-most first so earlier stamps win ties.
    for part in reversed(parts):
        canvas.stamp(part)
    for part in parts:
        if part.on_top:
            canvas.stamp(part, overwrite=True)
    canvas.shade()
    return canvas


def overlay(base, extra):
    """Paste RGBA image extra over base."""
    out = base.copy()
    out.alpha_composite(extra)
    return out


def scale_preview(images, scale=8, cols=6, bg=(48, 48, 56)):
    w = max(i.width for i in images)
    h = max(i.height for i in images)
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * (w * scale + 8) + 8, rows * (h * scale + 8) + 8), bg + (255,))
    for idx, img in enumerate(images):
        cx = 8 + (idx % cols) * (w * scale + 8)
        cy = 8 + (idx // cols) * (h * scale + 8)
        big = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
        sheet.alpha_composite(big, (cx, cy))
    return sheet
