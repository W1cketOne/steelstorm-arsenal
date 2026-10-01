"""Skins for the mod's humanoid enemies, in the 64x64 player layout with both skin layers.

Each body part is painted face by face through small shader functions (base colour, top light,
bottom shadow, folds, trims, rivets). Enemies with glowing eyes or runes also get a *_glow.png
with only those pixels, drawn fully lit by the game.

Run: python3 tools/gen_skins.py
"""
import math
import random

from PIL import Image

from texlib import clamp, hexc, mix, mul, save

# (u, v) of each part's base and overlay boxes in the player skin layout, with box sizes.
PARTS = {
    "head": ((0, 0), (32, 0), (8, 8, 8)),
    "body": ((16, 16), (16, 32), (8, 12, 4)),
    "right_arm": ((40, 16), (40, 32), (4, 12, 4)),
    "left_arm": ((32, 48), (48, 48), (4, 12, 4)),
    "right_leg": ((0, 16), (0, 32), (4, 12, 4)),
    "left_leg": ((16, 48), (0, 48), (4, 12, 4)),
}
FACES = ("top", "bottom", "right", "front", "left", "back")


def face_rects(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.rng = random.Random(seed)

    def paint(self, part, layer, fn):
        """fn(face, x, y, w, h) -> colour tuple, ("glow", colour) or None, for every pixel of the part."""
        base, over, (w, h, d) = PARTS[part]
        u, v = base if layer == 0 else over
        for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    c = fn(face, x, y, fw, fh)
                    if c is None:
                        continue
                    if isinstance(c, tuple) and len(c) == 2 and c[0] == "glow":
                        self.img.putpixel((fx + x, fy + y), c[1])
                        self.glow.putpixel((fx + x, fy + y), c[1])
                    else:
                        self.img.putpixel((fx + x, fy + y), c)

    def save(self, name, glow=False):
        save(self.img, "entity", name + ".png")
        if glow:
            save(self.glow, "entity", name + "_glow.png")


def lit(color, face, y, h, amount=0.18):
    """Top-lit shading: brighter near the top of a face, darker toward the bottom, sides dimmer."""
    f = 1.0 + amount * (0.5 - y / max(1, h - 1))
    if face in ("right", "left"):
        f *= 0.86
    elif face == "back":
        f *= 0.9
    elif face == "bottom":
        f *= 0.7
    elif face == "top":
        f *= 1.08
    return mul(color, f)


def noise(rng, color, amount=0.06):
    return mul(color, 1 + (rng.random() - 0.5) * amount * 2)


def C(h):
    return hexc(h)


# ----------------------------------------------------------------------------- faces

def face_shader(skin_tone, eyes=C("#2a1a10"), whites=C("#f0ece4"), brow=None, mouth=None, beard=None, bandana=None,
                eyepatch=False, scar=False):
    """A 8x8 human face on the head's front, with simple shading elsewhere."""

    def fn(face, x, y, w, h):
        if face != "front":
            return lit(skin_tone, face, y, h, 0.1)
        c = skin_tone
        if y == 3 and x in (1, 2, 5, 6) and brow is not None:
            c = brow
        if y == 4:
            if x in (1, 6):
                c = whites
            if x in (2, 5):
                c = eyes
            if eyepatch and x in (5, 6):
                c = C("#121012")
        if eyepatch and ((y == 3 and x == 4) or (y == 2 and x == 3) or (y == 4 and x == 7)):
            c = C("#121012")
        if y == 5 and x in (3, 4):
            c = mul(skin_tone, 0.85)
        if mouth is not None and y == 6 and 2 <= x <= 5:
            c = mouth
        if beard is not None and (y >= 6 or (y == 5 and x in (0, 1, 6, 7))):
            c = beard[(x + y) % len(beard)]
        if bandana is not None and y >= 5:
            c = bandana[0] if y == 5 else bandana[(x + y) % len(bandana)]
        if scar and x == 6 and y in (2, 3, 5):
            c = mul(skin_tone, 0.7)
        return lit(c, face, y, h, 0.12)

    return fn


def hair_cap(hair, top_rows=2, back_rows=8, sides_rows=3):
    def fn(face, x, y, w, h):
        if face == "top":
            return noise(random.Random(x * 7 + y), hair[(x + y) % len(hair)], 0.05)
        if face == "back" and y < back_rows:
            return lit(hair[(x + y) % len(hair)], face, y, h)
        if face in ("right", "left") and y < sides_rows:
            return lit(hair[(x + y) % len(hair)], face, y, h)
        if face == "front" and y < top_rows:
            return lit(hair[(x + y) % len(hair)], face, y, h)
        return None

    return fn


def combine(*fns):
    """Later shaders paint over earlier ones."""

    def fn(face, x, y, w, h):
        out = None
        for f in fns:
            c = f(face, x, y, w, h)
            if c is not None:
                out = c
        return out

    return fn


def cloth(base, fold=0.12, rng_seed=0, hem=None, trim=None, trim_rows=()):
    rng = random.Random(rng_seed)
    table = {}

    def fn(face, x, y, w, h):
        key = (face, x, y, w, h)
        if key not in table:
            table[key] = rng.random()
        c = base
        if face in ("front", "back", "right", "left") and (x + (1 if face == "back" else 0)) % 3 == 0:
            c = mul(c, 1 - fold)
        if hem is not None and face in ("front", "back", "right", "left") and y == h - 1:
            c = hem
        if trim is not None and face in ("front", "back", "right", "left") and y in trim_rows:
            c = trim
        return lit(mul(c, 1 + (table[key] - 0.5) * 0.08), face, y, h)

    return fn


def plate(base, rivet=None, seed=0, dents=0.0, rust=None, edge=None):
    rng = random.Random(seed)
    table = {}

    def fn(face, x, y, w, h):
        key = (face, x, y, w, h)
        if key not in table:
            table[key] = rng.random()
        c = base
        # Plate seams every 4 pixels down the face.
        if face in ("front", "back", "right", "left") and y % 4 == 3:
            c = mul(c, 0.72)
        elif face in ("front", "back", "right", "left") and y % 4 == 0:
            c = mul(c, 1.18)
        if x == 0 and face == "front":
            c = mul(c, 1.1)
        if edge is not None and face in ("front", "back", "right", "left") and (x == 0 or x == w - 1):
            c = edge
        if rivet is not None and face in ("front", "back") and y % 4 == 1 and x in (0, w - 1):
            c = rivet
        if rust is not None and table[key] < dents:
            c = rust
        return lit(mul(c, 1 + (table[key] - 0.5) * 0.1), face, y, h, 0.22)

    return fn


def mask(fn, faces=None, rows=None, cols=None):
    def out(face, x, y, w, h):
        if faces is not None and face not in faces:
            return None
        if rows is not None and not rows(y, h):
            return None
        if cols is not None and not cols(x, w):
            return None
        return fn(face, x, y, w, h)

    return out


# ----------------------------------------------------------------------------- mobs

def bandit_duelist():
    s = Skin(10)
    skin = C("#c69c6d")
    s.paint("head", 0, combine(face_shader(skin, brow=C("#3b2616"), bandana=[C("#a31f1f"), C("#b52525"), C("#8f1a1a")]),
                               hair_cap([C("#2b1d12"), C("#33231a")])))
    s.paint("head", 1, lambda f, x, y, w, h: (lit(C("#9a1d1d"), f, y, h) if (f in ("back", "right", "left") and y in (5, 6))
                                               or (f == "back" and x in (3, 4) and y == 7) else None))
    shirt = cloth(C("#a32a2a"), rng_seed=1)
    vest = cloth(C("#5a3a1e"), rng_seed=2, hem=C("#3a2412"))

    def body(face, x, y, w, h):
        if face == "front" and 2 <= x <= 5 and y < 9:
            return shirt(face, x, y, w, h)
        if y == 9:
            return C("#c9a227") if (face == "front" and x in (3, 4)) else lit(C("#2a1a0e"), face, y, h)
        if y > 9:
            return lit(C("#3a2a1e"), face, y, h)
        return vest(face, x, y, w, h)

    s.paint("body", 0, body)
    s.paint("body", 1, mask(vest, faces=("front", "back", "left", "right"), rows=lambda y, h: y < 9,
                            cols=lambda x, w: x < 2 or x > w - 3))
    for arm in ("right_arm", "left_arm"):
        s.paint(arm, 0, lambda f, x, y, w, h: lit(C("#a32a2a") if y < 4 else (C("#c69c6d") if y < 10 else C("#3a2414")), f, y, h))
        s.paint(arm, 1, lambda f, x, y, w, h: lit(C("#8f2020"), f, y, h) if y == 4 else None)
    for leg in ("right_leg", "left_leg"):
        s.paint(leg, 0, lambda f, x, y, w, h: lit(C("#3a2a1e") if y < 9 else C("#1e1a18"), f, y, h))
        s.paint(leg, 1, lambda f, x, y, w, h: lit(C("#2a1f18"), f, y, h) if y >= 9 else None)
    s.save("bandit_duelist")


def bandit_archer():
    s = Skin(11)
    skin = C("#c09468")
    hood = [C("#2f4a22"), C("#35522a"), C("#2a4220")]
    face = face_shader(skin, brow=C("#2a1c10"), mouth=C("#7a4a34"))

    def head(f, x, y, w, h):
        c = face(f, x, y, w, h)
        if f == "front" and y < 3:
            return lit(C("#1c2a14"), f, y, h)
        return c

    s.paint("head", 0, head)

    def hood_fn(f, x, y, w, h):
        if f == "front":
            return lit(hood[0], f, y, h) if (y < 2 or x in (0, 7)) else None
        if f == "bottom":
            return None
        return lit(hood[(x + y) % 3], f, y, h)

    s.paint("head", 1, hood_fn)
    tunic = cloth(C("#3d5c2c"), rng_seed=3, hem=C("#2a4220"))

    def body(f, x, y, w, h):
        if f == "front" and (x == y or x == y - 1):
            return lit(C("#5a3a1e"), f, y, h)
        if y == 8:
            return lit(C("#2a1a0e"), f, y, h)
        if f == "back" and 2 <= x <= 4 and y < 8:
            return lit(C("#6b4a2a"), f, y, h)
        return tunic(f, x, y, w, h)

    s.paint("body", 0, body)
    s.paint("body", 1, lambda f, x, y, w, h: (lit(C("#e8e0d0") if y == 0 else C("#7a5432"), f, y, h)
                                               if f == "back" and 2 <= x <= 4 and y < 5 else
                                               (lit(hood[1], f, y, h) if f in ("back",) and y >= 8 else None)))
    for arm in ("right_arm", "left_arm"):
        s.paint(arm, 0, lambda f, x, y, w, h: lit(C("#3d5c2c") if y < 5 else (C("#c09468") if y < 9 else C("#4a321c")), f, y, h))
    for leg in ("right_leg", "left_leg"):
        s.paint(leg, 0, lambda f, x, y, w, h: lit(C("#5a4630") if y < 8 else C("#3a2a1a"), f, y, h))
        s.paint(leg, 1, lambda f, x, y, w, h: lit(C("#33251a"), f, y, h) if y >= 8 and f != "top" else None)
    s.save("bandit_archer")


def bandit_captain():
    s = Skin(15)
    skin = C("#c28f62")
    beard = [C("#1a1210"), C("#221814")]
    s.paint("head", 0, combine(face_shader(skin, brow=C("#120c0a"), beard=beard, eyepatch=True, scar=True),
                               hair_cap([C("#1a1210"), C("#221814")], back_rows=6)))
    hat = [C("#1a1416"), C("#221a1d")]

    def hat_fn(f, x, y, w, h):
        if f == "top":
            return lit(hat[(x + y) % 2], f, y, h)
        if f in ("front", "back", "right", "left"):
            if y < 2:
                return lit(hat[(x + y) % 2], f, y, h)
            if y == 2:
                return lit(C("#c9a227"), f, y, h)
        return None

    s.paint("head", 1, hat_fn)
    coat = C("#5e1717")
    gold = C("#d9b23a")

    def body(f, x, y, w, h):
        if f == "front":
            if 3 <= x <= 4 and y < 9:
                return lit(C("#e8e2d4"), f, y, h)
            if x in (2, 5) and y in (2, 4, 6):
                return gold
            if y == 8:
                return C("#e0bb3a") if x in (3, 4) else lit(C("#151012"), f, y, h)
        if y == 8:
            return lit(C("#151012"), f, y, h)
        return cloth(coat, rng_seed=5)(f, x, y, w, h)

    s.paint("body", 0, body)
    s.paint("body", 1, lambda f, x, y, w, h: (lit(coat, f, y, h) if f in ("back", "right", "left") and y >= 9 else
                                               (gold if f in ("front", "back") and y >= 9 and x in (0, w - 1) else
                                                (lit(coat, f, y, h) if f == "front" and y >= 9 and (x < 3 or x > 4) else None))))
    for arm in ("right_arm", "left_arm"):
        s.paint(arm, 0, lambda f, x, y, w, h: lit(coat if y < 10 else C("#151012"), f, y, h) if not (y == 9) else gold)
        s.paint(arm, 1, lambda f, x, y, w, h: (lit(gold, f, y, h) if y == 0 or (f == "top") else None))
    for leg in ("right_leg", "left_leg"):
        s.paint(leg, 0, lambda f, x, y, w, h: lit(C("#2a2224") if y < 7 else C("#101010"), f, y, h))
        s.paint(leg, 1, lambda f, x, y, w, h: lit(C("#141012"), f, y, h) if y >= 6 and f != "top" else None)
    s.save("bandit_captain")


def iron_revenant():
    s = Skin(12)
    iron = C("#7d7d7d")
    rust = C("#8a5a3a")
    armor = plate(iron, rivet=C("#b0b0b0"), seed=12, dents=0.1, rust=rust)

    def head(f, x, y, w, h):
        if f == "front":
            if y in (3, 4) and 1 <= x <= 6:
                if y == 3 and x in (2, 5):
                    return ("glow", C("#ff3a2a"))
                return C("#121212")
            if y == 6 and x % 2 == 1:
                return C("#2a2a2a")
        return armor(f, x, y, w, h)

    s.paint("head", 0, head)
    s.paint("head", 1, lambda f, x, y, w, h: lit(C("#5a5a5a"), f, y, h) if (f == "top" and x in (3, 4)) or
                                              (f == "front" and y == 0) else None)
    s.paint("body", 0, lambda f, x, y, w, h: lit(C("#4a4a4a"), f, y, h) if y == 8 else armor(f, x, y, w, h))
    s.paint("body", 1, lambda f, x, y, w, h: (lit(C("#3c3c44"), f, y, h) if f in ("front", "back") and y >= 9 and (x + y) % 2 == 0
                                               else None))
    for arm in ("right_arm", "left_arm"):
        s.paint(arm, 0, lambda f, x, y, w, h: lit(C("#9a9a9a") if y < 4 else iron, f, y, h) if y != 11 else lit(C("#3a3a3a"), f, y, h))
        s.paint(arm, 1, lambda f, x, y, w, h: lit(C("#a5a5a5"), f, y, h) if y < 3 else None)
    for leg in ("right_leg", "left_leg"):
        s.paint(leg, 0, armor)
    s.save("iron_revenant", glow=True)


def crypt_knight():
    s = Skin(16)
    steel = C("#3f4652")
    dark = C("#262a32")
    soul = C("#8fb4ff")
    armor = plate(steel, rivet=C("#6b7380"), seed=16, dents=0.06, rust=C("#20242b"), edge=C("#2c313a"))

    def head(f, x, y, w, h):
        if f == "front":
            if y == 3 and 1 <= x <= 6:
                return ("glow", soul) if x in (2, 5) else C("#0a0b0e")
            if y == 4 and 2 <= x <= 5:
                return C("#0a0b0e")
            if x in (3, 4) and y >= 5:
                return lit(C("#59606c"), f, y, h)
        return armor(f, x, y, w, h)

    s.paint("head", 0, head)

    def crest(f, x, y, w, h):
        if f == "top" and x in (3, 4):
            return lit(C("#2c3e7a"), f, y, h)
        if f in ("front", "back") and x in (3, 4) and y == 0:
            return lit(C("#2c3e7a"), f, y, h)
        return None

    s.paint("head", 1, crest)
    tabard = C("#1d2a52")

    def body(f, x, y, w, h):
        if f == "front" and 2 <= x <= 5:
            c = tabard
            if (x, y) in ((3, 3), (4, 3), (3, 4), (4, 4), (2, 4), (5, 4), (3, 5), (4, 5)):
                return ("glow", C("#6f8fe0")) if (x, y) in ((3, 4), (4, 4)) else lit(C("#9aa6c4"), f, y, h)
            return lit(c, f, y, h)
        if f == "back" and 1 <= x <= 6 and y < 11:
            return lit(tabard, f, y, h)
        if y == 8:
            return lit(dark, f, y, h)
        return armor(f, x, y, w, h)

    s.paint("body", 0, body)
    s.paint("body", 1, lambda f, x, y, w, h: (lit(tabard, f, y, h) if f in ("front", "back") and 2 <= x <= 5 and y >= 9 and
                                               not (y == h - 1 and x % 2 == 0) else None))
    for arm in ("right_arm", "left_arm"):
        s.paint(arm, 0, lambda f, x, y, w, h: lit(C("#555d6a") if y < 4 else steel, f, y, h) if y != 11 else lit(dark, f, y, h))
        s.paint(arm, 1, lambda f, x, y, w, h: (lit(C("#59606c"), f, y, h) if y < 4 else
                                               (("glow", soul) if f == "front" and y == 6 and x == 1 else None)))
    for leg in ("right_leg", "left_leg"):
        s.paint(leg, 0, armor)
        s.paint(leg, 1, lambda f, x, y, w, h: lit(C("#4a515d"), f, y, h) if y in (4, 5) and f != "top" else None)
    s.save("crypt_knight", glow=True)


def fallen_warlord():
    s = Skin(13)
    plate_c = C("#2a1416")
    crimson = C("#6e1a1f")
    gold = C("#c9971a")
    armor = plate(plate_c, rivet=gold, seed=13, dents=0.05, rust=C("#1a0c0d"), edge=C("#3a1c1f"))

    def head(f, x, y, w, h):
        if f == "front":
            if y == 3 and x in (2, 5):
                return ("glow", C("#ff8a1a"))
            if y in (3, 4) and 1 <= x <= 6:
                return C("#0c0606")
            if y == 0 or (x in (3, 4) and y >= 5):
                return lit(gold, f, y, h)
        return armor(f, x, y, w, h)

    s.paint("head", 0, head)

    def horns(f, x, y, w, h):
        if f in ("right", "left") and y < 4 and (x + y) in (3, 4) and x < 4:
            return lit(C("#e8dcc0"), f, y, h)
        if f in ("front", "back", "right", "left") and y == 7:
            return gold
        return None

    s.paint("head", 1, horns)

    def body(f, x, y, w, h):
        if f in ("front", "back") and (y in (0, 8) or x in (0, 7)):
            return lit(gold, f, y, h)
        if f == "front" and 2 <= x <= 5 and 2 <= y <= 6:
            return lit(crimson if (x + y) % 2 else mul(crimson, 1.15), f, y, h)
        return armor(f, x, y, w, h)

    s.paint("body", 0, body)
    s.paint("body", 1, lambda f, x, y, w, h: lit(crimson if y < h - 1 else gold, f, y, h) if f == "back" else None)
    for arm in ("right_arm", "left_arm"):
        s.paint(arm, 0, lambda f, x, y, w, h: lit(gold if y < 1 else plate_c, f, y, h) if y != 4 else lit(gold, f, y, h))
        s.paint(arm, 1, lambda f, x, y, w, h: lit(C("#3a1c1f"), f, y, h) if y < 4 else None)
    for leg in ("right_leg", "left_leg"):
        s.paint(leg, 0, lambda f, x, y, w, h: lit(gold, f, y, h) if y >= 10 else armor(f, x, y, w, h))
    s.save("fallen_warlord", glow=True)


def storm_herald():
    s = Skin(17)
    robe = C("#1b2a4a")
    robe_dark = C("#121c33")
    silver = C("#c7d2e6")
    storm = C("#7fd8ff")

    def head(f, x, y, w, h):
        if f == "front":
            if y == 4 and x in (2, 5):
                return ("glow", C("#bff4ff"))
            return lit(C("#070a12"), f, y, h)
        return lit(robe_dark, f, y, h)

    s.paint("head", 0, head)

    def hood(f, x, y, w, h):
        if f == "front":
            if y < 2 or x in (0, 7):
                return lit(robe, f, y, h) if not (y == 1 and 1 <= x <= 6) else lit(silver, f, y, h)
            return None
        if f == "bottom":
            return None
        return lit(robe if (x + y) % 3 else mul(robe, 0.85), f, y, h)

    s.paint("head", 1, hood)

    def body(f, x, y, w, h):
        if f == "front":
            if x in (3, 4):
                if y in (2, 5, 8):
                    return ("glow", storm)
                return lit(silver, f, y, h)
        if y == 7:
            return lit(silver, f, y, h)
        return cloth(robe, rng_seed=17)(f, x, y, w, h)

    s.paint("body", 0, body)
    s.paint("body", 1, lambda f, x, y, w, h: (lit(robe, f, y, h) if f in ("back", "right", "left") and y >= 2 else
                                               (("glow", storm) if f == "back" and x in (3, 4) and y in (3, 6, 9) else None)))
    for arm in ("right_arm", "left_arm"):
        s.paint(arm, 0, lambda f, x, y, w, h: lit(robe if y < 9 else C("#9fb0cc"), f, y, h))
        s.paint(arm, 1, lambda f, x, y, w, h: (lit(silver, f, y, h) if y == 8 else
                                               (lit(robe, f, y, h) if y < 8 else None)))

    def robe_leg(f, x, y, w, h):
        if f == "front" and y in (3, 7) and x in (1, 2):
            return ("glow", storm)
        if y == h - 1:
            return lit(silver, f, y, h)
        return cloth(robe_dark, rng_seed=18)(f, x, y, w, h)

    for leg in ("right_leg", "left_leg"):
        s.paint(leg, 0, robe_leg)
        s.paint(leg, 1, lambda f, x, y, w, h: lit(robe, f, y, h) if f != "top" and f != "bottom" else None)
    s.save("storm_herald", glow=True)


# ----------------------------------------------------------------------------- armour layers

def stormsteel_armor():
    """Armour layer textures (64x32): layer 1 = helmet, chestplate, boots; layer 2 = leggings."""
    pal = [C("#1c326e"), C("#2c4fa1"), C("#3a62b8"), C("#4474cf"), C("#6c9ff0"), C("#a6cbff")]
    gold = C("#d8b240")
    rng = random.Random(5)

    def col(t):
        return pal[max(0, min(len(pal) - 1, int(t * len(pal))))]

    def layer(boxes):
        img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        for (u, v, w, h, d, painter) in boxes:
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                for y in range(fh):
                    for x in range(fw):
                        c = painter(face, x, y, fw, fh)
                        if c is not None:
                            img.putpixel((fx + x, fy + y), c)
        return img

    def metal(face, x, y, w, h):
        t = 0.62 - y / max(1, h) * 0.3 + (rng.random() - 0.5) * 0.08
        if face in ("right", "left"):
            t -= 0.08
        if y % 4 == 3:
            t -= 0.2
        return col(t)

    def helmet(face, x, y, w, h):
        if face == "front":
            if y in (3, 4) and 1 <= x <= 6:
                return C("#0d1630") if not (y == 3 and x in (2, 5)) else C("#7fd8ff")
            if x in (3, 4) and y >= 5:
                return gold
            if y == 0:
                return gold
            if y > 5 and 1 <= x <= 6:
                return None
        if face == "bottom":
            return None
        return metal(face, x, y, w, h)

    def chest(face, x, y, w, h):
        if face == "front":
            if (x, y) in ((3, 2), (4, 2), (3, 3), (4, 3), (2, 3), (5, 3), (3, 4), (4, 4), (3, 5), (4, 5)):
                return C("#7fd8ff") if (x, y) in ((3, 3), (4, 3)) else gold
            if y == 0 or y == h - 1:
                return gold
        return metal(face, x, y, w, h)

    def arm(face, x, y, w, h):
        if y < 5:
            return gold if y == 4 else metal(face, x, y, w, h)
        return None

    def boot(face, x, y, w, h):
        if y >= 7:
            return gold if y == 7 else metal(face, x, y, w, h)
        return None

    def legs(face, x, y, w, h):
        if y >= 9:
            return None
        return gold if y == 0 else metal(face, x, y, w, h)

    layer1 = layer([(0, 0, 8, 8, 8, helmet), (16, 16, 8, 12, 4, chest), (40, 16, 4, 12, 4, arm), (0, 16, 4, 12, 4, boot)])
    layer2 = layer([(16, 16, 8, 12, 4, lambda f, x, y, w, h: (gold if y == 0 else metal(f, x, y, w, h)) if y < 4 else None),
                    (0, 16, 4, 12, 4, legs)])
    save(layer1, "models", "armor", "stormsteel_layer_1.png")
    save(layer2, "models", "armor", "stormsteel_layer_2.png")


def main():
    bandit_duelist()
    bandit_archer()
    bandit_captain()
    iron_revenant()
    crypt_knight()
    fallen_warlord()
    storm_herald()
    stormsteel_armor()
    print("Skins and armour layers written")


if __name__ == "__main__":
    main()
