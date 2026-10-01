package com.steelstorm.arsenal.weapon;

import com.steelstorm.arsenal.SteelstormArsenal;
import com.steelstorm.arsenal.ability.AbilityTooltips;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.steelstorm.arsenal.entity.ThrownSpear;
import com.steelstorm.arsenal.registry.ModDataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * One class for every tiered melee weapon. Behaviour that differs per weapon type lives in the
 * combat handlers, keyed by {@link #type()}, so this class only carries stats and tooltips.
 */
public class WeaponItem extends SwordItem {
    public static final ResourceLocation REACH_MODIFIER_ID = SteelstormArsenal.id("weapon_reach");

    private final WeaponType type;
    @Nullable
    private final WeaponTier weaponTier;
    private final float attackDamage;

    public WeaponItem(WeaponType type, Tier tier, @Nullable WeaponTier weaponTier, float extraDamage, Properties properties) {
        super(tier, properties.attributes(createAttributes(type, tier, extraDamage)));
        this.type = type;
        this.weaponTier = weaponTier;
        this.attackDamage = 1.0F + WeaponType.SWORD_BASE_DAMAGE + type.damageDelta() + tier.getAttackDamageBonus() + extraDamage;
    }

    public static ItemAttributeModifiers createAttributes(WeaponType type, Tier tier, float extraDamage) {
        double damage = WeaponType.SWORD_BASE_DAMAGE + type.damageDelta() + tier.getAttackDamageBonus() + extraDamage;
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, type.attackSpeed() - WeaponType.PLAYER_BASE_SPEED,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
        if (type.reachBonus() != 0) {
            builder.add(Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(REACH_MODIFIER_ID, type.reachBonus(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    /**
     * Hold Use to guard; the first few ticks of the guard are the perfect parry window.
     * Spears thrown instead when the player is sneaking.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (isThrowable() && player.isShiftKeyDown()) {
            stack.set(ModDataComponents.THROW_MODE, true);
        } else {
            stack.remove(ModDataComponents.THROW_MODE);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return isThrowMode(stack) ? UseAnim.SPEAR : UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!isThrowMode(stack) || !(entity instanceof Player player)) {
            return;
        }
        stack.remove(ModDataComponents.THROW_MODE);
        int charged = getUseDuration(stack, entity) - timeLeft;
        if (charged < 10) {
            return;
        }
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(entity.getUsedItemHand()));
            if (stack.isEmpty()) {
                return;
            }
            ThrownSpear spear = new ThrownSpear(level, player, stack.copyWithCount(1));
            spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
            if (player.getAbilities().instabuild) {
                spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(spear);
            level.playSound(null, spear, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
            if (!player.getAbilities().instabuild) {
                player.getInventory().removeItem(stack);
            }
        }
    }

    public boolean isThrowable() {
        return type == WeaponType.SPEAR;
    }

    public static boolean isThrowMode(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModDataComponents.THROW_MODE));
    }

    /** Whether holding Use with this stack is guarding (not winding up a throw). */
    public boolean isGuarding(ItemStack stack) {
        return !isThrowMode(stack);
    }

    /** Battleaxes break shields like vanilla axes do. */
    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return type == WeaponType.BATTLEAXE || super.canDisableShield(stack, shield, entity, attacker);
    }

    public WeaponType type() {
        return type;
    }

    @Nullable
    public WeaponTier weaponTier() {
        return weaponTier;
    }

    public boolean isStormsteel() {
        return weaponTier == WeaponTier.STORMSTEEL;
    }

    /** Total damage dealt by a fully charged hit (player base damage included). */
    public float attackDamage() {
        return attackDamage;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.steelstorm.stats",
                        format(attackDamage), format(type.attackSpeed()),
                        type.reachBonus() == 0 ? "0" : "+" + format((float) type.reachBonus()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.steelstorm.passive").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(type.passiveKey()).withStyle(ChatFormatting.YELLOW)));
        AbilityTooltips.append(stack, tooltip, flag);
        if (isStormsteel()) {
            tooltip.add(Component.translatable("tooltip.steelstorm.stormsteel").withStyle(ChatFormatting.BLUE));
        }
        appendExtraTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.steelstorm.controls").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    protected void appendExtraTooltip(ItemStack stack, List<Component> tooltip) {
    }

    protected static String format(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
