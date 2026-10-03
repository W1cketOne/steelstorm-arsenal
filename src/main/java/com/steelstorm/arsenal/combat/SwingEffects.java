package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.fx.Fx;
import com.steelstorm.arsenal.registry.ModParticles;
import com.steelstorm.arsenal.registry.ModSounds;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponLooks;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Cosmetic swing feedback for other players. The swinging player's own client already drew the
 * slash and played the whoosh the instant they clicked; this repeats it for everyone nearby.
 */
public final class SwingEffects {
    public static void onSwing(ServerPlayer player, boolean heavy, float roll) {
        WeaponItem weapon = CombatUtil.heldWeapon(player);
        if (weapon == null || !player.isAlive() || player.isSpectator()) {
            return;
        }
        CombatData data = Stamina.data(player);
        long now = player.level().getGameTime();
        // Cosmetic only, but still don't let a modified client spam everyone's screens.
        if (now - data.lastSwingFx < 4) {
            return;
        }
        data.lastSwingFx = now;
        Fx.soundForOthers(player, heavy ? ModSounds.WEAPON_SWING_HEAVY : ModSounds.WEAPON_SWING, heavy ? 1.0F : 0.8F,
                0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    private SwingEffects() {
    }
}
