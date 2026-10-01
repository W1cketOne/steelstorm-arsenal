"""Generates every Steelstorm Arsenal texture.

Usage (from the repository root):
    python3 tools/textures/generate_textures.py [--preview DIR]

Writes PNGs into src/main/resources/assets/steelstorm/textures/. Re-running is safe; output is
deterministic.
"""
import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import weapons  # noqa: E402
from pixelart import scale_preview  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "steelstorm", "textures")


def save(img, *path):
    out = os.path.join(ASSETS, *path)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    return img


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", help="directory for preview sheets")
    args = parser.parse_args()

    weapon_images = []
    for kind, builder in weapons.ARCHETYPES.items():
        for tier in weapons.TIERS:
            weapon_images.append(save(builder(tier), "item", f"{tier}_{kind}.png"))

    import materials  # noqa: E402
    misc_images = materials.generate(save)

    import logo  # noqa: E402
    logo_img = logo.make_logo()
    logo_img.save(os.path.join(ROOT, "src", "main", "resources", "steelstorm_logo.png"))

    if args.preview:
        os.makedirs(args.preview, exist_ok=True)
        scale_preview(weapon_images, scale=6, cols=6).save(os.path.join(args.preview, "weapons.png"))
        scale_preview(weapon_images, scale=2, cols=6, bg=(139, 139, 139)).save(os.path.join(args.preview, "weapons_gui.png"))
        logo_img.resize((logo_img.width * 2, logo_img.height * 2)).save(os.path.join(args.preview, "logo.png"))
        if misc_images:
            scale_preview(misc_images, scale=8, cols=8).save(os.path.join(args.preview, "misc.png"))
    print(f"wrote {len(weapon_images) + len(misc_images)} textures")


if __name__ == "__main__":
    main()
