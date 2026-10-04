#!/usr/bin/env python3
"""Builds the Steelstorm Arsenal showcase world on a running dev server, over RCON.

Start the Fabric dev server on a fresh flat world (see the README's showcase section), then:
    python3 tools/showcase_world.py [--host 127.0.0.1] [--port 25575] [--password test]

It lays out a hub (weapon hall, armoury, bestiary, smithy, curios, welcome sign) south of the
starter outpost and places every structure around it, with paths and floating labels.
"""
import argparse
import json
import math
import socket
import struct
import time

G = -4  # grass level of the showcase's flat world (stand on y = G + 1)
Y = G + 1

TYPES = ["longsword", "greatsword", "katana", "dual_daggers", "spear", "warhammer", "scythe", "battleaxe"]
TYPE_NAMES = {"longsword": "Longswords", "greatsword": "Greatswords", "katana": "Katanas", "dual_daggers": "Dual Daggers",
              "spear": "Spears", "warhammer": "Warhammers", "scythe": "Scythes", "battleaxe": "Battleaxes"}
TIERS = [("stone", "Stone", "gray"), ("iron", "Iron", "white"), ("golden", "Golden", "yellow"), ("diamond", "Diamond", "aqua"),
         ("netherite", "Netherite", "dark_gray"), ("stormsteel", "Stormsteel", "blue")]
LEGENDARY = {"longsword": ("tempest_edge", "Tempest Edge"), "greatsword": ("rimecleaver", "Rimecleaver"),
             "katana": ("moonveil", "Moonveil"), "dual_daggers": ("bloodfang", "Bloodfang"), "spear": ("skypiercer", "Skypiercer"),
             "warhammer": ("earthshaker", "Earthshaker"), "scythe": ("voidreaver", "Voidreaver"), "battleaxe": ("kingsbane", "Kingsbane")}
MYTHIC = {"longsword": ("solaris", "Solaris"), "greatsword": ("worldsplitter", "Worldsplitter"), "katana": ("eclipse", "Eclipse"),
          "dual_daggers": ("venomfang", "Venomfang"), "spear": ("dragonspine", "Dragonspine"), "warhammer": ("starfall", "Starfall"),
          "scythe": ("soulreaper", "Soulreaper"), "battleaxe": ("titanbreaker", "Titanbreaker")}
ARMOUR = [("stormsteel", "Stormsteel Armour", "aqua", "stormsteel_longsword"),
          ("warlord", "Ember Warlord Armour", "gold", "kingsbane"),
          ("voidwalker", "Voidwalker Armour", "light_purple", "voidreaver"),
          ("celestial", "Celestial Armour", "yellow", "solaris"),
          ("dragonscale", "Dragonscale Armour", "red", "dragonspine")]
MOBS_FRONT = [("target_dummy", "Target Dummy", None), ("bandit_duelist", "Bandit Duelist", "iron_longsword"),
              ("bandit_archer", "Bandit Archer", "minecraft:bow"), ("bandit_captain", "Bandit Captain", "diamond_greatsword"),
              ("crypt_knight", "Crypt Knight", "netherite_longsword")]
MOBS_BACK = [("iron_revenant", "Iron Revenant", None), ("fallen_warlord", "The Fallen Warlord  (boss)", None),
             ("storm_herald", "The Storm Herald  (boss)", None), ("forge_colossus", "The Forge Colossus  (boss)", None),
             ("moonblade_revenant", "The Moonblade Revenant  (boss)", None)]
BLOCKS = [("weapon_rack", "Weapon Rack", "[facing=south]"), ("whetstone", "Whetstone", "[facing=south]"),
          ("rune_forge", "Rune Forge", ""), ("arena_gong", "Arena Gong", "[facing=south]"),
          ("champions_coffer", "Champion's Coffer", "[facing=south]"), ("bandit_vault", "Bandit Vault", "[facing=south]"),
          ("legendary_pedestal", "Legendary Pedestal", ""), ("storm_altar", "Storm Altar", "[charge=4]"),
          ("signal_brazier", "Signal Brazier", "[lit=true]"), ("stormsteel_ore", "Stormsteel Ore", ""),
          ("deepslate_stormsteel_ore", "Deepslate Stormsteel Ore", ""), ("stormsteel_block", "Block of Stormsteel", ""),
          ("raw_stormsteel_block", "Block of Raw Stormsteel", "")]
