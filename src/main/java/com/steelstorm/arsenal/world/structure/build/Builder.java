package com.steelstorm.arsenal.world.structure.build;

import com.steelstorm.arsenal.block.ItemHolderBlockEntity;
import com.steelstorm.arsenal.block.LockedChestBlock;
import com.steelstorm.arsenal.block.SarcophagusBlock;
import com.steelstorm.arsenal.block.WeaponRackBlockEntity;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Shared building helpers for the structure builders. Coordinates are the piece's local ones:
 * x across, z from the entrance (0) to the back, {@link #g} is the ground floor.
 */
public abstract class Builder {
    /** How far above the floor every structure clears trees and terrain. */
    private static final int MIN_CLEARANCE = 16;

    protected final SteelstormStructurePiece piece;
    protected final WorldGenLevel level;
    protected final BoundingBox box;
    protected final RandomSource random;
    protected final int w;
    protected final int d;
    protected final int g;

    protected Builder(SteelstormStructurePiece piece, WorldGenLevel level, BoundingBox box, RandomSource random) {
        this.piece = piece;
        this.level = level;
        this.box = box;
        this.random = random;
        this.w = piece.width();
        this.d = piece.depth();
        this.g = piece.floor();
    }

    public abstract void build();

    // ------------------------------------------------------------------ blocks

    protected void set(int x, int y, int z, BlockState state) {
        piece.place(level, box, x, y, z, state);
    }

    protected void set(int x, int y, int z, Block block) {
        set(x, y, z, block.defaultBlockState());
    }

    /**
     * Places a block whose shape depends on its neighbours (walls, panes, fences) so it joins up
     * with them: during world generation the chunk updates it once everything around is built.
     */
    protected void connect(int x, int y, int z, BlockState state) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        set(x, y, z, state);
        if (level.getChunk(pos) instanceof ProtoChunk chunk) {
            chunk.markPosForPostprocessing(pos);
        } else {
            level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), Block.UPDATE_CLIENTS);
        }
    }

    protected void connect(int x, int y, int z, Block block) {
        connect(x, y, z, block.defaultBlockState());
    }

    /** A lantern hanging from the block above on {@code chain} links of chain. */
    protected void hangingLantern(int x, int y, int z, boolean soul, int chain) {
        set(x, y, z, (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true));
        for (int i = 1; i <= chain; i++) {
            set(x, y + i, z, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
        }
    }

    protected BlockState get(int x, int y, int z) {
        return piece.get(level, box, x, y, z);
    }

    protected void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    protected void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        fill(x0, y0, z0, x1, y1, z1, block.defaultBlockState());
    }

    protected void air(int x0, int y0, int z0, int x1, int y1, int z1) {
        fill(x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
    }

    /** Walls of a box (no floor or ceiling). */
    protected void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                set(x, y, z0, state);
                set(x, y, z1, state);
            }
            for (int z = z0; z <= z1; z++) {
                set(x0, y, z, state);
                set(x1, y, z, state);
            }
        }
    }

    /**
     * Solid ground under the whole footprint (down to existing terrain) and clear air above it.
     * Structures are placed after trees, so this also clears a glade for them.
     */
    protected void groundwork(BlockState foundation, int clearHeight) {
        int clear = Math.max(clearHeight, MIN_CLEARANCE);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                fillDown(x, g - 1, z, foundation);
                for (int y = g + 1; y <= g + clear; y++) {
                    set(x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /**
     * Fills from (x, y, z) downward through air, water and plants until it meets solid ground, so
     * floors never rest on grass or flowers (sand and gravel would fall through).
     */
    protected void fillDown(int x, int y, int z, BlockState state) {
        BlockPos.MutableBlockPos pos = world(x, y, z).mutable();
        if (!box.isInside(pos)) {
            return;
        }
        int limit = level.getMinBuildHeight() + 1;
        while (pos.getY() > limit) {
            BlockState existing = level.getBlockState(pos);
            if (!existing.isAir() && existing.getFluidState().isEmpty() && !existing.canBeReplaced()) {
                break;
            }
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            pos.move(Direction.DOWN);
        }
    }

    /** Like {@link #groundwork} but only inside a circle around the centre. */
    protected void groundworkCircle(BlockState foundation, int clearHeight, double radius) {
        int clear = Math.max(clearHeight, MIN_CLEARANCE);
        int cx = w / 2;
        int cz = d / 2;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) > radius * radius) {
                    continue;
                }
                fillDown(x, g - 1, z, foundation);
                for (int y = g + 1; y <= g + clear; y++) {
                    set(x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    protected float noise(int x, int y, int z) {
        return piece.noise(x, y, z);
    }

    /** Stone bricks with some mossy and cracked ones mixed in. */
    protected BlockState brick(int x, int y, int z) {
        float n = noise(x, y, z);
        return n < 0.2F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                : n < 0.4F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
    }

    protected BlockState deepslateBrick(int x, int y, int z) {
        float n = noise(x, y, z);
        return n < 0.25F ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                : n < 0.45F ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    protected static BlockState facing(Block block, Direction facing) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    protected static BlockState stair(Block block, Direction facing, boolean upsideDown) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
    }

    protected BlockPos world(int x, int y, int z) {
        return piece.worldPos(x, y, z);
    }

    /**
     * A banner (standing or on a wall) in {@code base} colour with pattern layers given as
     * alternating pattern keys and colours.
     */
    @SuppressWarnings("unchecked")
    protected void banner(int x, int y, int z, BlockState state, DyeColor base, Object... layers) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        set(x, y, z, state);
        if (level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
            HolderGetter<BannerPattern> patterns = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
            BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
            for (int i = 0; i + 1 < layers.length; i += 2) {
                builder.addIfRegistered(patterns, (ResourceKey<BannerPattern>) layers[i], (DyeColor) layers[i + 1]);
            }
            ItemStack item = new ItemStack(Items.WHITE_BANNER);
            item.set(DataComponents.BANNER_PATTERNS, builder.build());
            banner.fromItem(item, base);
        }
    }

    // ------------------------------------------------------------------ furniture and loot

    protected void chest(int x, int y, int z, ResourceKey<LootTable> loot) {
        piece.chest(level, box, random, x, y, z, loot);
    }

    /** A Champion's Coffer or Bandit Vault, locked, filled from a loot table when first opened. */
    protected void lockedChest(Block block, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        set(x, y, z, facing(block, facing).setValue(LockedChestBlock.LOCKED, true));
        RandomizableContainer.setBlockEntityLootTable(level, random, pos, loot);
    }

    protected void rack(int x, int y, int z, Direction facing, ItemStack... weapons) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        set(x, y, z, facing(ModBlocks.WEAPON_RACK.get(), facing));
        if (level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) {
            for (int i = 0; i < weapons.length && i < WeaponRackBlockEntity.SLOTS; i++) {
                rack.items().set(i, weapons[i]);
            }
        }
    }

    protected void pedestal(int x, int y, int z, ItemStack weapon) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        set(x, y, z, ModBlocks.LEGENDARY_PEDESTAL.get());
        if (level.getBlockEntity(pos) instanceof ItemHolderBlockEntity holder) {
            holder.setItem(weapon);
        }
    }

    /** A closed sarcophagus: the foot at (x, y, z), the head one block toward {@code headDirection}. */
    protected void sarcophagus(int x, int y, int z, Direction headDirection) {
        BlockState base = facing(ModBlocks.SARCOPHAGUS.get(), headDirection).setValue(SarcophagusBlock.OPEN, false);
        // Locally NORTH is +z and EAST is +x.
        int hx = x + headDirection.getStepX();
        int hz = z - headDirection.getStepZ();
        set(x, y, z, base.setValue(SarcophagusBlock.PART, SarcophagusBlock.Part.FOOT));
        set(hx, y, hz, base.setValue(SarcophagusBlock.PART, SarcophagusBlock.Part.HEAD));
    }

    @Nullable
    protected <T extends Mob> T spawn(EntityType<T> type, int x, int y, int z) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return null;
        }
        T mob = type.create(level.getLevel());
        if (mob == null) {
            return null;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0);
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    /** An armour stand dressed in the given gear (empty stacks for bare slots), facing local {@code facing}. */
    protected void statue(int x, int y, int z, Direction facing, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet,
                          ItemStack mainHand) {
        BlockPos pos = world(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        ArmorStand stand = EntityType.ARMOR_STAND.create(level.getLevel());
        if (stand == null) {
            return;
        }
        float yaw = piece.worldDirection(facing).toYRot();
        stand.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
        stand.setShowArms(true);
        stand.setItemSlot(EquipmentSlot.HEAD, head);
        stand.setItemSlot(EquipmentSlot.CHEST, chest);
        stand.setItemSlot(EquipmentSlot.LEGS, legs);
        stand.setItemSlot(EquipmentSlot.FEET, feet);
        stand.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
        level.addFreshEntity(stand);
    }

    protected ItemStack randomWeapon(WeaponTier... tiers) {
        WeaponType[] types = WeaponType.values();
        return new ItemStack(ModItems.weapon(types[random.nextInt(types.length)], tiers[random.nextInt(tiers.length)]).get());
    }

    protected ItemStack weapon(WeaponType type, WeaponTier tier) {
        return new ItemStack(ModItems.weapon(type, tier).get());
    }
}
