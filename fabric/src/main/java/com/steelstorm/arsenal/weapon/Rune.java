package com.steelstorm.arsenal.weapon;

import com.mojang.serialization.Codec;
import com.steelstorm.arsenal.combat.WeaponEffects;
import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Elemental runes inscribed on weapons at a Rune Forge. One rune per weapon. */
public enum Rune implements StringRepresentable {
    EMBER("ember", "Ember", 0xFF7A2A, "Sets enemies on fire"),
    FROST("frost", "Frost", 0x9FF3FF, "Chills enemies, slowing them"),
    STORM("storm", "Storm", 0x7FD8FF, "30% chance to arc lightning to a nearby enemy"),
    VENOM("venom", "Venom", 0x7CFF6B, "Poisons enemies"),
    VAMPIRIC("vampiric", "Vampiric", 0xD0182C, "Heals you for 8% of the damage you deal"),
    GALE("gale", "Gale", 0xD8F5FF, "Blasts enemies back with a gust of wind");

    public static final Codec<Rune> CODEC = StringRepresentable.fromEnum(Rune::values);
    public static final StreamCodec<ByteBuf, Rune> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Rune::ordinal);

    private final String id;
    private final String displayName;
    private final int color;
    private final String effect;

    Rune(String id, String displayName, int color, String effect) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.effect = effect;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int color() {
        return color;
    }

    /** English description of the effect, for the language file. */
    public String effect() {
        return effect;
    }

    public String nameKey() {
        return "rune.steelstorm." + id;
    }

    public String effectKey() {
        return "rune.steelstorm." + id + ".effect";
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** The rune's effect when a hit with the weapon lands. */
    public void onHit(Player player, LivingEntity target, float damage) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = target.getBoundingBox().getCenter();
        switch (this) {
            case EMBER -> {
                target.igniteForSeconds(3);
                Fx.burst(level, ModParticles.GLOW.get(), color, 1.3F, at, 8, 0.3, 0.06);
            }
            case FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 40));
                Fx.burst(level, ModParticles.FROST.get(), color, 1.2F, at, 8, 0.35, 0.03);
            }
            case STORM -> {
                if (player.getRandom().nextFloat() < 0.3F) {
                    WeaponEffects.zap(player, target, 4.0F);
                    Fx.sparks(level, color, at, 8, 0.6);
                    Fx.sound(level, at, ModSounds.ABILITY_ZAP, 0.6F, 1.4F);
                }
            }
            case VENOM -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                Fx.burst(level, ModParticles.GLOW.get(), color, 1.1F, at, 6, 0.3, 0.03);
            }
            case VAMPIRIC -> {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(damage * 0.08F);
                    Fx.shoot(level, ModParticles.GLOW.get(), color, 1.3F, at, player.getBoundingBox().getCenter().subtract(at).scale(0.1));
                }
            }
            case GALE -> {
                Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? player.getLookAngle() : away.normalize();
                target.setDeltaMovement(away.x * 0.8, 0.35, away.z * 0.8);
                target.hurtMarked = true;
                Fx.burst(level, ModParticles.SMOKE.get(), 0xE8F6FF, 1.0F, at, 6, 0.3, 0.08);
            }
        }
    }
}
