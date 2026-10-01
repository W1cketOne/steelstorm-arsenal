package com.steelstorm.arsenal.weapon;

import com.steelstorm.arsenal.entity.ThrowingKnifeEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Stacks to 16. Right-click to throw one quickly; 30% chance to cause Bleed. */
public class ThrowingKnifeItem extends Item implements ThrowableWeapon {
    public ThrowingKnifeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            ThrowingKnifeEntity knife = new ThrowingKnifeEntity(level, player, stack.copyWithCount(1));
            knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.2F, 0.6F);
            if (player.getAbilities().instabuild) {
                knife.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(knife);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.8F, 1.7F);
        }
        player.getCooldowns().addCooldown(this, 6);
        player.swing(hand);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.steelstorm.throwing_knife").withStyle(ChatFormatting.YELLOW));
    }
}
