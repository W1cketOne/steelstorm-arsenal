package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import com.steelstorm.arsenal.world.outpost.CombatManual;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SteelstormArsenal.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARSENAL = TABS.register("arsenal", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.steelstorm"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> ModItems.weapon(WeaponType.LONGSWORD, WeaponTier.STORMSTEEL).get().getDefaultInstance())
            .displayItems((params, output) -> {
                for (WeaponType type : WeaponType.values()) {
                    for (WeaponTier tier : WeaponTier.values()) {
                        output.accept(ModItems.weapon(type, tier).get());
                    }
                }
                output.accept(ModItems.STORMSTEEL_INGOT.get());
                output.accept(CombatManual.create());
            })
            .build());

    private ModCreativeTabs() {
    }
}
