package com.steelstorm.arsenal.weapon;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.entity.ThrownSpear;
import com.steelstorm.arsenal.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

/**
 * The single shared melee weapon class. Every {@link WeaponType} x {@link WeaponTier} combination is
 * an instance of this item; behaviour that differs per archetype is driven by the type.
 */
public class WeaponItem extends SwordItem {
    public static final ResourceLocation REACH_MODIFIER_ID = SteelstormArsenal.id("weapon_reach");
    /** Minimum use ticks before a spear throw is released. */
    public static final int SPEAR_CHARGE_TICKS = 10;

    private final WeaponType type;
    private final WeaponTier weaponTier;

    public WeaponItem(WeaponType type, WeaponTier weaponTier, Item.Properties properties) {
        super(weaponTier.tier(), properties.attributes(createAttributes(type, weaponTier.tier())));
        this.type = type;
        this.weaponTier = weaponTier;
    }

    public static ItemAttributeModifiers createAttributes(WeaponType type, Tier tier) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 3.0 + tier.getAttackDamageBonus() + type.damageBonus(),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, type.attackSpeed() - 4.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
        if (type.reachBonus() != 0) {
            builder.add(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(REACH_MODIFIER_ID, type.reachBonus(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    public WeaponType getWeaponType() {
        return type;
    }

    public WeaponTier getWeaponTier() {
        return weaponTier;
    }

    /** Damage of a plain hit with this weapon, including the player's base 1 damage. */
    public float baseDamage() {
        return 1.0f + 3.0f + getTier().getAttackDamageBonus() + type.damageBonus();
    }

    // ------------------------------------------------------------------ sweeping

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (ability == ItemAbilities.SWORD_SWEEP) {
            return type.sweeps();
        }
        return super.canPerformAction(stack, ability);
    }

    @Override
    public AABB getSweepHitBox(ItemStack stack, Player player, Entity target) {
        double inflate = Math.max(1.0, type.sweepInflate());
        return target.getBoundingBox().inflate(inflate, 0.25, inflate);
    }

    // ------------------------------------------------------------------ use (spear throw)

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (type.throwable() && player.isShiftKeyDown()) {
            if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
                return InteractionResultHolder.fail(stack);
            }
            stack.set(ModDataComponents.THROW_READY.get(), true);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (isThrowReady(stack)) {
            return UseAnim.SPEAR;
        }
        return super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isThrowReady(stack) ? 72000 : super.getUseDuration(stack, entity);
    }

    public static boolean isThrowReady(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.THROW_READY.get()));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!isThrowReady(stack)) {
            return;
        }
        stack.remove(ModDataComponents.THROW_READY.get());
        if (!(entity instanceof Player player)) {
            return;
        }
        int charged = getUseDuration(stack, entity) - timeLeft;
        if (charged < SPEAR_CHARGE_TICKS) {
            return;
        }
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            if (stack.isEmpty()) {
                return;
            }
            ThrownSpear spear = new ThrownSpear(level, player, stack, baseDamage() * 1.15f);
            spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.6f, 0.6f);
            if (player.hasInfiniteMaterials()) {
                spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(spear);
            level.playSound(null, spear, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
            if (!player.hasInfiniteMaterials()) {
                player.getInventory().removeItem(stack);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        stack.remove(ModDataComponents.THROW_READY.get());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Clear a stale throw flag (e.g. the item was switched away mid-charge).
        if (isThrowReady(stack) && !(entity instanceof LivingEntity living && living.getUseItem() == stack)) {
            stack.remove(ModDataComponents.THROW_READY.get());
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    // ------------------------------------------------------------------ tooltip

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.steelstorm.type_tier", type.displayName(),
                Component.translatable(weaponTier.translationKey())).withStyle(weaponTier.color()));
        tooltip.add(Component.translatable("tooltip.steelstorm.passive", type.passiveName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" ").append(type.passiveDescription()));
        if (type.throwable()) {
            tooltip.add(Component.literal(" ").append(Component.translatable("tooltip.steelstorm.spear_throw")
                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
        if (weaponTier == WeaponTier.STORMSTEEL) {
            tooltip.add(Component.translatable("tooltip.steelstorm.stormsteel").withStyle(ChatFormatting.BLUE));
        }
        int kills = stack.getOrDefault(ModDataComponents.KILL_COUNT.get(), 0);
        if (kills > 0) {
            tooltip.add(Component.translatable("tooltip.steelstorm.kills", kills).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
