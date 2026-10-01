package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point. Never loaded on a dedicated server. */
@Mod(value = SteelstormArsenal.MODID, dist = Dist.CLIENT)
public class SteelstormClient {
    public SteelstormClient(ModContainer container) {
        // Mods screen > Steelstorm Arsenal > Config edits both the common and client settings.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
