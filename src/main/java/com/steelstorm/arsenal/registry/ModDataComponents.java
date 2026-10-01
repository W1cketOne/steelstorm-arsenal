package com.steelstorm.arsenal.registry;

import com.mojang.serialization.Codec;
import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import com.steelstorm.arsenal.weapon.Rune;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data lives in data components (not NBT) in 1.21.1. */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SteelstormArsenal.MODID);

    /** Set on a spear while it is being wound up for a throw (sneak + hold Use) instead of guarding. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> THROW_MODE =
            COMPONENTS.registerComponentType("throw_mode", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Hits left on a Whetstone sharpening (+2 damage each). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SHARPENED =
            COMPONENTS.registerComponentType("sharpened", b -> b.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The elemental rune inscribed on a weapon at a Rune Forge. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Rune>> RUNE =
            COMPONENTS.registerComponentType("rune", b -> b.persistent(Rune.CODEC).networkSynchronized(Rune.STREAM_CODEC));

    /** Which structure an Explorer's Compass searches for (an index into its list). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COMPASS_TARGET =
            COMPONENTS.registerComponentType("compass_target", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }
}
