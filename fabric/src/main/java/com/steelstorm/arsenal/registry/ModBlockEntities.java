package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.block.ArenaGongBlockEntity;
import com.steelstorm.arsenal.block.ItemHolderBlockEntity;
import com.steelstorm.arsenal.block.LockedChestBlockEntity;
import com.steelstorm.arsenal.block.WeaponRackBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.steelstorm.compat.neo.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SteelstormArsenal.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<WeaponRackBlockEntity>> WEAPON_RACK = BLOCK_ENTITIES.register("weapon_rack",
            () -> BlockEntityType.Builder.of(WeaponRackBlockEntity::new, ModBlocks.WEAPON_RACK.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<ItemHolderBlockEntity>> ITEM_HOLDER = BLOCK_ENTITIES.register("item_holder",
            () -> BlockEntityType.Builder.of(ItemHolderBlockEntity::new, ModBlocks.RUNE_FORGE.get(), ModBlocks.LEGENDARY_PEDESTAL.get())
                    .build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<LockedChestBlockEntity>> LOCKED_CHEST = BLOCK_ENTITIES.register("locked_chest",
            () -> BlockEntityType.Builder.of(LockedChestBlockEntity::new, ModBlocks.CHAMPIONS_COFFER.get(), ModBlocks.BANDIT_VAULT.get())
                    .build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<ArenaGongBlockEntity>> ARENA_GONG = BLOCK_ENTITIES.register("arena_gong",
            () -> BlockEntityType.Builder.of(ArenaGongBlockEntity::new, ModBlocks.ARENA_GONG.get()).build(null));

    private ModBlockEntities() {
    }
}
