package com.steelstorm.arsenal.registry;

import com.mojang.serialization.Codec;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.SteelstormConfig;
import com.steelstorm.arsenal.combat.CombatState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** NeoForge data attachments stored on players. */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SteelstormArsenal.MODID);

    /** Current stamina. Saved with the player; a fresh (or respawned) player starts full. */
    public static final Supplier<AttachmentType<Float>> STAMINA = ATTACHMENTS.register("stamina",
            () -> AttachmentType.builder(() -> SteelstormConfig.MAX_STAMINA.get().floatValue()).serialize(Codec.FLOAT).build());

    /** Short-lived combat bookkeeping (cooldowns, combo, guard timing). Not saved. */
    public static final Supplier<AttachmentType<CombatState>> COMBAT = ATTACHMENTS.register("combat",
            () -> AttachmentType.builder(CombatState::new).build());

    private ModAttachments() {
    }
}
