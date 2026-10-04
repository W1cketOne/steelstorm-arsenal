#!/usr/bin/env python3
"""Icons for the Mythic weapons and the Celestial / Dragonscale armour, recoloured from the
existing animated icons (frames and .mcmeta are kept, so they shimmer the same way).

Run after gen_icons.py:  python3 tools/gen_new_icons.py
"""
import colorsys
import os
import shutil

from PIL import Image

from texlib import ROOT, hexc

ITEM = os.path.join(ROOT, "item")

MYTHIC = {  # name: (base icon, dark, mid, light)
    "solaris": ("tempest_edge", "#4a1e00", "#ff9d00", "#fff6c8"),
    "worldsplitter": ("rimecleaver", "#00302a", "#14c4ae", "#d8fff8"),
    "eclipse": ("moonveil", "#12002a", "#7a2cff", "#e6d4ff"),
    "starfall": ("earthshaker", "#2a0a00", "#ff6a1a", "#fff0c0"),
    "soulreaper": ("voidreaver", "#002a18", "#2fe39a", "#e0fff0"),
    "venomfang": ("bloodfang", "#0f2400", "#6fe82a", "#efffd0"),
    "dragonspine": ("skypiercer", "#2a0400", "#ff4a1a", "#ffe0c0"),
    "titanbreaker": ("kingsbane", "#2a1a00", "#ffc23a", "#fffbe0"),
}


def ramp(dark, mid, light, t):
    a, b, c = (hexc(x) for x in (dark, mid, light))
    if t < 0.5:
        f = t / 0.5
        return tuple(int(a[i] + (b[i] - a[i]) * f) for i in range(3))
    f = (t - 0.5) / 0.5
    return tuple(int(b[i] + (c[i] - b[i]) * f) for i in range(3))


def remap(src, dst, fn):
    img = Image.open(os.path.join(ITEM, src + ".png")).convert("RGBA")
    out = img.copy()
    px = out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            px[x, y] = fn(r, g, b) + (a,)
    out.save(os.path.join(ITEM, dst + ".png"))
    meta = os.path.join(ITEM, src + ".png.mcmeta")
    if os.path.exists(meta):
        shutil.copy(meta, os.path.join(ITEM, dst + ".png.mcmeta"))


def mythic(dark, mid, light):
    def fn(r, g, b):
        h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
        # Wood and leather (warm, dull) keep a darkened version of their colour; metal takes the mythic ramp.
        if s < 0.35 and 0.02 < h < 0.15 and l < 0.5:
            nr, ng, nb = colorsys.hls_to_rgb(h, l * 0.8, s)
            return int(nr * 255), int(ng * 255), int(nb * 255)
        return ramp(dark, mid, light, min(1.0, l * 1.1))
    return fn


def celestial(r, g, b):
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    if 0.45 < h < 0.75 and s > 0.2:
        if h < 0.53 and l > 0.68:
            return ramp("#7a4a00", "#ffd26a", "#fffbe6", 0.85)
        return ramp("#3c3a40", "#c9c4bc", "#ffffff", min(1.0, 0.25 + l * 0.9))
    if 0.08 < h < 0.2 and s > 0.3:
        return ramp("#5a3500", "#f2b62c", "#fff2b0", l)
    return ramp("#2a2830", "#9a96a0", "#e8e6ee", l)


def dragonscale(r, g, b):
    h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    if 0.45 < h < 0.75 and s > 0.2:
        if h < 0.53 and l > 0.68:
            return ramp("#5a1000", "#ff6a1a", "#ffe6a0", 0.8)
        return ramp("#1a0204", "#a3162a", "#ff7a6a", min(1.0, l * 1.05))
    if 0.08 < h < 0.2 and s > 0.3:
        return ramp("#07020a", "#2a1f34", "#5a4c6e", l)
    return ramp("#100206", "#4a1018", "#a03040", l)


ULTS = {  # new ultimate icon: (icon it is recoloured from, mythic it belongs to)
    "solaris_supernova": ("warhammer_cataclysm", "solaris"),
    "worldsplitter_sunder": ("greatsword_crescent_wave", "worldsplitter"),
    "eclipse_total_eclipse": ("katana_thousand_cuts", "eclipse"),
    "starfall_meteor_storm": ("earthshaker_tectonic_spiral", "starfall"),
    "soulreaper_harvest": ("voidreaver_event_horizon", "soulreaper"),
    "venomfang_thousand_cuts": ("dual_daggers_death_blossom", "venomfang"),
    "dragonspine_dragons_descent": ("spear_dragon_dive", "dragonspine"),
    "titanbreaker_titanfall": ("warhammer_earthquake", "titanbreaker"),
}


def main():
    global ITEM
    item_dir = ITEM
    ITEM = os.path.join(ROOT, "gui", "ability")
    for name, (src, mythic_name) in ULTS.items():
        _, *cols = MYTHIC[mythic_name]
        remap(src, name, mythic(*cols))
    ITEM = item_dir
    for name, (base, *cols) in MYTHIC.items():
        remap(base, name, mythic(*cols))
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        remap(f"stormsteel_{piece}", f"celestial_{piece}", celestial)
        remap(f"stormsteel_{piece}", f"dragonscale_{piece}", dragonscale)
    print("new icons written")


if __name__ == "__main__":
    main()
