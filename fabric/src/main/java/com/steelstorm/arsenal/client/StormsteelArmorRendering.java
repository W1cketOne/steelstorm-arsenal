package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.item.StormsteelArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.client.event.EntityRenderersEvent;
import com.steelstorm.compat.neo.neoforge.client.extensions.common.IClientItemExtensions;
import com.steelstorm.compat.neo.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Renders Stormsteel armour with its own 3D model (pauldrons, crest, plates) instead of the flat vanilla shell. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class StormsteelArmorRendering {
    public static final ModelLayerLocation OUTER = new ModelLayerLocation(SteelstormArsenal.id("stormsteel_armor"), "outer");
    public static final ModelLayerLocation INNER = new ModelLayerLocation(SteelstormArsenal.id("stormsteel_armor"), "inner");
    public static final ModelLayerLocation W_OUTER = new ModelLayerLocation(SteelstormArsenal.id("warlord_armor"), "outer");
    public static final ModelLayerLocation W_INNER = new ModelLayerLocation(SteelstormArsenal.id("warlord_armor"), "inner");
    public static final ModelLayerLocation V_OUTER = new ModelLayerLocation(SteelstormArsenal.id("voidwalker_armor"), "outer");
    public static final ModelLayerLocation V_INNER = new ModelLayerLocation(SteelstormArsenal.id("voidwalker_armor"), "inner");
    public static final ModelLayerLocation C_OUTER = new ModelLayerLocation(SteelstormArsenal.id("celestial_armor"), "outer");
    public static final ModelLayerLocation C_INNER = new ModelLayerLocation(SteelstormArsenal.id("celestial_armor"), "inner");
    public static final ModelLayerLocation D_OUTER = new ModelLayerLocation(SteelstormArsenal.id("dragonscale_armor"), "outer");
    public static final ModelLayerLocation D_INNER = new ModelLayerLocation(SteelstormArsenal.id("dragonscale_armor"), "inner");

    private static EntityModelSet bakedFrom;
    private static final java.util.Map<ModelLayerLocation, HumanoidModel<LivingEntity>> MODELS = new java.util.HashMap<>();

    private StormsteelArmorRendering() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OUTER, StormsteelArmorLayers::outer);
        event.registerLayerDefinition(INNER, StormsteelArmorLayers::inner);
        event.registerLayerDefinition(W_OUTER, WarlordArmorLayers::outer);
        event.registerLayerDefinition(W_INNER, WarlordArmorLayers::inner);
        event.registerLayerDefinition(V_OUTER, VoidwalkerArmorLayers::outer);
        event.registerLayerDefinition(V_INNER, VoidwalkerArmorLayers::inner);
        event.registerLayerDefinition(C_OUTER, CelestialArmorLayers::outer);
        event.registerLayerDefinition(C_INNER, CelestialArmorLayers::inner);
        event.registerLayerDefinition(D_OUTER, DragonscaleArmorLayers::outer);
        event.registerLayerDefinition(D_INNER, DragonscaleArmorLayers::inner);
    }

    /** Whether this item is drawn with one of the mod's 3D armour models. */
    public static boolean isModArmor(net.minecraft.world.item.Item item) {
        return item instanceof StormsteelArmorItem || item instanceof com.steelstorm.arsenal.item.WarlordArmorItem
                || item instanceof com.steelstorm.arsenal.item.VoidwalkerArmorItem || item instanceof com.steelstorm.arsenal.item.CelestialArmorItem
                || item instanceof com.steelstorm.arsenal.item.DragonscaleArmorItem;
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                boolean legs = slot == EquipmentSlot.LEGS;
                net.minecraft.world.item.Item item = stack.getItem();
                if (item instanceof com.steelstorm.arsenal.item.VoidwalkerArmorItem) {
                    return model(legs ? V_INNER : V_OUTER);
                }
                if (item instanceof com.steelstorm.arsenal.item.WarlordArmorItem) {
                    return model(legs ? W_INNER : W_OUTER);
                }
                if (item instanceof com.steelstorm.arsenal.item.CelestialArmorItem) {
                    return model(legs ? C_INNER : C_OUTER);
                }
                if (item instanceof com.steelstorm.arsenal.item.DragonscaleArmorItem) {
                    return model(legs ? D_INNER : D_OUTER);
                }
                return model(legs ? INNER : OUTER);
            }
        };
        BuiltInRegistries.ITEM.forEach(item -> {
            if (isModArmor(item)) {
                event.registerItem(extensions, item);
            }
        });
    }

    private static HumanoidModel<LivingEntity> model(ModelLayerLocation layer) {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (models != bakedFrom) {
            // Re-bake after resource reloads.
            MODELS.clear();
            bakedFrom = models;
        }
        return MODELS.computeIfAbsent(layer, l -> new HumanoidModel<>(models.bakeLayer(l)));
    }
}
