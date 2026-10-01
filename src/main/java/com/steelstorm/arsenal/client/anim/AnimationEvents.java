package com.steelstorm.arsenal.client.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.anim.CastPose;
import com.steelstorm.arsenal.weapon.WeaponItem;
import com.steelstorm.arsenal.weapon.WeaponType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Hooks the weapon animations into item and player rendering. */
@EventBusSubscriber(modid = SteelstormArsenal.MODID, value = Dist.CLIENT)
public final class AnimationEvents {
    private AnimationEvents() {
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        BuiltInRegistries.ITEM.forEach(item -> {
            if (item instanceof WeaponItem weapon && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(SteelstormArsenal.MODID)) {
                event.registerItem(new Extensions(weapon.type()), item);
            }
        });
    }

    private record Extensions(WeaponType type) implements IClientItemExtensions {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            if (hand != InteractionHand.MAIN_HAND) {
                return null;
            }
            return (WeaponAnimator.isTwoHanded(type) ? WeaponPoses.TWO_HANDED : WeaponPoses.ONE_HANDED).getValue();
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
                                               float equip, float swing) {
            // Guarding and spear throws keep vanilla's poses.
            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0) {
                return false;
            }
            if (arm != player.getMainArm()) {
                return false;
            }
            WeaponAnimator.poseFirstPerson(pose, player, arm, type, partialTick, equip, swing);
            return true;
        }
    }

    // ------------------------------------------------------------------ whole-body motions

    /** Spins and dodge somersaults turn the whole player model. */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        ClientAnims.Active anim = ClientAnims.get(player, event.getPartialTick());
        event.getPoseStack().pushPose();
        if (anim == null) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        if (anim.pose() == CastPose.SPIN) {
            float turns = anim.t() < 0.5F ? anim.t() * 2 : 1;
            pose.mulPose(Axis.YP.rotationDegrees(-720 * (float) Math.sin(turns * Math.PI / 2)));
        } else if (anim.pose() == CastPose.DODGE) {
            Vec3 move = player.getDeltaMovement();
            float yaw = bodyYaw * Mth.DEG_TO_RAD;
            double forward = -move.x * Mth.sin(yaw) + move.z * Mth.cos(yaw);
            double side = move.x * Mth.cos(yaw) + move.z * Mth.sin(yaw);
            float angle = 360 * anim.t();
            pose.translate(0, 0.9, 0);
            pose.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            if (Math.abs(side) > Math.abs(forward)) {
                pose.mulPose(Axis.ZP.rotationDegrees(side > 0 ? -angle : angle));
            } else {
                pose.mulPose(Axis.XP.rotationDegrees(forward >= 0 ? angle : -angle));
            }
            pose.mulPose(Axis.YP.rotationDegrees(bodyYaw));
            pose.translate(0, -0.9, 0);
        } else if (anim.pose() == CastPose.LEAP) {
            pose.translate(0, 0.9, 0);
            pose.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            pose.mulPose(Axis.XP.rotationDegrees(-25 * Mth.sin(anim.t() * Mth.PI)));
            pose.mulPose(Axis.YP.rotationDegrees(bodyYaw));
            pose.translate(0, -0.9, 0);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        event.getPoseStack().popPose();
    }

    /** A quick camera roll while dodging in first person. */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        ClientAnims.Active anim = ClientAnims.get(mc.player, (float) event.getPartialTick());
        if (anim != null && anim.pose() == CastPose.DODGE) {
            event.setRoll(event.getRoll() + 12 * Mth.sin(anim.t() * Mth.TWO_PI));
            event.setPitch(event.getPitch() + 6 * Mth.sin(anim.t() * Mth.PI));
        }
    }

    /** Our additive particles change the blend function; put it back once particles are drawn. */
    @SubscribeEvent
    public static void afterParticles(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAnims.clear();
    }
}
