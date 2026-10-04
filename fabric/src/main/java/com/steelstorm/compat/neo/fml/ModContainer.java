package com.steelstorm.compat.neo.fml;

import com.steelstorm.compat.neo.fml.config.ModConfig;
import com.steelstorm.compat.neo.neoforge.common.ModConfigSpec;

/** Loads config specs from config/steelstorm-<type>.properties. */
public class ModContainer {
    public void registerConfig(ModConfig.Type type, ModConfigSpec spec) {
        spec.load("steelstorm-" + type.name().toLowerCase() + ".properties");
    }
}
