#!/usr/bin/env python3
"""Custom boss models: bone hierarchy, painted textures (base + glow) and the Java LayerDefinitions.

Run from the repository root:  python3 tools/gen_bosses.py   (needs Pillow)

Bones are given with absolute pivots (model space: y down, 24 = the ground) and cubes in absolute
coordinates too; the Java output converts them to parent-relative offsets. Textures are painted at
twice the declared UV resolution.
"""
import math
import os
import random

from PIL import Image

from texlib import Ramp, hexc, mix, save

SCALE = 2


class Bone:
    def __init__(self, name, parent, pivot, cubes, rot=(0, 0, 0)):
        self.name = name
        self.parent = parent
        self.pivot = pivot
        self.cubes = cubes  # (x, y, z, w, h, d, material, mirror) in absolute coords
        self.rot = rot


# ----------------------------------------------------------------------------- palettes

PAL = {
    # Forge Colossus
    "iron": Ramp("#0e0f12", "#24262c", "#363941", "#4a4e58", "#61666f", "#7b808a", "#9aa0aa"),
    "plate": Ramp("#140f0c", "#2c2420", "#43372f", "#5b4b40", "#756152", "#8f7866", "#ad957f"),
    "brass": Ramp("#2b1d06", "#5e420f", "#8c6619", "#b88d27", "#d8b240", "#efd477", "#fff2bf"),
    "rock": Ramp("#0b0a0a", "#1b1918", "#2a2725", "#3a3633", "#4a4542", "#5d5753", "#746d68"),
    # Moonblade Revenant
    "robe": Ramp("#07061a", "#120f33", "#1d1a4d", "#2a2768", "#3a3884", "#5150a3", "#7372c4"),
    "silver": Ramp("#22262b", "#565d66", "#7d8590", "#a5adb7", "#c9cfd6", "#e6eaee", "#ffffff"),
    "skin": Ramp("#2a2438", "#4c4361", "#6f6587", "#9087a8", "#b0a8c6", "#cdc7de", "#ebe8f5"),
    "hair": Ramp("#4a5266", "#7a849c", "#a3adc4", "#c6cee0", "#dfe5f1", "#f0f4fb", "#ffffff"),
    "blade": Ramp("#141a2b", "#33405e", "#55688f", "#7e93ba", "#a9bbdb", "#d4e0f2", "#ffffff"),
}
GLOWS = {
    "molten": ((255, 244, 200), (255, 120, 20)),
    "eye": ((255, 255, 230), (255, 200, 40)),
    "moon": ((255, 255, 255), (130, 190, 255)),
    "soul": ((240, 230, 255), (170, 110, 255)),
}


# ----------------------------------------------------------------------------- bosses

def colossus():
    B = []
    B.append(Bone("hips", "root", (0, 6, 0), [(-7, 2, -5, 14, 6, 10, "iron", False)]))
    B.append(Bone("torso", "hips", (0, 4, 0), [
        (-12, -16, -8, 24, 20, 16, "plate", False),
        (-10, -14, -9, 20, 4, 1, "brass", False),
        (-4, -10, -10, 8, 8, 2, "molten", False),
        (-6, -12, -9.5, 12, 1, 1, "brass", False),
        (-11, -18, 4, 22, 6, 6, "rock", False),
        (-3, -26, 4, 2, 9, 2, "iron", False),
        (1, -26, 4, 2, 9, 2, "iron", False),
    ]))
    B.append(Bone("head", "torso", (0, -16, -3), [
        (-5, -24, -8, 10, 8, 10, "iron", False),
        (-4, -20.5, -8.6, 8, 1.5, 1, "eye", False),
        (-6, -26, -6, 3, 4, 3, "brass", False),
        (3, -26, -6, 3, 4, 3, "brass", True),
        (-5.5, -17, -8.5, 11, 2, 5, "brass", False),
    ]))
    for side, sgn in (("right", -1), ("left", 1)):
        m = sgn > 0
        x0 = -21 if sgn < 0 else 12
        B.append(Bone(f"{side}_arm", "torso", (sgn * 14, -12, 0), [
            (x0, -16, -7, 9, 9, 14, "plate", m),
            (x0 + 1, -18, -3, 7, 2, 6, "brass", m),
            (x0 + 1.5, -7, -4, 6, 13, 8, "iron", m),
        ]))
        B.append(Bone(f"{side}_forearm", f"{side}_arm", (sgn * 16.5, 6, 0), [
            (x0 + 1, 6, -4.5, 7, 9, 9, "iron", m),
            (x0, 13, -6, 9, 2, 12, "brass", m),
            (x0 - 0.5, 15, -6, 10, 10, 12, "rock", m),
            (x0 + 2.5, 19, -6.6, 4, 3, 1, "molten", m),
        ]))
        B.append(Bone(f"{side}_leg", "root", (sgn * 6, 8, 0), [
            (sgn * 6 - 4.5, 8, -4.5, 9, 9, 9, "plate", m),
            (sgn * 6 - 4, 17, -4, 8, 4, 8, "iron", m),
            (sgn * 6 - 5, 21, -7, 10, 3, 11, "rock", m),
            (sgn * 6 - 1, 12, -5, 2, 2, 1, "molten", m),
        ]))
    return B


