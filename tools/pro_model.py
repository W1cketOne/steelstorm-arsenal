"""Model + texture-atlas toolkit for the "pro" weapon models.

Instead of tiling one 16x16 texture over every cube (which stretches it differently on every
face), each face gets its own patch of a per-model texture sheet at a fixed density of
D texels per model unit, and is painted by a material shader that knows where on the weapon
that pixel is. That gives continuous gradients down a blade made of many cubes, crisp bevels
on every edge, wrapped grips whose bands line up, and so on.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

from texlib import R, Ramp, hexc

D = 2  # texels per model unit (32 px per block)

ASSETS = os.path.join(os.path.dirname(__file__), "..", "fabric", "src", "main", "resources", "assets", "steelstorm")
MODEL_DIR = os.path.join(ASSETS, "models", "item", "3d")
TEX_DIR = os.path.join(ASSETS, "textures", "item", "3d", "pro")

FACES = ("north", "south", "east", "west", "up", "down")
NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0 - 0.5
GLOW_TINT = 15  # tint index the client re-emits at full brightness


def face_quad(f, t, face):
    x0, y0, z0 = f
    x1, y1, z1 = t
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


def glow_ramp(core, outer):
    a, b = np.array(hexc(outer)[:3], float), np.array(hexc(core)[:3], float)
    dark = a * 0.45
    cols = [dark * 0.6, dark, a * 0.75, a, (a + b) / 2, b * 0.9 + 25, b]
    return Ramp(*[tuple(int(min(255, max(0, v))) for v in c) + (255,) for c in cols])


# ----------------------------------------------------------------------------- materials

class Mat:
    """A material: a colour ramp plus a shader giving light levels 0..1 per pixel."""

    def __init__(self, kind, ramp, glow=False, bevel=True, noise=0.02, ramp2=None, **p):
        self.kind = kind
        self.ramp = R[ramp] if isinstance(ramp, str) else ramp
        self.ramp2 = R[ramp2] if isinstance(ramp2, str) else ramp2
        self.glow = glow
        self.bevel = bevel
        self.noise = noise
        self.p = p

    def with_(self, **p):
        q = dict(self.p)
        q.update(p)
        return Mat(self.kind, self.ramp, self.glow, self.bevel, self.noise, self.ramp2, **q)


def shade(mat, P, n, face, s, t, rng, Pl=None, nl=None):
    """Light levels (and an optional second-ramp mask) for a face's pixels."""
    k = mat.kind
    p = mat.p
    if p.get("local") and Pl is not None:
        P, n = Pl, nl
    x, y, z = P[..., 0], P[..., 1], P[..., 2]
    mask = None
    flat = face in ("north", "south") if abs(n[2]) > 0.5 else False
    edge_face = abs(n[0]) > 0.5
    if k == "blade":
        if p.get("axis") == "x":
            # Blade running along x: swap roles so "y" is along the blade, "x" across it.
            x, y = y, x
            n = np.array([n[1], n[0], n[2]])
        cx = p.get("cx", 8.0)
        cx = cx(y) if callable(cx) else cx
        hw = p["hw"](y)
        e = np.clip(np.abs(x - cx) / np.maximum(hw, 1e-3), 0, 1.2)
        y0, y1 = p.get("span", (y.min(), y.max() + 1e-3))
        if p.get("flip_g"):
            y0, y1 = y1, y0
        g = np.clip((y - y0) / max(1e-3, y1 - y0), 0, 1)
        side = np.sign(x - cx + 1e-6)  # -1: lit (left) half, +1: shadowed half
        lit = side < 0
        single = p.get("single")  # +1 / -1: only that side is sharpened
        if abs(n[2]) > 0.5:
            if single:
                sharp = side == single
                L = np.where(sharp, 0.6 - 0.12 * e, 0.5 - 0.1 * e)
                L = np.where(~sharp & (e > 0.72), 0.32, L)  # spine
                L = np.where(sharp & (e > 0.84), 0.98, L)  # honed edge
                if p.get("hamon"):
                    wave = 0.5 + 0.07 * np.sin(y * 1.4) + 0.05 * np.sin(y * 3.3 + 1)
                    L = np.where(sharp & (e > wave) & (e <= 0.84), 0.84, L)
                    L = np.where(sharp & (np.abs(e - wave) < 0.06), 0.93, L)
            else:
                L = np.where(lit, 0.6 - 0.1 * e, 0.44 - 0.08 * e)
                L = np.where(e < 0.13, np.where(lit, 0.86, 0.6), L)
                L = np.where((e > 0.7) & (e <= 0.86), np.where(lit, 0.76, 0.28), L)
                L = np.where(e > 0.86, np.where(lit, 0.98, 0.8), L)
            fl = p.get("fuller")
            if fl:
                fy0, fy1, fw = fl
                inside = (np.abs(x - cx) < fw) & (y > fy0) & (y < fy1)
                L = np.where(inside, 0.24, L)
                L = np.where(inside & (x - cx > fw - 0.55), 0.62, L)
            L = L + 0.08 * g - 0.04
            scratch = rng.random(L.shape) < 0.012
            L = np.where(scratch, L + 0.15, L)
        elif abs(n[0]) > 0.5:
            outer = e > 0.75
            facing_lit = n[0] < 0
            if single:
                L = np.where(outer, np.where(side == single, 0.95, 0.42), np.where(facing_lit, 0.66, 0.34))
            else:
                L = np.where(outer, 0.92 if facing_lit else 0.74, 0.7 if facing_lit else 0.34)
        else:
            L = np.full(x.shape, 0.62)
        return L, None
    if k == "metal" or k == "trim":
        L = 0.5 + 0.18 * (1 - t) - 0.1 * s
        streak = np.abs((s + (1 - t)) - 0.9) < 0.12
        L = np.where(streak, L + 0.2, L)
        if k == "trim" and p.get("engrave", True):
            w, h = s.shape[1], s.shape[0]
            if h >= 5 and w >= 3:
                line = (np.abs(t - 0.5) < 0.5 / h) & (s > 0.2) & (s < 0.8)
                L = np.where(line, L - 0.22, L)
            elif w >= 5 and h >= 3:
                line = (np.abs(s - 0.5) < 0.5 / w) & (t > 0.2) & (t < 0.8)
                L = np.where(line, L - 0.22, L)
        rough = p.get("rough", 0)
        if rough:
            L = L + rough * (rng.random(L.shape) - 0.5)
        return L, None
    if k == "grip":
        pitch = p.get("pitch", 2.2)
        ph = ((y + (x + z) * 0.35) / pitch) % 1.0
        L = 0.7 - 0.36 * np.abs(ph - 0.45) * 2
        L = np.where(ph < 0.16, 0.12, L)
        return L, None
    if k == "ito":
        cell = 2.6
        a = ((y / cell) % 1.0) - 0.5
        b = (((x + z) / cell * 0.5) % 1.0) - 0.5
        diamond = np.abs(a) + np.abs(b) * 1.4 < 0.36
        L = np.where(diamond, 0.75 - 0.4 * np.abs(a), 0.42)
        return L, diamond
    if k == "wood":
        cols = (x + z) * D
        grain = np.sin(cols * 1.7 + np.sin(y * 0.6) * 1.5) * 0.12 + np.sin(cols * 0.6) * 0.08
        L = 0.56 + grain - 0.12 * s
        knot = rng.random(L.shape) < 0.01
        L = np.where(knot, 0.2, L)
        return L, None
    if k == "gem":
        r = np.hypot(s - 0.5, t - 0.5)
        L = 0.78 - 0.55 * ((s + t) / 2) + 0.15 * (np.abs(s - t) < 0.12)
        L = np.where((np.abs(s - 0.3) < 0.13) & (np.abs(t - 0.3) < 0.13), 1.0, L)
        L = np.where(r > 0.62, 0.2, L)
        return L, None
    if k == "glow":
        w, h = s.shape[1], s.shape[0]
        if min(w, h) <= 2:
            L = np.full(s.shape, 0.86)
        else:
            L = 0.95 - 0.5 * np.maximum(np.abs(s - 0.5) * 2 * (w > 2), np.abs(t - 0.5) * 2 * (h > 2) * 0.6)
        return L, None
    if k == "cloth":
        L = 0.6 + 0.16 * np.sin((x + z) * 2.4) - 0.2 * (1 - t) * 0 - 0.15 * t
        return L, None
    if k == "stone":
        L = 0.55 + 0.12 * (1 - t) + 0.25 * (rng.random(x.shape) - 0.5)
        return L, None
    raise ValueError(k)


