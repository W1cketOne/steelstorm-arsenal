package com.steelstorm.arsenal.registry;

import com.mojang.serialization.Codec;
import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data lives in data components (not NBT) in 1.21.1. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SteelstormArsenal.MODID);

    /** Set on a spear while it is being wound up for a throw (sneak + hold Use) instead of guarding. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> THROW_MODE =
            COMPONENTS.registerComponentType("throw_mode", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private ModDataComponents() {
    }
}
