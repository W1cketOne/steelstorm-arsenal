package com.steelstorm.arsenal.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.steelstorm.arsenal.SteelstormArsenal;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.steelstorm";

    public static final KeyMapping DODGE = key("dodge", GLFW.GLFW_KEY_LEFT_ALT);
    public static final KeyMapping ABILITY_1 = key("ability_1", GLFW.GLFW_KEY_R);
    public static final KeyMapping ABILITY_2 = key("ability_2", GLFW.GLFW_KEY_G);
    public static final KeyMapping ABILITY_3 = key("ability_3", GLFW.GLFW_KEY_V);
    public static final KeyMapping ULTIMATE = key("ultimate", GLFW.GLFW_KEY_Z);
    /** The ability keys in slot order. */
    public static final KeyMapping[] ABILITIES = {ABILITY_1, ABILITY_2, ABILITY_3, ULTIMATE};

    private static KeyMapping key(String name, int defaultKey) {
        return new KeyMapping("key.steelstorm." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DODGE);
        for (KeyMapping key : ABILITIES) {
            event.register(key);
        }
    }

    private ModKeyMappings() {
    }
}
