package com.steelstorm.arsenal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common (gameplay + world generation) settings. Values are read on the logical server,
 * which is authoritative for every combat number.
 */
public final class SteelstormConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- World ----
    static {
        BUILDER.comment("World generation").push("structures");
    }

    public static final ModConfigSpec.BooleanValue STARTER_OUTPOST = BUILDER
            .comment("Place the Warrior's Outpost next to the spawn point when a new world is created.")
            .define("starterOutpost", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SteelstormConfig() {
    }
}
