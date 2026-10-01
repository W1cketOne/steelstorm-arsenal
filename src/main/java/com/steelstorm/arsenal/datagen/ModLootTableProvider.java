package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModItems;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModLootTableProvider {
    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)
        ), lookup);
    }

    public static class BlockLoot extends BlockLootSubProvider {
        public BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            // Ores drop raw Stormsteel (Fortune applies), or themselves with Silk Touch.
            add(ModBlocks.STORMSTEEL_ORE.get(), block -> createOreDrop(block, ModItems.RAW_STORMSTEEL.get()));
            add(ModBlocks.DEEPSLATE_STORMSTEEL_ORE.get(), block -> createOreDrop(block, ModItems.RAW_STORMSTEEL.get()));
            dropSelf(ModBlocks.STORMSTEEL_BLOCK.get());
            dropSelf(ModBlocks.RAW_STORMSTEEL_BLOCK.get());
            dropSelf(ModBlocks.WEAPON_RACK.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::value).map(b -> (Block) b).toList();
        }
    }

    private ModLootTableProvider() {
    }
}
