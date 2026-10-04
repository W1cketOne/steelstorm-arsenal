package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The Colossus Forge: a ring of basalt walls around a scorched arena, with lava channels running
 * into a great anvil. The Forge Colossus stands guard over its forge and the treasure behind it.
 */
public class ColossusForgeBuilder extends Builder {
    public ColossusForgeBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        int c = w / 2;
        groundworkCircle(Blocks.BLACKSTONE.defaultBlockState(), 14, c + 0.5);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                double r = Math.hypot(x - c, z - c);
                if (r > c + 0.5) {
                    continue;
                }
                float n = noise(x, g, z);
                BlockState floor = r < 3.5 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : n < 0.25F ? Blocks.MAGMA_BLOCK.defaultBlockState()
                        : n < 0.6F ? Blocks.BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
                set(x, g, z, floor);
                // Walls of basalt columns, broken at the front for the entrance.
                if (r >= c - 1.5 && !(Math.abs(x - c) <= 2 && z < c)) {
                    int top = 5 + (int) (noise(x, 9, z) * 5);
                    for (int y = g + 1; y <= g + top; y++) {
                        set(x, y, z, noise(x, y, z) < 0.2F ? Blocks.MAGMA_BLOCK.defaultBlockState()
                                : Blocks.POLISHED_BASALT.defaultBlockState());
                    }
                }
            }
        }
        // Four lava channels running from the walls to the central forge, covered by iron grates.
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int i = 4; i < c - 2; i++) {
                int x = c + dir.getStepX() * i;
                int z = c - dir.getStepZ() * i;
                if (dir == Direction.SOUTH) {
                    continue;
                }
                set(x, g - 1, z, Blocks.LAVA);
                set(x, g, z, Blocks.IRON_BARS);
            }
        }
        // The forge: an anvil on a dais with braziers and a furnace bank.
        fill(c - 2, g + 1, c - 2, c + 2, g + 1, c + 2, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        set(c, g + 2, c, Blocks.ANVIL);
        for (int[] p : new int[][]{{c - 2, c - 2}, {c + 2, c - 2}, {c - 2, c + 2}, {c + 2, c + 2}}) {
            set(p[0], g + 2, p[1], ModBlocks.SIGNAL_BRAZIER.get());
        }
        for (int x = c - 3; x <= c + 3; x++) {
            set(x, g + 1, d - 3, facing(Blocks.BLAST_FURNACE, Direction.SOUTH));
        }
        // The spoils behind the forge.
        lockedChest(ModBlocks.CHAMPIONS_COFFER.get(), c, g + 1, d - 5, Direction.SOUTH, ModLootTables.CHAMPIONS_COFFER);
        chest(c - 3, g + 1, d - 5, ModLootTables.STORMSTEEL_MINE);
        chest(c + 3, g + 1, d - 5, ModLootTables.BLACKSMITH);
        rack(c - 5, g + 1, c, Direction.EAST, randomWeapon(com.steelstorm.arsenal.weapon.WeaponTier.DIAMOND),
                randomWeapon(com.steelstorm.arsenal.weapon.WeaponTier.STORMSTEEL), net.minecraft.world.item.ItemStack.EMPTY);
        spawn(ModEntities.FORGE_COLOSSUS.get(), c, g + 1, c - 4);
    }
}
