package com.steelstorm.arsenal.world.build;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Writes a structure described in local coordinates into a level.
 * <p>
 * Local space: +x is "right", +y is up, +z is the structure's front (where its entrance faces).
 * {@code facing} is the world direction of local +z. Block states are rotated to match.
 * <p>
 * When {@code clip} is set (world generation), nothing outside that box is touched, so a structure
 * piece can be drawn chunk by chunk. All randomness comes from {@link #noise} which depends only on
 * the seed and local position, so every chunk agrees on the result.
 */
public class BuildContext {
    private final WorldGenLevel level;
    private final BlockPos origin;
    private final Direction facing;
    private final Rotation rotation;
    @Nullable
    private final BoundingBox clip;
    private final long seed;
    private final int flags;
    private final List<BlockPos> shapeUpdates = new ArrayList<>();

    public BuildContext(WorldGenLevel level, BlockPos origin, Direction facing, @Nullable BoundingBox clip, long seed, int flags) {
        this.level = level;
        this.origin = origin;
        this.facing = facing;
        this.rotation = rotationFor(facing);
        this.clip = clip;
        this.seed = seed;
        this.flags = flags;
    }

    public static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** World position of a local coordinate. */
    public static BlockPos transform(BlockPos origin, Direction facing, int x, int y, int z) {
        return switch (rotationFor(facing)) {
            case CLOCKWISE_90 -> origin.offset(-z, y, x);
            case CLOCKWISE_180 -> origin.offset(-x, y, -z);
            case COUNTERCLOCKWISE_90 -> origin.offset(z, y, -x);
            default -> origin.offset(x, y, z);
        };
    }

    /** World-space bounding box of a local box. */
    public static BoundingBox worldBox(BlockPos origin, Direction facing, int x1, int y1, int z1, int x2, int y2, int z2) {
        return BoundingBox.fromCorners(transform(origin, facing, x1, y1, z1), transform(origin, facing, x2, y2, z2));
    }

    public WorldGenLevel level() {
        return level;
    }

    public Direction facing() {
        return facing;
    }

    public Rotation rotation() {
        return rotation;
    }

    public BlockPos pos(int x, int y, int z) {
        return transform(origin, facing, x, y, z);
    }

    public boolean canWrite(BlockPos pos) {
        return clip == null || clip.isInside(pos);
    }

    /** Rotates a local horizontal direction into world space. */
    public Direction dir(Direction local) {
        return rotation.rotate(local);
    }

    /** Rotates a local yaw (0 = facing local +z) into world yaw. */
    public float yaw(float localYaw) {
        return localYaw + 90.0f * rotation.ordinal();
    }

    // ------------------------------------------------------------------ randomness

    /** Deterministic value in [0, 1) for a local position and salt. */
    public float noise(int x, int y, int z, int salt) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h ^= y * 0xC2B2AE3D27D4EB4FL;
        h ^= z * 0x165667B19E3779F9L;
        h ^= salt * 0xD6E8FEB86659FD93L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h = h ^ (h >>> 31);
        return (h >>> 40) / (float) (1L << 24);
    }

    public float noise(int x, int y, int z) {
        return noise(x, y, z, 0);
    }

    public RandomSource random(int x, int y, int z) {
        return RandomSource.create(seed ^ ((long) x * 341873128712L + (long) z * 132897987541L + y * 31L));
    }

    // ------------------------------------------------------------------ blocks

    public BlockState get(int x, int y, int z) {
        return level.getBlockState(pos(x, y, z));
    }

    public void set(int x, int y, int z, BlockState state) {
        BlockPos pos = pos(x, y, z);
        if (!canWrite(pos)) {
            return;
        }
        BlockState rotated = state.rotate(rotation);
        level.setBlock(pos, rotated, flags);
        if (needsShapeUpdate(rotated)) {
            shapeUpdates.add(pos);
        }
    }

    public void set(int x, int y, int z, Block block) {
        set(x, y, z, block.defaultBlockState());
    }

    /** Places the state only if the noise roll passes, otherwise leaves the position untouched. */
    public void setChance(int x, int y, int z, BlockState state, float chance, int salt) {
        if (noise(x, y, z, salt) < chance) {
            set(x, y, z, state);
        }
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
    }

    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
    }

    /** Hollow rectangle (walls only) on one layer. */
    public void ring(int x1, int y, int z1, int x2, int z2, BlockState state) {
        for (int x = x1; x <= x2; x++) {
            set(x, y, z1, state);
            set(x, y, z2, state);
        }
        for (int z = z1; z <= z2; z++) {
            set(x1, y, z, state);
            set(x2, y, z, state);
        }
    }

    /**
     * Extends a column downward from local y {@code fromY} until it meets solid ground,
     * so structures never float over dips or water.
     */
    public void foundation(int x, int z, int fromY, BlockState state, int maxDepth) {
        for (int y = fromY; y > fromY - maxDepth; y--) {
            BlockPos pos = pos(x, y, z);
            if (level.isOutsideBuildHeight(pos)) {
                return;
            }
            BlockState existing = level.getBlockState(pos);
            if (!isReplaceableGround(existing)) {
                return;
            }
            set(x, y, z, state);
        }
    }

    public static boolean isReplaceableGround(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty()
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW);
    }

    // ------------------------------------------------------------------ block entities

    public <T extends BlockEntity> Optional<T> blockEntity(int x, int y, int z, Class<T> type) {
        BlockPos pos = pos(x, y, z);
        if (!canWrite(pos)) {
            return Optional.empty();
        }
        BlockEntity be = level.getBlockEntity(pos);
        return type.isInstance(be) ? Optional.of(type.cast(be)) : Optional.empty();
    }

    /** A chest facing a local direction, filled from a loot table when opened. */
    public void lootChest(int x, int y, int z, Direction localFacing, ResourceKey<LootTable> lootTable) {
        set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, localFacing));
        BlockPos pos = pos(x, y, z);
        if (canWrite(pos)) {
            RandomizableContainer.setBlockEntityLootTable(level, random(x, y, z), pos, lootTable);
        }
    }

    public void lootBarrel(int x, int y, int z, ResourceKey<LootTable> lootTable) {
        set(x, y, z, Blocks.BARREL.defaultBlockState());
        BlockPos pos = pos(x, y, z);
        if (canWrite(pos)) {
            RandomizableContainer.setBlockEntityLootTable(level, random(x, y, z), pos, lootTable);
        }
    }

    // ------------------------------------------------------------------ entities

    /**
     * Adds an entity at a local position (block centre). In clipped mode the entity is only
     * added by the chunk that contains it, so it is never duplicated.
     */
    public boolean spawn(Entity entity, double x, double y, double z, float localYaw) {
        BlockPos block = pos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        if (!canWrite(block)) {
            return false;
        }
        double fx = x - Math.floor(x);
        double fz = z - Math.floor(z);
        // Rotate the fractional offset with the structure.
        double wx;
        double wz;
        switch (rotation) {
            case CLOCKWISE_90 -> { wx = 1 - fz; wz = fx; }
            case CLOCKWISE_180 -> { wx = 1 - fx; wz = 1 - fz; }
            case COUNTERCLOCKWISE_90 -> { wx = fz; wz = 1 - fx; }
            default -> { wx = fx; wz = fz; }
        }
        float yaw = yaw(localYaw);
        entity.moveTo(block.getX() + wx, block.getY() + (y - Math.floor(y)), block.getZ() + wz, yaw, 0.0f);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        return level.addFreshEntity(entity);
    }

    // ------------------------------------------------------------------ finishing

    private static boolean needsShapeUpdate(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CrossCollisionBlock || block instanceof WallBlock || block instanceof StairBlock
                || block instanceof FenceGateBlock;
    }

    /** Connects fences, panes, walls and stairs to their neighbours. Call once after building. */
    public void finish() {
        for (BlockPos pos : shapeUpdates) {
            BlockState state = level.getBlockState(pos);
            BlockState updated = Block.updateFromNeighbourShapes(state, level, pos);
            if (updated != state) {
                level.setBlock(pos, updated, flags);
            }
        }
        shapeUpdates.clear();
    }
}
