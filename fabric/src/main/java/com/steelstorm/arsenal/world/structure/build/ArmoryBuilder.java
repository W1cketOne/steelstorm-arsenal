package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** A small crumbling stone armory with a couple of chests, a weapon rack and a whetstone. */
public class ArmoryBuilder extends Builder {
    public ArmoryBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        groundwork(Blocks.COBBLESTONE.defaultBlockState(), 7);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                float n = noise(x, g, z);
                set(x, g, z, n < 0.3F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                        : n < 0.45F ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
            }
        }
        // Crumbling walls: the higher up, the more blocks are missing.
        for (int y = g + 1; y <= g + 4; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                    boolean door = z == 0 && x >= 4 && x <= 6 && y <= g + 3;
                    if (!edge || door) {
                        continue;
                    }
                    boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                    if (corner || noise(x, y, z) > 0.18F + 0.14F * (y - g)) {
                        set(x, y, z, corner ? Blocks.SPRUCE_LOG.defaultBlockState() : brick(x, y, z));
                    }
                }
            }
        }
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (noise(x, g + 5, z) > 0.55F) {
                    set(x, g + 5, z, slab);
                }
            }
        }
        set(1, g + 3, 1, Blocks.COBWEB);
        set(w - 2, g + 3, d - 2, Blocks.COBWEB);
        set(w - 2, g + 1, 1, Blocks.COBWEB);

        chest(2, g + 1, d - 2, ModLootTables.ABANDONED_ARMORY);
        chest(w - 3, g + 1, d - 2, ModLootTables.ABANDONED_ARMORY);
        rack(5, g + 1, d - 2, Direction.SOUTH, randomWeapon(WeaponTier.STONE, WeaponTier.IRON), ItemStack.EMPTY,
                randomWeapon(WeaponTier.IRON));
        statue(1, g + 1, d - 2, Direction.SOUTH, ItemStack.EMPTY, new ItemStack(Items.CHAINMAIL_CHESTPLATE), ItemStack.EMPTY,
                ItemStack.EMPTY, randomWeapon(WeaponTier.STONE));
        set(1, g + 1, 3, Blocks.CHIPPED_ANVIL);
        set(1, g + 1, 5, Blocks.BARREL);
        set(w - 2, g + 1, 4, facing(ModBlocks.WHETSTONE.get(), Direction.WEST));
        if (noise(5, 0, 5) < 0.5F) {
            spawn(ModEntities.IRON_REVENANT.get(), 5, g + 1, 4);
        }
    }
}
