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

    public static final KeyMapping DODGE = new KeyMapping("key.steelstorm.dodge", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping SPECIAL = new KeyMapping("key.steelstorm.special", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DODGE);
        event.register(SPECIAL);
    }

    private ModKeyMappings() {
    }
}
