package com.steelstorm.arsenal.world.outpost;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.world.build.BuildContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** Finds a dry, level spot beside the world spawn and builds the Warrior's Outpost there. */
public final class OutpostPlacer {
    private static final int[] SEARCH_RADII = {16, 22, 28, 36, 46, 58};
    private static final int GOOD_ENOUGH_SCORE = 3;

    private OutpostPlacer() {
    }

    private record Site(BlockPos origin, Direction facing, int groundY, int score, boolean dry) {
    }

    public static void place(ServerLevel level, OutpostSavedData data) {
        BlockPos spawn = level.getSharedSpawnPos();
        Site best = null;
        for (int radius : SEARCH_RADII) {
            int steps = Math.max(8, radius / 2);
            for (int i = 0; i < steps; i++) {
                double angle = Math.PI * 2 * i / steps;
                int cx = spawn.getX() + (int) Math.round(Math.cos(angle) * radius);
                int cz = spawn.getZ() + (int) Math.round(Math.sin(angle) * radius);
                Site site = evaluate(level, cx, cz, facingToward(cx, cz, spawn));
                if (best == null || site.score() < best.score()) {
                    best = site;
                }
            }
            if (best != null && best.dry() && best.score() <= GOOD_ENOUGH_SCORE) {
                break;
            }
        }
        if (best == null) {
            return;
        }

        BlockPos origin = best.origin().atY(best.groundY());
        BlockState surface = pickSurface(level, BuildContext.transform(origin, best.facing(),
                OutpostBuilder.WIDTH / 2, 0, OutpostBuilder.DEPTH / 2));
        BlockState subsoil = surface.is(Blocks.SAND) ? Blocks.SANDSTONE.defaultBlockState()
                : surface.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.defaultBlockState()
                : Blocks.DIRT.defaultBlockState();

        BuildContext ctx = new BuildContext(level, origin, best.facing(), null, level.getSeed() ^ 0x5EE15707L,
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        OutpostBuilder.build(ctx, surface, subsoil);

        BlockPos safeSpawn = ctx.pos(OutpostBuilder.SPAWN_X, 1, OutpostBuilder.SPAWN_Z);
        float yaw = ctx.yaw(180.0f);
        BoundingBox bounds = BuildContext.worldBox(origin, best.facing(), 0, -1, 0,
                OutpostBuilder.WIDTH - 1, OutpostBuilder.HEIGHT, OutpostBuilder.DEPTH - 1);
        level.setDefaultSpawnPos(safeSpawn, yaw);
        data.markPlaced(safeSpawn, yaw, bounds);
        SteelstormArsenal.LOGGER.info("Placed the Warrior's Outpost at {} (facing {})", origin, best.facing());
    }

    private static Direction facingToward(int fromX, int fromZ, BlockPos target) {
        int dx = target.getX() - fromX;
        int dz = target.getZ() - fromZ;
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Scores a footprint centred on (cx, cz): height spread plus heavy penalties for water and trees. */
    private static Site evaluate(ServerLevel level, int cx, int cz, Direction facing) {
        BlockPos centerOffset = BuildContext.transform(BlockPos.ZERO, facing, OutpostBuilder.WIDTH / 2, 0, OutpostBuilder.DEPTH / 2);
        BlockPos origin = new BlockPos(cx - centerOffset.getX(), 0, cz - centerOffset.getZ());

        int samples = 0;
        int[] heights = new int[64];
        int wet = 0;
        int trees = 0;
        for (int x = 0; x < OutpostBuilder.WIDTH; x += 3) {
            for (int z = 0; z < OutpostBuilder.DEPTH; z += 3) {
                BlockPos column = BuildContext.transform(origin, facing, x, 0, z);
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) - 1;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ()) - 1;
                BlockState state = level.getBlockState(column.atY(top));
                if (!state.getFluidState().isEmpty() || state.is(BlockTags.ICE)) {
                    wet++;
                }
                if (surface - top > 2) {
                    trees++;
                }
                if (samples < heights.length) {
                    heights[samples++] = top;
                }
            }
        }
        int[] sorted = Arrays.copyOf(heights, samples);
        Arrays.sort(sorted);
        int ground = sorted[samples / 2];
        int spread = sorted[samples - 1] - sorted[0];
        int score = spread + wet * 12 + trees;
        return new Site(origin, facing, ground, score, wet == 0);
    }

    private static BlockState pickSurface(ServerLevel level, BlockPos column) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) - 1;
        BlockState state = level.getBlockState(column.atY(top));
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.PODZOL)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.DIRT)) {
            return state.is(Blocks.GRASS_BLOCK) ? Blocks.GRASS_BLOCK.defaultBlockState() : state;
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    /** Whether a player standing at {@code pos} would be inside (or on top of) the outpost. */
    public static boolean isInside(OutpostSavedData data, @Nullable BlockPos pos) {
        BoundingBox box = data.bounds();
        return box != null && pos != null && box.inflatedBy(1).isInside(pos);
    }
}
