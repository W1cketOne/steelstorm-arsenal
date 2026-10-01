package com.steelstorm.arsenal.world;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.block.WeaponRackBlockEntity;
import com.steelstorm.arsenal.entity.TargetDummyEntity;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Builds the Warrior's Outpost in code, right next to world spawn.
 *
 * <p>Layout in local coordinates: x 0..10 across, z 0..8 from the door (z = 0, facing spawn) to the
 * back wall (z = 8). The back half is a roofed armory with the weapon wall and starter chest, the
 * front half is a fenced training yard with target dummies.</p>
 */
public final class OutpostBuilder {
    private static final int WIDTH = 11;
    private static final int DEPTH = 9;
    private static final int MAX_SLOPE = 4;

    private final ServerLevel level;
    private final BlockPos origin;
    private final Direction front;
    private final Direction right;
    private final Direction back;
    private final int groundY;

    private OutpostBuilder(ServerLevel level, BlockPos origin, Direction front, int groundY) {
        this.level = level;
        this.origin = origin;
        this.front = front;
        this.back = front.getOpposite();
        this.right = front.getCounterClockWise();
        this.groundY = groundY;
    }

    /** Finds a dry, fairly flat spot beside spawn, builds the outpost and moves spawn to its door. */
    public static BlockPos placeNearSpawn(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        Site site = findSite(level, spawn);
        if (site == null) {
            // Nothing dry and flat nearby (e.g. an ocean spawn): build on a stone foundation instead.
            Direction front = Direction.WEST;
            BlockPos center = spawn.relative(Direction.EAST, 10);
            int ground = Math.max(level.getSeaLevel() + 1, groundHeight(level, center.getX(), center.getZ()));
            site = new Site(center, front, ground);
            SteelstormArsenal.LOGGER.info("No natural site for the Warrior's Outpost near spawn, building on a foundation");
        }
        OutpostBuilder builder = new OutpostBuilder(level, originFor(site.center, site.front), site.front, site.groundY);
        builder.build();
        BlockPos doorstep = builder.world(5, 0, -2);
        level.setDefaultSpawnPos(doorstep, site.front.getOpposite().toYRot());
        // Spawn exactly at the door so players never land on the roof, inside a wall, or in water.
        level.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, level.getServer());
        SteelstormArsenal.LOGGER.info("Placed the Warrior's Outpost at {}", builder.origin);
        return builder.origin;
    }

    private record Site(BlockPos center, Direction front, int groundY) {
    }

    private static BlockPos originFor(BlockPos center, Direction front) {
        Direction right = front.getCounterClockWise();
        Direction back = front.getOpposite();
        return center.relative(right, -WIDTH / 2).relative(back, -DEPTH / 2);
    }

    @Nullable
    private static Site findSite(ServerLevel level, BlockPos spawn) {
        Site best = null;
        double bestScore = Double.MAX_VALUE;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int distance = 10; distance <= 26; distance += 4) {
            for (int[] dir : directions) {
                double len = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);
                BlockPos center = spawn.offset((int) Math.round(dir[0] / len * distance), 0, (int) Math.round(dir[1] / len * distance));
                // The door faces back towards spawn.
                Direction front = Direction.getNearest(spawn.getX() - center.getX(), 0, spawn.getZ() - center.getZ());
                BlockPos origin = originFor(center, front);
                OutpostBuilder probe = new OutpostBuilder(level, origin, front, 0);
                Integer ground = probe.evaluateGround(spawn);
                if (ground == null) {
                    continue;
                }
                double score = probe.lastSlope * 3.0 + distance * 0.25;
                if (score < bestScore) {
                    bestScore = score;
                    best = new Site(center, front, ground);
                }
            }
            if (best != null && bestScore < 6) {
                break;
            }
        }
        return best;
    }

    private int lastSlope;

    /** Returns the floor height for this footprint, or null if it is wet, too steep, or covers spawn. */
    @Nullable
    private Integer evaluateGround(BlockPos spawn) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        List<Integer> heights = new ArrayList<>();
        for (int x = -1; x <= WIDTH; x++) {
            for (int z = -2; z <= DEPTH; z++) {
                BlockPos column = world(x, 0, z);
                if (Math.abs(column.getX() - spawn.getX()) <= 1 && Math.abs(column.getZ() - spawn.getZ()) <= 1) {
                    return null;
                }
                int h = groundHeight(level, column.getX(), column.getZ());
                BlockPos top = new BlockPos(column.getX(), h - 1, column.getZ());
                if (!level.getFluidState(top).isEmpty() || !level.getFluidState(top.above()).isEmpty()) {
                    return null;
                }
                heights.add(h);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (max - min > MAX_SLOPE) {
            return null;
        }
        lastSlope = max - min;
        heights.sort(Integer::compare);
        return heights.get(heights.size() / 2);
    }

    /** The height of the first free block above the real ground, looking through trees and plants. */
    private static int groundHeight(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
        while (pos.getY() > level.getMinBuildHeight()) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && !state.canBeReplaced()) {
                break;
            }
            pos.move(Direction.DOWN);
        }
        return pos.getY() + 1;
    }

    /** Local (x across, y up from the floor surface, z front-to-back) to world coordinates. */
    private BlockPos world(int x, int y, int z) {
        return origin.relative(right, x).relative(back, z).atY(groundY + y);
    }

    private void set(int x, int y, int z, BlockState state) {
        level.setBlock(world(x, y, z), state, Block.UPDATE_ALL);
    }

    private void build() {
        prepareGround();
        buildFloor();
        buildArmory();
        buildYard();
        decorate();
        fillChest();
        placeWeaponWall();
        placeDummies();
    }

    private void prepareGround() {
        for (int x = -1; x <= WIDTH; x++) {
            for (int z = -2; z <= DEPTH; z++) {
                // Clear trees and terrain above the floor.
                for (int y = 0; y <= 12; y++) {
                    BlockPos pos = world(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && (y <= 6 || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                // Solid foundation down to the real ground, so nothing floats.
                for (int y = -2, depth = 0; depth < 48; y--, depth++) {
                    BlockPos pos = world(x, y, z);
                    if (pos.getY() <= level.getMinBuildHeight()) {
                        break;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (!state.canBeReplaced() && state.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private void buildFloor() {
        for (int x = -1; x <= WIDTH; x++) {
            for (int z = -2; z <= DEPTH; z++) {
                boolean inside = x >= 0 && x < WIDTH && z >= 0 && z < DEPTH;
                BlockState floor;
                if (!inside) {
                    floor = Blocks.GRASS_BLOCK.defaultBlockState();
                } else if (z >= 4) {
                    floor = (x == 0 || x == WIDTH - 1 || z == DEPTH - 1) ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
                } else {
                    floor = Blocks.COARSE_DIRT.defaultBlockState();
                }
                set(x, -1, z, floor);
            }
        }
        // A gravel path from the door to the new spawn point.
        for (int z = -2; z <= 0; z++) {
            for (int x = 4; x <= 6; x++) {
                set(x, -1, z, Blocks.GRAVEL.defaultBlockState());
            }
        }
        set(5, -1, -2, Blocks.STONE_BRICKS.defaultBlockState());
    }

    private void buildArmory() {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        for (int y = 0; y <= 3; y++) {
            for (int x = 0; x < WIDTH; x++) {
                set(x, y, DEPTH - 1, (x + y) % 5 == 0 ? mossy : wall);
            }
            for (int z = 4; z < DEPTH; z++) {
                set(0, y, z, (z + y) % 4 == 0 ? mossy : wall);
                set(WIDTH - 1, y, z, (z + y) % 4 == 1 ? mossy : wall);
            }
            set(0, y, 4, log);
            set(WIDTH - 1, y, 4, log);
            set(0, y, DEPTH - 1, log);
            set(WIDTH - 1, y, DEPTH - 1, log);
        }
        // Windows in the side walls.
        set(0, 1, 6, Blocks.AIR.defaultBlockState());
        set(0, 2, 6, Blocks.AIR.defaultBlockState());
        set(WIDTH - 1, 1, 6, Blocks.AIR.defaultBlockState());
        set(WIDTH - 1, 2, 6, Blocks.AIR.defaultBlockState());
        // Roof over the armory, with a beam along the open edge.
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = -1; x <= WIDTH; x++) {
            for (int z = 3; z <= DEPTH; z++) {
                set(x, 4, z, slab);
            }
            if (x >= 0 && x < WIDTH) {
                set(x, 3, 4, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, right.getAxis()));
            }
        }
    }

    private void buildYard() {
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int z = 0; z <= 3; z++) {
            set(0, 0, z, fence);
            set(WIDTH - 1, 0, z, fence);
        }
        for (int x = 0; x < WIDTH; x++) {
            if (x < 4 || x > 6) {
                set(x, 0, 0, fence);
            }
        }
        // Gate posts with torches.
        set(3, 1, 0, Blocks.TORCH.defaultBlockState());
        set(7, 1, 0, Blocks.TORCH.defaultBlockState());
        set(0, 1, 0, Blocks.TORCH.defaultBlockState());
        set(WIDTH - 1, 1, 0, Blocks.TORCH.defaultBlockState());
        // Hay bales behind the dummies to catch stray blows.
        set(1, 0, 3, Blocks.HAY_BLOCK.defaultBlockState());
        set(WIDTH - 2, 0, 3, Blocks.HAY_BLOCK.defaultBlockState());
        // Sign at the gate.
        BlockPos signPos = world(4, 0, -1);
        level.setBlock(signPos, Blocks.SPRUCE_SIGN.defaultBlockState()
                .setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(front)), Block.UPDATE_ALL);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText text = new SignText()
                    .setMessage(0, Component.literal("Warrior's"))
                    .setMessage(1, Component.literal("Outpost"))
                    .setMessage(2, Component.literal("Read the book"))
                    .setMessage(3, Component.literal("in the chest!"));
            sign.setText(text, true);
            sign.setChanged();
        }
    }

    private void decorate() {
        set(1, 0, DEPTH - 3, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(WIDTH - 2, 0, DEPTH - 3, Blocks.SMITHING_TABLE.defaultBlockState());
        set(1, 0, DEPTH - 2, Blocks.BARREL.defaultBlockState());
        set(WIDTH - 2, 0, DEPTH - 2, Blocks.GRINDSTONE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.GrindstoneBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                .setValue(net.minecraft.world.level.block.GrindstoneBlock.FACING, front));
        set(3, 3, 6, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(7, 3, 6, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // A whetstone to sharpen blades, and a signal brazier to light when night falls.
        set(WIDTH - 2, 0, 5, ModBlocks.WHETSTONE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, right.getOpposite()));
        set(1, 0, 1, ModBlocks.SIGNAL_BRAZIER.get().defaultBlockState());
        // Wall torches on the inside of the back wall corners.
        set(1, 2, 5, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, right));
        set(WIDTH - 2, 2, 5, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, right.getOpposite()));
    }

    private void fillChest() {
        BlockPos chestPos = world(5, 0, DEPTH - 2);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, front), Block.UPDATE_ALL);
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            List<ItemStack> loot = StarterKit.contents();
            for (int i = 0; i < loot.size() && i < chest.getContainerSize(); i++) {
                chest.setItem(i, loot.get(i));
            }
            chest.setChanged();
        }
    }

    private void placeWeaponWall() {
        // Two weapon racks against the back wall showing off the mod's weapons.
        rack(3, new ItemStack[]{
                new ItemStack(ModItems.weapon(WeaponType.GREATSWORD, WeaponTier.IRON).get()),
                new ItemStack(ModItems.weapon(WeaponType.KATANA, WeaponTier.IRON).get()),
                new ItemStack(ModItems.weapon(WeaponType.SPEAR, WeaponTier.IRON).get())});
        rack(7, new ItemStack[]{
                new ItemStack(ModItems.weapon(WeaponType.WARHAMMER, WeaponTier.IRON).get()),
                new ItemStack(ModItems.weapon(WeaponType.SCYTHE, WeaponTier.GOLD).get()),
                new ItemStack(ModItems.weapon(WeaponType.BATTLEAXE, WeaponTier.IRON).get())});
    }

    private void rack(int x, ItemStack[] weapons) {
        BlockPos pos = world(x, 0, DEPTH - 2);
        level.setBlock(pos, ModBlocks.WEAPON_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, front), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) {
            for (int i = 0; i < weapons.length && i < WeaponRackBlockEntity.SLOTS; i++) {
                rack.setWeapon(i, weapons[i]);
            }
        }
    }

    private void placeDummies() {
        for (int x : new int[]{2, WIDTH - 3}) {
            BlockPos pos = world(x, 0, 2);
            TargetDummyEntity dummy = ModEntities.TARGET_DUMMY.get().create(level);
            if (dummy == null) {
                continue;
            }
            float yaw = front.getOpposite().toYRot();
            dummy.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
            dummy.setYHeadRot(yaw);
            dummy.setYBodyRot(yaw);
            level.addFreshEntity(dummy);
        }
    }

    /** Contents of the starter chest, including the controls book. */
    public static final class StarterKit {
        public static List<ItemStack> contents() {
            List<ItemStack> items = new ArrayList<>();
            items.add(controlsBook());
            items.add(new ItemStack(ModItems.weapon(WeaponType.LONGSWORD, WeaponTier.STONE).get()));
            items.add(new ItemStack(ModItems.weapon(WeaponType.DUAL_DAGGERS, WeaponTier.STONE).get()));
            items.add(new ItemStack(ModItems.THROWING_KNIFE.get(), 8));
            items.add(new ItemStack(Items.BREAD, 8));
            items.add(new ItemStack(Items.TORCH, 16));
            return items;
        }

        public static ItemStack controlsBook() {
            List<Filterable<Component>> pages = new ArrayList<>();
            pages.add(page(Component.literal("§lSteelstorm Arsenal§r\n\nWelcome, warrior. Fighting here takes skill, not spam-clicking.\n\nWatch your §9stamina§r bar above your hunger bar: dodging, heavy attacks and abilities all spend it.")));
            pages.add(page(Component.literal("§lAbilities§r\nEvery weapon has three abilities:\n")
                    .append(Component.keybind("key.steelstorm.ability_1")).append(", ")
                    .append(Component.keybind("key.steelstorm.ability_2")).append(" and ")
                    .append(Component.keybind("key.steelstorm.ability_3"))
                    .append(".\n\nHold §oShift§r over a weapon to read what they do. Each has its own cooldown, shown on the bar above your hotbar.")));
            pages.add(page(Component.literal("§lUltimate§r\nLanding hits and taking blows fills the §6ultimate meter§r. When it glows, press ")
                    .append(Component.keybind("key.steelstorm.ultimate"))
                    .append(" to unleash your weapon's ultimate. Legendary weapons have their own.")));
            pages.add(page(Component.literal("§lDodge Roll§r\nPress ")
                    .append(Component.keybind("key.steelstorm.dodge"))
                    .append(" while moving to roll about 3 blocks. You can't be hurt for a moment mid-roll.")));
            pages.add(page(Component.literal("§lParry§r\nHold §oUse§r (right-click) with a melee weapon to guard.\n\nGet hit just as you raise it for a §6perfect parry§r: no damage and the attacker is staggered.")));
            pages.add(page(Component.literal("§lHeavy Attack§r\nSneak while you attack for 1.5x damage and extra knockback.\n\n§lCombos§r\nLanding hits in a row builds your combo counter.")));
            pages.add(page(Component.literal("§lSmithing§r\nUse a weapon on a §lWhetstone§r to sharpen it: +2 damage for 20 hits.\n\nPlace a §lrune§r on a §lRune Forge§r, then use a weapon on it to burn the rune in: fire, frost, storm, venom, blood or wind.")));
            pages.add(page(Component.literal("§lAdventure§r\nExplore to find armories, bandit camps, watchtowers, blacksmiths, crypts, mines, storm shrines, proving grounds and colosseums.\n\nStrike an §lArena Gong§r to fight for its coffer. Light a §lSignal Brazier§r to reveal lurking foes.")));
            pages.add(page(Component.literal("§lTraining Yard§r\nPractice on the target dummies outside: they show the damage of every hit.\n\nAll keys can be changed in Options > Controls > Key Binds > Steelstorm Arsenal.")));
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                    Filterable.passThrough("Warrior's Handbook"), "Outpost Quartermaster", 0, pages, true));
            return book;
        }

        private static Filterable<Component> page(Component text) {
            return Filterable.passThrough(text);
        }

        private StarterKit() {
        }
    }
}
