package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    private ModArmorMaterials() {
    }
}
