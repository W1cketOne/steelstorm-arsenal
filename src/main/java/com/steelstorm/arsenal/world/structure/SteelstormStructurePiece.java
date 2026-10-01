package com.steelstorm.arsenal.world.structure;

import com.steelstorm.arsenal.block.WeaponRackBlockEntity;
import com.steelstorm.arsenal.entity.FallenWarlord;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.registry.ModStructures;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;

/**
 * Builds the Abandoned Armory, Bandit Camp and Ruined Colosseum block by block. Local coordinates
 * are rotated by {@link StructurePiece}'s orientation; local y = {@link #G} is the floor layer.
 * Ruin decisions use a position hash, so the result is identical no matter which chunk builds it.
 */
public class SteelstormStructurePiece extends StructurePiece {
    /** Local y of the floor layer (the bounding box starts two blocks lower for foundations). */
    private static final int G = 2;

    private final SteelstormStructure.Kind kind;

    public SteelstormStructurePiece(SteelstormStructure.Kind kind, BlockPos origin, Direction facing) {
        super(ModStructures.PIECE.get(), 0,
                makeBoundingBox(origin.getX(), origin.getY() - G, origin.getZ(), facing, kind.width, height(kind) + G + 1, kind.depth));
        this.kind = kind;
        setOrientation(facing);
    }

