package com.steelstorm.arsenal.world.structure;

import com.steelstorm.arsenal.registry.ModStructures;
import com.steelstorm.arsenal.world.structure.build.ArmoryBuilder;
import com.steelstorm.arsenal.world.structure.build.BlacksmithBuilder;
import com.steelstorm.arsenal.world.structure.build.CampBuilder;
import com.steelstorm.arsenal.world.structure.build.ColosseumBuilder;
import com.steelstorm.arsenal.world.structure.build.CryptBuilder;
import com.steelstorm.arsenal.world.structure.build.MineBuilder;
import com.steelstorm.arsenal.world.structure.build.ProvingGroundsBuilder;
import com.steelstorm.arsenal.world.structure.build.StormShrineBuilder;
import com.steelstorm.arsenal.world.structure.build.WatchtowerBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * One building, placed block by block by the builder for its kind. Local coordinates are rotated
 * by {@link StructurePiece}'s orientation: x runs across, z runs from the entrance (z = 0) to the
 * back, and local y = {@link #floor()} is the ground floor (foundations, crypts and mine shafts
 * go below it). Block states are given facing as seen locally: SOUTH faces the entrance, NORTH
 * faces the back.
 *
 * <p>The bounding box starts one block above the floor. Terrain adaptation levels the ground to
 * the bottom of the box, so this leaves the surrounding ground flush with the floor; blocks
 * outside the box's height are still placed, because a piece may build anywhere in the chunk
 * columns it covers.</p>
 *
 * <p>Random decisions use a hash of the position, so the result is identical no matter which
 * chunk builds which part.</p>
 */
public class SteelstormStructurePiece extends StructurePiece {
    private final SteelstormStructure.Kind kind;
    private final int floor;

    public SteelstormStructurePiece(SteelstormStructure.Kind kind, BlockPos origin, Direction facing) {
        super(ModStructures.PIECE.get(), 0,
                makeBoundingBox(origin.getX(), origin.getY() + 1, origin.getZ(), facing, kind.width, kind.height, kind.depth));
        this.kind = kind;
        this.floor = -1;
        setOrientation(facing);
    }

    public SteelstormStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.PIECE.get(), tag);
        this.kind = SteelstormStructure.Kind.valueOf(tag.getString("Kind"));
        // Pieces saved by version 1.0 had the box starting two blocks below the floor.
        this.floor = tag.contains("Floor") ? tag.getInt("Floor") : 2;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", kind.name());
        tag.putInt("Floor", floor);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        switch (kind) {
            case ABANDONED_ARMORY -> new ArmoryBuilder(this, level, box, random).build();
            case BANDIT_CAMP -> new CampBuilder(this, level, box, random).build();
            case RUINED_COLOSSEUM -> new ColosseumBuilder(this, level, box, random).build();
            case PROVING_GROUNDS -> new ProvingGroundsBuilder(this, level, box, random).build();
            case KNIGHTS_CRYPT -> new CryptBuilder(this, level, box, random).build();
            case STORM_SHRINE -> new StormShrineBuilder(this, level, box, random).build();
            case BLACKSMITH -> new BlacksmithBuilder(this, level, box, random).build();
            case WATCHTOWER -> new WatchtowerBuilder(this, level, box, random).build();
            case STORMSTEEL_MINE -> new MineBuilder(this, level, box, random).build();
            case COLOSSUS_FORGE -> new com.steelstorm.arsenal.world.structure.build.ColossusForgeBuilder(this, level, box, random).build();
            case MOONLIT_SANCTUM -> new com.steelstorm.arsenal.world.structure.build.MoonlitSanctumBuilder(this, level, box, random).build();
        }
    }

    // ------------------------------------------------------------------ access for the builders

    public SteelstormStructure.Kind kind() {
        return kind;
    }

    public int width() {
        return kind.width;
    }

    public int depth() {
        return kind.depth;
    }

    /** Local y of the ground floor. */
    public int floor() {
        return floor;
    }

    public void place(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        if (level instanceof WorldGenRegion) {
            placeBlock(level, state, x, y, z, box);
            return;
        }
        // Placed into a running world (the /place command): vanilla's world-generation bookkeeping
        // for fences and ladders would only log warnings here, and the world updates shapes itself.
        BlockPos pos = getWorldPos(x, y, z);
        if (box.isInside(pos)) {
            level.setBlock(pos, state.mirror(getMirror()).rotate(getRotation()), Block.UPDATE_CLIENTS);
        }
    }

    public BlockState get(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        return getBlock(level, x, y, z, box);
    }

    public BlockPos worldPos(int x, int y, int z) {
        return getWorldPos(x, y, z);
    }

    /** The world direction that a direction given in local coordinates ends up as. */
    public Direction worldDirection(Direction local) {
        return getRotation().rotate(getMirror().mirror(local));
    }

    /** Fills from (x, y, z) downward until it meets solid ground. */
    public void columnDown(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box) {
        fillColumnDown(level, state, x, y, z, box);
    }

    public void chest(WorldGenLevel level, BoundingBox box, RandomSource random, int x, int y, int z, ResourceKey<LootTable> loot) {
        createChest(level, box, random, x, y, z, loot);
    }

    /** Deterministic 0..1 noise from local coordinates and this structure's position. */
    public float noise(int x, int y, int z) {
        long seed = Mth.getSeed(boundingBox.minX() + x * 31, y * 17 + boundingBox.minY(), boundingBox.minZ() + z * 13);
        return (float) ((seed >>> 16) & 0xFFFF) / 65535.0F;
    }
}
