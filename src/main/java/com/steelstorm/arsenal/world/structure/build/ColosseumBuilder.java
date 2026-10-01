package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.entity.FallenWarlord;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A ruined arena where the Fallen Warlord waits. Its Arena Gong calls up challengers; win and the
 * Champion's Coffer on the dais opens.
 */
public class ColosseumBuilder extends Builder {
    public ColosseumBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        int c = w / 2;
        groundworkCircle(Blocks.STONE_BRICKS.defaultBlockState(), 12, c + 0.5);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                double r = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
                if (r > c + 0.5) {
                    continue;
                }
                float n = noise(x, g, z);
                BlockState floor;
                if (r <= 2.5) {
                    floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                } else if (r <= 11) {
                    floor = n < 0.15F ? Blocks.GRAVEL.defaultBlockState() : (n < 0.3F ? Blocks.COARSE_DIRT.defaultBlockState()
                            : Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                } else {
                    floor = brick(x, g, z);
                }
                set(x, g, z, floor);
                double angle = Math.toDegrees(Math.atan2(z - c, x - c)) + 180.0;
                // Outer ring wall with arches and eroded tops; the entrance faces local south (z = 0).
                if (r >= 13.5) {
                    int sector = (int) (angle / 30.0);
                    boolean arch = (angle % 30.0) < 7.0 && r > 14.5;
                    boolean collapsed = sector == 4 || sector == 9;
                    int top = collapsed ? 2 : 8 - (int) (noise(x, 99, z) * 3.5F);
                    boolean entrance = Math.abs(x - c) <= 1 && z < c;
                    for (int y = g + 1; y <= g + top; y++) {
                        if ((entrance || arch) && y <= g + 4) {
                            continue;
                        }
                        set(x, y, z, y == g + top && !collapsed ? Blocks.STONE_BRICK_SLAB.defaultBlockState() : brick(x, y, z));
                    }
                }
                // Stepped stands just inside the wall.
                if (r >= 12 && r < 13.5 && !(Math.abs(x - c) <= 1 && z < c)) {
                    set(x, g + 1, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
                }
            }
        }
        // Pillars with soul lanterns.
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + Math.PI / 8;
            int px = c + (int) Math.round(Math.cos(a) * 10);
            int pz = c + (int) Math.round(Math.sin(a) * 10);
            int ph = 3 + (int) (noise(px, 7, pz) * 3);
            for (int y = g + 1; y <= g + ph; y++) {
                set(px, y, pz, Blocks.CHISELED_STONE_BRICKS);
            }
            if (ph >= 5) {
                set(px, g + ph + 1, pz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
            }
        }
        // The gong by the entrance, and the spoils on the dais.
        set(c, g + 1, 4, facing(ModBlocks.ARENA_GONG.get(), Direction.NORTH));
        set(c, g + 1, c, Blocks.POLISHED_BLACKSTONE);
        lockedChest(ModBlocks.CHAMPIONS_COFFER.get(), c, g + 1, c + 2, Direction.SOUTH, ModLootTables.CHAMPIONS_COFFER);
        chest(c - 3, g + 1, c, ModLootTables.RUINED_COLOSSEUM);
        chest(c + 3, g + 1, c, ModLootTables.RUINED_COLOSSEUM);
        rack(c, g + 1, c + 4, Direction.SOUTH, randomWeapon(WeaponTier.IRON, WeaponTier.DIAMOND), randomWeapon(WeaponTier.IRON),
                ItemStack.EMPTY);
        FallenWarlord boss = spawn(ModEntities.FALLEN_WARLORD.get(), c, g + 2, c);
        if (boss != null) {
            boss.setArenaCenter(world(c, g + 1, c).immutable());
        }
    }
}
