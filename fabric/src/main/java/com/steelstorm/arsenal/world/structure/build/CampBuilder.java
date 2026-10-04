package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A bandit camp behind a ring of stakes: two small tents, the captain's big tent with the locked
 * Bandit Vault inside, a lookout post, a caged prisoner, supplies, and the bandits themselves,
 * led by their captain.
 */
public class CampBuilder extends Builder {
    public CampBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        groundwork(Blocks.DIRT.defaultBlockState(), 7);
        int c = w / 2;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                int dist2 = (x - c) * (x - c) + (z - c) * (z - c);
                float n = noise(x, g, z);
                BlockState ground = dist2 <= 10 ? (n < 0.5F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState())
                        : (x == c && z < c) ? Blocks.DIRT_PATH.defaultBlockState()
                        : (n < 0.22F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                set(x, g, z, ground);
            }
        }
        stakes(c);
        // Campfire with log seats.
        set(c, g + 1, c, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        BlockState logX = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState logZ = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        set(c - 2, g + 1, c, logZ);
        set(c + 2, g + 1, c, logZ);
        set(c, g + 1, c - 2, logX);
        // Two small tents at the front corners, the captain's tent at the back.
        tent(4, 2, 4, Blocks.BROWN_WOOL.defaultBlockState(), 2);
        tent(w - 5, 2, 4, Blocks.GREEN_WOOL.defaultBlockState(), 2);
        tent(c, d - 8, 6, Blocks.RED_WOOL.defaultBlockState(), 3);
        for (int x : new int[]{c - 4, c + 4}) {
            banner(x, g + 1, d - 8, Blocks.BLACK_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 0), DyeColor.BLACK,
                    BannerPatterns.SKULL, DyeColor.WHITE, BannerPatterns.CURLY_BORDER, DyeColor.RED);
        }
        // The captain keeps the loot in a vault in his tent.
        lockedChest(ModBlocks.BANDIT_VAULT.get(), c, g + 1, d - 4, Direction.SOUTH, ModLootTables.BANDIT_VAULT);
        set(c - 1, g + 1, d - 4, Blocks.RED_CARPET);
        set(c + 1, g + 1, d - 4, Blocks.RED_CARPET);
        set(c - 2, g + 1, d - 5, Blocks.BARREL);
        chest(4, g + 1, 5, ModLootTables.BANDIT_CAMP);
        chest(w - 5, g + 1, 5, ModLootTables.BANDIT_CAMP);
        // Supplies and lights.
        set(2, g + 1, c, Blocks.BARREL);
        set(2, g + 1, c + 1, Blocks.HAY_BLOCK);
        set(2, g + 2, c + 1, Blocks.HAY_BLOCK);
        set(w - 3, g + 1, c, Blocks.BARREL);
        set(w - 3, g + 1, c + 1, Blocks.BARREL);
        for (int[] p : new int[][]{{2, c - 2}, {w - 3, c - 2}, {c - 4, 1}, {c + 4, 1}}) {
            set(p[0], g + 1, p[1], Blocks.SPRUCE_FENCE);
            set(p[0], g + 2, p[1], Blocks.LANTERN);
        }
        set(w - 3, g + 1, c + 3, facing(ModBlocks.WHETSTONE.get(), Direction.WEST));
        rack(w - 3, g + 1, c + 4, Direction.WEST, randomWeapon(WeaponTier.STONE, WeaponTier.IRON), randomWeapon(WeaponTier.STONE),
                ItemStack.EMPTY);
        rack(2, g + 1, c + 3, Direction.EAST, randomWeapon(WeaponTier.IRON), ItemStack.EMPTY, randomWeapon(WeaponTier.STONE));
        lookout(2, d - 5);
        cage(w - 5, d - 5);
        // The bandits themselves.
        spawn(ModEntities.BANDIT_DUELIST.get(), c - 3, g + 1, c - 1);
        spawn(ModEntities.BANDIT_DUELIST.get(), c + 3, g + 1, c + 1);
        spawn(ModEntities.BANDIT_ARCHER.get(), c - 1, g + 1, c + 3);
        spawn(ModEntities.BANDIT_ARCHER.get(), 3, g + 6, d - 4);
        spawn(ModEntities.BANDIT_CAPTAIN.get(), c, g + 1, d - 6);
    }

    /** A ring of sharpened stakes around the camp, open at the front. */
    private void stakes(int c) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                if (!edge || (z == 0 && Math.abs(x - c) <= 1)) {
                    continue;
                }
                float n = noise(x, 70, z);
                if (n < 0.1F) {
                    continue;
                }
                boolean tall = (x + z) % 3 == 0 || (z == 0 && Math.abs(x - c) == 2);
                if (tall) {
                    set(x, g + 1, z, Blocks.SPRUCE_LOG);
                    set(x, g + 2, z, n < 0.5F ? Blocks.SPRUCE_FENCE.defaultBlockState() : Blocks.SPRUCE_LOG.defaultBlockState());
                    set(x, g + 3, z, Blocks.SPRUCE_FENCE);
                } else {
                    set(x, g + 1, z, Blocks.SPRUCE_FENCE);
                }
            }
        }
        set(c - 2, g + 4, 0, Blocks.SKELETON_SKULL);
        set(c + 2, g + 4, 0, Blocks.SKELETON_SKULL);
    }

    /** A raised lookout platform on four posts with a ladder, at (x0..x0+2, z0..z0+2). */
    private void lookout(int x0, int z0) {
        int top = g + 5;
        for (int[] p : new int[][]{{x0, z0}, {x0 + 2, z0}, {x0, z0 + 2}, {x0 + 2, z0 + 2}}) {
            fill(p[0], g + 1, p[1], p[0], top + 1, p[1], Blocks.SPRUCE_LOG.defaultBlockState());
        }
        fill(x0 + 1, g + 1, z0 + 1, x0 + 1, top, z0 + 1, Blocks.SPRUCE_LOG.defaultBlockState());
        for (int x = x0; x <= x0 + 2; x++) {
            for (int z = z0; z <= z0 + 2; z++) {
                boolean post = (x == x0 || x == x0 + 2) && (z == z0 || z == z0 + 2);
                if (!post && !(x == x0 + 1 && z == z0 + 1)) {
                    set(x, top, z, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
                }
            }
        }
        set(x0 + 1, top, z0, Blocks.AIR.defaultBlockState());
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = g + 1; y <= top; y++) {
            set(x0 + 1, y, z0, ladder);
        }
        set(x0 + 2, top + 1, z0 + 1, Blocks.SPRUCE_FENCE);
        set(x0 + 1, top + 1, z0 + 2, Blocks.SPRUCE_FENCE);
        set(x0, top + 1, z0 + 1, Blocks.SPRUCE_FENCE);
        set(x0 + 1, top + 1, z0 + 1, Blocks.AIR.defaultBlockState());
        set(x0 + 2, top + 2, z0 + 2, Blocks.LANTERN);
    }

    /** An iron cage holding a villager the bandits took prisoner. */
    private void cage(int x0, int z0) {
        for (int x = x0 - 1; x <= x0 + 1; x++) {
            for (int z = z0 - 1; z <= z0 + 1; z++) {
                set(x, g, z, Blocks.COBBLESTONE);
                boolean edge = x != x0 || z != z0;
                for (int y = g + 1; y <= g + 2; y++) {
                    if (edge) {
                        connect(x, y, z, Blocks.IRON_BARS);
                    } else {
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                    }
                }
                set(x, g + 3, z, Blocks.SPRUCE_SLAB);
            }
        }
        BlockPos pos = world(x0, g + 1, z0);
        if (!box.isInside(pos)) {
            return;
        }
        Villager prisoner = EntityType.VILLAGER.create(level.getLevel());
        if (prisoner != null) {
            prisoner.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
            prisoner.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            prisoner.setVillagerData(prisoner.getVillagerData().setType(VillagerType.byBiome(level.getBiome(pos))));
            prisoner.setPersistenceRequired();
            level.addFreshEntityWithPassengers(prisoner);
        }
    }

    /** An A-frame tent running along z from {@code z0}, open at the front, ridge on {@code cx}. */
    private void tent(int cx, int z0, int length, BlockState wool, int halfWidth) {
        for (int z = z0; z < z0 + length; z++) {
            for (int i = 0; i <= halfWidth; i++) {
                int y = g + 1 + (halfWidth - i);
                set(cx - i, y, z, wool);
                set(cx + i, y, z, wool);
                for (int yy = g + 1; yy < y; yy++) {
                    boolean back = z == z0 + length - 1;
                    set(cx - i, yy, z, back ? wool : Blocks.AIR.defaultBlockState());
                    set(cx + i, yy, z, back ? wool : Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int z = z0 + 1; z < z0 + length - 1; z++) {
            set(cx, g, z, Blocks.SPRUCE_PLANKS);
        }
    }
}
