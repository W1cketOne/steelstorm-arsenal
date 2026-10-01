package com.steelstorm.arsenal.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.steelstorm.arsenal.Config;
import com.steelstorm.arsenal.registry.ModStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * One structure type for all three Steelstorm structures; the JSON's {@code kind} field picks which
 * one. The buildings themselves are placed block by block in {@link SteelstormStructurePiece}.
 */
public class SteelstormStructure extends Structure {
    public enum Kind implements StringRepresentable {
        ABANDONED_ARMORY("abandoned_armory", 11, 9),
        BANDIT_CAMP("bandit_camp", 17, 17),
        RUINED_COLOSSEUM("ruined_colosseum", 33, 33);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;
        final int width;
        final int depth;

        Kind(String name, int width, int depth) {
            this.name = name;
            this.width = width;
            this.depth = depth;
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
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        // Sample the four corners and centre: skip steep or underwater spots.
        int halfW = kind.width / 2;
        int halfD = kind.depth / 2;
        int[][] samples = {{0, 0}, {-halfW, -halfD}, {halfW, -halfD}, {-halfW, halfD}, {halfW, halfD}};
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        for (int[] s : samples) {
            int h = context.chunkGenerator().getFirstOccupiedHeight(x + s[0], z + s[1], Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
            min = Math.min(min, h);
            max = Math.max(max, h);
            sum += h;
            NoiseColumn column = context.chunkGenerator().getBaseColumn(x + s[0], z + s[1], context.heightAccessor(), context.randomState());
            if (!column.getBlock(h).getFluidState().isEmpty() || !column.getBlock(h - 1).getFluidState().isEmpty()) {
                return Optional.empty();
            }
        }
        int maxSlope = kind == Kind.RUINED_COLOSSEUM ? 8 : 5;
        if (max - min > maxSlope) {
            return Optional.empty();
        }
        int floor = Math.round(sum / (float) samples.length);
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        BlockPos origin = new BlockPos(x - halfW, floor, z - halfD);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new SteelstormStructurePiece(kind, origin, facing))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.STEELSTORM_STRUCTURE.get();
    }
}
