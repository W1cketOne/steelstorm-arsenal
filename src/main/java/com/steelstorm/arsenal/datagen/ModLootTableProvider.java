package com.steelstorm.arsenal.datagen;

import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.arsenal.registry.ModEntities;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.registry.ModLootTables;
import com.steelstorm.arsenal.weapon.LegendaryWeaponItem;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModLootTableProvider {
    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(EntityLoot::new, LootContextParamSets.ENTITY),
                new LootTableProvider.SubProviderEntry(ChestLoot::new, LootContextParamSets.CHEST)
        ), lookup);
    }

    static LootItem.Builder<?> item(Item item, int weight, float min, float max) {
        return LootItem.lootTableItem(item).setWeight(weight)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }

    static LootItem.Builder<?> weapon(WeaponType type, WeaponTier tier, int weight) {
        return LootItem.lootTableItem(ModItems.weapon(type, tier).get()).setWeight(weight);
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

    public static class EntityLoot extends EntityLootSubProvider {
        public EntityLoot(HolderLookup.Provider registries) {
            super(FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        public void generate() {
            add(ModEntities.BANDIT_DUELIST.get(), LootTable.lootTable()
                    .withPool(LootPool.lootPool().add(item(Items.IRON_NUGGET, 1, 1, 4)))
                    .withPool(LootPool.lootPool().when(LootItemKilledByPlayerCondition.killedByPlayer())
                            .add(item(Items.EMERALD, 1, 1, 2)).add(EmptyLootItem.emptyItem().setWeight(2))));
            add(ModEntities.BANDIT_ARCHER.get(), LootTable.lootTable()
                    .withPool(LootPool.lootPool().add(item(Items.ARROW, 1, 2, 6)))
                    .withPool(LootPool.lootPool().add(item(ModItems.THROWING_KNIFE.get(), 1, 1, 3)).add(EmptyLootItem.emptyItem().setWeight(2)))
                    .withPool(LootPool.lootPool().when(LootItemKilledByPlayerCondition.killedByPlayer())
                            .add(item(Items.EMERALD, 1, 1, 2)).add(EmptyLootItem.emptyItem().setWeight(2))));
            add(ModEntities.IRON_REVENANT.get(), LootTable.lootTable()
                    .withPool(LootPool.lootPool().add(item(Items.ROTTEN_FLESH, 1, 0, 2)))
                    .withPool(LootPool.lootPool().add(item(Items.IRON_INGOT, 3, 1, 2)).add(item(ModItems.RAW_STORMSTEEL.get(), 1, 1, 1))
                            .add(EmptyLootItem.emptyItem().setWeight(4))));
            LootPool.Builder legendary = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
            for (LegendaryWeaponItem.Legendary l : LegendaryWeaponItem.Legendary.values()) {
                legendary.add(LootItem.lootTableItem(ModItems.legendary(l).get()));
            }
            add(ModEntities.FALLEN_WARLORD.get(), LootTable.lootTable()
                    .withPool(legendary)
                    .withPool(LootPool.lootPool().add(item(ModItems.STORMSTEEL_INGOT.get(), 1, 3, 6)))
                    .withPool(LootPool.lootPool().add(item(Items.DIAMOND, 1, 2, 4)))
                    .withPool(LootPool.lootPool().add(item(Items.NETHERITE_SCRAP, 1, 1, 2))));
        }

        @Override
        protected Stream<EntityType<?>> getKnownEntityTypes() {
            return Stream.of(ModEntities.BANDIT_DUELIST.get(), ModEntities.BANDIT_ARCHER.get(), ModEntities.IRON_REVENANT.get(),
                    ModEntities.FALLEN_WARLORD.get());
        }
    }

    public record ChestLoot(HolderLookup.Provider registries) implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            output.accept(ModLootTables.ABANDONED_ARMORY, LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 3))
                            .add(weapon(WeaponType.LONGSWORD, WeaponTier.IRON, 3)).add(weapon(WeaponType.KATANA, WeaponTier.IRON, 3))
                            .add(weapon(WeaponType.DUAL_DAGGERS, WeaponTier.IRON, 3)).add(weapon(WeaponType.SPEAR, WeaponTier.IRON, 3))
                            .add(weapon(WeaponType.WARHAMMER, WeaponTier.STONE, 3)).add(weapon(WeaponType.BATTLEAXE, WeaponTier.STONE, 3))
                            .add(weapon(WeaponType.GREATSWORD, WeaponTier.STONE, 3)).add(weapon(WeaponType.SCYTHE, WeaponTier.STONE, 3))
                            .add(weapon(WeaponType.LONGSWORD, WeaponTier.STORMSTEEL, 1))
                            .add(item(ModItems.CHAKRAM.get(), 1, 1, 1)))
                    .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 5))
                            .add(item(ModItems.RAW_STORMSTEEL.get(), 4, 1, 3)).add(item(ModItems.STORMSTEEL_INGOT.get(), 2, 1, 2))
                            .add(item(ModItems.THROWING_KNIFE.get(), 4, 2, 8)).add(item(Items.IRON_INGOT, 5, 1, 4))
                            .add(item(Items.BREAD, 4, 1, 3)).add(item(Items.ARROW, 3, 2, 8))));
            output.accept(ModLootTables.BANDIT_CAMP, LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                            .add(weapon(WeaponType.KATANA, WeaponTier.IRON, 3)).add(weapon(WeaponType.DUAL_DAGGERS, WeaponTier.IRON, 3))
                            .add(weapon(WeaponType.LONGSWORD, WeaponTier.GOLD, 2)).add(weapon(WeaponType.SPEAR, WeaponTier.IRON, 2))
                            .add(item(ModItems.CHAKRAM.get(), 2, 1, 1)).add(item(ModItems.THROWING_KNIFE.get(), 3, 4, 12)))
                    .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 6))
                            .add(item(Items.EMERALD, 3, 1, 4)).add(item(Items.GOLD_INGOT, 3, 1, 3)).add(item(Items.IRON_INGOT, 4, 1, 4))
                            .add(item(Items.COOKED_BEEF, 4, 2, 5)).add(item(Items.ARROW, 4, 4, 12)).add(item(Items.LEATHER, 3, 1, 4))
                            .add(item(Items.SADDLE, 1, 1, 1))));
            output.accept(ModLootTables.RUINED_COLOSSEUM, LootTable.lootTable()
                    .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                            .add(weapon(WeaponType.GREATSWORD, WeaponTier.DIAMOND, 2)).add(weapon(WeaponType.KATANA, WeaponTier.DIAMOND, 2))
                            .add(weapon(WeaponType.WARHAMMER, WeaponTier.STORMSTEEL, 2)).add(weapon(WeaponType.SCYTHE, WeaponTier.STORMSTEEL, 2))
                            .add(weapon(WeaponType.BATTLEAXE, WeaponTier.STORMSTEEL, 2)).add(weapon(WeaponType.LONGSWORD, WeaponTier.STORMSTEEL, 2))
                            .apply(EnchantWithLevelsFunction.enchantWithLevels(registries, UniformGenerator.between(15, 30))))
                    .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                            .when(LootItemRandomChanceCondition.randomChance(0.08F))
                            .add(LootItem.lootTableItem(ModItems.legendary(LegendaryWeaponItem.Legendary.TEMPEST_EDGE).get()))
                            .add(LootItem.lootTableItem(ModItems.legendary(LegendaryWeaponItem.Legendary.RIMECLEAVER).get()))
                            .add(LootItem.lootTableItem(ModItems.legendary(LegendaryWeaponItem.Legendary.VOIDREAVER).get())))
                    .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 6))
                            .add(item(ModItems.STORMSTEEL_INGOT.get(), 4, 2, 5)).add(item(Items.DIAMOND, 3, 1, 3))
                            .add(item(Items.GOLDEN_APPLE, 2, 1, 2)).add(item(Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 1))
                            .add(item(Items.NETHERITE_SCRAP, 1, 1, 1)).add(item(Items.EXPERIENCE_BOTTLE, 3, 2, 6))
                            .add(item(Items.GOLD_INGOT, 4, 2, 6))));
        }
    }

    private ModLootTableProvider() {
    }
}
