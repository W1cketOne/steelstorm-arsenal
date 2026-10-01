# Steelstorm Arsenal

A skill-based combat mod for **Minecraft Java 1.21.1** on **NeoForge 21.1.252**. It adds 8 melee weapon types in 6 tiers, thrown weapons, stamina, dodge rolls, parries, heavy attacks, special moves, Stormsteel ore, new enemies, a three-phase boss, and structures. That includes a starter outpost built right next to your spawn point.

The full design brief is in [PROMPT.md](PROMPT.md).

---

## 1. Play it in normal Minecraft (just the .jar)

1. Install **NeoForge 21.1.252** for Minecraft 1.21.1. Download the installer from <https://neoforged.net/>, run it, and choose **Install client**.
2. Open the Minecraft Launcher, pick the **NeoForge 1.21.1** profile, and press **Play** once so it creates its folders. Then close the game.
3. Copy `steelstorm-1.0.0.jar` into your **mods** folder:
   - **Windows:** press `Win + R`, type `%appdata%\.minecraft\mods`, and press Enter.
   - **Mac:** in Finder press `Cmd + Shift + G` and go to `~/Library/Application Support/minecraft/mods`.
   - If the `mods` folder doesn't exist, create it.
4. Start the NeoForge profile again and **create a new world**.

To build the jar yourself, see section 3. The finished file is at `build/libs/steelstorm-1.0.0.jar`.

> **Structures only generate in chunks that haven't been explored yet.** The Warrior's Outpost is only built when a world is first created. Use a **new world** to see everything.

---

## 2. Run it from the source code (for testing and changes)

### Install Java 21

**Windows**
1. Go to <https://adoptium.net/temurin/releases/?version=21>, choose **Windows / x64 / JDK / .msi**, and run the installer.
2. On the "Custom Setup" page, turn on **Set JAVA_HOME variable** (click it and choose "Will be installed on local hard drive").
3. Open a new **Command Prompt** and check: `java -version` should say `21`.

**Mac**
1. Go to <https://adoptium.net/temurin/releases/?version=21> and pick **macOS**, with **aarch64** for Apple Silicon (M1/M2/M3/M4) or **x64** for Intel, and the **.pkg** download. Run it.
2. Open **Terminal** and check: `java -version` should say `21`.

### Get the code and start the game

1. Download this repository. On GitHub use **Code > Download ZIP** and unzip it, or run `git clone`.
2. Open a terminal **inside the project folder**: Command Prompt on Windows (`cd` into the folder), or Terminal on Mac.
3. Start Minecraft with the mod loaded:
   - **Windows:** `gradlew runClient`
   - **Mac:** `./gradlew runClient` (if it says "permission denied", run `chmod +x gradlew` first)

The first run downloads Minecraft and NeoForge, which takes a few minutes. Later runs are much faster. A dedicated server can be started with `gradlew runServer`.

---

## 3. Build the .jar

- **Windows:** `gradlew build`
- **Mac:** `./gradlew build`

The mod is written to **`build/libs/steelstorm-1.0.0.jar`**. Copy that file into a `mods` folder as described in section 1.

If you change recipes, models, tags, translations or loot tables in the `datagen` package, run `gradlew runData` first. It regenerates `src/generated/resources`. If you change textures, run `python3 tools/gen_textures.py` (needs `pip install pillow`).

---

## 4. Controls

| Action | Default key | Notes |
| --- | --- | --- |
| Dodge roll | **Left Alt** | Rolls in the direction you're moving (backwards if standing still). Costs 20 stamina, 1 s cooldown, brief invulnerability. |
| Special ability | **R** | Depends on the weapon in your main hand. Costs stamina, has a cooldown. |
| Guard / parry | **Hold right-click** with a melee weapon | Getting hit in the first 5 ticks of raising your guard is a **perfect parry**: no damage, and the attacker is staggered. After that, guarding halves damage and costs stamina. |
| Heavy attack | **Sneak + attack** | 1.5x damage and extra knockback. Costs 15 stamina. Needs a fully charged swing. |
| Throw spear | **Sneak + hold right-click**, then release | Charge for half a second, then release. Pick the spear back up afterwards. |
| Throw chakram or knife | **Right-click** | The chakram comes back to you. |

All keys can be changed under **Options > Controls > Key Binds > Steelstorm Arsenal**. Settings are under **Mods > Steelstorm Arsenal > Config**.

---

## 5. What's in the mod

**Weapons** (Stone, Iron, Gold, Diamond, Netherite and Stormsteel). Each has a passive and a special:

| Weapon | Stats vs. same-tier sword | Passive | Special |
| --- | --- | --- | --- |
| Longsword | +0 dmg, 1.6 speed, +0.5 reach | Every 3rd hit in a combo deals +30% | Rising Slash: launches the enemy up |
| Greatsword | +3 dmg, 0.8 speed, +1 reach | Wider sweep | Ground Cleave: hits everything in a cone |
| Katana | +1 dmg, 1.8 speed, +0.5 reach | Crits cause Bleed | Flash Step: dash through enemies, stops at walls |
| Dual Daggers | -2 dmg, 3.0 speed | Double damage from behind | Flurry: 5 quick hits |
| Spear | +0 dmg, 1.2 speed, +2 reach | Can be thrown | Impale: pins the enemy in place |
| Warhammer | +4 dmg, 0.7 speed | Hits cause Armor Break | Earthquake: launches nearby enemies |
| Scythe | +2 dmg, 1.0 speed, +1 reach | Heals 10% of damage dealt | Reap: 360° spin |
| Battleaxe | +3 dmg, 0.9 speed | Extra damage to blocking enemies, breaks shields | Whirlwind: 3 spins |

