package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.WeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, SteelstormArsenal.MODID, files);
    }

    @Override
    protected void registerModels() {
        ModItems.weapons().values().forEach(byTier -> byTier.values().forEach(holder -> weapon(holder.get())));
        basicItem(ModItems.STORMSTEEL_INGOT.get());
    }

    private void weapon(WeaponItem item) {
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        String parent = item.getWeaponType().large() ? "item/handheld_large" : "item/handheld_weapon";
        withExistingParent(name, modLoc(parent)).texture("layer0", modLoc("item/" + name));
    }
}
