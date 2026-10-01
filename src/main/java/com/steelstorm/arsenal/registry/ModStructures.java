package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.world.structure.SteelstormStructure;
import com.steelstorm.arsenal.world.structure.SteelstormStructurePiece;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Structure types and pieces. Placement and biomes are data-driven JSON in data/steelstorm/worldgen. */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, SteelstormArsenal.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, SteelstormArsenal.MODID);

    public static final Supplier<StructureType<SteelstormStructure>> STEELSTORM_STRUCTURE =
            STRUCTURE_TYPES.register("steelstorm_structure", () -> () -> SteelstormStructure.CODEC);
    public static final Supplier<StructurePieceType> PIECE =
            PIECES.register("steelstorm_piece", () -> (StructurePieceType.ContextlessType) tag -> new SteelstormStructurePiece(null, tag));

    private ModStructures() {
    }
}
