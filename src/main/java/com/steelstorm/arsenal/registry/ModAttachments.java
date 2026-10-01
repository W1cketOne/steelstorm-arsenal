package com.steelstorm.arsenal.registry;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.combat.CombatData;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SteelstormArsenal.MODID);

    /** Stamina, cooldowns and combo state. Saved with the player. */
    public static final Supplier<AttachmentType<CombatData>> COMBAT = ATTACHMENTS.register("combat",
            () -> AttachmentType.builder(CombatData::new).serialize(CombatData.CODEC).build());

    private ModAttachments() {
    }
}