def moonblade():
    B = []
    B.append(Bone("body", "root", (0, -6, 0), [
        (-4, -18, -2.5, 8, 12, 5, "robe", False),
        (-4.5, -18, -3, 9, 3, 6, "silver", False),
        (-1, -12, -3.1, 2, 5, 1, "moon", False),
    ]))
    B.append(Bone("skirt", "body", (0, -6, 0), [
        (-5, -6, -3.5, 10, 7, 7, "robe", False),
        (-5.5, 0, -4, 11, 2, 8, "silver", False),
    ]))
    B.append(Bone("tail", "skirt", (0, 2, 0), [
        (-4, 2, -3, 8, 6, 6, "robe", False),
        (-3, 8, -2, 6, 5, 4, "robe", False),
        (-2, 13, -1.5, 4, 4, 3, "soul", False),
    ]))
    B.append(Bone("head", "body", (0, -18, 0), [
        (-3.5, -25, -3.5, 7, 7, 7, "skin", False),
        (-3.5, -21.5, -3.6, 7, 1, 1, "moon", False),
        (-4, -26, -4, 8, 3, 8, "hair", False),
        (-4, -25, 2.5, 8, 9, 2, "hair", False),
        (-4.5, -29, -1, 9, 3, 1, "silver", False),
        (-1, -31, -1, 2, 2, 1, "moon", False),
    ]))
    for pair, y in (("upper", -17), ("lower", -12)):
        for side, sgn in (("right", -1), ("left", 1)):
            m = sgn > 0
            ax = sgn * 4.5
            x0 = ax - 1.5
            B.append(Bone(f"{pair}_{side}_arm", "body", (ax, y, 0), [
                (x0, y - 1, -1.5, 3, 9, 3, "skin" if pair == "lower" else "robe", m),
                (x0 - 0.3, y + 5, -1.8, 3.6, 2, 3.6, "silver", m),
            ]))
            B.append(Bone(f"{pair}_{side}_blade", f"{pair}_{side}_arm", (ax, y + 8, 0), [
                (ax - 1.5, y + 8, -1.5, 3, 1, 3, "silver", m),
                (ax - 0.5, y + 9, -1, 1, 12, 2, "blade", m),
                (ax - 0.5, y + 9, -1.4, 1, 12, 0.4, "moon", m),
                (ax - 0.5, y + 21, -0.5, 1, 2, 1, "moon", m),
            ]))
    return B


# ----------------------------------------------------------------------------- UV packing and painting

def uv_size(w, h, d):
    W, H, D = math.ceil(w), math.ceil(h), math.ceil(d)
    return 2 * D + 2 * W, D + H


def pack(bones, uv_w, uv_h):
    cubes = [(b.name, i, c) for b in bones for i, c in enumerate(b.cubes)]
    cubes.sort(key=lambda t: -uv_size(*t[2][3:6])[1])
    out = {}
    x = y = shelf = 0
    for name, i, c in cubes:
        bw, bh = uv_size(*c[3:6])
        if x + bw > uv_w:
            x, y, shelf = 0, y + shelf, 0
        if y + bh > uv_h:
            raise ValueError(f"UVs don't fit in {uv_w}x{uv_h}")
        out[(name, i)] = (x, y)
        x += bw
        shelf = max(shelf, bh)
    return out


def faces(u, v, w, h, d):
    W, H, D = math.ceil(w), math.ceil(h), math.ceil(d)
    return {"top": (u + D, v, W, D), "bottom": (u + D + W, v, W, D), "right": (u, v + D, D, H),
            "front": (u + D, v + D, W, H), "left": (u + D + W, v + D, D, H), "back": (u + 2 * D + W, v + D, W, H)}


