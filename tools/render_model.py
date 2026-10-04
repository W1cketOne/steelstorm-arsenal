#!/usr/bin/env python3
"""Offline preview renderer for Minecraft JSON item models (no game needed).

    python3 tools/render_model.py out.png steelstorm:item/3d/iron_longsword [more models...]

Draws each model side by side with Minecraft-style face shading, so model and texture work can
be checked in seconds. Options: --yaw/--pitch (degrees), --size (pixels per model).
"""
import argparse
import json
import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "fabric", "src", "main", "resources", "assets")
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
_tex_cache = {}


def res(loc, kind, ext):
    ns, path = loc.split(":", 1) if ":" in loc else ("minecraft", loc)
    return os.path.join(ROOT, ns, kind, path + ext)


def load_model(loc):
    with open(res(loc, "models", ".json")) as f:
        m = json.load(f)
    if "parent" in m and m["parent"].startswith("steelstorm:"):
        p = load_model(m["parent"])
        tex = dict(p.get("textures", {}))
        tex.update(m.get("textures", {}))
        p = dict(p)
        p["textures"] = tex
        if "elements" in m:
            p["elements"] = m["elements"]
        if "display" in m:
            p["display"] = m["display"]
        return p
    return m


def texture(model, ref):
    seen = 0
    while ref.startswith("#") and seen < 8:
        ref = model["textures"].get(ref[1:], "")
        seen += 1
    if ref not in _tex_cache:
        path = res(ref, "textures", ".png")
        img = Image.open(path).convert("RGBA") if os.path.exists(path) else Image.new("RGBA", (16, 16), (255, 0, 255, 255))
        _tex_cache[ref] = np.asarray(img).astype(np.float32) / 255.0
    return _tex_cache[ref]


def face_quad(f, t, face):
    x0, y0, z0 = f
    x1, y1, z1 = t
    # corners in uv order: (u0,v0), (u1,v0), (u1,v1), (u0,v1)
    return {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[face]


def rot_matrix(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def quads(model):
    out = []
    for el in model.get("elements", []):
        r = el.get("rotation")
        glow = bool(el.get("neoforge_data")) or bool(el.get("light_emission"))
        for face, fd in el["faces"].items():
            pts = np.array(face_quad(el["from"], el["to"], face), dtype=np.float64)
            normal = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0),
                      "up": (0, 1, 0), "down": (0, -1, 0)}[face]
            normal = np.array(normal, dtype=np.float64)
            if r:
                o = np.array(r["origin"], dtype=np.float64)
                m = rot_matrix(r["axis"], r["angle"])
                pts = (pts - o) @ m.T + o
                normal = m @ normal
            uv = fd.get("uv", [0, 0, 16, 16])
            u0, v0, u1, v1 = uv
            uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            rot = fd.get("rotation", 0) // 90
            uvs = uvs[rot:] + uvs[:rot]
            out.append((pts, np.array(uvs, dtype=np.float64), fd["texture"], normal, glow, face))
    return out


LIGHT = np.array([0.35, 0.85, 0.4])
LIGHT /= np.linalg.norm(LIGHT)


def render(model, yaw, pitch, size, bg):
    qs = quads(model)
    if not qs:
        return Image.new("RGBA", (size, size), bg)
    view = rot_matrix("x", pitch) @ rot_matrix("y", yaw)
    allp = np.concatenate([q[0] for q in qs]) - 8
    pv = allp @ view.T
    lo, hi = pv.min(0), pv.max(0)
    span = max(hi[0] - lo[0], hi[1] - lo[1]) * 1.08
    cx, cy = (hi[0] + lo[0]) / 2, (hi[1] + lo[1]) / 2
    ss = size * 2
    scale = ss / span
    img = np.zeros((ss, ss, 3), dtype=np.float32)
    img[:] = np.array(bg[:3]) / 255.0
    depth = np.full((ss, ss), -1e9, dtype=np.float32)
    for pts, uvs, ref, normal, glow, face in qs:
        p = (pts - 8) @ view.T
        n = view @ normal
        if n[2] <= 1e-6:
            continue  # back face (camera looks down -z; visible faces point +z)
        sx = (p[:, 0] - cx) * scale + ss / 2
        sy = ss / 2 - (p[:, 1] - cy) * scale
        tex = texture(model, ref)
        th, tw = tex.shape[:2]
        if glow:
            shade = 1.0
        else:
            shade = 0.55 + 0.45 * max(0.0, float(normal @ LIGHT)) if False else SHADE[face] if not np.any(np.abs(normal - np.round(normal)) > 1e-3) else 0.5 + 0.5 * max(0.0, float(normal @ LIGHT))
        for tri in ((0, 1, 2), (0, 2, 3)):
            ax, ay = sx[tri[0]], sy[tri[0]]
            bx, by = sx[tri[1]], sy[tri[1]]
            qx, qy = sx[tri[2]], sy[tri[2]]
            x_lo, x_hi = int(max(0, math.floor(min(ax, bx, qx)))), int(min(ss - 1, math.ceil(max(ax, bx, qx))))
            y_lo, y_hi = int(max(0, math.floor(min(ay, by, qy)))), int(min(ss - 1, math.ceil(max(ay, by, qy))))
            if x_hi < x_lo or y_hi < y_lo:
                continue
            area = (bx - ax) * (qy - ay) - (qx - ax) * (by - ay)
            if abs(area) < 1e-9:
                continue
            ys, xs = np.mgrid[y_lo:y_hi + 1, x_lo:x_hi + 1]
            px, py = xs + 0.5, ys + 0.5
            w1 = ((px - ax) * (qy - ay) - (qx - ax) * (py - ay)) / area
            w2 = ((bx - ax) * (py - ay) - (px - ax) * (by - ay)) / area
            w0 = 1 - w1 - w2
            inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
            if not inside.any():
                continue
            z = w0 * p[tri[0], 2] + w1 * p[tri[1], 2] + w2 * p[tri[2], 2]
            u = w0 * uvs[tri[0], 0] + w1 * uvs[tri[1], 0] + w2 * uvs[tri[2], 0]
            v = w0 * uvs[tri[0], 1] + w1 * uvs[tri[1], 1] + w2 * uvs[tri[2], 1]
            tx = np.clip((u / 16 * tw).astype(int), 0, tw - 1)
            ty = np.clip((v / 16 * th).astype(int), 0, th - 1)
            col = tex[ty, tx]
            ok = inside & (col[..., 3] > 0.1) & (z > depth[ys, xs])
            if not ok.any():
                continue
            yy, xx = ys[ok], xs[ok]
            depth[yy, xx] = z[ok]
            img[yy, xx] = col[ok][:, :3] * shade
    im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    return im.resize((size, size), Image.LANCZOS)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("models", nargs="+")
    ap.add_argument("--yaw", type=float, default=35)
    ap.add_argument("--pitch", type=float, default=15)
    ap.add_argument("--size", type=int, default=320)
    ap.add_argument("--cols", type=int, default=0)
    args = ap.parse_args()
    bg = (52, 56, 66, 255)
    ims = [render(load_model(m if ":" in m else "steelstorm:item/3d/" + m), args.yaw, args.pitch, args.size, bg) for m in args.models]
    cols = args.cols or len(ims)
    rows = (len(ims) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * args.size, rows * args.size), bg[:3])
    for i, im in enumerate(ims):
        sheet.paste(im, ((i % cols) * args.size, (i // cols) * args.size))
    sheet.save(args.out)


if __name__ == "__main__":
    main()
