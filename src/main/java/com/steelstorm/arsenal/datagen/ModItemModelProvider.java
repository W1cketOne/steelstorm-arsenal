package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, SteelstormArsenal.MODID, files);
    }

    @Override
    protected void registerModels() {
        for (DeferredItem<WeaponItem> weapon : ModItems.tieredWeapons()) {
            handheldItem(weapon.get());
        }
        basicItem(ModItems.STORMSTEEL_INGOT.get());
        basicItem(ModItems.RAW_STORMSTEEL.get());
        handheldItem(ModItems.CHAKRAM.get());
        handheldItem(ModItems.THROWING_KNIFE.get());
        basicItem(ModItems.TARGET_DUMMY.get());
    }
}
