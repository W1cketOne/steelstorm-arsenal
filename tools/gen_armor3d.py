#!/usr/bin/env python3
"""The 3D Stormsteel armour: its cube layout and a double-resolution texture painted to fit it.

Run from the repository root:  python3 tools/gen_armor3d.py   (needs Pillow)

Writes textures/models/armor/stormsteel_3d_{outer,inner}.png and the Java class
client/StormsteelArmorLayers.java with the matching LayerDefinitions, so both always agree.
"""
import math
import os
import random

from PIL import Image

from texlib import R, hexc, mix, save

UV_W, UV_H = 128, 64
SCALE = 2  # texture pixels per UV unit

STEEL = R["stormsteel"]
GOLD = R["gold"]
DARK = R["black_iron"]
GLOW = (110, 235, 255, 255)
GLOW_HOT = (225, 252, 255, 255)

# part -> list of cubes: (name, x, y, z, w, h, d, inflate, material, mirror)
OUTER = {
    "head": [
        ("helm", -4, -8, -4, 8, 8, 8, 1.0, "steel", False),
        ("crest", -0.5, -12.5, -5, 1, 4, 10, 0.0, "gold", False),
        ("brow", -4.5, -6.5, -5.7, 9, 2, 1, 0.0, "gold", False),
        ("cheek_r", -5.6, -4.5, -3.5, 1, 4, 5, 0.0, "steel", False),
        ("cheek_l", 4.6, -4.5, -3.5, 1, 4, 5, 0.0, "steel", True),
        ("wing_r", -6.8, -10, -1, 1, 5, 4, 0.0, "gold", False),
        ("wing_l", 5.8, -10, -1, 1, 5, 4, 0.0, "gold", True),
    ],
    "body": [
        ("cuirass", -4, 0, -2, 8, 12, 4, 1.01, "steel", False),
        ("plate", -3.5, 1, -3.8, 7, 6, 1, 0.0, "plate", False),
        ("core", -1, 3, -4.6, 2, 2, 1, 0.0, "glow", False),
        ("backplate", -3, 1, 2.8, 6, 8, 1, 0.0, "steel", False),
        ("collar", -4.5, -1, -3, 9, 2, 6, 0.0, "gold", False),
    ],
    "right_arm": [
        ("sleeve", -3, -2, -2, 4, 12, 4, 1.0, "steel", False),
        ("pauldron", -5, -4, -3.5, 6, 4, 7, 0.0, "pauldron", False),
        ("ridge", -4, -5.5, -1, 4, 2, 2, 0.0, "gold", False),
        ("bracer", -3.5, 6, -2.5, 5, 3, 5, 0.25, "plate", False),
    ],
    "left_arm": [
        ("sleeve", -1, -2, -2, 4, 12, 4, 1.0, "steel", True),
        ("pauldron", -1, -4, -3.5, 6, 4, 7, 0.0, "pauldron", True),
        ("ridge", 0, -5.5, -1, 4, 2, 2, 0.0, "gold", True),
        ("bracer", -1.5, 6, -2.5, 5, 3, 5, 0.25, "plate", True),
    ],
    "right_leg": [
        ("boot", -2, 6, -2, 4, 6, 4, 1.0, "steel", False),
        ("toe", -2.5, 10, -4, 5, 2, 2, 0.0, "gold", False),
        ("shin", -2, 5, -3.6, 4, 4, 1, 0.0, "plate", False),
    ],
    "left_leg": [
        ("boot", -2, 6, -2, 4, 6, 4, 1.0, "steel", True),
        ("toe", -2.5, 10, -4, 5, 2, 2, 0.0, "gold", True),
        ("shin", -2, 5, -3.6, 4, 4, 1, 0.0, "plate", True),
    ],
}

INNER = {
    "body": [
        ("belt", -4, 9, -2, 8, 3, 4, 0.6, "gold", False),
        ("tasset_r", -4, 11.5, -3.0, 3.5, 4, 1, 0.0, "plate", False),
        ("tasset_l", 0.5, 11.5, -3.0, 3.5, 4, 1, 0.0, "plate", True),
        ("buckle", -1, 9.5, -3.0, 2, 2, 1, 0.0, "glow", False),
    ],
    "right_leg": [
        ("cuisse", -2, 0, -2, 4, 8, 4, 0.55, "steel", False),
        ("knee", -2.5, 5, -3.3, 5, 3, 1.5, 0.0, "pauldron", False),
    ],
    "left_leg": [
        ("cuisse", -2, 0, -2, 4, 8, 4, 0.55, "steel", True),
        ("knee", -2.5, 5, -3.3, 5, 3, 1.5, 0.0, "pauldron", True),
    ],
}

PARTS = ["head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"]
PIVOTS = {"head": (0, 0, 0), "hat": (0, 0, 0), "body": (0, 0, 0), "right_arm": (-5, 2, 0), "left_arm": (5, 2, 0),
          "right_leg": (-1.9, 12, 0), "left_leg": (1.9, 12, 0)}


# ----------------------------------------------------------------------------- UV packing


def uv_size(w, h, d):
    W, H, D = math.ceil(w), math.ceil(h), math.ceil(d)
    return 2 * D + 2 * W, D + H


def pack(model):
    """Shelf-packs every cube's UV box; returns {(part, name): (u, v)}."""
    cubes = [(part, c) for part, cs in model.items() for c in cs]
    cubes.sort(key=lambda pc: -uv_size(*pc[1][4:7])[1])
    out = {}
    x = y = shelf = 0
    for part, c in cubes:
        bw, bh = uv_size(*c[4:7])
        if x + bw > UV_W:
            x = 0
            y += shelf
            shelf = 0
        if y + bh > UV_H:
            raise ValueError("armour UVs don't fit")
        out[(part, c[0])] = (x, y)
        x += bw
        shelf = max(shelf, bh)
    return out


