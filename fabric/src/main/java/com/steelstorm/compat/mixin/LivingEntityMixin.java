package com.steelstorm.compat.mixin;

import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingDamageEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingFallEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.steelstorm.compat.neo.neoforge.event.entity.living.MobEffectEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private float steelstorm$amount;
    @Unique
    private float steelstorm$fallDistance;
    @Unique
    private float steelstorm$fallMultiplier;
    @Unique
    private float steelstorm$healthBefore;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void steelstorm$incomingDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        steelstorm$amount = amount;
        if (self.level().isClientSide || self.isInvulnerableTo(source) || self.isDeadOrDying()) {
            return;
        }
        LivingIncomingDamageEvent event = NeoBus.post(new LivingIncomingDamageEvent(self, source, amount));
        if (event.isCanceled() || event.getAmount() <= 0) {
            cir.setReturnValue(false);
            return;
        }
        steelstorm$amount = event.getAmount();
    }

    // Applied just past vanilla's early-out checks, where NeoForge fires its event.
    @ModifyVariable(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), argsOnly = true)
    private float steelstorm$applyIncomingDamage(float amount) {
        return steelstorm$amount;
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void steelstorm$beforeDamage(DamageSource source, float amount, CallbackInfo ci) {
        steelstorm$healthBefore = ((LivingEntity) (Object) this).getHealth();
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void steelstorm$afterDamage(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        float dealt = steelstorm$healthBefore - self.getHealth();
        if (dealt > 0 && !self.level().isClientSide) {
            NeoBus.post(new LivingDamageEvent.Post(self, source, dealt));
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void steelstorm$fall(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingFallEvent event = NeoBus.post(new LivingFallEvent((LivingEntity) (Object) this, distance, multiplier));
        steelstorm$fallDistance = event.getDistance();
        steelstorm$fallMultiplier = event.getDamageMultiplier();
        if (event.isCanceled()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyArgs(method = "causeFallDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;calculateFallDamage(FF)I"))
    private void steelstorm$applyFall(Args args) {
        args.set(0, steelstorm$fallDistance);
        args.set(1, steelstorm$fallMultiplier);
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void steelstorm$effectApplicable(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        MobEffectEvent.Applicable event = NeoBus.post(new MobEffectEvent.Applicable((LivingEntity) (Object) this, effect));
        if (event.getResult() == MobEffectEvent.Applicable.Result.DO_NOT_APPLY) {
            cir.setReturnValue(false);
        } else if (event.getResult() == MobEffectEvent.Applicable.Result.APPLY) {
            cir.setReturnValue(true);
        }
    }

    // Battleaxes break shields like vanilla axes.
    @Inject(method = "canDisableShield", at = @At("RETURN"), cancellable = true)
    private void steelstorm$battleaxeShield(CallbackInfoReturnable<Boolean> cir) {
        if (((LivingEntity) (Object) this).getMainHandItem().getItem() instanceof com.steelstorm.arsenal.weapon.WeaponItem weapon
                && weapon.canDisableShield()) {
            cir.setReturnValue(true);
        }
    }

    // Ulting players can't be knocked back.
    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void steelstorm$ultKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (com.steelstorm.arsenal.combat.UltGuard.isProtected((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
