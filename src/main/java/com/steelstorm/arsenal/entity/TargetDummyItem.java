package com.steelstorm.arsenal.entity;

import com.steelstorm.arsenal.registry.ModEntities;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class TargetDummyItem extends Item {
    public TargetDummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.noCollision(ModEntities.TARGET_DUMMY.get().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            TargetDummyEntity dummy = ModEntities.TARGET_DUMMY.get().create(server, null, pos, MobSpawnType.SPAWN_EGG, false, false);
            if (dummy == null) {
                return InteractionResult.FAIL;
            }
            float yaw = context.getPlayer() == null ? 0 : context.getPlayer().getYRot() + 180.0F;
            dummy.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
            dummy.setYHeadRot(yaw);
            dummy.setYBodyRot(yaw);
            server.addFreshEntity(dummy);
            level.playSound(null, pos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        context.getItemInHand().shrink(context.getPlayer() != null && context.getPlayer().getAbilities().instabuild ? 0 : 1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.steelstorm.target_dummy").withStyle(ChatFormatting.GRAY));
    }
}
