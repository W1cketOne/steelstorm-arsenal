package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                               CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper files) {
        super(output, lookup, blockTags, SteelstormArsenal.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var melee = tag(ModTags.Items.MELEE_WEAPONS);
        ModItems.weapons().values().forEach(byTier -> byTier.values().forEach(w -> melee.add(w.get())));
        // Being a "sword" lets every vanilla sword enchantment (Sharpness, Looting, Sweeping Edge, ...) apply.
        tag(ItemTags.SWORDS).addTag(ModTags.Items.MELEE_WEAPONS);
    }
}