def quantize(L, ramp, ramp2=None, mask=None, dither=True):
    h, w = L.shape
    d = BAYER[np.arange(h)[:, None] % 4, np.arange(w)[None, :] % 4] * 0.07 if dither else 0
    idx = np.clip(np.round(np.clip(L, 0, 1) * 5 + d * 5), 0, 5).astype(int) + 1
    cols = np.array([c for c in ramp.c], dtype=np.uint8)
    out = cols[idx]
    if ramp2 is not None and mask is not None:
        c2 = np.array([c for c in ramp2.c], dtype=np.uint8)
        out = np.where(mask[..., None], c2[idx], out)
    return out


# ----------------------------------------------------------------------------- model

class ProModel:
    def __init__(self, name, seed=1):
        self.name = name
        self.elements = []
        self.rng = np.random.default_rng(seed)

    def box(self, frm, to, mat, rot=None, faces=FACES, side=None, ends=None):
        """A cuboid. `mat` paints north/south, `side` east/west, `ends` up/down (default: mat)."""
        frm = [float(v) for v in frm]
        to = [float(v) for v in to]
        for i in range(3):
            if frm[i] > to[i]:
                frm[i], to[i] = to[i], frm[i]
        for v in frm + to:
            if not -16 <= v <= 32:
                raise ValueError(f"{self.name}: coordinate {v} outside -16..32 ({frm} -> {to})")
        mats = {"north": mat, "south": mat, "east": side or mat, "west": side or mat, "up": ends or side or mat,
                "down": ends or side or mat}
        self.elements.append({"from": frm, "to": to, "rot": rot, "mats": mats, "faces": faces})
        return self

    def mirror_x(self, cx=8.0):
        for el in self.elements:
            f, t = el["from"], el["to"]
            f[0], t[0] = 2 * cx - t[0], 2 * cx - f[0]
            if el["rot"]:
                (o, axis, ang) = el["rot"]
                o = (2 * cx - o[0], o[1], o[2])
                el["rot"] = (o, axis, -ang if axis in ("y", "z") else ang)
            for m in el["mats"].values():
                if m.kind == "blade" and "cx" in m.p:
                    pass
        return self

    # ------------------------------------------------------------------ baking

    def bake(self, display):
        rects = []
        for ei, el in enumerate(self.elements):
            for face in el["faces"]:
                c = np.array(face_quad(el["from"], el["to"], face), dtype=float)
                w = max(1, int(round(np.linalg.norm(c[1] - c[0]) * D)))
                h = max(1, int(round(np.linalg.norm(c[3] - c[0]) * D)))
                rects.append((ei, face, w, h))
        size, pos = pack([(w + 2, h + 2) for (_, _, w, h) in rects])
        atlas = np.zeros((size, size, 4), dtype=np.uint8)
        out_elements = []
        uvs = {}
        for (ei, face, w, h), (px, py) in zip(rects, pos):
            el = self.elements[ei]
            mat = el["mats"][face]
            c = np.array(face_quad(el["from"], el["to"], face), dtype=float)
            n = np.array(NORMALS[face], dtype=float)
            if el["rot"]:
                o, axis, ang = el["rot"]
                m = rot_matrix(axis, ang)
                c = (c - np.array(o)) @ m.T + np.array(o)
                n = m @ n
            s = (np.arange(w) + 0.5) / w
            t = (np.arange(h) + 0.5) / h
            S, T = np.meshgrid(s, t)
            P = c[0] + S[..., None] * (c[1] - c[0]) + T[..., None] * (c[3] - c[0])
            cl = np.array(face_quad(el["from"], el["to"], face), dtype=float)
            Pl = cl[0] + S[..., None] * (cl[1] - cl[0]) + T[..., None] * (cl[3] - cl[0])
            L, mask = shade(mat, P, n, face, S, T, self.rng, Pl, np.array(NORMALS[face], dtype=float))
            L = L + mat.noise * (self.rng.random(L.shape) - 0.5)
            if mat.bevel and mat.kind != "blade" and w >= 2 and h >= 2:
                L[0, :] += 0.16
                L[:, 0] += 0.06
                L[-1, :] -= 0.18
                L[:, -1] -= 0.08
            rgb = quantize(L, mat.ramp, mat.ramp2, mask, dither=mat.kind not in ("gem", "glow"))
            ox, oy = px + 1, py + 1
            atlas[oy:oy + h, ox:ox + w] = rgb
            # Extrude one pixel all round so filtering never bleeds in a neighbour.
            atlas[oy - 1, ox:ox + w] = rgb[0]
            atlas[oy + h, ox:ox + w] = rgb[-1]
            atlas[oy - 1:oy + h + 1, ox - 1] = atlas[oy - 1:oy + h + 1, ox]
            atlas[oy - 1:oy + h + 1, ox + w] = atlas[oy - 1:oy + h + 1, ox + w - 1]
            uvs[(ei, face)] = [ox / size * 16, oy / size * 16, (ox + w) / size * 16, (oy + h) / size * 16]
        for ei, el in enumerate(self.elements):
            faces = {}
            for face in el["faces"]:
                fd = {"uv": [round(v, 4) for v in uvs[(ei, face)]], "texture": "#atlas"}
                if el["mats"][face].glow:
                    fd["tintindex"] = GLOW_TINT
                faces[face] = fd
            e = {"from": [round(v, 3) for v in el["from"]], "to": [round(v, 3) for v in el["to"]], "faces": faces}
            if el["rot"]:
                o, axis, ang = el["rot"]
                e["rotation"] = {"origin": [round(v, 3) for v in o], "axis": axis, "angle": ang}
            if any(m.glow for m in el["mats"].values()):
                e["shade"] = False
            out_elements.append(e)
        os.makedirs(TEX_DIR, exist_ok=True)
        Image.fromarray(atlas, "RGBA").save(os.path.join(TEX_DIR, self.name + ".png"))
        tex = f"steelstorm:item/3d/pro/{self.name}"
        data = {"credit": "Steelstorm Arsenal (generated)", "textures": {"atlas": tex, "particle": tex},
                "elements": out_elements, "display": display}
        os.makedirs(MODEL_DIR, exist_ok=True)
        with open(os.path.join(MODEL_DIR, self.name + ".json"), "w") as f:
            json.dump(data, f, indent=1)
        return len(out_elements), size


def pack(sizes):
    """Shelf-packs rectangles into the smallest square power-of-two sheet; returns (size, positions)."""
    order = sorted(range(len(sizes)), key=lambda i: -sizes[i][1])
    size = 32
    while True:
        pos = [None] * len(sizes)
        x = y = shelf = 0
        ok = True
        for i in order:
            w, h = sizes[i]
            if w > size:
                ok = False
                break
            if x + w > size:
                x, y = 0, y + shelf
                shelf = 0
            if y + h > size:
                ok = False
                break
            pos[i] = (x, y)
            x += w
            shelf = max(shelf, h)
        if ok:
            return size, pos
        size *= 2
