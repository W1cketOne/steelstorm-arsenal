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
    private static HumanoidModel<LivingEntity> vOuter;
    private static HumanoidModel<LivingEntity> vInner;
    private static HumanoidModel<LivingEntity> wOuter;
    private static HumanoidModel<LivingEntity> wInner;

    private static EntityModelSet bakedFrom;
    private static HumanoidModel<LivingEntity> outer;
    private static HumanoidModel<LivingEntity> inner;

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
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (stack.getItem() instanceof com.steelstorm.arsenal.item.VoidwalkerArmorItem) {
                    model(false, false);
                    return slot == EquipmentSlot.LEGS ? vInner : vOuter;
                }
                return model(slot == EquipmentSlot.LEGS, stack.getItem() instanceof com.steelstorm.arsenal.item.WarlordArmorItem);
            }
        };
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item instanceof StormsteelArmorItem || item instanceof com.steelstorm.arsenal.item.WarlordArmorItem
                    || item instanceof com.steelstorm.arsenal.item.VoidwalkerArmorItem) {
                event.registerItem(extensions, item);
            }
        });
    }

    private static HumanoidModel<LivingEntity> model(boolean legs, boolean warlord) {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (models != bakedFrom || outer == null) {
            // Re-bake after resource reloads.
            outer = new HumanoidModel<>(models.bakeLayer(OUTER));
            inner = new HumanoidModel<>(models.bakeLayer(INNER));
            wOuter = new HumanoidModel<>(models.bakeLayer(W_OUTER));
            wInner = new HumanoidModel<>(models.bakeLayer(W_INNER));
            vOuter = new HumanoidModel<>(models.bakeLayer(V_OUTER));
            vInner = new HumanoidModel<>(models.bakeLayer(V_INNER));
            bakedFrom = models;
        }
        if (warlord) {
            return legs ? wInner : wOuter;
        }
        return legs ? inner : outer;
    }
}
