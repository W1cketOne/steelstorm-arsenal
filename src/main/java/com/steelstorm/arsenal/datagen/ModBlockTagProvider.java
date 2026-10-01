package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, SteelstormArsenal.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
    }
}
