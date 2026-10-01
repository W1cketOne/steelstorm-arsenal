package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModTags;
import com.steelstorm.arsenal.weapon.WeaponItem;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                              CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper files) {
        super(output, lookup, blockTags, SteelstormArsenal.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredItem<WeaponItem> weapon : ModItems.tieredWeapons()) {
            tag(ModTags.Items.MELEE_WEAPONS).add(weapon.get());
        }
        // Joining minecraft:swords makes every vanilla sword enchantment (Sharpness, Smite, Looting,
        // Fire Aspect, Knockback, Sweeping Edge, Unbreaking, Mending...) apply to our melee weapons.
        tag(ItemTags.SWORDS).addTag(ModTags.Items.MELEE_WEAPONS);
        tag(Tags.Items.MELEE_WEAPON_TOOLS).addTag(ModTags.Items.MELEE_WEAPONS);
        tag(ModTags.Items.STORMSTEEL_INGOTS).add(ModItems.STORMSTEEL_INGOT.get());
        tag(Tags.Items.INGOTS).addTag(ModTags.Items.STORMSTEEL_INGOTS);
    }
}