Also: **Chakram** (bounces between 3 enemies, then returns), **Throwing Knives** (stack to 16, can cause Bleed), and the legendary **Tempest Edge** (combo finishers call lightning), **Rimecleaver** (freezes enemies) and **Voidreaver** (Reap pulls enemies into a vortex). Legendaries can't be crafted. They're found in Ruined Colosseums or dropped by the boss.

**Status effects:** Bleed (stacking damage over time), Stagger (slowed, can't land melee hits), Armor Break (-4 armor).

**Enchantments:** Lacerate (chance to Bleed), Executioner (bonus damage below 35% health), Momentum (stamina on hit). All vanilla sword enchantments work on the new melee weapons.

**Stormsteel:** ore and deepslate ore in mountain biomes. Smelt raw Stormsteel into ingots. It's a tier between diamond and netherite, and Stormsteel weapons sometimes zap a second nearby enemy.

**Enemies:** Bandit Duelist (sometimes parries, lunges), Bandit Archer (keeps its distance), Iron Revenant (slow armored knight, ground slam, also spawns at night), and **the Fallen Warlord**. The Warlord has 300 health and a boss bar. Phase 1 is greatsword attacks; Phase 2 summons Revenants; Phase 3 makes him enraged and faster, and he leaps at you. He always drops a random legendary. Big attacks are telegraphed: the enemy stops, raises its weapon overhead, shows angry particles and plays a sound. That's your cue to dodge or parry. After a big attack they pause briefly, which is your opening.

**Structures:**
- **Warrior's Outpost**: built once, next to spawn, when a new world is created. It has weapon racks, target dummies, a starter chest (stone longsword, stone daggers, throwing knives, food) and a controls handbook.
- **Abandoned Armory** (common): a ruined building with loot chests.
- **Bandit Camp** (uncommon): tents, a campfire and loot, guarded by bandits. Found in plains, savanna and forest.
- **Ruined Colosseum** (rare): an arena where the Fallen Warlord waits. Best loot, with a small chance of a legendary.

**Other blocks and items:** the Target Dummy shows the damage of each hit and your DPS. Sneak-punch it with an empty hand to pick it up. The Weapon Rack displays up to 3 weapons. The creative tab **Steelstorm Arsenal** has everything, including spawn eggs.

**Config** (`config/steelstorm-common.toml` and `steelstorm-client.toml`) covers: stamina max, regen and costs; damage multipliers; parry window; dodge timings; structure chances; turning the starter outpost on or off; and screen shake and the combo counter (client).

---

## 6. In-game test checklist

- [ ] New world: you spawn at the door of the Warrior's Outpost, on dry ground, not inside a wall.
- [ ] The outpost chest has the handbook, stone longsword, stone dual daggers and throwing knives. Both weapon racks show weapons.
- [ ] Leave the world and rejoin: no second outpost is built.
- [ ] The stamina bar sits above the hunger bar and refills about a second after you stop using it.
- [ ] Left Alt dodge rolls about 3 blocks. Rolling through an attack takes no damage.
- [ ] Hold right-click as a zombie swings: "Perfect Parry!", sparks, and the zombie is staggered. Holding the guard longer only halves damage.
- [ ] Sneak + fully charged attack: bigger knockback, small screen shake.
- [ ] Hit a target dummy: damage numbers and DPS appear above it.
- [ ] Press R with each weapon type near a mob. Each special works, costs stamina, and the cooldown shows next to the hotbar.
- [ ] Longsword: the combo counter appears by the crosshair, and every 3rd hit flashes.
- [ ] Throw the spear (sneak + hold right-click), the chakram and the knives, then pick them back up.
- [ ] Find Stormsteel ore in mountains (around y 30-120), smelt it, and craft a Stormsteel weapon.
- [ ] Enchant a weapon at an enchanting table: Lacerate, Executioner or Momentum can appear.
- [ ] `/locate structure steelstorm:bandit_camp` (and `abandoned_armory`, `ruined_colosseum`): visit each one.
- [ ] Fight the Fallen Warlord in diamond gear. The boss bar changes color at each phase, Revenants are summoned in Phase 2, he gets faster in Phase 3, and he drops a legendary.
- [ ] Multiplayer: start `gradlew runServer`, join with two clients, and check that stamina and specials work for both.

---

## 7. Things that were simplified

- **Chakram and Throwing Knives** are single items, not one per tier.
- **Sounds** reuse vanilla sound effects (pitched and layered) instead of new audio files.
- **Damage numbers** on the Target Dummy appear in its name tag (with DPS) rather than as floating numbers.
- **Weapon Rack** is a floor-standing rack rather than a wall-mounted one.
- **Enemy models** reuse the vanilla humanoid (zombie-shaped) model with new skins. The boss is scaled up 1.4x. The telegraph is a raised-weapon pose, particles and a sound, not a custom animation.
- **Structures are built in code** (no hand-made `.nbt` files), so the designs are fairly simple. Their spacing is fixed in the structure set JSON, and the config sets the chance that each candidate spot actually gets a structure. Lowering a chance makes that structure rarer; 0 turns it off.
- **Spear throwing** uses sneak + hold right-click, because plain hold right-click is the guard/parry for every melee weapon.
- **Stagger** stops melee hits. It doesn't stop a staggered archer from shooting.
- **Bandits** only appear in Bandit Camps. Iron Revenants also spawn at night in overworld land biomes.
- **Spawn point:** when the outpost is built, the world's `spawnRadius` game rule is set to 0, so everyone spawns exactly at the outpost door.
- **Mod compatibility:** only tested on its own with NeoForge 21.1.252.