CURIOS = [("ember_rune", "Ember Rune"), ("frost_rune", "Frost Rune"), ("storm_rune", "Storm Rune"), ("venom_rune", "Venom Rune"),
          ("vampiric_rune", "Vampiric Rune"), ("gale_rune", "Gale Rune"), ("stormsteel_ingot", "Stormsteel Ingot"),
          ("raw_stormsteel", "Raw Stormsteel"), ("chakram", "Chakram"), ("throwing_knife", "Throwing Knives"),
          ("explorers_compass", "Explorer's Compass"), ("vault_key", "Vault Key"), ("target_dummy", "Target Dummy")]
STRUCTURES = [("ruined_colosseum", "Ruined Colosseum", 0, 165), ("colossus_forge", "Colossus Forge  (boss lair)", 95, 60),
              ("moonlit_sanctum", "Moonlit Sanctum  (boss lair)", -95, 60), ("bandit_camp", "Bandit Camp", 80, 140),
              ("knights_crypt", "Knight's Crypt", -80, 140), ("storm_shrine", "Storm Shrine", 80, -25),
              ("watchtower", "Watchtower", -70, -25), ("stormsteel_mine", "Stormsteel Mine", 150, 115),
              ("abandoned_armory", "Abandoned Armory", -145, 110), ("blacksmith", "Blacksmith", 40, -55),
              ("proving_grounds", "Proving Grounds", -45, -70)]


class Rcon:
    def __init__(self, host, port, password):
        self.s = socket.create_connection((host, port))
        self._send(1, 3, password)
        self._read()

    def _send(self, i, t, body):
        b = body.encode() + b"\x00\x00"
        self.s.sendall(struct.pack("<iii", len(b) + 8, i, t) + b)

    def _read(self):
        n = struct.unpack("<i", self._recv(4))[0]
        return self._recv(n)[8:-2].decode(errors="replace")

    def _recv(self, n):
        data = b""
        while len(data) < n:
            chunk = self.s.recv(n - len(data))
            if not chunk:
                raise ConnectionError("rcon closed")
            data += chunk
        return data

    def cmd(self, c):
        self._send(2, 2, c)
        return self._read()


def text(t, color="white", bold=False):
    return json.dumps({"text": t, "color": color, "bold": bold})


def label(x, y, z, t, color="white", scale=0.55, bold=False, sub=None, sub_color="gray"):
    comp = [{"text": t, "color": color, "bold": bold}]
    if sub:
        comp.append({"text": "\n" + sub, "color": sub_color, "bold": False})
    j = json.dumps(comp).replace("\\", "\\\\").replace("'", "\\'")
    return (f"summon text_display {x} {y} {z} {{text:'{j}',billboard:\"center\",background:1073741824,shadow:1b,"
            f"alignment:\"center\",line_width:260,transformation:{{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
            f"translation:[0f,0f,0f],scale:[{scale}f,{scale}f,{scale}f]}},Tags:[\"showcase\"]}}")


def quat_y(deg):
    a = math.radians(deg) / 2
    return f"[0f,{math.sin(a):.4f}f,0f,{math.cos(a):.4f}f]"


def item_display(x, y, z, item, scale=0.6, yaw=30, ctx="none", glow=False, billboard="vertical"):
    b = "brightness:{sky:15,block:15}," if glow else ""
    return (f"summon item_display {x} {y} {z} {{item:{{id:\"{item}\",count:1}},item_display:\"{ctx}\",{b}"
            f"billboard:\"{billboard}\","
            f"transformation:{{left_rotation:{quat_y(yaw)},right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],"
            f"scale:[{scale}f,{scale}f,{scale}f]}},Tags:[\"showcase\"]}}")


