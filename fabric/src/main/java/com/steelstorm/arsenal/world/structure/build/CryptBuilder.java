package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A walled graveyard with a mausoleum. Inside it, stairs lead down to a buried hall of sarcophagi
 * and, on a dais at its end, a Legendary Pedestal that stays sealed until every sarcophagus has
 * been opened and the Crypt Knights inside have been laid to rest.
 */
public class CryptBuilder extends Builder {
    public CryptBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        int c = w / 2;
        int hallFloor = g - 9;
        graveyard(c);
        mausoleum(c);
        hall(c, hallFloor);
        // Stairs from the mausoleum floor down to the hall door, cut in after the hall.
        stairway(c, 3, g, hallFloor);
    }

    // ------------------------------------------------------------------ the surface

    private void graveyard(int c) {
        groundwork(Blocks.STONE.defaultBlockState(), 9);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                float n = noise(x, g, z);
                set(x, g, z, n < 0.25F ? Blocks.MOSS_BLOCK.defaultBlockState() : n < 0.45F ? Blocks.PODZOL.defaultBlockState()
                        : n < 0.6F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        // A path of gravel from the gate to the mausoleum door.
        for (int z = 0; z < 2; z++) {
            for (int x = c - 1; x <= c + 1; x++) {
                set(x, g, z, Blocks.GRAVEL);
            }
        }
        // A crumbling wall around the yard with a gate at the front.
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                if (!edge || (z == 0 && Math.abs(x - c) <= 1)) {
                    continue;
                }
                boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                boolean gatePost = z == 0 && Math.abs(x - c) == 2;
                if (corner || gatePost) {
                    fill(x, g + 1, z, x, g + 2, z, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                    set(x, g + 3, z, gatePost ? Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false)
                            : Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState());
                    continue;
                }
                float n = noise(x, 40, z);
                if (n < 0.12F) {
                    continue;
                }
                BlockState wall = n < 0.5F ? Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState() : Blocks.COBBLESTONE_WALL.defaultBlockState();
                connect(x, g + 1, z, wall.setValue(WallBlock.UP, true));
            }
        }
        // Rows of graves; a few have been dug up.
        int[] xs = {2, 5, 11, 14};
        for (int z = 11; z <= 21; z += 3) {
            for (int x : xs) {
                grave(x, z);
            }
        }
        grave(2, 3);
        grave(14, 3);
        grave(2, 6);
        grave(14, 6);
    }

    /** A grave with the headstone at the back (+z) end. */
    private void grave(int x, int z) {
        float n = noise(x, 50, z);
        boolean dug = n < 0.18F;
        BlockState plot = n < 0.5F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PODZOL.defaultBlockState();
        if (dug) {
            set(x, g, z - 1, Blocks.AIR.defaultBlockState());
            set(x, g, z, Blocks.AIR.defaultBlockState());
            set(x, g - 1, z - 1, Blocks.BONE_BLOCK);
            set(x, g - 1, z, Blocks.COARSE_DIRT);
            set(x + 1, g + 1, z - 1, Blocks.COARSE_DIRT);
        } else {
            set(x, g, z - 1, plot);
            set(x, g, z, plot);
        }
        float h = noise(x, 51, z);
        BlockState stone = h < 0.35F ? Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState().setValue(WallBlock.UP, true)
                : h < 0.7F ? Blocks.STONE_BRICK_WALL.defaultBlockState().setValue(WallBlock.UP, true)
                : stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.NORTH, false);
        set(x, g + 1, z + 1, stone);
        float extra = noise(x, 52, z);
        if (!dug && extra < 0.25F) {
            set(x, g + 1, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + (int) (extra * 8)));
        } else if (!dug && extra > 0.85F) {
            set(x, g + 1, z - 1, Blocks.DEAD_BUSH);
        }
    }

    private void mausoleum(int c) {
        int x0 = c - 3;
        int x1 = c + 3;
        int z0 = 2;
        int z1 = 8;
        fill(x0, g, z0, x1, g, z1, Blocks.POLISHED_ANDESITE.defaultBlockState());
        for (int y = g + 1; y <= g + 4; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    if (!edge) {
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                    boolean door = z == z0 && Math.abs(x - c) <= 1 && y <= g + 3;
                    boolean window = !corner && y == g + 2 && (x == x0 || x == x1) && z == z0 + 3;
                    if (door) {
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                    } else if (window) {
                        set(x, y, z, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true)
                                .setValue(IronBarsBlock.SOUTH, true));
                    } else {
                        set(x, y, z, corner ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : brick(x, y, z));
                    }
                }
            }
        }
        // Stepped roof over a flat ceiling.
        BlockState tiles = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        for (int i = 0; i <= 3; i++) {
            for (int x = x0 - 1 + i; x <= x1 + 1 - i; x++) {
                for (int z = z0 - 1 + i; z <= z1 + 1 - i; z++) {
                    boolean rim = x == x0 - 1 + i || x == x1 + 1 - i || z == z0 - 1 + i || z == z1 + 1 - i;
                    if (rim || i == 0 || i == 3) {
                        set(x, g + 5 + i, z, i == 3 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : tiles);
                    }
                }
            }
        }
        set(c, g + 9, z0 + 3, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        // The door frame and its guardians.
        set(c, g + 4, z0, Blocks.CHISELED_STONE_BRICKS);
        set(c - 2, g + 1, z0 - 1, Blocks.SKELETON_SKULL);
        set(c + 2, g + 1, z0 - 1, Blocks.SKELETON_SKULL);
        hangingLantern(c, g + 3, z0 + 4, true, 1);
        // Two small burial niches either side of the stairs.
        set(x0 + 1, g + 1, z1 - 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        set(x1 - 1, g + 1, z1 - 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
    }

    // ------------------------------------------------------------------ the crypt below

    private void hall(int c, int hallFloor) {
        int hx0 = 1;
        int hx1 = w - 2;
        int hz0 = 13;
        int hz1 = d - 2;
        int ceiling = hallFloor + 5;
        for (int x = hx0 - 1; x <= hx1 + 1; x++) {
            for (int z = hz0 - 1; z <= hz1 + 1; z++) {
                for (int y = hallFloor - 1; y <= ceiling; y++) {
                    boolean shell = x == hx0 - 1 || x == hx1 + 1 || z == hz0 - 1 || z == hz1 + 1 || y == hallFloor - 1 || y == ceiling;
                    set(x, y, z, shell ? deepslateBrick(x, y, z) : Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int x = hx0; x <= hx1; x++) {
            for (int z = hz0; z <= hz1; z++) {
                float n = noise(x, hallFloor, z);
                set(x, hallFloor - 1, z, n < 0.15F ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
            }
        }
        // A carved door frame where the stairs come in.
        fill(c - 2, hallFloor, hz0 - 1, c - 2, hallFloor + 4, hz0 - 1, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(c + 2, hallFloor, hz0 - 1, c + 2, hallFloor + 4, hz0 - 1, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        fill(c - 1, hallFloor + 4, hz0 - 1, c + 1, hallFloor + 4, hz0 - 1, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        // Pillars and soul lanterns on chains.
        for (int z = hz0 + 1; z <= hz1 - 1; z += 3) {
            for (int x : new int[]{hx0 + 2, hx1 - 2}) {
                fill(x, hallFloor, z, x, ceiling - 1, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                set(x, ceiling - 1, z, Blocks.CHISELED_DEEPSLATE);
            }
        }
        for (int z : new int[]{hz0 + 2, hz0 + 6}) {
            hangingLantern(c - 2, ceiling - 2, z, true, 1);
            hangingLantern(c + 2, ceiling - 2, z, true, 1);
        }
        // A worn runner down the middle.
        for (int z = hz0; z <= hz1 - 4; z++) {
            if (noise(c, 60, z) > 0.2F) {
                set(c, hallFloor, z, Blocks.GRAY_CARPET);
            }
        }
        // Four sarcophagi against the side walls, a candle at each foot.
        for (int z : new int[]{hz0 + 2, hz0 + 6}) {
            sarcophagus(hx0 + 1, hallFloor, z, Direction.WEST);
            sarcophagus(hx1 - 1, hallFloor, z, Direction.EAST);
            set(hx0 + 2, hallFloor, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
            set(hx1 - 2, hallFloor, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
        }
        // The pedestal on a dais at the far end, between soul fires, under black banners.
        fill(c - 2, hallFloor, hz1 - 2, c + 2, hallFloor, hz1, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int x = c - 1; x <= c + 1; x++) {
            set(x, hallFloor, hz1 - 3, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, false));
        }
        ItemStack prize = noise(c, 77, c) < 0.4F
                ? new ItemStack(ModItems.legendary(LegendaryWeaponItem.Legendary.values()[random.nextInt(8)]).get())
                : new ItemStack(ModItems.weapon(WeaponType.values()[random.nextInt(WeaponType.values().length)],
                random.nextBoolean() ? WeaponTier.DIAMOND : WeaponTier.STORMSTEEL).get());
        pedestal(c, hallFloor + 1, hz1 - 1, prize);
        BlockState soulFire = Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        set(c - 2, hallFloor + 1, hz1 - 1, soulFire);
        set(c + 2, hallFloor + 1, hz1 - 1, soulFire);
        set(c - 2, hallFloor + 1, hz1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        set(c + 2, hallFloor + 1, hz1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
        BlockState banner = Blocks.BLACK_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH);
        for (int x : new int[]{c - 4, c + 4}) {
            banner(x, hallFloor + 3, hz1, banner, DyeColor.BLACK, BannerPatterns.SKULL, DyeColor.WHITE, BannerPatterns.BORDER, DyeColor.GRAY);
        }
        chest(hx0, hallFloor, hz1, ModLootTables.KNIGHTS_CRYPT);
        chest(hx1, hallFloor, hz1, ModLootTables.KNIGHTS_CRYPT);
        // Bones and cobwebs.
        set(hx0, hallFloor, hz0, Blocks.SKELETON_SKULL);
        set(hx1, hallFloor, hz1 - 3, Blocks.BONE_BLOCK);
        for (int[] p : new int[][]{{hx0, hz0}, {hx1, hz0}, {hx0, hz1 - 2}, {hx1, hz1 - 2}, {c, hz0 + 4}}) {
            set(p[0], ceiling - 1, p[1], Blocks.COBWEB);
        }
    }

    /**
     * A 3-wide stair running toward +z from the mausoleum floor ({@code top}) to the hall floor
     * ({@code bottom}); its last step sits in the hall's doorway.
     */
    private void stairway(int c, int zStart, int top, int bottom) {
        int z = zStart;
        for (int y = top; y >= bottom; y--, z++) {
            boolean underMausoleum = z <= 8;
            for (int x = c - 1; x <= c + 1; x++) {
                for (int yy = y + 1; yy <= y + 3; yy++) {
                    set(x, yy, z, Blocks.AIR.defaultBlockState());
                }
                set(x, y, z, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, false));
                set(x, y - 1, z, Blocks.DEEPSLATE_BRICKS);
                if (!underMausoleum && y > bottom) {
                    set(x, y + 4, z, Blocks.DEEPSLATE_BRICKS);
                }
            }
            if (y > bottom) {
                // Side walls, kept below the mausoleum floor so the room above stays open.
                int wallTop = underMausoleum ? Math.min(y + 4, top - 1) : y + 4;
                fill(c - 2, y - 1, z, c - 2, wallTop, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                fill(c + 2, y - 1, z, c + 2, wallTop, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            }
            if (z == 9 || z == 11) {
                set(c - 1, y + 3, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
        }
    }
}
