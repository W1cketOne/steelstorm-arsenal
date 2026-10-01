#!/usr/bin/env python3
"""Textures for the big glowy effects: halo bands, sparkle stars, orbs and light rays.

Run from the repository root:  python3 tools/gen_vfx2.py   (needs Pillow)
All are white/greyscale; the game tints them. Drawn with additive blending, so black = invisible.
"""
import math

from PIL import Image

from texlib import save


def halo_band(w=64, h=32):
    """A ring band: V runs across the band (0 outer edge, 1 inner edge). Bright thin core, wide soft glow."""
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        v = (y + 0.5) / h
        d = abs(v - 0.35)
        core = math.exp(-(d / 0.045) ** 2)
        glow = math.exp(-(d / 0.22) ** 2) * 0.55
        inner_fade = math.exp(-max(0, v - 0.35) / 0.25) * 0.25 if v > 0.35 else 0
        for x in range(w):
            shimmer = 0.9 + 0.1 * math.sin(x / w * math.tau * 6 + v * 9)
            l = min(1.0, (core + glow + inner_fade) * shimmer)
            c = int(255 * l)
            img.putpixel((x, y), (c, c, c, 255))
    return img


def sparkle(n=32):
    """A four-pointed star with a soft centre glow."""
    img = Image.new("RGBA", (n, n))
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            dx, dy = (x - c) / c, (y - c) / c
            r = math.hypot(dx, dy)
            arm = max(math.exp(-(abs(dx) / 0.07) ** 2) * max(0, 1 - abs(dy)) ** 1.6,
                      math.exp(-(abs(dy) / 0.07) ** 2) * max(0, 1 - abs(dx)) ** 1.6)
            diag = max(math.exp(-(abs(dx - dy) / 0.06) ** 2), math.exp(-(abs(dx + dy) / 0.06) ** 2)) * max(0, 1 - r * 1.6) * 0.5
            core = math.exp(-(r / 0.18) ** 2)
            l = min(1.0, arm + diag + core)
            v = int(255 * l)
            img.putpixel((x, y), (v, v, v, int(255 * min(1.0, l * 1.4))))
    return img


def orb(n=32):
    """A hollow glowing bubble: bright rim, faint centre, a highlight."""
    img = Image.new("RGBA", (n, n))
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            r = math.hypot(x - c, y - c) / c
            rim = math.exp(-((r - 0.72) / 0.12) ** 2)
            fill = 0.25 * max(0, 1 - r) if r < 0.72 else 0
            hl = math.exp(-(math.hypot(x - c * 0.7, y - c * 0.65) / (c * 0.16)) ** 2) * 0.9
            outer = math.exp(-((r - 0.72) / 0.3) ** 2) * 0.3
            l = min(1.0, rim + fill + hl + outer)
            v = int(255 * l)
            img.putpixel((x, y), (v, v, v, int(255 * min(1.0, l * 1.3))))
    return img


def ray(w=16, h=64):
    """A tapering ray of light, brightest at the bottom (its origin)."""
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        t = y / (h - 1)
        width = 0.5 * (1 - t) ** 0.8 + 0.02
        for x in range(w):
            u = abs((x + 0.5) / w - 0.5) * 2
            l = math.exp(-(u / max(0.02, width)) ** 2) * (1 - t) ** 1.2
            v = int(255 * min(1.0, l))
            img.putpixel((x, y), (v, v, v, 255))
    return img


def main():
    save(halo_band(), "entity", "halo_band.png")
    save(sparkle(), "particle", "sparkle.png")
    save(orb(), "particle", "orb.png")
    save(orb(), "entity", "orb.png")
    save(sparkle(), "entity", "sparkle.png")
    save(ray(), "entity", "ray.png")
    print("VFX textures written")


if __name__ == "__main__":
    main()
