package com.steelstorm.arsenal.registry;

import com.mojang.serialization.Codec;
import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components (1.21 replacement for item NBT). */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SteelstormArsenal.MODID);

    /** Number of foes slain with this weapon. Persistent and shown in the tooltip. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KILL_COUNT =
            REGISTER.registerComponentType("kill_count", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Set while a spear is being wound up for a throw (sneak + use) instead of guarding. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> THROW_READY =
            REGISTER.registerComponentType("throw_ready", b -> b.networkSynchronized(ByteBufCodecs.BOOL));

    private ModDataComponents() {
    }
}
