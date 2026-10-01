package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, SteelstormArsenal.MODID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        cube(ModBlocks.STORMSTEEL_ORE.get());
        cube(ModBlocks.DEEPSLATE_STORMSTEEL_ORE.get());
        cube(ModBlocks.STORMSTEEL_BLOCK.get());
        cube(ModBlocks.RAW_STORMSTEEL_BLOCK.get());
        // The rack's model has custom elements, so it is hand-written in src/main/resources.
        ModelFile rack = models().getExistingFile(modLoc("block/weapon_rack"));
        horizontalBlock(ModBlocks.WEAPON_RACK.get(), rack);
        simpleBlockItem(ModBlocks.WEAPON_RACK.get(), rack);
    }

    private void cube(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
