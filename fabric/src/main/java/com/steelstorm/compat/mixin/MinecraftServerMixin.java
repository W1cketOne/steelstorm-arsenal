package com.steelstorm.compat.mixin;

import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.neo.neoforge.event.level.LevelEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
    private static void steelstorm$createSpawn(ServerLevel level, ServerLevelData data, boolean bonusChest, boolean debug, CallbackInfo ci) {
        if (NeoBus.post(new LevelEvent.CreateSpawnPosition(level)).isCanceled()) {
            ci.cancel();
        }
    }
}
