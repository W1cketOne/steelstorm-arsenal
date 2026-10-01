package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, SteelstormArsenal.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.STORMSTEEL_ORE.get(), ModBlocks.DEEPSLATE_STORMSTEEL_ORE.get(),
                ModBlocks.STORMSTEEL_BLOCK.get(), ModBlocks.RAW_STORMSTEEL_BLOCK.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.STORMSTEEL_ORE.get(), ModBlocks.DEEPSLATE_STORMSTEEL_ORE.get(),
                ModBlocks.STORMSTEEL_BLOCK.get(), ModBlocks.RAW_STORMSTEEL_BLOCK.get());
        tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.WEAPON_RACK.get());
        tag(ModTags.Blocks.STORMSTEEL_ORES).add(ModBlocks.STORMSTEEL_ORE.get(), ModBlocks.DEEPSLATE_STORMSTEEL_ORE.get());
        tag(Tags.Blocks.ORES).addTag(ModTags.Blocks.STORMSTEEL_ORES);
        tag(Tags.Blocks.ORES_IN_GROUND_STONE).add(ModBlocks.STORMSTEEL_ORE.get());
        tag(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).add(ModBlocks.DEEPSLATE_STORMSTEEL_ORE.get());
        tag(ModTags.Blocks.STORMSTEEL_STORAGE_BLOCKS).add(ModBlocks.STORMSTEEL_BLOCK.get());
        tag(Tags.Blocks.STORAGE_BLOCKS).addTag(ModTags.Blocks.STORMSTEEL_STORAGE_BLOCKS);
        tag(BlockTags.BEACON_BASE_BLOCKS).add(ModBlocks.STORMSTEEL_BLOCK.get());
    }
}
