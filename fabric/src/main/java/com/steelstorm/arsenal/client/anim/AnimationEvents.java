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
import com.steelstorm.compat.neo.api.distmarker.Dist;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.client.event.ClientPlayerNetworkEvent;
import com.steelstorm.compat.neo.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.steelstorm.compat.neo.neoforge.client.event.RenderPlayerEvent;
import com.steelstorm.compat.neo.neoforge.client.event.ViewportEvent;
import com.steelstorm.compat.neo.neoforge.client.extensions.common.IClientItemExtensions;

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
            return HumanoidModel.ArmPose.ITEM;
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

    // ------------------------------------------------------------------ first-person arm

    /**
     * In first person, draws the player's own arm gripping the weapon (vanilla hides the arm when
     * holding anything), then the weapon itself, both moved by the same swing animation.
     */
    @SubscribeEvent
    public static void onRenderHand(com.steelstorm.compat.neo.neoforge.client.event.RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack stack = event.getItemStack();
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof WeaponItem weapon)
                || player.isInvisible() || (player.isUsingItem() && player.getUseItemRemainingTicks() > 0)) {
            return;
        }
        event.setCanceled(true);
        HumanoidArm arm = player.getMainArm();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        WeaponAnimator.poseFirstPerson(pose, player, arm, weapon.type(), event.getPartialTick(), event.getEquipProgress(),
                event.getSwingProgress());
        pose.pushPose();
        drawArm(mc, player, pose, event, side, 0);
        pose.popPose();
        net.minecraft.world.item.ItemDisplayContext ctx = side > 0 ? net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                : net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack, ctx, side < 0, pose,
                event.getMultiBufferSource(), event.getPackedLight());
        float progress = SwingClock.progress(player, player.getAttackAnim(event.getPartialTick()), weapon.type(), event.getPartialTick());
        AttackAnims.Attack attack = SwingClock.attack(player, weapon.type());
        BladeTrail.renderFirstPerson(pose, event.getMultiBufferSource(), player, stack, weapon.type(), ctx, side < 0,
                progress > attack.strikeFrom && progress < attack.strikeTo, com.steelstorm.arsenal.weapon.WeaponLooks.trailColor(stack));
        pose.popPose();
    }

    /**
     * Draws one arm with its fist closed on the grip point, or `down` blocks further down the
     * weapon's haft, the forearm running back toward that arm's side of the screen.
     */
    private static void drawArm(Minecraft mc, LocalPlayer player, PoseStack pose, com.steelstorm.compat.neo.neoforge.client.event.RenderHandEvent event,
                                int armSide, float down) {
        int hand = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        pose.pushPose();
        // Grip point (gen_models.HAND_FP), then down the weapon's axis in first person.
        pose.translate(hand * 1.13F / 16.0F, 2.0F / 16.0F, 0.15F / 16.0F);
        pose.translate(hand * 0.211F * down, -0.906F * down, 0.366F * down);
        org.joml.Vector3f along = armSide == hand
                ? new org.joml.Vector3f(hand * -0.22F, 0.72F, -0.66F)
                : new org.joml.Vector3f(hand * 0.55F, 0.62F, -0.56F);
        along.normalize();
        pose.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0, 1, 0), along));
        pose.mulPose(Axis.YP.rotationDegrees(armSide * 90));
        pose.translate(armSide * 6.0F / 16.0F, -10.5F / 16.0F, 0);
        net.minecraft.client.renderer.entity.EntityRenderer<? super LocalPlayer> r = mc.getEntityRenderDispatcher().getRenderer(player);
        if (r instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
            if (armSide > 0) {
                pr.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
            } else {
                pr.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
            }
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------ whole-body motions

    /** Spins and dodge somersaults turn the whole player model. */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        ClientAnims.Active anim = ClientAnims.get(player, event.getPartialTick());
        event.getPoseStack().pushPose();
        PoseStack pose = event.getPoseStack();
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        if (anim == null) {
            movement(pose, player, bodyYaw, event.getPartialTick());
            return;
        }
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

    /**
     * Everyday movement for every player: leaning into a sprint, stretching on the way up, tucking
     * at the top of a jump and squashing a little on landing.
     */
    private static void movement(PoseStack pose, Player player, float bodyYaw, float partial) {
        MovementAnims.State s = MovementAnims.get(player);
        float sprint = s.sprint(partial);
        float air = s.air(partial);
        float land = s.land(partial);
        if (sprint <= 0.001F && air <= 0.001F && land <= 0.001F) {
            return;
        }
        float vy = (float) player.getDeltaMovement().y;
        float lean = 9.0F * sprint * (1 - 0.5F * air) - air * Mth.clamp(vy * 18.0F, -6.0F, 8.0F);
        if (lean != 0) {
            pose.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            pose.mulPose(Axis.XP.rotationDegrees(-lean));
            pose.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        }
        // Stretch while rising, squash on impact.
        float stretch = air * Mth.clamp(vy * 0.35F, -0.03F, 0.06F) - land * 0.1F;
        pose.scale(1 - stretch * 0.5F, 1 + stretch, 1 - stretch * 0.5F);
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
    public static void afterParticles(com.steelstorm.compat.neo.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == com.steelstorm.compat.neo.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAnims.clear();
    }
}