def paint(bones, uvs, uv_w, uv_h, seed):
    base = Image.new("RGBA", (uv_w * SCALE, uv_h * SCALE))
    glow = Image.new("RGBA", (uv_w * SCALE, uv_h * SCALE))
    rng = random.Random(seed)
    for b in bones:
        for i, (x, y, z, w, h, d, mat, mirror) in enumerate(b.cubes):
            u, v = uvs[(b.name, i)]
            for face, (fx0, fy0, fw, fh) in faces(u, v, w, h, d).items():
                x0, y0, W, H = fx0 * SCALE, fy0 * SCALE, fw * SCALE, fh * SCALE
                light = {"top": 0.8, "front": 0.62, "left": 0.52, "right": 0.5, "back": 0.42, "bottom": 0.28}[face]
                for py in range(H):
                    for px in range(W):
                        tx, ty = px / max(1, W - 1), py / max(1, H - 1)
                        edge = min(px, py, W - 1 - px, H - 1 - py)
                        if mat in GLOWS:
                            hot, cool = GLOWS[mat]
                            r = math.hypot(tx - 0.5, ty - 0.5) * 2
                            flick = 0.15 * math.sin(px * 1.3 + py * 0.7)
                            c = mix(hot + (255,), cool + (255,), min(1.0, max(0.0, r * 0.8 + flick)))
                            base.putpixel((x0 + px, y0 + py), c)
                            glow.putpixel((x0 + px, y0 + py), c)
                            continue
                        ramp = PAL[mat]
                        l = light - 0.14 * ty + 0.04 * math.sin(px * 0.8 + py * 0.5)
                        if edge == 0:
                            l = 0.06 if (px == W - 1 or py == H - 1) else 0.9
                        elif edge == 1:
                            l += 0.1 if (px <= 1 or py <= 1) else -0.1
                        if mat in ("rock", "plate") and rng.random() < 0.1:
                            l += rng.choice([-0.16, 0.1])
                        if mat == "robe" and (px // 2 + py // 6) % 5 == 0:
                            l -= 0.08
                        if mat == "blade":
                            l = 0.95 - 0.5 * tx
                        col = ramp.at(l)
                        # Molten cracks through the colossus' rock and plate.
                        if mat in ("rock", "plate") and face in ("front", "left", "right", "top") and edge > 1:
                            if rng.random() < 0.012 or (abs(math.sin(px * 0.35 + py * 0.22) - 0.97) < 0.012 and mat == "rock"):
                                col = (255, 140, 30, 255)
                                glow.putpixel((x0 + px, y0 + py), col)
                        base.putpixel((x0 + px, y0 + py), col)
    return base, glow


# ----------------------------------------------------------------------------- Java

def java_layer(bones, uvs, method, uv_w, uv_h, root_y=24):
    by = {b.name: b for b in bones}
    lines = [f"    public static LayerDefinition {method}() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             f"        PartDefinition root = mesh.getRoot().addOrReplaceChild(\"root\", CubeListBuilder.create(), PartPose.offset(0F, {root_y}F, 0F));"]
    var = {"root": "root"}

    def abs_pivot(name):
        return (0, root_y, 0) if name == "root" else by[name].pivot

    for b in bones:
        pp = abs_pivot(b.parent)
        off = tuple(round(b.pivot[k] - pp[k], 3) for k in range(3))
        builder = "CubeListBuilder.create()"
        for i, (x, y, z, w, h, d, mat, mirror) in enumerate(b.cubes):
            u, v = uvs[(b.name, i)]
            rx, ry, rz = (round(x - b.pivot[0], 3), round(y - b.pivot[1], 3), round(z - b.pivot[2], 3))
            builder += f"\n                .texOffs({u}, {v})" + (".mirror()" if mirror else ".mirror(false)")
            builder += f".addBox({rx}F, {ry}F, {rz}F, {w}F, {h}F, {d}F)"
        rot = b.rot
        pose = (f"PartPose.offsetAndRotation({off[0]}F, {off[1]}F, {off[2]}F, {rot[0]}F, {rot[1]}F, {rot[2]}F)"
                if any(rot) else f"PartPose.offset({off[0]}F, {off[1]}F, {off[2]}F)")
        v = b.name.replace("-", "_")
        lines.append(f'        PartDefinition {v} = {var[b.parent]}.addOrReplaceChild("{b.name}", {builder},\n                {pose});')
        var[b.name] = v
    lines.append(f"        return LayerDefinition.create(mesh, {uv_w}, {uv_h});")
    lines.append("    }")
    return "\n".join(lines)


def main():
    out = []
    for name, bones, uv_w, uv_h in (("forge_colossus", colossus(), 256, 128), ("moonblade_revenant", moonblade(), 128, 64)):
        uvs = pack(bones, uv_w, uv_h)
        base, glow = paint(bones, uvs, uv_w, uv_h, len(name))
        save(base, "entity", f"{name}.png")
        save(glow, "entity", f"{name}_glow.png")
        method = "".join(p.capitalize() if i else p for i, p in enumerate(name.split("_")))
        out.append(java_layer(bones, uvs, method, uv_w, uv_h))
    java = """package com.steelstorm.arsenal.client.boss;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Boss model layouts, generated by tools/gen_bosses.py together with their textures. */
public final class BossLayers {
""" + "\n\n".join(out) + """

    private BossLayers() {
    }
}
"""
    path = os.path.join(os.path.dirname(__file__), "..", "src", "main", "java", "com", "steelstorm", "arsenal", "client", "boss",
                        "BossLayers.java")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(java)
    print("Boss models written")


if __name__ == "__main__":
    main()
