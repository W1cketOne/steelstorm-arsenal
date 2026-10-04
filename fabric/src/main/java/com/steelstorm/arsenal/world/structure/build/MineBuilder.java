package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * An abandoned Stormsteel mine. On the surface: a timber headframe with its pulley over the shaft,
 * the miners' shed, an ore heap and a cart track. Below: a chamber whose walls glitter with
 * Stormsteel, and two timbered drifts running off it, one caved in, one ending in a rich vein.
 * The miners never left; they guard it as Iron Revenants.
 */
public class MineBuilder extends Builder {
    private static final int CHAMBER_RADIUS = 5;

    private int c;
    private int m;
    private int bottom;

    public MineBuilder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        super(piece, level, box, random);
    }

    @Override
    public void build() {
        c = w / 2;
        m = d / 2;
        bottom = g - 14;
        groundwork(Blocks.STONE.defaultBlockState(), 9);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                float n = noise(x, g, z);
                set(x, g, z, n < 0.3F ? Blocks.GRAVEL.defaultBlockState() : n < 0.55F ? Blocks.COARSE_DIRT.defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        chamber();
        drift(c - CHAMBER_RADIUS, 2, false);
        drift(c + CHAMBER_RADIUS, w - 3, true);
        shaft();
        headframe();
        shed(2, m + 2);
        oreHeap(w - 6, m + 4);
        // A cart track from the edge of the camp to the shaft, with a cart left on it.
        for (int z = 0; z < m - 2; z++) {
            set(c, g + 1, z, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
        }
        cart(c, g + 1, 2, false);
        for (int[] p : new int[][]{{c - 3, 1}, {c + 3, 1}}) {
            set(p[0], g + 1, p[1], Blocks.SPRUCE_FENCE);
            set(p[0], g + 2, p[1], Blocks.LANTERN);
        }
        // The dead miners.
        spawn(ModEntities.IRON_REVENANT.get(), c - 2, bottom, m + 2);
        spawn(ModEntities.IRON_REVENANT.get(), c + 3, bottom, m - 1);
        spawn(ModEntities.IRON_REVENANT.get(), w - 5, bottom, m);
        spawn(ModEntities.IRON_REVENANT.get(), 5, bottom, m);
    }

    // ------------------------------------------------------------------ underground

    private boolean chamberAir(int x, int y, int z) {
        return y >= bottom && y <= bottom + 4 && chamberDistance(x, y, z) <= CHAMBER_RADIUS - 0.5 + noise(x, y, z) * 0.8;
    }

    private double chamberDistance(int x, int y, int z) {
        return Math.hypot(x - c, (z - m) * 1.1) + (y - bottom - 2) * (y - bottom - 2) * 0.12;
    }

    /** Raw rock for the walls: stone and deepslate with ore. */
    private BlockState rock(int x, int y, int z, float oreChance) {
        float n = noise(x, y, z);
        if (n < oreChance) {
            return (y < bottom + 2 ? ModBlocks.DEEPSLATE_STORMSTEEL_ORE : ModBlocks.STORMSTEEL_ORE).get().defaultBlockState();
        }
        if (n > 0.95F) {
            return Blocks.IRON_ORE.defaultBlockState();
        }
        if (n > 0.9F) {
            return Blocks.COAL_ORE.defaultBlockState();
        }
        return y < bottom + 1 && n < 0.5F ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    private void chamber() {
        int r = CHAMBER_RADIUS;
        for (int x = c - r - 1; x <= c + r + 1; x++) {
            for (int z = m - r - 1; z <= m + r + 1; z++) {
                for (int y = bottom - 1; y <= bottom + 5; y++) {
                    if (chamberAir(x, y, z)) {
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                    } else if (chamberDistance(x, y, z) <= r + 1.5) {
                        set(x, y, z, rock(x, y, z, 0.14F));
                    }
                }
            }
        }
        // Timber supports and their cross beams.
        for (int[] p : new int[][]{{c - 3, m - 3}, {c + 3, m - 3}, {c - 3, m + 3}, {c + 3, m + 3}}) {
            fill(p[0], bottom, p[1], p[0], bottom + 3, p[1], Blocks.SPRUCE_LOG.defaultBlockState());
            set(p[0], bottom + 4, p[1], Blocks.SPRUCE_PLANKS);
        }
        BlockState beam = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        fill(c - 3, bottom + 4, m - 3, c + 3, bottom + 4, m - 3, beam);
        fill(c - 3, bottom + 4, m + 3, c + 3, bottom + 4, m + 3, beam);
        set(c - 2, bottom + 3, m - 3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(c + 2, bottom + 3, m + 3, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // The track runs straight through, from drift to drift.
        for (int x = c - CHAMBER_RADIUS; x <= c + CHAMBER_RADIUS; x++) {
            set(x, bottom - 1, m, Blocks.STONE);
            set(x, bottom, m, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        cart(c + 2, bottom, m, true);
        chest(c - 2, bottom, m + 3, ModLootTables.STORMSTEEL_MINE);
        set(c - 1, bottom, m + 3, Blocks.BARREL);
        set(c + 2, bottom, m - 2, Blocks.COBWEB);
        set(c - 2, bottom + 4, m - 1, Blocks.COBWEB);
    }

    /** A timbered tunnel along x from the chamber wall at {@code from} to {@code to}. */
    private void drift(int from, int to, boolean richEnd) {
        int step = to > from ? 1 : -1;
        BlockState beam = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        for (int x = from; x != to + 2 * step; x += step) {
            boolean end = x == to + step;
            for (int z = m - 2; z <= m + 2; z++) {
                for (int y = bottom - 1; y <= bottom + 3; y++) {
                    boolean inside = !end && Math.abs(z - m) <= 1 && y >= bottom && y <= bottom + 2;
                    if (inside) {
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                    } else if (!chamberAir(x, y, z)) {
                        set(x, y, z, rock(x, y, z, end && richEnd ? 0.75F : 0.1F));
                    }
                }
            }
            if (end) {
                continue;
            }
            set(x, bottom, m, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
            int along = Math.abs(x - from);
            if (along % 3 == 1) {
                fill(x, bottom, m - 1, x, bottom + 1, m - 1, Blocks.SPRUCE_LOG.defaultBlockState());
                fill(x, bottom, m + 1, x, bottom + 1, m + 1, Blocks.SPRUCE_LOG.defaultBlockState());
                fill(x, bottom + 2, m - 1, x, bottom + 2, m + 1, beam);
                if (along % 6 == 1) {
                    Direction away = step > 0 ? Direction.EAST : Direction.WEST;
                    set(x + step, bottom + 1, m - 1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, away));
                }
            }
        }
        if (!richEnd) {
            // Caved in: rubble fills the last stretch.
            for (int i = 0; i < 3; i++) {
                int x = to - i * step;
                for (int z = m - 1; z <= m + 1; z++) {
                    int height = 2 - i + (noise(x, 90, z) < 0.5F ? 1 : 0);
                    for (int y = bottom; y < bottom + Math.min(3, height); y++) {
                        set(x, y, z, noise(x, y + 91, z) < 0.6F ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                    }
                }
            }
        } else {
            chest(to, bottom, m - 1, ModLootTables.STORMSTEEL_MINE);
        }
    }

    private void shaft() {
        for (int y = bottom + 5; y <= g + 1; y++) {
            for (int x = c - 1; x <= c + 1; x++) {
                for (int z = m - 1; z <= m + 1; z++) {
                    set(x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
            if (y > g) {
                continue;
            }
            for (int x = c - 2; x <= c + 2; x++) {
                for (int z = m - 2; z <= m + 2; z++) {
                    if (Math.abs(x - c) == 2 || Math.abs(z - m) == 2) {
                        boolean corner = Math.abs(x - c) == 2 && Math.abs(z - m) == 2;
                        set(x, y, z, corner ? Blocks.SPRUCE_LOG.defaultBlockState()
                                : (y % 4 == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.STONE.defaultBlockState()));
                    }
                }
            }
        }
        // The ladder runs down the back of the shaft and on down a timber post to the chamber floor.
        fill(c, bottom, m + 2, c, bottom + 4, m + 2, Blocks.SPRUCE_LOG.defaultBlockState());
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = bottom; y <= g; y++) {
            set(c, y, m + 1, ladder);
        }
    }

    // ------------------------------------------------------------------ surface

    private void headframe() {
        int top = g + 8;
        for (int[] p : new int[][]{{c - 2, m - 2}, {c + 2, m - 2}, {c - 2, m + 2}, {c + 2, m + 2}}) {
            fill(p[0], g + 1, p[1], p[0], top - 1, p[1], Blocks.SPRUCE_LOG.defaultBlockState());
        }
        BlockState beamX = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState beamZ = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        fill(c - 2, top, m - 2, c + 2, top, m - 2, beamX);
        fill(c - 2, top, m + 2, c + 2, top, m + 2, beamX);
        fill(c, top, m - 1, c, top, m + 1, beamZ);
        fill(c - 2, g + 4, m - 2, c - 2, g + 4, m + 2, beamZ);
        fill(c + 2, g + 4, m - 2, c + 2, g + 4, m + 2, beamZ);
        set(c - 2, top + 1, m, Blocks.SPRUCE_SLAB);
        set(c + 2, top + 1, m, Blocks.SPRUCE_SLAB);
        // The pulley wheel and its chain down into the shaft.
        set(c, top - 1, m, Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.CEILING)
                .setValue(GrindstoneBlock.FACING, Direction.EAST));
        for (int y = g - 2; y < top - 1; y++) {
            set(c, y, m, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
        }
        set(c - 1, top - 1, m - 2, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(c + 1, top - 1, m + 2, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // A railing round the shaft mouth, open where the track and the ladder come in.
        for (int x = c - 2; x <= c + 2; x++) {
            for (int z = m - 2; z <= m + 2; z++) {
                boolean ring = Math.abs(x - c) == 2 || Math.abs(z - m) == 2;
                boolean corner = Math.abs(x - c) == 2 && Math.abs(z - m) == 2;
                if (ring && !corner && x != c) {
                    set(x, g + 1, z, Blocks.SPRUCE_FENCE);
                }
            }
        }
    }

    /** The miners' shed: a plank hut with a slab roof, at (x0..x0+5, z0..z0+5). */
    private void shed(int x0, int z0) {
        int x1 = x0 + 5;
        int z1 = z0 + 5;
        fill(x0, g, z0, x1, g, z1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int y = g + 1; y <= g + 3; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                    boolean door = x == x1 && z == z0 + 2 && y <= g + 2;
                    boolean window = y == g + 2 && !corner && (z == z1 && x == x0 + 2 || x == x0 && z == z0 + 3);
                    if (!edge || door) {
                        set(x, y, z, Blocks.AIR.defaultBlockState());
                    } else if (corner) {
                        set(x, y, z, Blocks.STRIPPED_SPRUCE_LOG);
                    } else if (window) {
                        set(x, y, z, Blocks.GLASS_PANE.defaultBlockState()
                                .setValue(z == z1 ? IronBarsBlock.EAST : IronBarsBlock.NORTH, true)
                                .setValue(z == z1 ? IronBarsBlock.WEST : IronBarsBlock.SOUTH, true));
                    } else {
                        set(x, y, z, Blocks.SPRUCE_PLANKS);
                    }
                }
            }
        }
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        fill(x0 - 1, g + 4, z0 - 1, x1 + 1, g + 4, z1 + 1, slab);
        set(x0 + 1, g + 1, z1 - 1, Blocks.CRAFTING_TABLE);
        set(x0 + 1, g + 1, z0 + 1, Blocks.BARREL);
        set(x0 + 2, g + 1, z0 + 1, Blocks.BARREL);
        set(x0 + 1, g + 2, z0 + 1, Blocks.LANTERN);
        set(x0 + 3, g + 1, z1 - 1, facing(ModBlocks.WHETSTONE.get(), Direction.SOUTH));
        chest(x0 + 2, g + 1, z1 - 1, ModLootTables.STORMSTEEL_MINE);
        set(x0 + 1, g + 3, z1 - 1, Blocks.COBWEB);
    }

    /** A heap of spoil and ore beside the shaft. */
    private void oreHeap(int x0, int z0) {
        for (int x = x0 - 2; x <= x0 + 2; x++) {
            for (int z = z0 - 2; z <= z0 + 2; z++) {
                int height = 2 - Math.max(Math.abs(x - x0), Math.abs(z - z0)) + (noise(x, 80, z) < 0.4F ? 1 : 0);
                for (int y = g + 1; y < g + 1 + height; y++) {
                    float n = noise(x, y + 81, z);
                    set(x, y, z, n < 0.12F ? ModBlocks.STORMSTEEL_ORE.get().defaultBlockState()
                            : n < 0.2F ? Blocks.IRON_ORE.defaultBlockState()
                            : n < 0.6F ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
    }

    private void cart(int x, int y, int z, boolean withChest) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        if (withChest) {
            MinecartChest cart = new MinecartChest(level.getLevel(), pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
            cart.setLootTable(ModLootTables.STORMSTEEL_MINE, random.nextLong());
            level.addFreshEntity(cart);
        } else {
            level.addFreshEntity(new Minecart(level.getLevel(), pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5));
        }
    }
}
