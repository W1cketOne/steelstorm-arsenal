# Steelstorm Arsenal: build brief

Build me a complete Minecraft Java Edition mod in this repository. Read the whole brief before writing code.

## 1. Basics
- Mod name: Steelstorm Arsenal (mod ID: `steelstorm`, package: `com.steelstorm.arsenal`)
- Minecraft Java 1.21.1, NeoForge (latest stable for 1.21.1), Java 21, Gradle based on the official NeoForge MDK (https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)
- No extra library dependencies
- Use correct 1.21.1 APIs: data components instead of item NBT, data-driven JSON enchantments. If you're unsure a method exists in this version, check the real sources instead of guessing.

## 2. The idea
A combat overhaul that makes fighting take skill instead of spam-clicking. Lots of different weapons that each feel unique, plus stamina, dodging, parrying, special moves, new enemies, a boss, and structures to explore. Fun matters more than realism.

## 3. Weapons
Melee weapons come in Stone, Iron, Gold, Diamond, Netherite, and a new Stormsteel tier. Stats are relative to a vanilla sword of the same tier. Each has a passive and a special ability (Special keybind, costs stamina, has a cooldown).

- LONGSWORD (+0 dmg, 1.6 speed, +0.5 reach): every 3rd hit in a row deals +30%. Special: Rising Slash launches the enemy up.
- GREATSWORD (+3 dmg, 0.8 speed, +1 reach): wider sweep. Special: Ground Cleave hits everything in a cone.
- KATANA (+1 dmg, 1.8 speed, +0.5 reach): crits cause Bleed. Special: Flash Step dashes forward through enemies (not through walls).
- DUAL DAGGERS (-2 dmg, 3.0 speed): double damage from behind. Special: Flurry, 5 quick hits.
- SPEAR (+0 dmg, 1.2 speed, +2 reach): can be thrown. Special: Impale pins an enemy in place.
- WARHAMMER (+4 dmg, 0.7 speed): hits cause Armor Break. Special: Earthquake launches nearby enemies.
- SCYTHE (+2 dmg, 1.0 speed, +1 reach): heals you for 10% of damage dealt. Special: Reap, a 360° spin.
- BATTLEAXE (+3 dmg, 0.9 speed): extra damage to blocking enemies. Special: Whirlwind spin attack.
- CHAKRAM: thrown, bounces between 3 enemies, then returns to you.
- THROWING KNIVES: stack to 16, fast throws, can cause Bleed.

Legendary weapons (not craftable, found in structures and boss drops):
- TEMPEST EDGE (longsword): combo finishers call down lightning.
- RIMECLEAVER (greatsword): freezes enemies.
- VOIDREAVER (scythe): Reap pulls enemies into a vortex.

## 4. Combat mechanics
- STAMINA: 100 max, HUD bar above hunger, regenerates after 1 second of not using it. Used by dodging, heavy attacks, and specials. Store it with NeoForge Data Attachments.
- HEAVY ATTACK: sneak + attack for 1.5x damage and extra knockback.
- DODGE ROLL: Left Alt dashes ~3 blocks with a short invulnerability window. 1s cooldown.
- PARRY: hold right-click with a melee weapon to guard. Getting hit in the first 5 ticks is a perfect parry: no damage, attacker is staggered, cool sound and sparks. After that, guarding just reduces damage.
- STATUS EFFECTS: Bleed (stacking damage over time), Stagger (can't attack, slowed), Armor Break (-4 armor).
- Hits should feel good: particles, sounds, and small screen shake on heavy hits (toggle in config).

## 5. Stormsteel
New ore in mountain biomes (normal and deepslate versions). Smelts into ingots. Tier between diamond and netherite. Stormsteel weapons sometimes zap a second nearby enemy.

## 6. Enemies and boss
Reuse vanilla humanoid models with new textures. Enemies should telegraph big attacks so the player can dodge or parry.
- BANDIT DUELIST: fights with a sword and sometimes parries.
- BANDIT ARCHER: keeps its distance.
- IRON REVENANT: slow armored undead knight with a warhammer.
- BOSS, THE FALLEN WARLORD: 300 health, boss bar, 3 phases (greatsword attacks → summons Revenants → enraged and faster). Drops a random legendary weapon. Beatable with diamond gear and good dodging.

## 7. Structures
This part is important. I want the world to feel full of this mod as soon as I load in.

STARTER STRUCTURE (spawns at world spawn):
- WARRIOR'S OUTPOST: When a NEW world is created, place a small outpost right next to the player's spawn point, one time only. It has a weapon rack wall showing a few mod weapons, a training area with target dummies, a chest with starter weapons (stone longsword, stone daggers, a few throwing knives) and a book explaining the controls (dodge, parry, heavy attack, special).
- Use SavedData so it only ever places once, and make sure it sits on solid ground and doesn't spawn the player inside a wall or in water.

NATURALLY GENERATING STRUCTURES (spread around the world):
- ABANDONED ARMORY (common): small ruined building with loot chests full of mod weapons and Stormsteel.
- BANDIT CAMP (uncommon): tents, campfire, loot, guarded by Bandit Duelists and Archers. Plains, savanna, forest.
- RUINED COLOSSEUM (rare): large arena where the Fallen Warlord spawns. Best loot, small chance of a legendary.

Write all the JSON needed for worldgen (structure, structure set, biome tags, loot tables). Build the structures in code (or generate the .nbt files with a script) so I don't have to build anything by hand. Remind me that structures only generate in new chunks, so I need a new world to see them.

## 8. Other stuff
- TARGET DUMMY: placeable block/entity that shows damage numbers when hit.
- WEAPON RACK: block that displays up to 3 weapons.
- Enchantments: Lacerate (Bleed chance), Executioner (bonus damage on low-health enemies), Momentum (stamina on hit). Vanilla sword enchantments should work on all new melee weapons.
- Keybinds in a "Steelstorm Arsenal" category. HUD shows stamina, special cooldown, and combo counter. Tooltips show stats, passive, and special.
- Creative tab with everything.
- Config for stamina settings, damage multipliers, structure frequency, and screen shake.

## 9. Code rules
- Must work in multiplayer and on dedicated servers. Keybinds send packets, the server checks cooldowns and stamina. Client-only code stays in its own package.
- Write complete code, no "TODO" stubs for core features.
- Use one shared weapon type system and register every type × tier in a loop instead of writing a class for each.
- Use DeferredRegister and data generation for recipes, models, tags, lang, and loot tables.
- Make real pixel-art textures for every item and block (generate them with a Python/Pillow script) so nothing shows the purple-and-black missing texture.

## 10. Phases
Build in phases. It must compile after each one.
1. Setup, registries, creative tab, first 5 weapon types in all tiers, recipes, and the Warrior's Outpost starter structure.
2. Stamina, dodge, parry, heavy attacks, specials, HUD, keybinds, networking.
3. Remaining weapons, status effects, Stormsteel, enchantments, target dummy, weapon rack.
4. Enemies, boss, natural structures, legendary weapons, config.

## 11. How to work
- Run `./gradlew build` after each phase and fix every error until it compiles. If the build can't download NeoForge or Minecraft files, tell me to change the cloud environment's network access to **Full**.
- Commit and push after each phase.
- Keep going through all 4 phases unless something blocks you.
- At the end, write a README.md with beginner steps to run the mod (Windows and Mac): installing Java 21, `gradlew runClient`, and where to find the built .jar to put in a Minecraft mods folder. Also include a checklist of things to test in-game and a list of anything you simplified.

Keep everything original, don't copy other mods, and if something isn't possible, tell me and suggest an alternative.
