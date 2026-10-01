package com.steelstorm.arsenal.network;

import com.steelstorm.arsenal.SteelstormArsenal;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: everything the HUD needs. Cooldowns are ticks remaining for the four
 * ability slots of the weapon set named by `setId` (what the server thinks is held).
 */
public record CombatSyncPayload(float stamina, float maxStamina, float ultimate, int combo, int dodgeCooldownLeft, String setId,
                                List<Integer> cooldownLeft, List<Integer> cooldownTotal) implements CustomPacketPayload {
    public static final Type<CombatSyncPayload> TYPE = new Type<>(SteelstormArsenal.id("combat_sync"));
    public static final StreamCodec<ByteBuf, CombatSyncPayload> STREAM_CODEC = StreamCodec.of(CombatSyncPayload::write, CombatSyncPayload::read);
    private static final int MAX_SLOTS = 8;

    private static void write(ByteBuf buf, CombatSyncPayload p) {
        buf.writeFloat(p.stamina);
        buf.writeFloat(p.maxStamina);
        buf.writeFloat(p.ultimate);
        VarInt.write(buf, p.combo);
        VarInt.write(buf, p.dodgeCooldownLeft);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.setId);
        writeInts(buf, p.cooldownLeft);
        writeInts(buf, p.cooldownTotal);
    }

    private static CombatSyncPayload read(ByteBuf buf) {
        return new CombatSyncPayload(buf.readFloat(), buf.readFloat(), buf.readFloat(), VarInt.read(buf), VarInt.read(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), readInts(buf), readInts(buf));
    }

    private static void writeInts(ByteBuf buf, List<Integer> values) {
        VarInt.write(buf, Math.min(MAX_SLOTS, values.size()));
        for (int i = 0; i < Math.min(MAX_SLOTS, values.size()); i++) {
            VarInt.write(buf, values.get(i));
        }
    }

    private static List<Integer> readInts(ByteBuf buf) {
        int n = Math.min(MAX_SLOTS, VarInt.read(buf));
        List<Integer> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(VarInt.read(buf));
        }
        return out;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
