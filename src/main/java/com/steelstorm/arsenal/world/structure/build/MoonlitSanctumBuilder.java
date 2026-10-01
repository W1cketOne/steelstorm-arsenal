package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The Moonlit Sanctum: a pale circle of quartz and amethyst under open sky, ringed by tall
 * pillars crowned with crystal. The Moonblade Revenant dances at its heart.
 */
public class MoonlitSanctumBuilder extends Builder {
    public MoonlitSanctumBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        int c = w / 2;
        double radius = c - 0.5;
        groundworkCircle(Blocks.CALCITE.defaultBlockState(), 14, radius + 0.5);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                double r = Math.hypot(x - c, z - c);
                if (r > radius) {
                    continue;
                }
                BlockState floor;
                if (r < 2.5) {
                    floor = Blocks.AMETHYST_BLOCK.defaultBlockState();
                } else if (Math.abs(r - 6) < 0.6 || Math.abs(r - radius + 1) < 0.6) {
                    floor = Blocks.PURPUR_BLOCK.defaultBlockState();
                } else {
                    floor = (x + z) % 2 == 0 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
                }
                set(x, g, z, floor);
            }
        }
        // Eight pillars with crystal crowns and hanging soul lanterns.
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int px = c + (int) Math.round(Math.cos(a) * (radius - 2));
            int pz = c + (int) Math.round(Math.sin(a) * (radius - 2));
            int h = 7 + (i % 2) * 2;
            for (int y = g + 1; y <= g + h; y++) {
                set(px, y, pz, Blocks.QUARTZ_PILLAR);
            }
            set(px, g + h + 1, pz, Blocks.AMETHYST_BLOCK);
            set(px, g + h + 2, pz, Blocks.AMETHYST_CLUSTER);
            set(px, g + 1, pz + (pz > c ? -1 : 1), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        // A moon-glass dais at the centre, and the reward on the far side.
        set(c, g + 1, c, Blocks.SEA_LANTERN);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            set(c + dir.getStepX() * 3, g + 1, c - dir.getStepZ() * 3, Blocks.AMETHYST_CLUSTER);
        }
        lockedChest(ModBlocks.CHAMPIONS_COFFER.get(), c, g + 1, d - 3, Direction.SOUTH, ModLootTables.CHAMPIONS_COFFER);
        chest(c - 2, g + 1, d - 3, ModLootTables.KNIGHTS_CRYPT);
        chest(c + 2, g + 1, d - 3, ModLootTables.STORM_SHRINE);
        spawn(ModEntities.MOONBLADE_REVENANT.get(), c, g + 1, c + 2);
    }
}
