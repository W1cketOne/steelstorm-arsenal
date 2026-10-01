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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Renders Stormsteel armour with its own 3D model (pauldrons, crest, plates) instead of the flat vanilla shell. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class StormsteelArmorRendering {
    public static final ModelLayerLocation OUTER = new ModelLayerLocation(SteelstormArsenal.id("stormsteel_armor"), "outer");
    public static final ModelLayerLocation INNER = new ModelLayerLocation(SteelstormArsenal.id("stormsteel_armor"), "inner");

    private static EntityModelSet bakedFrom;
    private static HumanoidModel<LivingEntity> outer;
    private static HumanoidModel<LivingEntity> inner;

    private StormsteelArmorRendering() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OUTER, StormsteelArmorLayers::outer);
        event.registerLayerDefinition(INNER, StormsteelArmorLayers::inner);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return model(slot == EquipmentSlot.LEGS);
            }
        };
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item instanceof StormsteelArmorItem) {
                event.registerItem(extensions, item);
            }
        });
    }

    private static HumanoidModel<LivingEntity> model(boolean legs) {
        EntityModelSet models = Minecraft.getInstance().getEntityModels();
        if (models != bakedFrom || outer == null) {
            // Re-bake after resource reloads.
            outer = new HumanoidModel<>(models.bakeLayer(OUTER));
            inner = new HumanoidModel<>(models.bakeLayer(INNER));
            bakedFrom = models;
        }
        return legs ? inner : outer;
    }
}
