package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import com.steelstorm.compat.neo.neoforge.registries.DeferredHolder;
import com.steelstorm.compat.neo.neoforge.registries.DeferredRegister;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, SteelstormArsenal.MODID);

    /** Between diamond and netherite: diamond protection with some toughness. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STORMSTEEL = MATERIALS.register("stormsteel", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(ModItems.STORMSTEEL_INGOT.get()),
                List.of(new ArmorMaterial.Layer(SteelstormArsenal.id("stormsteel"))), 2.5F, 0.05F);
    });

    /** Netherite-grade plate with extra toughness: the Ember Warlord set. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WARLORD = MATERIALS.register("warlord", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT),
                List.of(new ArmorMaterial.Layer(SteelstormArsenal.id("warlord"))), 3.5F, 0.15F);
    });

    /** Diamond-grade plate, light and quick: the Voidwalker set. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> VOIDWALKER = MATERIALS.register("voidwalker", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 20, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(net.minecraft.world.item.Items.AMETHYST_SHARD),
                List.of(new ArmorMaterial.Layer(SteelstormArsenal.id("voidwalker"))), 2.5F, 0.0F);
    });

    /** Above netherite: the Celestial set, blessed gold-and-white plate. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CELESTIAL = MATERIALS.register("celestial", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 5);
        defense.put(ArmorItem.Type.LEGGINGS, 8);
        defense.put(ArmorItem.Type.CHESTPLATE, 10);
        defense.put(ArmorItem.Type.HELMET, 5);
        defense.put(ArmorItem.Type.BODY, 14);
        return new ArmorMaterial(defense, 25, SoundEvents.ARMOR_EQUIP_GOLD, () -> Ingredient.of(net.minecraft.world.item.Items.NETHER_STAR),
                List.of(new ArmorMaterial.Layer(SteelstormArsenal.id("celestial"))), 6.0F, 0.3F);
    });

    /** The strongest plate in the game: Dragonscale, forged from the dragon's own hide. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DRAGONSCALE = MATERIALS.register("dragonscale", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 6);
        defense.put(ArmorItem.Type.LEGGINGS, 9);
        defense.put(ArmorItem.Type.CHESTPLATE, 11);
        defense.put(ArmorItem.Type.HELMET, 6);
        defense.put(ArmorItem.Type.BODY, 15);
        return new ArmorMaterial(defense, 22, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(net.minecraft.world.item.Items.DRAGON_BREATH),
                List.of(new ArmorMaterial.Layer(SteelstormArsenal.id("dragonscale"))), 7.0F, 0.4F);
    });

    private ModArmorMaterials() {
    }
}
