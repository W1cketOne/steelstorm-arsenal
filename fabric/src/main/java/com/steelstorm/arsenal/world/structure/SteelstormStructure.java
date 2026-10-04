package com.steelstorm.arsenal.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.registry.ModStructures;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * One structure type for every Steelstorm structure; the JSON's {@code kind} field picks which one.
 * The buildings themselves are placed block by block by the builders in this package.
 */
public class SteelstormStructure extends Structure {
    public enum Kind implements StringRepresentable {
        ABANDONED_ARMORY("abandoned_armory", 11, 9, 7, 8),
        BANDIT_CAMP("bandit_camp", 19, 19, 7, 7),
        RUINED_COLOSSEUM("ruined_colosseum", 33, 33, 12, 10),
        PROVING_GROUNDS("proving_grounds", 23, 23, 8, 7),
        KNIGHTS_CRYPT("knights_crypt", 17, 25, 9, 7),
        STORM_SHRINE("storm_shrine", 19, 19, 10, 9),
        BLACKSMITH("blacksmith", 15, 13, 11, 7),
        WATCHTOWER("watchtower", 11, 11, 21, 8),
        STORMSTEEL_MINE("stormsteel_mine", 27, 17, 10, 8),
        COLOSSUS_FORGE("colossus_forge", 27, 27, 14, 9),
        MOONLIT_SANCTUM("moonlit_sanctum", 25, 25, 12, 8);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;
        final int width;
        final int depth;
        /** Layers above the floor. */
        final int height;
        /** The largest height difference across the footprint the structure accepts. */
        final int maxSlope;

        Kind(String name, int width, int depth, int height, int maxSlope) {
            this.name = name;
            this.width = width;
            this.depth = depth;
            this.height = height;
            this.maxSlope = maxSlope;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        double chance() {
            return switch (this) {
                case ABANDONED_ARMORY -> Config.ARMORY_CHANCE.get();
                case BANDIT_CAMP -> Config.BANDIT_CAMP_CHANCE.get();
                case RUINED_COLOSSEUM -> Config.COLOSSEUM_CHANCE.get();
                case PROVING_GROUNDS -> Config.PROVING_GROUNDS_CHANCE.get();
                case KNIGHTS_CRYPT -> Config.KNIGHTS_CRYPT_CHANCE.get();
                case STORM_SHRINE -> Config.STORM_SHRINE_CHANCE.get();
                case BLACKSMITH -> Config.BLACKSMITH_CHANCE.get();
                case WATCHTOWER -> Config.WATCHTOWER_CHANCE.get();
                case STORMSTEEL_MINE -> Config.STORMSTEEL_MINE_CHANCE.get();
                case COLOSSUS_FORGE, MOONLIT_SANCTUM -> Config.BOSS_LAIR_CHANCE.get();
            };
        }
    }

    public static final MapCodec<SteelstormStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Kind.CODEC.fieldOf("kind").forGetter(s -> s.kind)
    ).apply(i, SteelstormStructure::new));

    private final Kind kind;

    public SteelstormStructure(StructureSettings settings, Kind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // The structure set decides where a structure *could* go; the config decides how many do.
        if (context.random().nextDouble() >= kind.chance()) {
            return Optional.empty();
        }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        boolean alongX = facing.getAxis() == Direction.Axis.X;
        int sizeX = alongX ? kind.depth : kind.width;
        int sizeZ = alongX ? kind.width : kind.depth;
        int middleX = context.chunkPos().getMiddleBlockX();
        int middleZ = context.chunkPos().getMiddleBlockZ();
        // Try the middle of the chunk and a few spots around it; build on the flattest dry one.
        Site best = null;
        for (int[] offset : new int[][]{{0, 0}, {6, 0}, {-6, 0}, {0, 6}, {0, -6}}) {
            Site site = survey(context, middleX + offset[0], middleZ + offset[1], sizeX, sizeZ);
            if (site != null && (best == null || site.slope() < best.slope())) {
                best = site;
                if (best.slope() <= 2) {
                    break;
                }
            }
        }
        if (best == null || best.slope() > kind.maxSlope) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(best.x() - sizeX / 2, best.floor(), best.z() - sizeZ / 2);
        return Optional.of(new GenerationStub(new BlockPos(best.x(), best.floor(), best.z()),
                builder -> builder.addPiece(new SteelstormStructurePiece(kind, origin, facing))));
    }

    private record Site(int x, int z, int floor, int slope) {
    }

    /** Samples the centre and corners of a footprint; null if any of them is under water. */
    @Nullable
    private static Site survey(GenerationContext context, int x, int z, int sizeX, int sizeZ) {
        int halfX = sizeX / 2;
        int halfZ = sizeZ / 2;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        int[][] samples = {{0, 0}, {-halfX, -halfZ}, {halfX, -halfZ}, {-halfX, halfZ}, {halfX, halfZ}};
        for (int[] s : samples) {
            int h = context.chunkGenerator().getFirstOccupiedHeight(x + s[0], z + s[1], Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
            NoiseColumn column = context.chunkGenerator().getBaseColumn(x + s[0], z + s[1], context.heightAccessor(), context.randomState());
            if (!column.getBlock(h).getFluidState().isEmpty() || !column.getBlock(h - 1).getFluidState().isEmpty()) {
                return null;
            }
            min = Math.min(min, h);
            max = Math.max(max, h);
            sum += h;
        }
        return new Site(x, z, Math.round(sum / (float) samples.length), max - min);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.STEELSTORM_STRUCTURE.get();
    }
}