    public SteelstormStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.PIECE.get(), tag);
        this.kind = SteelstormStructure.Kind.valueOf(tag.getString("Kind"));
    }

    private static int height(SteelstormStructure.Kind kind) {
        return switch (kind) {
            case ABANDONED_ARMORY -> 7;
            case BANDIT_CAMP -> 7;
            case RUINED_COLOSSEUM -> 12;
        };
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", kind.name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, net.minecraft.world.level.ChunkPos chunkPos, BlockPos pivot) {
        switch (kind) {
            case ABANDONED_ARMORY -> buildArmory(level, box, random);
            case BANDIT_CAMP -> buildCamp(level, box, random);
            case RUINED_COLOSSEUM -> buildColosseum(level, box, random);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Deterministic 0..1 noise from local coordinates and this structure's position. */
    private float noise(int x, int y, int z) {
        long seed = Mth.getSeed(boundingBox.minX() + x * 31, y * 17 + boundingBox.minY(), boundingBox.minZ() + z * 13);
        return (float) ((seed >>> 16) & 0xFFFF) / 65535.0F;
    }

    private void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        placeBlock(level, state, x, y, z, box);
    }

    /** Solid ground under the footprint and clear air above it, so nothing floats or is buried. */
    private void prepare(WorldGenLevel level, BoundingBox box, BlockState foundation, int clearHeight, boolean circular) {
        int c = kind.width / 2;
        for (int x = 0; x < kind.width; x++) {
            for (int z = 0; z < kind.depth; z++) {
                if (circular && (x - c) * (x - c) + (z - c) * (z - c) > (c + 0.5) * (c + 0.5)) {
                    continue;
                }
                fillColumnDown(level, foundation, x, G - 1, z, box);
                for (int y = G + 1; y <= G + clearHeight; y++) {
                    set(level, box, x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private BlockState brick(int x, int y, int z) {
        float n = noise(x, y, z);
        return n < 0.2F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                : n < 0.4F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
    }

    private void chest(WorldGenLevel level, BoundingBox box, RandomSource random, int x, int y, int z, ResourceKey<LootTable> loot) {
        createChest(level, box, random, x, y, z, loot);
    }

    private void rack(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, ItemStack... weapons) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        set(level, box, x, y, z, ModBlocks.WEAPON_RACK.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
        if (level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) {
            for (int i = 0; i < weapons.length && i < WeaponRackBlockEntity.SLOTS; i++) {
                rack.items().set(i, weapons[i]);
            }
        }
    }

    private <T extends Mob> T spawn(WorldGenLevel level, BoundingBox box, EntityType<T> type, int x, int y, int z) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) {
            return null;
        }
        T mob = type.create(level.getLevel());
        if (mob == null) {
            return null;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0);
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    private ItemStack randomWeapon(RandomSource random, WeaponTier... tiers) {
        WeaponType[] types = WeaponType.values();
        return new ItemStack(ModItems.weapon(types[random.nextInt(types.length)], tiers[random.nextInt(tiers.length)]).get());
    }

    // ------------------------------------------------------------------ Abandoned Armory

    private void buildArmory(WorldGenLevel level, BoundingBox box, RandomSource random) {
        int w = kind.width;
        int d = kind.depth;
        prepare(level, box, Blocks.COBBLESTONE.defaultBlockState(), 7, false);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                float n = noise(x, G, z);
                set(level, box, x, G, z, n < 0.3F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : n < 0.45F ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
            }
        }
        // Crumbling walls: the higher up, the more blocks are missing.
        for (int y = G + 1; y <= G + 4; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                    boolean door = z == 0 && x >= 4 && x <= 6 && y <= G + 3;
                    if (!edge || door) {
                        continue;
                    }
                    boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                    if (corner || noise(x, y, z) > 0.18F + 0.14F * (y - G)) {
                        set(level, box, x, y, z, corner ? Blocks.SPRUCE_LOG.defaultBlockState() : brick(x, y, z));
                    }
                }
            }
        }
        // What is left of the roof.
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (noise(x, G + 5, z) > 0.55F) {
                    set(level, box, x, G + 5, z, slab);
                }
            }
        }
        // Cobwebs in the corners.
        set(level, box, 1, G + 3, 1, Blocks.COBWEB.defaultBlockState());
        set(level, box, w - 2, G + 3, d - 2, Blocks.COBWEB.defaultBlockState());
        set(level, box, w - 2, G + 1, 1, Blocks.COBWEB.defaultBlockState());

        chest(level, box, random, 2, G + 1, d - 2, ModLootTables.ABANDONED_ARMORY);
        chest(level, box, random, w - 3, G + 1, d - 2, ModLootTables.ABANDONED_ARMORY);
        rack(level, box, 5, G + 1, d - 2, Direction.NORTH,
                randomWeapon(random, WeaponTier.STONE, WeaponTier.IRON), ItemStack.EMPTY, randomWeapon(random, WeaponTier.IRON));
        set(level, box, 1, G + 1, 3, Blocks.CHIPPED_ANVIL.defaultBlockState());
        set(level, box, 1, G + 1, 5, Blocks.BARREL.defaultBlockState());
        set(level, box, w - 2, G + 1, 4, Blocks.GRINDSTONE.defaultBlockState());
        if (noise(5, 0, 5) < 0.5F) {
            spawn(level, box, ModEntities.IRON_REVENANT.get(), 5, G + 1, 4);
        }
    }

    // ------------------------------------------------------------------ Bandit Camp

    private void buildCamp(WorldGenLevel level, BoundingBox box, RandomSource random) {
        int w = kind.width;
        prepare(level, box, Blocks.DIRT.defaultBlockState(), 7, false);
        int c = w / 2;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < kind.depth; z++) {
                int dist2 = (x - c) * (x - c) + (z - c) * (z - c);
                float n = noise(x, G, z);
                BlockState ground = dist2 <= 9 ? (n < 0.5F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState())
                        : (n < 0.25F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                set(level, box, x, G, z, ground);
            }
        }
        // Campfire with log seats.
        set(level, box, c, G + 1, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        BlockState logX = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState logZ = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        set(level, box, c - 2, G + 1, c, logZ);
        set(level, box, c + 2, G + 1, c, logZ);
        set(level, box, c, G + 1, c - 2, logX);
        set(level, box, c, G + 1, c + 2, logX);
        // Three tents.
        tent(level, box, 3, 1, Blocks.RED_WOOL.defaultBlockState());
        tent(level, box, w - 4, 1, Blocks.BROWN_WOOL.defaultBlockState());
        tent(level, box, c, kind.depth - 6, Blocks.WHITE_WOOL.defaultBlockState());
        chest(level, box, random, c, G + 1, kind.depth - 3, ModLootTables.BANDIT_CAMP);
        chest(level, box, random, 3, G + 1, 4, ModLootTables.BANDIT_CAMP);
        // Supplies.
        set(level, box, 1, G + 1, c, Blocks.BARREL.defaultBlockState());
        set(level, box, 1, G + 1, c + 1, Blocks.HAY_BLOCK.defaultBlockState());
        set(level, box, w - 2, G + 1, c, Blocks.BARREL.defaultBlockState());
        set(level, box, w - 2, G + 1, c - 1, Blocks.SPRUCE_FENCE.defaultBlockState());
        set(level, box, w - 2, G + 2, c - 1, Blocks.LANTERN.defaultBlockState());
        set(level, box, 1, G + 1, c - 2, Blocks.SPRUCE_FENCE.defaultBlockState());
        set(level, box, 1, G + 2, c - 2, Blocks.LANTERN.defaultBlockState());
        rack(level, box, w - 2, G + 1, c + 2, Direction.WEST,
                randomWeapon(random, WeaponTier.STONE, WeaponTier.IRON), randomWeapon(random, WeaponTier.STONE), ItemStack.EMPTY);
        // The bandits themselves.
        spawn(level, box, ModEntities.BANDIT_DUELIST.get(), c - 3, G + 1, c - 1);
        spawn(level, box, ModEntities.BANDIT_DUELIST.get(), c + 3, G + 1, c + 1);
        spawn(level, box, ModEntities.BANDIT_ARCHER.get(), c - 1, G + 1, c + 3);
        spawn(level, box, ModEntities.BANDIT_ARCHER.get(), c + 1, G + 1, c - 3);
    }

    /** An A-frame tent four blocks long (along z), open at the front, ridge centred on {@code cx}. */
    private void tent(WorldGenLevel level, BoundingBox box, int cx, int z0, BlockState wool) {
        for (int z = z0; z < z0 + 4; z++) {
            set(level, box, cx - 2, G + 1, z, wool);
            set(level, box, cx + 2, G + 1, z, wool);
            set(level, box, cx - 1, G + 2, z, wool);
            set(level, box, cx + 1, G + 2, z, wool);
            set(level, box, cx, G + 3, z, wool);
            set(level, box, cx - 1, G + 1, z, Blocks.AIR.defaultBlockState());
            set(level, box, cx, G + 1, z, z == z0 + 3 ? wool : Blocks.AIR.defaultBlockState());
            set(level, box, cx, G + 2, z, z == z0 + 3 ? wool : Blocks.AIR.defaultBlockState());
            set(level, box, cx + 1, G + 1, z, Blocks.AIR.defaultBlockState());
        }
        set(level, box, cx - 1, G + 1, z0 + 3, wool);
        set(level, box, cx + 1, G + 1, z0 + 3, wool);
        set(level, box, cx + 1, G + 1, z0 + 1, Blocks.RED_CARPET.defaultBlockState());
        set(level, box, cx + 1, G + 1, z0 + 2, Blocks.RED_CARPET.defaultBlockState());
    }

    // ------------------------------------------------------------------ Ruined Colosseum

    private void buildColosseum(WorldGenLevel level, BoundingBox box, RandomSource random) {
        int c = kind.width / 2;
        prepare(level, box, Blocks.STONE_BRICKS.defaultBlockState(), 12, true);
        for (int x = 0; x < kind.width; x++) {
            for (int z = 0; z < kind.depth; z++) {
                double r = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
                if (r > c + 0.5) {
                    continue;
                }
                float n = noise(x, G, z);
                BlockState floor;
                if (r <= 2.5) {
                    floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                } else if (r <= 11) {
                    floor = n < 0.15F ? Blocks.GRAVEL.defaultBlockState() : (n < 0.3F ? Blocks.COARSE_DIRT.defaultBlockState()
                            : Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                } else {
                    floor = brick(x, G, z);
                }
                set(level, box, x, G, z, floor);
                // Outer ring wall with arches and eroded tops.
                if (r >= 13.5 && r <= c + 0.5) {
                    double angle = Math.toDegrees(Math.atan2(z - c, x - c)) + 180.0;
                    int sector = (int) (angle / 30.0);
                    boolean arch = (angle % 30.0) < 7.0 && r > 14.5;
                    boolean collapsed = sector == 4 || sector == 9;
                    int top = collapsed ? 2 : 8 - (int) (noise(x, 99, z) * 3.5F);
                    boolean entrance = sector == 0 && (angle % 30.0) < 9.0;
                    for (int y = G + 1; y <= G + top; y++) {
                        if (entrance && y <= G + 4) {
                            continue;
                        }
                        if (arch && y <= G + 4) {
                            continue;
                        }
                        set(level, box, x, y, z, y == G + top && !collapsed ? Blocks.STONE_BRICK_SLAB.defaultBlockState() : brick(x, y, z));
                    }
                }
                // Stepped stands just inside the wall.
                if (r >= 12 && r < 13.5) {
                    double angle = Math.toDegrees(Math.atan2(z - c, x - c)) + 180.0;
                    if (!(angle % 30.0 < 9.0 && (int) (angle / 30.0) == 0)) {
                        set(level, box, x, G + 1, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
                    }
                }
            }
        }
        // Pillars with soul lanterns.
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + Math.PI / 8;
            int px = c + (int) Math.round(Math.cos(a) * 10);
            int pz = c + (int) Math.round(Math.sin(a) * 10);
            int ph = 3 + (int) (noise(px, 7, pz) * 3);
            for (int y = G + 1; y <= G + ph; y++) {
                set(level, box, px, y, pz, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            }
            if (ph >= 5) {
                set(level, box, px, G + ph + 1, pz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
            }
        }
        // The dais, the boss and the spoils.
        set(level, box, c, G + 1, c, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        chest(level, box, random, c - 3, G + 1, c, ModLootTables.RUINED_COLOSSEUM);
        chest(level, box, random, c + 3, G + 1, c, ModLootTables.RUINED_COLOSSEUM);
        rack(level, box, c, G + 1, c + 3, Direction.NORTH,
                randomWeapon(random, WeaponTier.IRON, WeaponTier.DIAMOND), randomWeapon(random, WeaponTier.IRON), ItemStack.EMPTY);
        FallenWarlord boss = spawn(level, box, ModEntities.FALLEN_WARLORD.get(), c, G + 2, c);
        if (boss != null) {
            boss.setArenaCenter(getWorldPos(c, G + 1, c).immutable());
        }
    }
}
