package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.block.WeaponRackBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SteelstormArsenal.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<WeaponRackBlockEntity>> WEAPON_RACK = BLOCK_ENTITIES.register("weapon_rack",
            () -> BlockEntityType.Builder.of(WeaponRackBlockEntity::new, ModBlocks.WEAPON_RACK.get()).build(null));

    private ModBlockEntities() {
    }
}
