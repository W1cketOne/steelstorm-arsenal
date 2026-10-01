"""Material items and blocks (16x16)."""
import random

from PIL import Image

from pixelart import hex_rgb


def ingot(base, light, dark, outline, highlight, glow=None):
    """A vanilla-proportioned ingot seen at an angle."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # Ingot outline polygon rows (x ranges) from top to bottom.
    rows = {
        5: (5, 12), 6: (4, 13), 7: (3, 13), 8: (2, 13), 9: (2, 12), 10: (2, 11), 11: (3, 10),
    }
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            px[x, y] = base + (255,)
    # Top face (lighter), sides darker.
    for y in (5, 6):
        x0, x1 = rows[y]
        for x in range(x0, x1 + 1):
            px[x, y] = light + (255,)
    for y in (9, 10, 11):
        x0, x1 = rows[y]
        for x in range(x0, x1 + 1):
            if x >= x1 - 1:
                px[x, y] = dark + (255,)
    # Outline.
    for y, (x0, x1) in rows.items():
        px[x0, y] = outline + (255,)
        px[x1, y] = outline + (255,)
    for x in range(rows[5][0], rows[5][1] + 1):
        px[x, 4] = outline + (255,)
    for x in range(rows[11][0], rows[11][1] + 1):
        px[x, 12] = outline + (255,)
    px[4, 5] = outline + (255,)
    px[3, 6] = outline + (255,)
    px[2, 7] = outline + (255,)
    px[13, 9] = outline + (255,)
    px[12, 10] = outline + (255,)
    px[11, 11] = outline + (255,)
    # Highlights.
    for x, y in ((6, 5), (7, 5), (5, 6), (4, 7), (3, 8)):
        px[x, y] = highlight + (255,)
    if glow:
        for x, y in ((8, 8), (9, 8), (7, 9), (8, 9), (6, 10)):
            px[x, y] = glow + (255,)
    return img


def generate(save):
    out = []
    out.append(save(ingot(hex_rgb("#5577c8"), hex_rgb("#8fb1f4"), hex_rgb("#33508e"), hex_rgb("#121a35"),
                          hex_rgb("#e6f0ff"), glow=hex_rgb("#6ff0ff")), "item", "stormsteel_ingot.png"))
    return out
