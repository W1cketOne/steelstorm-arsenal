package com.steelstorm.compat.client;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Stands in for NeoForge's separate_transforms loader: items show their flat icon in the GUI, on
 * the ground and in frames, and their 3D "_held" model everywhere else.
 */
public final class HeldModels {
    private static final String[] NAMES = {
            "bloodfang", "chakram", "diamond_battleaxe", "diamond_dual_daggers", "diamond_greatsword", "diamond_katana",
            "diamond_longsword", "diamond_scythe", "diamond_spear", "diamond_warhammer", "earthshaker",
            "golden_battleaxe", "golden_dual_daggers", "golden_greatsword", "golden_katana", "golden_longsword",
            "golden_scythe", "golden_spear", "golden_warhammer", "iron_battleaxe", "iron_dual_daggers",
            "iron_greatsword", "iron_katana", "iron_longsword", "iron_scythe", "iron_spear", "iron_warhammer",
            "kingsbane", "moonveil", "netherite_battleaxe", "netherite_dual_daggers", "netherite_greatsword",
            "netherite_katana", "netherite_longsword", "netherite_scythe", "netherite_spear", "netherite_warhammer",
            "rimecleaver", "skypiercer", "stone_battleaxe", "stone_dual_daggers", "stone_greatsword", "stone_katana",
            "stone_longsword", "stone_scythe", "stone_spear", "stone_warhammer", "stormsteel_battleaxe",
            "stormsteel_dual_daggers", "stormsteel_greatsword", "stormsteel_katana", "stormsteel_longsword",
            "stormsteel_scythe", "stormsteel_spear", "stormsteel_warhammer", "tempest_edge", "throwing_knife",
            "voidreaver", "solaris", "worldsplitter", "eclipse", "starfall", "soulreaper", "venomfang",
            "dragonspine", "titanbreaker",
    };
    private static final Map<Item, ResourceLocation> HELD = new HashMap<>();
    private static final Map<BakedModel, BakedModel> GLOWING = new java.util.IdentityHashMap<>();

    public static void init() {
        ModelLoadingPlugin.register(ctx -> {
            for (String name : NAMES) {
                ctx.addModels(ResourceLocation.fromNamespaceAndPath("steelstorm", "item/" + name + "_held"));
            }
        });
    }

    private static Map<Item, ResourceLocation> held() {
        if (HELD.isEmpty()) {
            for (String name : NAMES) {
                HELD.put(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("steelstorm", name)),
                        ResourceLocation.fromNamespaceAndPath("steelstorm", "item/" + name + "_held"));
            }
        }
        return HELD;
    }

    /** The model to draw for this stack in this context. */
    public static BakedModel swap(ItemStack stack, ItemDisplayContext ctx, BakedModel model) {
        if (stack.isEmpty() || ctx == ItemDisplayContext.GUI || ctx == ItemDisplayContext.GROUND || ctx == ItemDisplayContext.FIXED) {
            return model;
        }
        ResourceLocation id = held().get(stack.getItem());
        if (id == null) {
            return model;
        }
        BakedModel heldModel = Minecraft.getInstance().getModelManager().getModel(id);
        if (heldModel == null) {
            return model;
        }
        if (GLOWING.size() > 512) {
            GLOWING.clear();  // stale entries from before a resource reload
        }
        return GLOWING.computeIfAbsent(heldModel, GlowModel::new);
    }

    private HeldModels() {
    }
}
