package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A raised circle of storm-blackened stone. Pillars topped with lightning rods ring the Storm
 * Altar at its heart; veins of Stormsteel break through the platform. Offer four ingots at the
 * altar to call down the Storm Herald.
 */
public class StormShrineBuilder extends Builder {
    public StormShrineBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        int c = w / 2;
        double radius = 8.5;
        groundworkCircle(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 10, radius + 0.5);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                double r = Math.hypot(x - c, z - c);
                if (r > radius) {
                    continue;
                }
                float n = noise(x, g, z);
                BlockState top;
                if (r <= 2.5) {
                    top = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                } else if (Math.abs(r - 4.5) < 0.5) {
                    top = Blocks.LAPIS_BLOCK.defaultBlockState();
                } else if (n < 0.06F) {
                    top = ModBlocks.DEEPSLATE_STORMSTEEL_ORE.get().defaultBlockState();
                } else {
                    top = n < 0.3F ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
                }
                // The platform stands one block proud of the ground, with a lower rim.
                set(x, g, z, r > radius - 1 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                if (r <= radius - 1) {
                    set(x, g + 1, z, top);
                }
            }
        }
        // Steps up on all four sides.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int sx = dir.getStepX();
            int sz = -dir.getStepZ();
            for (int i = -1; i <= 1; i++) {
                int x = c + sx * 8 + (sz != 0 ? i : 0);
                int z = c + sz * 8 + (sx != 0 ? i : 0);
                set(x, g + 1, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, dir.getOpposite(), false));
            }
        }
        // Eight pillars with lightning rods; some crumbled.
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int px = c + (int) Math.round(Math.cos(a) * 6.5);
            int pz = c + (int) Math.round(Math.sin(a) * 6.5);
            boolean broken = noise(px, 31, pz) < 0.25F;
            int height = broken ? 2 + (int) (noise(px, 32, pz) * 2) : 5;
            for (int y = g + 2; y < g + 2 + height; y++) {
                set(px, y, pz, y == g + 2 + height - 1 && !broken ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : deepslateBrick(px, y, pz));
            }
            if (!broken) {
                set(px, g + 2 + height, pz, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
            } else {
                set(px + (i % 2 == 0 ? 1 : 0), g + 2, pz + (i % 2 == 0 ? 0 : 1), Blocks.COBBLED_DEEPSLATE_SLAB);
            }
        }
        // The altar on a dais, sea lanterns glowing under glass at the compass points.
        fill(c - 1, g + 1, c - 1, c + 1, g + 1, c + 1, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        set(c, g + 2, c, ModBlocks.STORM_ALTAR.get());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            set(c + dir.getStepX() * 3, g + 1, c - dir.getStepZ() * 3, Blocks.SEA_LANTERN);
        }
        set(c + 2, g + 2, c + 2, ModBlocks.SIGNAL_BRAZIER.get());
        set(c - 2, g + 2, c + 2, Blocks.LIGHT_BLUE_CANDLE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3));
        chest(c - 2, g + 2, c - 2, ModLootTables.STORM_SHRINE);
    }
}
