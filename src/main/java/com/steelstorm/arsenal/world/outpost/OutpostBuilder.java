package com.steelstorm.arsenal.world.outpost;

import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import com.steelstorm.arsenal.world.build.BuildContext;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Warrior's Outpost: a timber-and-stone hall with a weapon wall and the Combat Manual,
 * a fenced training yard with practice dummies, and a lantern-lit path to the spawn point.
 * <p>
 * Footprint is {@link #WIDTH} x {@link #DEPTH} in local space; local y = 0 is ground level and
 * the entrance faces local +z.
 */
public final class OutpostBuilder {
    public static final int WIDTH = 19;
    public static final int DEPTH = 17;
    public static final int HEIGHT = 12;
    /** Where new players are placed: on the path, looking at the hall door. */
    public static final int SPAWN_X = 6;
    public static final int SPAWN_Z = 15;

    private static final int HALL_X1 = 1, HALL_X2 = 11, HALL_Z1 = 1, HALL_Z2 = 9;
    private static final int YARD_X1 = 13, YARD_X2 = 18, YARD_Z1 = 1, YARD_Z2 = 13;
    private static final int DOOR_X = 6;

    private OutpostBuilder() {
    }

    public static void build(BuildContext ctx, BlockState surface, BlockState subsoil) {
        prepareGround(ctx, surface, subsoil);
        buildHall(ctx);
        buildRoof(ctx);
        furnishHall(ctx);
        buildYard(ctx);
        buildPath(ctx);
        ctx.finish();
        placeEntities(ctx);
    }

    // ------------------------------------------------------------------ terrain

    private static void prepareGround(BuildContext ctx, BlockState surface, BlockState subsoil) {
        // Clear the volume, then lay a flat floor with a solid foundation down to the real terrain.
        ctx.clear(0, 1, 0, WIDTH - 1, HEIGHT + 4, DEPTH - 1);
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                boolean hall = x >= HALL_X1 && x <= HALL_X2 && z >= HALL_Z1 && z <= HALL_Z2;
                ctx.set(x, 0, z, hall ? stone : surface);
                ctx.foundation(x, z, -1, hall ? Blocks.COBBLESTONE.defaultBlockState() : subsoil, 24);
            }
        }
    }

    // ------------------------------------------------------------------ hall

    private static void buildHall(BuildContext ctx) {
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState beamX = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState beamZ = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);

        // Floor: planks inside a stone border.
        ctx.fill(HALL_X1 + 1, 0, HALL_Z1 + 1, HALL_X2 - 1, 0, HALL_Z2 - 1, planks);

        // Stone base course with a few mossy bricks, plank walls above.
        for (int x = HALL_X1; x <= HALL_X2; x++) {
            for (int z = HALL_Z1; z <= HALL_Z2; z++) {
                boolean edge = x == HALL_X1 || x == HALL_X2 || z == HALL_Z1 || z == HALL_Z2;
                if (!edge) {
                    continue;
                }
                ctx.set(x, 1, z, ctx.noise(x, 1, z) < 0.25f ? mossy : bricks);
                ctx.fill(x, 2, z, x, 3, z, planks);
                boolean alongX = z == HALL_Z1 || z == HALL_Z2;
                ctx.set(x, 4, z, alongX ? beamX : beamZ);
            }
        }
        // Timber posts at the corners and wall midpoints.
        int[][] posts = {{HALL_X1, HALL_Z1}, {HALL_X2, HALL_Z1}, {HALL_X1, HALL_Z2}, {HALL_X2, HALL_Z2},
                {HALL_X1, 5}, {HALL_X2, 5}, {HALL_X1 + 3, HALL_Z1}, {HALL_X2 - 3, HALL_Z1}};
        for (int[] p : posts) {
            ctx.fill(p[0], 1, p[1], p[0], 4, p[1], log);
        }

        // Windows.
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        for (int y = 2; y <= 3; y++) {
            for (int x : new int[]{3, 4, 8, 9}) {
                ctx.set(x, y, HALL_Z2, pane);
            }
            for (int z : new int[]{3, 4, 6, 7}) {
                ctx.set(HALL_X1, y, z, pane);
                ctx.set(HALL_X2, y, z, pane);
            }
        }

        // Front door with a little stone step and lanterns either side.
        BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        ctx.set(DOOR_X, 1, HALL_Z2, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        ctx.set(DOOR_X, 2, HALL_Z2, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        ctx.set(DOOR_X, 3, HALL_Z2, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        ctx.set(DOOR_X, 0, HALL_Z2 + 1, Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        for (int x : new int[]{DOOR_X - 1, DOOR_X + 1}) {
            ctx.fill(x, 1, HALL_Z2 + 1, x, 2, HALL_Z2 + 1, Blocks.SPRUCE_FENCE.defaultBlockState());
            ctx.set(x, 3, HALL_Z2 + 1, Blocks.LANTERN);
        }
    }

    /** Roof height (local y of the stair) at local z, for the gable running along x. */
    private static int roofY(int z) {
        int fromEdge = Math.min(z, (HALL_Z2 + 1) - z);
        return 5 + fromEdge;
    }

    private static void buildRoof(BuildContext ctx) {
        BlockState stairs = Blocks.DARK_OAK_STAIRS.defaultBlockState();
        BlockState gable = Blocks.SPRUCE_PLANKS.defaultBlockState();
        int ridgeZ = (HALL_Z2 + 1) / 2; // 5
        for (int x = HALL_X1 - 1; x <= HALL_X2 + 1; x++) {
            for (int z = HALL_Z1 - 1; z <= HALL_Z2 + 1; z++) {
                int y = roofY(z);
                if (z < ridgeZ) {
                    ctx.set(x, y, z, stairs.setValue(StairBlock.FACING, Direction.SOUTH));
                } else if (z > ridgeZ) {
                    ctx.set(x, y, z, stairs.setValue(StairBlock.FACING, Direction.NORTH));
                } else {
                    ctx.set(x, y - 1, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
                    ctx.set(x, y, z, Blocks.DARK_OAK_SLAB.defaultBlockState());
                }
            }
        }
        // Eaves trim: upside-down stairs under the overhang edges.
        for (int x = HALL_X1 - 1; x <= HALL_X2 + 1; x++) {
            ctx.set(x, 4, HALL_Z1 - 1, stairs.setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.TOP));
            ctx.set(x, 4, HALL_Z2 + 1, stairs.setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.TOP));
        }
        // Gable ends.
        for (int z = HALL_Z1; z <= HALL_Z2; z++) {
            for (int y = 5; y < roofY(z); y++) {
                ctx.set(HALL_X1, y, z, gable);
                ctx.set(HALL_X2, y, z, gable);
            }
        }
        // Round window in each gable.
        ctx.set(HALL_X1, 6, ridgeZ, Blocks.GLASS_PANE.defaultBlockState());
        ctx.set(HALL_X2, 6, ridgeZ, Blocks.GLASS_PANE.defaultBlockState());
        // Chandeliers hanging from the ridge.
        for (int x : new int[]{3, 6, 9}) {
            ctx.fill(x, 6, ridgeZ, x, 8, ridgeZ, Blocks.CHAIN.defaultBlockState());
            ctx.set(x, 5, ridgeZ, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    private static void furnishHall(BuildContext ctx) {
        // Red runner from the door to the lectern.
        for (int z = 4; z <= HALL_Z2 - 1; z++) {
            ctx.set(DOOR_X, 1, z, Blocks.RED_CARPET);
        }
        // Lectern with the Combat Manual, facing the door.
        ctx.set(DOOR_X, 1, 3, Blocks.LECTERN.defaultBlockState()
                .setValue(LecternBlock.FACING, Direction.SOUTH).setValue(LecternBlock.HAS_BOOK, true));
        ctx.blockEntity(DOOR_X, 1, 3, LecternBlockEntity.class).ifPresent(be -> be.setBook(CombatManual.create()));

        // Weapon wall: banners framing the display on the back wall.
        BlockState banner = Blocks.RED_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH);
        ctx.set(HALL_X1 + 1, 3, HALL_Z1 + 1, banner);
        ctx.set(HALL_X2 - 1, 3, HALL_Z1 + 1, banner);

        // East side: crafting, smithing and the starter chest.
        ctx.set(HALL_X2 - 1, 1, 3, Blocks.CRAFTING_TABLE);
        ctx.set(HALL_X2 - 1, 1, 4, Blocks.SMITHING_TABLE);
        ctx.set(HALL_X2 - 1, 1, 6, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
        ctx.blockEntity(HALL_X2 - 1, 1, 6, ChestBlockEntity.class).ifPresent(OutpostBuilder::fillStarterChest);
        ctx.set(HALL_X2 - 1, 1, 7, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        ctx.set(HALL_X2 - 1, 2, 7, Blocks.POTTED_FERN);

        // West side: grindstone, supply chest, firewood.
        ctx.set(HALL_X1 + 1, 1, 3, Blocks.GRINDSTONE.defaultBlockState()
                .setValue(GrindstoneBlock.FACE, AttachFace.FLOOR).setValue(GrindstoneBlock.FACING, Direction.EAST));
        ctx.set(HALL_X1 + 1, 1, 5, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        ctx.blockEntity(HALL_X1 + 1, 1, 5, ChestBlockEntity.class).ifPresent(OutpostBuilder::fillSupplyChest);
        ctx.set(HALL_X1 + 1, 1, 7, Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        ctx.set(HALL_X1 + 1, 1, 8, Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        ctx.set(HALL_X1 + 1, 2, 8, Blocks.POTTED_RED_MUSHROOM);
    }

    private static void fillStarterChest(ChestBlockEntity chest) {
        chest.setItem(10, new ItemStack(ModItems.weapon(WeaponType.LONGSWORD, WeaponTier.STONE).get()));
        chest.setItem(12, new ItemStack(ModItems.weapon(WeaponType.DUAL_DAGGERS, WeaponTier.STONE).get()));
        chest.setItem(14, CombatManual.create());
        chest.setItem(16, new ItemStack(Items.SHIELD));
    }

    private static void fillSupplyChest(ChestBlockEntity chest) {
        chest.setItem(4, new ItemStack(Items.BREAD, 8));
        chest.setItem(11, new ItemStack(Items.TORCH, 16));
        chest.setItem(13, new ItemStack(Items.COOKED_PORKCHOP, 4));
        chest.setItem(15, new ItemStack(Items.OAK_SAPLING, 2));
        chest.setItem(22, new ItemStack(Items.WHITE_BED));
    }

    // ------------------------------------------------------------------ training yard

    private static void buildYard(BuildContext ctx) {
        // Packed training ground.
        for (int x = YARD_X1; x <= YARD_X2; x++) {
            for (int z = YARD_Z1; z <= YARD_Z2; z++) {
                float n = ctx.noise(x, 0, z, 7);
                Block floor = n < 0.55f ? Blocks.COARSE_DIRT : n < 0.8f ? Blocks.PACKED_MUD : Blocks.GRAVEL;
                ctx.set(x, 0, z, floor);
            }
        }
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        ctx.ring(YARD_X1, 1, YARD_Z1, YARD_X2, YARD_Z2, fence);
        // Gate facing the path.
        ctx.set(15, 1, YARD_Z2, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH));
        // Lantern posts on the corners.
        for (int[] c : new int[][]{{YARD_X1, YARD_Z1}, {YARD_X2, YARD_Z1}, {YARD_X1, YARD_Z2}, {YARD_X2, YARD_Z2}}) {
            ctx.set(c[0], 2, c[1], fence);
            ctx.set(c[0], 3, c[1], Blocks.LANTERN);
        }
        // Hay and an archery target.
        ctx.set(YARD_X2 - 1, 1, YARD_Z1 + 1, Blocks.HAY_BLOCK);
        ctx.set(YARD_X2 - 1, 2, YARD_Z1 + 1, Blocks.HAY_BLOCK);
        ctx.set(YARD_X2 - 2, 1, YARD_Z1 + 1, Blocks.HAY_BLOCK);
        ctx.set(YARD_X2 - 1, 1, YARD_Z2 - 1, Blocks.HAY_BLOCK);
        ctx.set(YARD_X2 - 1, 2, YARD_Z2 - 1, Blocks.TARGET);
        ctx.set(YARD_X1 + 1, 1, YARD_Z2 - 1, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        // A water trough.
        ctx.set(YARD_X1 + 1, 1, YARD_Z1 + 1, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
    }

    // ------------------------------------------------------------------ path and signage

    private static void buildPath(BuildContext ctx) {
        BlockState path = Blocks.DIRT_PATH.defaultBlockState();
        for (int z = HALL_Z2 + 2; z < DEPTH; z++) {
            for (int x = DOOR_X - 1; x <= DOOR_X + 1; x++) {
                ctx.set(x, 0, z, ctx.noise(x, 0, z, 3) < 0.2f ? Blocks.GRAVEL.defaultBlockState() : path);
            }
        }
        for (int x = DOOR_X + 2; x <= 15; x++) {
            ctx.set(x, 0, 14, path);
        }
        // Lantern posts.
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int x : new int[]{DOOR_X - 2, DOOR_X + 2}) {
            ctx.fill(x, 1, 12, x, 2, 12, fence);
            ctx.set(x, 3, 12, Blocks.LANTERN);
        }
        // Welcome sign.
        ctx.set(DOOR_X + 2, 1, 16, Blocks.SPRUCE_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0));
        ctx.blockEntity(DOOR_X + 2, 1, 16, SignBlockEntity.class).ifPresent(sign -> {
            SignText text = new SignText()
                    .setMessage(0, Component.translatable("sign.steelstorm.outpost.line1"))
                    .setMessage(1, Component.translatable("sign.steelstorm.outpost.line2"))
                    .setMessage(2, Component.translatable("sign.steelstorm.outpost.line3"))
                    .setMessage(3, Component.translatable("sign.steelstorm.outpost.line4"))
                    .setHasGlowingText(true);
            sign.setText(text, true);
        });
        // A campfire with log benches.
        ctx.set(1, 1, 14, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
                .setValue(CampfireBlock.SIGNAL_FIRE, false));
        ctx.set(1, 0, 14, Blocks.COBBLESTONE);
        ctx.set(1, 1, 12, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        ctx.set(3, 1, 14, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
    }

    // ------------------------------------------------------------------ entities

    private static void placeEntities(BuildContext ctx) {
        // Weapon wall display.
        WeaponType[] shown = {WeaponType.LONGSWORD, WeaponType.KATANA, WeaponType.GREATSWORD, WeaponType.SPEAR, WeaponType.DUAL_DAGGERS};
        for (int i = 0; i < shown.length; i++) {
            int x = 4 + i;
            ItemFrame frame = new ItemFrame(ctx.level().getLevel(), ctx.pos(x, 2, HALL_Z1 + 1), ctx.dir(Direction.SOUTH));
            frame.setItem(new ItemStack(ModItems.weapon(shown[i], WeaponTier.IRON).get()), false);
            frame.setRotation(1);
            if (ctx.canWrite(frame.getPos())) {
                ctx.level().addFreshEntity(frame);
            }
        }
        // Practice dummies.
        for (int z : new int[]{4, 7, 10}) {
            ArmorStand dummy = EntityType.ARMOR_STAND.create(ctx.level().getLevel());
            if (dummy != null) {
                dummy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                dummy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
                ctx.spawn(dummy, 16.5, 1.0, z + 0.5, 90.0f);
            }
        }
    }
}