def build(r, log=print, structures=True):
    cmds = []
    add = cmds.append

    # World rules: always noon, clear skies, nothing spawns or burns.
    for rule in ("doDaylightCycle false", "doWeatherCycle false", "doMobSpawning false", "doFireTick false", "mobGriefing false",
                 "keepInventory true", "announceAdvancements false", "doTraderSpawning false", "doPatrolSpawning false",
                 "doInsomnia false"):
        add(f"gamerule {rule}")
    add("time set 6000")
    add("weather clear")
    add("kill @e[tag=showcase]")
    add("kill @e[type=minecraft:slime]")
    add("kill @e[type=minecraft:item]")
    add("forceload add -48 16 48 104")

    # ------------------------------------------------------------------ hub floor
    x0, x1, z0, z1 = -38, 38, 22, 98
    add(f"fill {x0} {G + 1} {z0} {x1} {G + 12} {z1} air")
    add(f"fill {x0} {G} {z0} {x1} {G} {z1} polished_andesite")
    # Floor pattern: a stone-brick grid with mossy accents.
    for gx in range(x0, x1 + 1, 8):
        add(f"fill {gx} {G} {z0} {gx} {G} {z1} stone_bricks")
    for gz in range(z0, z1 + 1, 8):
        add(f"fill {x0} {G} {gz} {x1} {G} {gz} stone_bricks")
    add(f"fill {x0} {G} {z0} {x1} {G} {z0} stone_bricks")
    add(f"fill {x0} {G} {z1} {x1} {G} {z1} stone_bricks")
    add(f"fill {x0} {G} {z0} {x0} {G} {z1} stone_bricks")
    add(f"fill {x1} {G} {z0} {x1} {G} {z1} stone_bricks")
    # Cross-shaped walkways.
    add(f"fill -2 {G} {z0} 2 {G} {z1} smooth_stone")
    add(f"fill {x0} {G} 58 {x1} {G} 62 smooth_stone")
    add(f"fill -1 {G} {z0} 1 {G} {z1} polished_deepslate")
    add(f"fill {x0} {G} 59 {x1} {G} 61 polished_deepslate")
    # Low wall with lantern posts around the hub.
    for x in range(x0, x1 + 1, 6):
        for z in (z0, z1):
            if abs(x) > 3:
                add(f"setblock {x} {Y} {z} stone_brick_wall")
                add(f"setblock {x} {Y + 1} {z} lantern")
    for z in range(z0, z1 + 1, 6):
        for x in (x0, x1):
            if abs(z - 60) > 3:
                add(f"setblock {x} {Y} {z} stone_brick_wall")
                add(f"setblock {x} {Y + 1} {z} lantern")
    # Planters of flowering azalea along the inside of the wall.
    for x in range(x0 + 3, x1 - 1, 6):
        for z in (z0 + 1, z1 - 1):
            if abs(x) > 6:
                add(f"setblock {x} {G} {z} moss_block")
                add(f"setblock {x} {Y} {z} flowering_azalea")
    for z in range(z0 + 3, z1 - 1, 6):
        for x in (x0 + 1, x1 - 1):
            if abs(z - 60) > 4:
                add(f"setblock {x} {G} {z} moss_block")
                add(f"setblock {x} {Y} {z} flowering_azalea")
    # Entrance arch facing the outpost.
    add(f"fill -4 {Y} {z0} -4 {Y + 5} {z0} polished_blackstone_bricks")
    add(f"fill 4 {Y} {z0} 4 {Y + 5} {z0} polished_blackstone_bricks")
    add(f"fill -4 {Y + 6} {z0} 4 {Y + 6} {z0} polished_blackstone_brick_slab")
    add(f"setblock 0 {Y + 7} {z0} steelstorm:signal_brazier[lit=true]")
    for bx in (-4, 4):
        add(f"setblock {bx} {Y + 7} {z0} lantern")
    add(f"fill -1 {G} 0 1 {G} {z0 - 1} dirt_path")

    # Welcome sign.
    add(label(0, Y + 3.2, z0 + 4, "STEELSTORM ARSENAL", "gold", 1.6, True,
              "Showcase World  -  every weapon, armour set, mob, boss, block and structure", "yellow"))
    add(label(0, Y + 1.8, z0 + 4, "Weapon Hall ahead  |  Armoury & Curios to the left  |  Bestiary to the right", "white", 0.7, False,
              "Structures all around the hub - follow the paths  |  Try the Target Dummy to test damage", "gray"))

    # ------------------------------------------------------------------ weapon hall
    for i, kind in enumerate(TYPES):
        x = -28 + i * 8
        add(label(x, Y + 3.3, 27.5, TYPE_NAMES[kind], "gold", 0.9, True))
        add(f"fill {x - 1} {G} 29 {x + 1} {G} 54 polished_blackstone_bricks")
        items = [(f"{p}_{kind}", f"{n} {TYPE_NAMES[kind][:-1]}" if kind != "dual_daggers" else f"{n} Dual Daggers", c, "quartz_pillar")
                 for p, n, c in TIERS]
        items.append((LEGENDARY[kind][0], LEGENDARY[kind][1], "light_purple", "purpur_pillar"))
        items.append((MYTHIC[kind][0], MYTHIC[kind][1], "gold", "gold_block"))
        for j, (item, name, color, pedestal) in enumerate(items):
            z = 31 + j * 3
            add(f"setblock {x} {Y} {z} {pedestal}")
            add(f"setblock {x} {Y + 1} {z} {'polished_blackstone_slab' if j < 6 else 'smooth_quartz_slab'}")
            add(item_display(x + 0.5, Y + 3.0, z + 0.5, f"steelstorm:{item}", 0.8 if kind != "dual_daggers" else 1.1, 25,
                             glow=j >= 6))
            tag = "LEGENDARY" if j == 6 else ("MYTHIC" if j == 7 else None)
            add(label(x + 0.5, Y + 1.75, z + 0.5 - 1.0, name, color, 0.5, j >= 6, tag, "dark_purple" if j == 6 else "gold"))
        add(f"setblock {x} {Y} 55 lantern")

    # ------------------------------------------------------------------ smithy (blocks)
    add(label(0, Y + 3.0, 64.5, "THE SMITHY  -  blocks of Steelstorm Arsenal", "gold", 0.8, True))
    for i, (block, name, state) in enumerate(BLOCKS):
        x = -30 + i * 5
        if abs(x) <= 2:
            x += 3
        add(f"setblock {x} {G} 65 polished_blackstone_bricks")
        add(f"setblock {x} {Y} 65 steelstorm:{block}{state}")
        if block == "weapon_rack":
            add(f"data merge block {x} {Y} 65 {{}}")
        add(label(x + 0.5, Y + 1.9, 65.5, name, "white", 0.42))
    sx = 33
    add(f"setblock {sx} {Y} 65 steelstorm:sarcophagus[facing=east,part=foot]")
    add(f"setblock {sx + 1} {Y} 65 steelstorm:sarcophagus[facing=east,part=head]")
    add(label(sx + 1, Y + 1.9, 65.5, "Sarcophagus", "white", 0.42))

    # ------------------------------------------------------------------ armoury
    add(label(-18, Y + 3.6, 70, "THE ARMOURY", "gold", 0.9, True, "Five full sets, each with its own powers", "yellow"))
    for i, (setname, name, color, weapon) in enumerate(ARMOUR):
        x = -32 + i * 6
        z = 76
        add(f"fill {x - 1} {G} {z - 1} {x + 1} {G} {z + 1} chiseled_polished_blackstone")
        add(f"setblock {x} {Y} {z} {'gold_block' if setname in ('celestial', 'dragonscale') else 'polished_blackstone'}")
        armor = ",".join(f'{{id:"steelstorm:{setname}_{p}",count:1}}' for p in ("boots", "leggings", "chestplate", "helmet"))
        add(f"summon armor_stand {x + 0.5} {Y + 1} {z + 0.5} {{ShowArms:1b,NoBasePlate:1b,Invulnerable:1b,NoGravity:1b,"
            f"Rotation:[180f,0f],ArmorItems:[{armor}],HandItems:[{{id:\"steelstorm:{weapon}\",count:1}},{{}}],"
            f"Pose:{{RightArm:[-70f,0f,0f],LeftArm:[-10f,0f,-10f]}},Tags:[\"showcase\"]}}")
        add(label(x + 0.5, Y + 3.9, z + 0.5 - 1.5, name, color, 0.5, True))

    # ------------------------------------------------------------------ curios
    add(label(-18, Y + 2.8, 84.5, "CURIOS  -  runes, materials and tools", "gold", 0.7, True))
    for i, (item, name) in enumerate(CURIOS):
        x = -34 + (i % 7) * 5
        z = 87 + (i // 7) * 6
        add(f"setblock {x} {Y} {z} polished_blackstone_wall")
        add(item_display(x + 0.5, Y + 1.9, z + 0.5, f"steelstorm:{item}", 1.4, 0, ctx="fixed"))
        add(label(x + 0.5, Y + 2.8, z + 0.5, name, "white", 0.45))

    # Loadout chests: everything to try out (the world opens in creative too).
    loadouts = [
        [f"steelstorm:{MYTHIC[k][0]}" for k in TYPES] + [f"steelstorm:{LEGENDARY[k][0]}" for k in TYPES]
        + [f"steelstorm:{s}_{p}" for s in ("celestial", "dragonscale") for p in ("helmet", "chestplate", "leggings", "boots")],
        [f"steelstorm:stormsteel_{k}" for k in TYPES] + [f"steelstorm:{c[0]}" for c in CURIOS]
        + [f"steelstorm:{s}_{p}" for s in ("stormsteel", "warlord", "voidwalker") for p in ("helmet", "chestplate", "leggings", "boots")],
    ]
    for n, items in enumerate(loadouts):
        x = -6 + n * 2
        nbt = ",".join(f'{{Slot:{i}b,id:"{it}",count:{16 if "rune" in it or "ingot" in it or "knife" in it else 1}}}'
                       for i, it in enumerate(items[:27]))
        add(f"setblock {x} {Y} 92 chest[facing=north]{{Items:[{nbt}]}}")
    add(label(-4.5, Y + 1.9, 92.5, "Loadout chests", "aqua", 0.5, True, "Mythics, legendaries & armour", "gray"))

    # ------------------------------------------------------------------ bestiary
    add(label(19, Y + 3.6, 69.5, "THE BESTIARY", "gold", 0.9, True, "Every foe - frozen in place, safe to inspect", "yellow"))
    for row, mobs, z, gap in ((0, MOBS_FRONT, 74, 6), (1, MOBS_BACK, 88, 7)):
        for i, (mob, name, held) in enumerate(mobs):
            x = 6 + i * gap
            add(f"fill {x - 1} {G} {z - 1} {x + 1} {G} {z + 1} {'deepslate_tiles' if row == 0 else 'crying_obsidian'}")
            hand = ""
            if held:
                hid = held if ":" in held else f"steelstorm:{held}"
                hand = f",HandItems:[{{id:\"{hid}\",count:1}},{{}}]"
            add(f"summon steelstorm:{mob} {x + 0.5} {Y} {z + 0.5} {{NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Silent:1b,"
                f"Rotation:[180f,0f],Tags:[\"showcase\"]{hand}}}")
            add(label(x + 0.5, Y + (5.6 if row == 1 else 3.0), z + 0.5 - 1.5, name, "red" if row == 1 else "white", 0.5, row == 1))
    # A practice dummy by the entrance, to try weapons on.
    add(f"summon steelstorm:target_dummy 6.5 {Y} 30.5 {{Rotation:[180f,0f],Tags:[\"showcase\"]}}")
    add(label(6.5, Y + 2.6, 30.5, "Practice Dummy", "green", 0.5, True, "hit me - shows damage and DPS", "gray"))

    # ------------------------------------------------------------------ outpost label + spawn
    add(label(6, Y + 7, -5, "Warrior's Outpost", "gold", 0.9, True, "the starter base every new world gets", "yellow"))
    add(f"setworldspawn 0 {Y} {z0 + 2}")

    log(f"hub: {len(cmds)} commands")
    run(r, cmds, log)
    if not structures:
        return

    # ------------------------------------------------------------------ structures
    for sid, name, sx, sz in STRUCTURES:
        placed = False
        for attempt in range(8):
            ox, oz = sx + (attempt % 3) * 16 - 16 * (attempt // 3 % 2), sz + (attempt // 3) * 16
            res = r.cmd(f"place structure steelstorm:{sid} {ox} {Y} {oz}")
            if "Generated" in res or "generated" in res:
                placed = True
                log(f"{sid}: placed at {ox},{oz}")
                sx, sz = ox, oz
                break
        if not placed:
            log(f"{sid}: FAILED ({res})")
            continue
        path(r, sx, sz)
        r.cmd(label(sx, Y + 14, sz, name, "gold", 1.6, True))


def path(r, sx, sz):
    """A dirt path from the hub's nearest edge toward the structure (stopping short of it)."""
    hx = max(-38, min(38, sx))
    hz = max(22, min(98, sz))
    steps = int(max(abs(sx - hx), abs(sz - hz)))
    for k in range(0, max(0, steps - 14)):
        t = k / max(1, steps)
        x = round(hx + (sx - hx) * t)
        z = round(hz + (sz - hz) * t)
        r.cmd(f"fill {x - 1} {G} {z - 1} {x + 1} {G} {z + 1} dirt_path replace grass_block")


def run(r, cmds, log):
    bad = 0
    for c in cmds:
        res = r.cmd(c)
        if any(w in res for w in ("Unknown", "Incorrect", "Expected", "Invalid", "Could not", "Unparseable", "Can't", "not")) \
                and "Changed" not in res and "Summoned" not in res:
            bad += 1
            if bad < 60:
                log(f"! {c[:110]}\n    -> {res[:200]}")
        time.sleep(0.005)
    log(f"done ({bad} warnings)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=25575)
    ap.add_argument("--password", default="test")
    ap.add_argument("--hub-only", action="store_true", help="rebuild the hub without placing structures again")
    a = ap.parse_args()
    build(Rcon(a.host, a.port, a.password), structures=not a.hub_only)


if __name__ == "__main__":
    main()