# ----------------------------------------------------------------------------- painting


def faces(u, v, w, h, d):
    """UV rectangles of a cube's faces (Minecraft box-UV layout): name -> (x, y, fw, fh)."""
    W, H, D = math.ceil(w), math.ceil(h), math.ceil(d)
    return {
        "top": (u + D, v, W, D), "bottom": (u + D + W, v, W, D),
        "right": (u, v + D, D, H), "front": (u + D, v + D, W, H),
        "left": (u + D + W, v + D, D, H), "back": (u + 2 * D + W, v + D, W, H),
    }


def paint_face(img, rect, material, face, rng, name):
    x0, y0, fw, fh = (r * SCALE for r in rect)
    if fw == 0 or fh == 0:
        return
    ramp = {"steel": STEEL, "gold": GOLD, "plate": STEEL, "pauldron": STEEL, "glow": STEEL}[material]
    light = {"top": 0.78, "front": 0.62, "left": 0.52, "right": 0.5, "back": 0.42, "bottom": 0.3}[face]
    for py in range(fh):
        for px in range(fw):
            fx, fy = px / max(1, fw - 1), py / max(1, fh - 1)
            l = light - 0.12 * fy + 0.05 * math.sin(px * 0.9 + py * 0.4)
            edge = min(px, py, fw - 1 - px, fh - 1 - py)
            if edge == 0:
                l = 0.08 if (px == fw - 1 or py == fh - 1) else 0.95
            elif edge == 1:
                l += 0.12 if (px <= 1 or py <= 1) else -0.12
            if rng.random() < 0.05:
                l += rng.choice([-0.1, 0.08])
            color = ramp.at(l)
            if material == "glow":
                r = math.hypot(fx - 0.5, fy - 0.5)
                color = mix(GLOW_HOT, GLOW, min(1.0, r * 2))
            elif material == "pauldron" and edge <= 1 and face in ("front", "top", "left", "right"):
                color = GOLD.at(0.9 if edge == 0 else 0.65)
            elif material == "plate" and face == "front":
                # Engraved chevrons with a glowing seam.
                cx = abs(fx - 0.5)
                if abs(fy - (0.25 + cx * 0.8)) < 0.06 or abs(fy - (0.6 + cx * 0.8)) < 0.05:
                    color = GLOW if edge > 1 else color
                elif edge > 1 and abs(fx - 0.5) < 0.04:
                    color = STEEL.at(0.95)
            elif material == "steel" and face == "front" and name in ("helm",):
                # Visor slit glowing across the eyes.
                if 0.42 < fy < 0.55 and 0.12 < fx < 0.88:
                    color = GLOW if abs(fx - 0.5) > 0.06 else DARK.at(0.3)
            elif material == "steel" and name in ("cuirass", "sleeve", "cuisse", "boot") and face in ("front", "left", "right", "back"):
                # Panel lines.
                if edge > 1 and (py % (6 * SCALE) == 3 * SCALE):
                    color = STEEL.at(0.25)
                if name == "cuirass" and face == "front" and edge > 2 and abs(fx - 0.5) < 0.03:
                    color = GLOW
            img.putpixel((x0 + px, y0 + py), color)


def paint(model, uvs, seed):
    img = Image.new("RGBA", (UV_W * SCALE, UV_H * SCALE), (0, 0, 0, 0))
    rng = random.Random(seed)
    for part, cubes in model.items():
        for (name, x, y, z, w, h, d, inflate, material, mirror) in cubes:
            u, v = uvs[(part, name)]
            for face, rect in faces(u, v, w, h, d).items():
                paint_face(img, rect, material, face, rng, name)
    return img


# ----------------------------------------------------------------------------- Java


def java_layer(model, uvs, method):
    lines = [f"    public static LayerDefinition {method}() {{",
             "        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);",
             "        PartDefinition root = mesh.getRoot();"]
    for part in PARTS:
        px, py, pz = PIVOTS[part]
        cubes = model.get(part, [])
        builder = "CubeListBuilder.create()"
        for (name, x, y, z, w, h, d, inflate, material, mirror) in cubes:
            u, v = uvs[(part, name)]
            builder += f"\n                .texOffs({u}, {v})" + (".mirror()" if mirror else ".mirror(false)")
            builder += f".addBox({x}F, {y}F, {z}F, {w}F, {h}F, {d}F, new CubeDeformation({inflate}F))"
        lines.append(f'        root.addOrReplaceChild("{part}", {builder},\n                PartPose.offset({px}F, {py}F, {pz}F));')
    lines.append(f"        return LayerDefinition.create(mesh, {UV_W}, {UV_H});")
    lines.append("    }")
    return "\n".join(lines)


def main():
    out_uv = pack(OUTER)
    in_uv = pack(INNER)
    save(paint(OUTER, out_uv, 1), "models", "armor", "stormsteel_3d_outer.png")
    save(paint(INNER, in_uv, 2), "models", "armor", "stormsteel_3d_inner.png")
    java = f"""package com.steelstorm.arsenal.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Cube layout of the 3D Stormsteel armour. Generated by tools/gen_armor3d.py together with its
 * textures; edit the script, not this file.
 */
public final class StormsteelArmorLayers {{
{java_layer(OUTER, out_uv, "outer")}

{java_layer(INNER, in_uv, "inner")}

    private StormsteelArmorLayers() {{
    }}
}}
"""
    path = os.path.join(os.path.dirname(__file__), "..", "src", "main", "java", "com", "steelstorm", "arsenal", "client",
                        "StormsteelArmorLayers.java")
    with open(path, "w") as f:
        f.write(java)
    print("3D armour written")


if __name__ == "__main__":
    main()
