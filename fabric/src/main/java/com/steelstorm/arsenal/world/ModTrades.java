package com.steelstorm.arsenal.world;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.registry.ModItems;
import com.steelstorm.arsenal.weapon.Rune;
import com.steelstorm.arsenal.weapon.WeaponTier;
import com.steelstorm.arsenal.weapon.WeaponType;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.village.VillagerTradesEvent;

/** Weaponsmiths everywhere stock Steelstorm weapons, runes and Stormsteel. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class ModTrades {
    private ModTrades() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.WEAPONSMITH) {
            return;
        }
        add(event, 1, 6, 3, 2, r -> weapon(WeaponType.LONGSWORD, WeaponTier.IRON));
        add(event, 1, 3, 8, 2, r -> new ItemStack(ModItems.THROWING_KNIFE.get(), 4));
        add(event, 2, 8, 3, 10, r -> weapon(randomType(r), WeaponTier.IRON));
        add(event, 2, 8, 3, 10, r -> new ItemStack(ModItems.CHAKRAM.get()));
        add(event, 3, 4, 4, 10, r -> new ItemStack(ModItems.WHETSTONE.get()));
        add(event, 3, 12, 3, 15, r -> new ItemStack(ModItems.rune(Rune.values()[r.nextInt(Rune.values().length)]).get()));
        add(event, 4, 20, 2, 20, r -> weapon(randomType(r), WeaponTier.DIAMOND));
        add(event, 5, 16, 4, 30, r -> new ItemStack(ModItems.STORMSTEEL_INGOT.get()));
        add(event, 5, 24, 1, 30, r -> new ItemStack(ModItems.RUNE_FORGE.get()));
    }

    /** Adds an offer of {@code emeralds} emeralds for an item, worked out per villager. */
    private static void add(VillagerTradesEvent event, int level, int emeralds, int maxUses, int xp, Function<RandomSource, ItemStack> item) {
        List<VillagerTrades.ItemListing> listings = event.getTrades().get(level);
        listings.add((trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), item.apply(random), maxUses, xp, 0.05F));
    }

    private static WeaponType randomType(RandomSource random) {
        return WeaponType.values()[random.nextInt(WeaponType.values().length)];
    }

    private static ItemStack weapon(WeaponType type, WeaponTier tier) {
        return new ItemStack(ModItems.weapon(type, tier).get());
    }
}
