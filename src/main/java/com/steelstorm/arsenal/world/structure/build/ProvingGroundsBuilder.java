package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.entity.TargetDummyEntity;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * An open-air training arena ringed by a palisade: a sand fighting circle, spectator benches,
 * practice dummies and weapon racks. Strike the Arena Gong to fight three waves of challengers for
 * the Champion's Coffer.
 */
public class ProvingGroundsBuilder extends Builder {
    public ProvingGroundsBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        int c = w / 2;
        groundwork(Blocks.DIRT.defaultBlockState(), 8);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                double r = Math.hypot(x - c, z - c);
                float n = noise(x, g, z);
                BlockState ground;
                if (r <= 7.5) {
                    ground = n < 0.15F ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
                } else if (r <= 8.5) {
                    ground = Blocks.POLISHED_ANDESITE.defaultBlockState();
                } else {
                    ground = n < 0.25F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                }
                set(x, g, z, ground);
                // Palisade of sharpened logs with four gates.
                boolean gate = Math.abs(x - c) <= 1 || Math.abs(z - c) <= 1;
                if (r > 10.4 && r <= 11.4 && !gate) {
                    int top = 2 + (int) (noise(x, 5, z) * 2.5F);
                    for (int y = g + 1; y <= g + top; y++) {
                        set(x, y, z, Blocks.SPRUCE_LOG);
                    }
                    set(x, g + top + 1, z, Blocks.SPRUCE_FENCE);
                }
                // Benches for spectators along the inside of the palisade.
                if (r > 9.3 && r <= 10.3 && !gate && noise(x, 3, z) < 0.8F) {
                    double a = Math.atan2(z - c, x - c);
                    Direction facing = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a))
                            ? (Math.cos(a) > 0 ? Direction.WEST : Direction.EAST)
                            : (Math.sin(a) > 0 ? Direction.SOUTH : Direction.NORTH);
                    set(x, g + 1, z, stair(Blocks.SPRUCE_STAIRS, facing.getOpposite(), false));
                }
            }
        }
        // Gate posts with lanterns; the main gate gets a lintel and the arena's red banners.
        for (int[] p : new int[][]{{c - 2, 0}, {c + 2, 0}, {c - 2, d - 1}, {c + 2, d - 1}, {0, c - 2}, {0, c + 2}, {w - 1, c - 2},
                {w - 1, c + 2}}) {
            fill(p[0], g + 1, p[1], p[0], g + 4, p[1], Blocks.SPRUCE_LOG.defaultBlockState());
            if (p[1] != 0) {
                set(p[0], g + 5, p[1], Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
            }
        }
        fill(c - 2, g + 5, 0, c + 2, g + 5, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        for (int x : new int[]{c - 2, c + 2}) {
            banner(x, g + 6, 0, Blocks.RED_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 0), DyeColor.RED,
                    BannerPatterns.CROSS, DyeColor.YELLOW, BannerPatterns.CIRCLE_MIDDLE, DyeColor.RED, BannerPatterns.BORDER, DyeColor.YELLOW);
        }
        set(c, g + 6, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        set(c - 1, g + 4, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(c + 1, g + 4, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // Centre: a sparring ring with the gong on one side and the prize on the other.
        set(c, g + 1, c + 6, facing(ModBlocks.ARENA_GONG.get(), Direction.SOUTH));
        set(c, g, c - 6, Blocks.CHISELED_STONE_BRICKS);
        lockedChest(ModBlocks.CHAMPIONS_COFFER.get(), c, g + 1, c - 6, Direction.NORTH, ModLootTables.CHAMPIONS_COFFER);
        // Statues of past champions watch over the prize.
        set(c - 2, g, c - 6, Blocks.POLISHED_ANDESITE);
        set(c + 2, g, c - 6, Blocks.POLISHED_ANDESITE);
        statue(c - 2, g + 1, c - 6, Direction.NORTH, new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE),
                ItemStack.EMPTY, new ItemStack(Items.CHAINMAIL_BOOTS), weapon(WeaponType.GREATSWORD, WeaponTier.IRON));
        statue(c + 2, g + 1, c - 6, Direction.NORTH, new ItemStack(Items.CHAINMAIL_HELMET), new ItemStack(Items.CHAINMAIL_CHESTPLATE),
                new ItemStack(Items.CHAINMAIL_LEGGINGS), ItemStack.EMPTY, weapon(WeaponType.SPEAR, WeaponTier.IRON));
        // Training gear around the ring.
        rack(c - 6, g + 1, c + 4, Direction.EAST, weapon(WeaponType.LONGSWORD, WeaponTier.IRON), weapon(WeaponType.SPEAR, WeaponTier.IRON),
                weapon(WeaponType.DUAL_DAGGERS, WeaponTier.STONE));
        rack(c + 6, g + 1, c + 4, Direction.WEST, weapon(WeaponType.KATANA, WeaponTier.IRON), ItemStack.EMPTY,
                weapon(WeaponType.WARHAMMER, WeaponTier.STONE));
        set(c - 6, g + 1, c - 3, facing(ModBlocks.WHETSTONE.get(), Direction.EAST));
        set(c + 6, g + 1, c - 3, Blocks.BARREL);
        set(c + 6, g + 2, c - 3, Blocks.TARGET);
        dummy(c - 4, c - 5);
        dummy(c + 4, c - 5);
        dummy(c + 5, c + 1);
    }

    private void dummy(int x, int z) {
        BlockPos pos = world(x, g + 1, z);
        if (!box.isInside(pos)) {
            return;
        }
        TargetDummyEntity dummy = ModEntities.TARGET_DUMMY.get().create(level.getLevel());
        if (dummy != null) {
            dummy.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
            level.addFreshEntity(dummy);
        }
    }
}
