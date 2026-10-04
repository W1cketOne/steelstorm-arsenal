package com.steelstorm.arsenal.client;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.SteelstormBoss;
import com.steelstorm.arsenal.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.client.event.ClientTickEvent;

/** Battle music that fades in while a Steelstorm boss is nearby and fades out once it is gone. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class BossMusic {
    private static final double RANGE = 40.0;

    private static final class Track extends AbstractTickableSoundInstance {
        boolean wanted = true;

        Track() {
            super(ModSounds.MUSIC_BOSS.get(), SoundSource.MUSIC, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            volume = wanted ? Math.min(1.0F, volume + 0.03F) : volume - 0.02F;
            if (volume <= 0) {
                stop();
            }
        }
    }

    private static Track track;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            track = null;
            return;
        }
        boolean boss = false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof SteelstormBoss b && b.isAlive() && b.distanceToSqr(mc.player) < RANGE * RANGE) {
                boss = true;
                break;
            }
        }
        if (boss) {
            mc.getMusicManager().stopPlaying();
            if (track == null || track.isStopped()) {
                track = new Track();
                mc.getSoundManager().play(track);
            }
            track.wanted = true;
        } else if (track != null) {
            track.wanted = false;
            if (track.isStopped()) {
                track = null;
            }
        }
    }

    private BossMusic() {
    }
}
