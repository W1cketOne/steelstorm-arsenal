package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.combat.CombatData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
    /** Stamina, cooldowns and combo state. Saved with the player. */
    public static final AttachmentType<CombatData> COMBAT = AttachmentRegistry.<CombatData>builder()
            .persistent(CombatData.CODEC)
            .initializer(CombatData::new)
            .buildAndRegister(SteelstormArsenal.id("combat"));

    public static void init() {
    }

    private ModAttachments() {
    }
}
