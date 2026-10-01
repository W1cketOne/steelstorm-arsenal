package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.block.ArenaGongBlock;
import com.steelstorm.arsenal.block.LegendaryPedestalBlock;
import com.steelstorm.arsenal.block.LockedChestBlock;
import com.steelstorm.arsenal.block.RuneForgeBlock;
import com.steelstorm.arsenal.block.SarcophagusBlock;
import com.steelstorm.arsenal.block.SignalBrazierBlock;
import com.steelstorm.arsenal.block.StormAltarBlock;
import com.steelstorm.arsenal.block.WeaponRackBlock;
import com.steelstorm.arsenal.block.WhetstoneBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SteelstormArsenal.MODID);

    public static final DeferredBlock<Block> STORMSTEEL_ORE = BLOCKS.register("stormsteel_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3), BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5F, 3.0F)));
    public static final DeferredBlock<Block> DEEPSLATE_STORMSTEEL_ORE = BLOCKS.register("deepslate_stormsteel_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3), BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 3.0F).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> STORMSTEEL_BLOCK = BLOCKS.register("stormsteel_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops().strength(5.5F, 7.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> RAW_STORMSTEEL_BLOCK = BLOCKS.register("raw_stormsteel_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F)));
    public static final DeferredBlock<WeaponRackBlock> WEAPON_RACK = BLOCKS.register("weapon_rack",
            () -> new WeaponRackBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));

    // Interactive blocks found in structures (and craftable where it makes sense).
    public static final DeferredBlock<WhetstoneBlock> WHETSTONE = BLOCKS.register("whetstone",
            () -> new WhetstoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 6.0F)
                    .sound(SoundType.STONE).noOcclusion()));
    public static final DeferredBlock<RuneForgeBlock> RUNE_FORGE = BLOCKS.register("rune_forge",
            () -> new RuneForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F).sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 7).noOcclusion()));
    public static final DeferredBlock<ArenaGongBlock> ARENA_GONG = BLOCKS.register("arena_gong",
            () -> new ArenaGongBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<LockedChestBlock> CHAMPIONS_COFFER = BLOCKS.register("champions_coffer",
            () -> new LockedChestBlock(LockedChestBlock.Kind.COFFER, BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .strength(2.5F, 1200.0F).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 4)));
    public static final DeferredBlock<LockedChestBlock> BANDIT_VAULT = BLOCKS.register("bandit_vault",
            () -> new LockedChestBlock(LockedChestBlock.Kind.VAULT, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5F, 1200.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<SarcophagusBlock> SARCOPHAGUS = BLOCKS.register("sarcophagus",
            () -> new SarcophagusBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops()
                    .strength(3.0F, 9.0F).sound(SoundType.STONE).noOcclusion()));
    public static final DeferredBlock<LegendaryPedestalBlock> LEGENDARY_PEDESTAL = BLOCKS.register("legendary_pedestal",
            () -> new LegendaryPedestalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops()
                    .strength(3.0F, 1200.0F).sound(SoundType.STONE).lightLevel(s -> 6).noOcclusion()));
    public static final DeferredBlock<StormAltarBlock> STORM_ALTAR = BLOCKS.register("storm_altar",
            () -> new StormAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> 3 + s.getValue(StormAltarBlock.CHARGE) * 3)));
    public static final DeferredBlock<SignalBrazierBlock> SIGNAL_BRAZIER = BLOCKS.register("signal_brazier",
            () -> new SignalBrazierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(s -> s.getValue(SignalBrazierBlock.LIT) ? 15 : 0)));

    private ModBlocks() {
    }
}
