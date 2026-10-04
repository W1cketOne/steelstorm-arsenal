package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import com.steelstorm.compat.neo.neoforge.registries.DeferredHolder;
import com.steelstorm.compat.neo.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SteelstormArsenal.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARSENAL = TABS.register("arsenal", () -> net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.steelstorm.arsenal"))
            .icon(() -> ModItems.weapon(WeaponType.LONGSWORD, WeaponTier.STORMSTEEL).get().getDefaultInstance())
            // Everything the mod registers goes in this tab, in registration order.
            .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());

    private ModCreativeTabs() {
    }
}
