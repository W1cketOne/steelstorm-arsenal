"""Weapon sprite definitions (32x32), one builder per weapon archetype, shared tier palettes."""
import math

from pixelart import Canvas, Part, compose, palette

# ----------------------------------------------------------------------------- palettes

BLADE = {
    "stone": palette("#262626", "#585858", "#767676", "#969696", "#bdbdbd"),
    "iron": palette("#2e3034", "#878c92", "#b6bbc0", "#dce0e4", "#ffffff"),
    "golden": palette("#5a3606", "#b47d18", "#dfb02e", "#f7dc66", "#fffbd1"),
    "diamond": palette("#0b3a37", "#15958a", "#30d2bf", "#88f3e2", "#e6fffb"),
    "stormsteel": palette("#121a35", "#33508e", "#5577c8", "#8fb1f4", "#e6f0ff"),
    "netherite": palette("#141016", "#332a33", "#463c45", "#5e525c", "#857883"),
}

GUARD = {
    "stone": palette("#232323", "#474747", "#5f5f5f", "#7b7b7b", "#9a9a9a"),
    "iron": palette("#25262a", "#474a50", "#686d74", "#8f949b", "#bfc4c9"),
    "golden": palette("#4d2e05", "#9a6812", "#cf9a25", "#f0cc52", "#fff2a3"),
    "diamond": palette("#4d2e05", "#9a6812", "#cf9a25", "#f0cc52", "#fff2a3"),
    "stormsteel": palette("#131826", "#29334b", "#3e4b6b", "#5a6a91", "#8394bd"),
    "netherite": palette("#3a2207", "#835317", "#b47a28", "#daa651", "#f6d68f"),
}

GRIP = {
    "stone": palette("#26170b", "#4a2f15", "#654221", "#82582c", "#9d6c37"),
    "iron": palette("#2a180c", "#57351e", "#784a29", "#986236", "#b57b47"),
    "golden": palette("#2a0e0e", "#5a1c1c", "#7b2727", "#9d3737", "#c04f4f"),
    "diamond": palette("#0d1a2a", "#1d3350", "#2c4a71", "#3d6491", "#5a84b2"),
    "stormsteel": palette("#0f0f12", "#242228", "#36323b", "#4a4551", "#655f6e"),
    "netherite": palette("#0d0a0d", "#201920", "#312731", "#443744", "#5b4a5b"),
}

GEM = {
    "golden": palette("#3a0a0a", "#8f1b1b", "#d0352b", "#f57c62", "#ffe0d6"),
    "diamond": palette("#0b3a37", "#16a090", "#3ce0cb", "#a0f8ea", "#ffffff"),
    "stormsteel": palette("#0a3f4d", "#139bb8", "#46dcff", "#a4f4ff", "#ffffff"),
    "netherite": palette("#3a0c08", "#8a1f12", "#c8401f", "#f07a3a", "#ffd0a0"),
}

WOOD = {
    "stone": palette("#25170b", "#4c3016", "#684320", "#87592b", "#a26c35"),
    "iron": palette("#25170b", "#4c3016", "#684320", "#87592b", "#a26c35"),
    "golden": palette("#22140b", "#47291a", "#603823", "#7d4a2f", "#98603d"),
    "diamond": palette("#1c1209", "#3e2814", "#56391d", "#714b27", "#8b5e31"),
    "stormsteel": palette("#141019", "#2c2434", "#3d3248", "#51445f", "#685878"),
    "netherite": palette("#140c0a", "#2d1a14", "#3f251c", "#553326", "#6c4331"),
}

GLOW = {
    "stormsteel": palette("#0a3f4d", "#139bb8", "#46dcff", "#a4f4ff", "#ffffff"),
    "netherite": palette("#3a2207", "#835317", "#b47a28", "#daa651", "#f6d68f"),
}

# Main diagonal for a full-size weapon: grip end bottom-left, tip top-right.
DIAG = Canvas.axis(1.0, 31.0, 31.0, 1.0)


def seg(u0, u1, hw, vc=0.0):
    return lambda u, v: u0 <= u <= u1 and abs(v - vc) <= hw


def taper(u0, u1, hw0, hw1, curve=None):
    def test(u, v):
        if not (u0 <= u <= u1):
            return False
        t = (u - u0) / (u1 - u0)
        hw = hw0 + (hw1 - hw0) * t
        off = curve(u) if curve else 0.0
        return abs(v - off) <= hw
    return test


def disc(uc, vc, r):
    return lambda u, v: (u - uc) ** 2 + (v - vc) ** 2 <= r * r


def union(*tests):
    return lambda u, v: any(t(u, v) for t in tests)


def fuller_line(u0, u1, width=0.45, curve=None):
    def test(u, v):
        off = curve(u) if curve else 0.0
        return u0 <= u <= u1 and abs(v - off) < width
    return test


def gem_or_metal(tier, test, axis):
    if tier in GEM:
        return Part(test, GEM[tier], "gem", axis=axis, on_top=True)
    return Part(test, GUARD[tier], "metal", axis=axis, on_top=True)


def blade_part(tier, test, axis, fuller=None):
    return Part(test, BLADE[tier], "blade", axis=axis, fuller=fuller, speckle=(tier == "stone"))


def accent_vein(tier, test, axis):
    """Glowing vein (stormsteel) or gold inlay (netherite) drawn over the blade."""
    if tier in GLOW:
        return Part(test, GLOW[tier], "flat", axis=axis, on_top=True, tone=3)
    return None


def finish(parts):
    return compose([p for p in parts if p is not None]).image()


# ----------------------------------------------------------------------------- archetypes

