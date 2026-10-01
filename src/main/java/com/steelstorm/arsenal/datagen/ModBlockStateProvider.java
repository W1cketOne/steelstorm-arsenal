package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import com.steelstorm.arsenal.block.LockedChestBlock;
import com.steelstorm.arsenal.block.SarcophagusBlock;
import com.steelstorm.arsenal.block.SignalBrazierBlock;
import com.steelstorm.arsenal.block.StormAltarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
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

        // Interactive blocks: models are drawn by tools/gen_blocks3.py.
        horizontalBlock(ModBlocks.WHETSTONE.get(), existing("whetstone"));
        simpleBlockItem(ModBlocks.WHETSTONE.get(), existing("whetstone"));
        simpleBlock(ModBlocks.RUNE_FORGE.get(), existing("rune_forge"));
        simpleBlockItem(ModBlocks.RUNE_FORGE.get(), existing("rune_forge"));
        horizontalBlock(ModBlocks.ARENA_GONG.get(), existing("arena_gong"));
        simpleBlockItem(ModBlocks.ARENA_GONG.get(), existing("arena_gong"));
        simpleBlock(ModBlocks.LEGENDARY_PEDESTAL.get(), existing("legendary_pedestal"));
        simpleBlockItem(ModBlocks.LEGENDARY_PEDESTAL.get(), existing("legendary_pedestal"));
        lockable(ModBlocks.CHAMPIONS_COFFER.get(), "champions_coffer");
        lockable(ModBlocks.BANDIT_VAULT.get(), "bandit_vault");
        getVariantBuilder(ModBlocks.STORM_ALTAR.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(existing("storm_altar_" + state.getValue(StormAltarBlock.CHARGE))).build());
        simpleBlockItem(ModBlocks.STORM_ALTAR.get(), existing("storm_altar_0"));
        getVariantBuilder(ModBlocks.SIGNAL_BRAZIER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(existing(state.getValue(SignalBrazierBlock.LIT) ? "signal_brazier_lit" : "signal_brazier")).build());
        simpleBlockItem(ModBlocks.SIGNAL_BRAZIER.get(), existing("signal_brazier"));
        getVariantBuilder(ModBlocks.SARCOPHAGUS.get()).forAllStates(state -> {
            String part = state.getValue(SarcophagusBlock.PART).getSerializedName();
            String name = "sarcophagus_" + part + (state.getValue(SarcophagusBlock.OPEN) ? "_open" : "");
            return ConfiguredModel.builder().modelFile(existing(name))
                    .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360).build();
        });
        simpleBlockItem(ModBlocks.SARCOPHAGUS.get(), existing("sarcophagus_item"));
    }

    private ModelFile existing(String name) {
        return models().getExistingFile(modLoc("block/" + name));
    }

    /** A chest that shows chains or a padlock while locked. */
    private void lockable(Block block, String name) {
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(existing(state.getValue(LockedChestBlock.LOCKED) ? name + "_locked" : name))
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360).build());
        simpleBlockItem(block, existing(name));
    }

    private void cube(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
