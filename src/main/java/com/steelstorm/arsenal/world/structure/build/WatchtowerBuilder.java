package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A three-storey stone watchtower held by bandit archers. A ladder climbs through every floor
 * to a crenellated lookout with a Signal Brazier.
 */
public class WatchtowerBuilder extends Builder {
    public WatchtowerBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        groundwork(Blocks.COBBLESTONE.defaultBlockState(), 20);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                set(x, g, z, noise(x, g, z) < 0.35F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        int x0 = 2;
        int x1 = w - 3;
        int z0 = 2;
        int z1 = d - 3;
        int c = w / 2;
        int top = g + 15;
        fill(x0, g, z0, x1, g, z1, Blocks.COBBLESTONE.defaultBlockState());
        for (int y = g + 1; y < top; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                    if (!edge) {
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    boolean door = z == z0 && x == c && y <= g + 2;
                    boolean slit = !corner && (y - g) % 5 == 3 && (x == c || z == c);
                    if (door) {
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                    } else if (slit) {
                        set(x, y, z, Blocks.IRON_BARS);
                    } else {
                        set(x, y, z, corner ? Blocks.SPRUCE_LOG.defaultBlockState() : brick(x, y, z));
                    }
                }
            }
        }
        // Floors with a ladder hole, and the ladder up the back wall.
        for (int floor : new int[]{g + 5, g + 10, top}) {
            fill(x0 + 1, floor, z0 + 1, x1 - 1, floor, z1 - 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
            set(c, floor, z1 - 1, Blocks.AIR.defaultBlockState());
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = g + 1; y <= top + 1; y++) {
            set(c, y, z1 - 1, ladder);
        }
        set(c, top + 1, z1, brick(c, top + 1, z1));
        // Lookout platform overhanging the walls, ringed by crenellations.
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean rim = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
                if (rim) {
                    set(x, top, z, Blocks.STONE_BRICKS);
                    if ((x + z) % 2 == 0) {
                        set(x, top + 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState().setValue(WallBlock.UP, true));
                    } else {
                        set(x, top + 1, z, Blocks.STONE_BRICK_SLAB);
                    }
                } else if (!(x == c && z == z1 - 1)) {
                    set(x, top, z, Blocks.SPRUCE_PLANKS);
                }
            }
        }
        for (int x = x0; x <= x1; x++) {
            set(x, top - 1, z0 - 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true));
            set(x, top - 1, z1 + 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
        }
        for (int z = z0; z <= z1; z++) {
            set(x0 - 1, top - 1, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
            set(x1 + 1, top - 1, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true));
        }
        // The signal fire and a banner pole.
        set(c, top + 1, c - 1, ModBlocks.SIGNAL_BRAZIER.get());
        set(x0, top + 1, z0, Blocks.SPRUCE_FENCE);
        set(x0, top + 2, z0, Blocks.SPRUCE_FENCE);
        banner(x0, top + 3, z0, Blocks.BLACK_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 4), DyeColor.BLACK,
                BannerPatterns.SKULL, DyeColor.WHITE, BannerPatterns.CURLY_BORDER, DyeColor.RED);
        // Inside: lanterns, supplies and the loot.
        set(x0 + 1, g + 4, z0 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(x1 - 1, g + 9, z0 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(x0 + 1, g + 14, z0 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(x0 + 1, g + 1, z1 - 1, Blocks.BARREL);
        set(x1 - 1, g + 1, z1 - 1, Blocks.HAY_BLOCK);
        chest(x1 - 1, g + 6, z1 - 1, ModLootTables.WATCHTOWER);
        set(x0 + 1, g + 6, z1 - 1, Blocks.BARREL);
        set(x0 + 1, g + 11, z0 + 1, Blocks.CRAFTING_TABLE);
        chest(x1 - 1, g + 11, z0 + 1, ModLootTables.WATCHTOWER);
        // Archers on the lookout and a duelist guarding the door.
        spawn(ModEntities.BANDIT_ARCHER.get(), x0 + 1, top + 1, z0 + 1);
        spawn(ModEntities.BANDIT_ARCHER.get(), x1 - 1, top + 1, z1 - 1);
        spawn(ModEntities.BANDIT_DUELIST.get(), c, g + 1, z0 - 1);
    }
}