def longsword(tier):
    ax = DIAG
    blade = union(seg(13.2, 35.5, 1.85), taper(35.5, 41.6, 1.85, 0.2))
    vein = fuller_line(15.0, 33.0, 0.45) if tier in GLOW else None
    return finish([
        accent_vein(tier, vein, ax) if vein else None,
        gem_or_metal(tier, disc(12.1, 0.0, 0.95), ax) if tier in GEM else None,
        Part(lambda u, v: 11.0 <= u - 0.06 * v * v <= 13.2 and abs(v) <= 4.7, GUARD[tier], "metal", axis=ax),
        Part(seg(3.9, 11.0, 1.05), GRIP[tier], "wrap", axis=ax, band=1.45),
        Part(disc(2.7, 0.0, 1.95), GUARD[tier], "metal", axis=ax),
        blade_part(tier, blade, ax, fuller=fuller_line(14.5, 34.0)),
    ])


def greatsword(tier):
    ax = DIAG
    blade = union(seg(15.0, 36.5, 3.0), taper(36.5, 42.0, 3.0, 0.3))
    vein = union(fuller_line(16.5, 34.0, 0.5)) if tier in GLOW else None
    return finish([
        accent_vein(tier, vein, ax) if vein else None,
        gem_or_metal(tier, disc(13.8, 0.0, 1.15), ax) if tier in GEM else None,
        Part(lambda u, v: 12.4 <= u + 0.07 * v * v <= 15.0 and abs(v) <= 6.4, GUARD[tier], "metal", axis=ax),
        Part(seg(4.1, 12.4, 1.15), GRIP[tier], "wrap", axis=ax, band=1.5),
        Part(disc(2.6, 0.0, 2.2), GUARD[tier], "metal", axis=ax),
        blade_part(tier, blade, ax, fuller=fuller_line(16.5, 34.5, 0.5)),
    ])


def katana(tier):
    ax = DIAG

    def bend(u):
        # Sori: the blade arcs toward the upper-left as it nears the tip.
        return -0.0052 * max(0.0, u - 13.0) ** 2

    blade = taper(14.6, 39.0, 2.05, 1.5, curve=bend)
    tip = lambda u, v: 39.0 <= u <= 42.6 and -1.5 <= v - bend(u) <= 1.5 - (u - 39.0) * 0.95
    hamon = lambda u, v: 15.5 <= u <= 38.5 and 0.2 <= (v - bend(u)) <= 1.1
    return finish([
        accent_vein(tier, fuller_line(16.0, 37.0, 0.35, curve=bend), ax) if tier in GLOW else None,
        Part(seg(13.4, 14.6, 1.75), GUARD["golden"] if tier != "stone" else GUARD["stone"], "metal", axis=ax, on_top=True),
        Part(lambda u, v: (u - 12.6) ** 2 / 1.1 + v * v / 11.0 <= 1.0, GUARD[tier], "metal", axis=ax),
        Part(seg(2.6, 11.6, 1.25), GRIP[tier], "wrap", axis=ax, band=1.2),
        Part(seg(1.0, 2.6, 1.35), GUARD[tier], "metal", axis=ax),
        Part(union(blade, tip), BLADE[tier], "blade", axis=ax, fuller=hamon, speckle=(tier == "stone")),
    ])


def _dagger_parts(tier, ax, back=False):
    blade = union(seg(11.2, 25.0, 1.75), taper(25.0, 33.0, 1.75, 0.2))
    parts = [
        Part(lambda u, v: 9.6 <= u - 0.06 * v * v <= 11.2 and abs(v) <= 3.4, GUARD[tier], "metal", axis=ax),
        Part(seg(3.4, 9.6, 1.05), GRIP[tier], "wrap", axis=ax, band=1.3),
        Part(disc(2.4, 0.0, 1.6), GUARD[tier], "metal", axis=ax),
        blade_part(tier, blade, ax, fuller=fuller_line(12.0, 26.0, 0.4)),
    ]
    if tier in GLOW:
        parts.append(accent_vein(tier, fuller_line(12.5, 24.0, 0.4), ax))
    return parts


def dual_daggers(tier):
    front = Canvas.axis(1.5, 30.5, 28.0, 4.0)
    back = Canvas.axis(30.5, 30.5, 4.0, 4.0)
    return finish(_dagger_parts(tier, back, back=True) + _dagger_parts(tier, front))


def spear(tier):
    ax = DIAG

    def leaf(u, v):
        if not (29.0 <= u <= 42.8):
            return False
        t = (u - 29.0) / 13.8
        if t < 0.35:
            hw = 0.9 + 2.6 * (t / 0.35) ** 0.8
        else:
            hw = 3.5 * (1.0 - (t - 0.35) / 0.65) ** 0.9
        return abs(v) <= max(0.3, hw)

    return finish([
        Part(seg(27.6, 29.6, 1.35), GUARD[tier], "metal", axis=ax),
        Part(union(seg(23.6, 24.9, 1.3), seg(25.5, 26.8, 1.3)), GRIP[tier], "wrap", axis=ax, band=0.65),
        Part(seg(0.6, 2.4, 1.3), GUARD[tier], "metal", axis=ax),
        Part(seg(0.6, 29.0, 0.95), WOOD[tier], "wood", axis=ax),
        blade_part(tier, leaf, ax, fuller=fuller_line(30.0, 40.5, 0.45)),
        accent_vein(tier, fuller_line(31.0, 39.0, 0.4), ax) if tier in GLOW else None,
    ])


ARCHETYPES = {
    "longsword": longsword,
    "greatsword": greatsword,
    "katana": katana,
    "dual_daggers": dual_daggers,
    "spear": spear,
}

TIERS = ["stone", "iron", "golden", "diamond", "stormsteel", "netherite"]
