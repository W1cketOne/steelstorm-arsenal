package com.steelstorm.arsenal.block;

import com.steelstorm.arsenal.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WeaponRackBlockEntity extends BlockEntity {
    public static final int SLOTS = 3;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public WeaponRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEAPON_RACK.get(), pos, state);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public ItemStack getWeapon(int slot) {
        return items.get(slot);
    }

    public void setWeapon(int slot, ItemStack stack) {
        items.set(slot, stack);
        changed();
    }

    public ItemStack removeWeapon(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        changed();
        return stack;
    }

    /** The preferred slot if empty, otherwise the closest empty one, or -1 when full. */
    public int firstFreeSlot(int preferred) {
        if (items.get(preferred).isEmpty()) {
            return preferred;
        }
        for (int offset = 1; offset < SLOTS; offset++) {
            for (int s : new int[]{preferred - offset, preferred + offset}) {
                if (s >= 0 && s < SLOTS && items.get(s).isEmpty()) {
                    return s;
                }
            }
        }
        return -1;
    }

    public int filledSlotNear(int preferred) {
        if (!items.get(preferred).isEmpty()) {
            return preferred;
        }
        for (int offset = 1; offset < SLOTS; offset++) {
            for (int s : new int[]{preferred - offset, preferred + offset}) {
                if (s >= 0 && s < SLOTS && !items.get(s).isEmpty()) {
                    return s;
                }
            }
        }
        return -1;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < SLOTS; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
