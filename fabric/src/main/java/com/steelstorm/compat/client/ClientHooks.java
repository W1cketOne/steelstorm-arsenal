package com.steelstorm.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.steelstorm.arsenal.item.StormsteelArmorItem;
import com.steelstorm.arsenal.item.VoidwalkerArmorItem;
import com.steelstorm.arsenal.item.WarlordArmorItem;
import com.steelstorm.arsenal.registry.ModBlocks;
import com.steelstorm.compat.NeoBus;
import com.steelstorm.compat.SpawnDataPayload;
import com.steelstorm.compat.neo.neoforge.client.event.ClientPlayerNetworkEvent;
import com.steelstorm.compat.neo.neoforge.client.event.ClientTickEvent;
import com.steelstorm.compat.neo.neoforge.client.event.EntityRenderersEvent;
import com.steelstorm.compat.neo.neoforge.client.event.RegisterGuiLayersEvent;
import com.steelstorm.compat.neo.neoforge.client.event.RegisterKeyMappingsEvent;
import com.steelstorm.compat.neo.neoforge.client.event.RegisterParticleProvidersEvent;
import com.steelstorm.compat.neo.neoforge.client.event.RenderLevelStageEvent;
import com.steelstorm.compat.neo.neoforge.client.extensions.common.IClientItemExtensions;
import com.steelstorm.compat.neo.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.steelstorm.compat.neo.neoforge.entity.IEntityWithComplexSpawn;
import com.steelstorm.compat.neo.neoforge.event.tick.LevelTickEvent;
import com.steelstorm.compat.neo.neoforge.network.PacketDistributor;
import com.steelstorm.compat.neo.neoforge.network.handling.IPayloadHandler;
import com.steelstorm.compat.neo.neoforge.network.registration.PayloadRegistrar;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

public final class ClientHooks {
    private static final ResourceLocation SWIRL = ResourceLocation.fromNamespaceAndPath("steelstorm", "textures/misc/suit_swirl.png");

    public static void init() {
        com.steelstorm.compat.Subscribers.registerClient();
        PacketDistributor.clientSender = ClientPlayNetworking::send;
        com.steelstorm.compat.FabricHooks.shiftDown = net.minecraft.client.gui.screens.Screen::hasShiftDown;
        for (PayloadRegistrar.ClientEntry<?> entry : PayloadRegistrar.CLIENT_HANDLERS) {
            registerClientHandler(entry);
        }
        ClientPlayNetworking.registerGlobalReceiver(SpawnDataPayload.TYPE, (payload, ctx) -> {
            Entity entity = ctx.client().level == null ? null : ctx.client().level.getEntity(payload.entityId());
            if (entity instanceof IEntityWithComplexSpawn complex) {
                complex.readSpawnData(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), entity.registryAccess()));
            }
        });

        HeldModels.init();
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.RUNE_FORGE.get(), ModBlocks.SARCOPHAGUS.get(),
                ModBlocks.LEGENDARY_PEDESTAL.get(), ModBlocks.STORM_ALTAR.get(), ModBlocks.SIGNAL_BRAZIER.get());

        NeoBus.post(new EntityRenderersEvent.RegisterLayerDefinitions());
        NeoBus.post(new EntityRenderersEvent.RegisterRenderers());
        NeoBus.post(new RegisterKeyMappingsEvent());
        NeoBus.post(new RegisterParticleProvidersEvent());
        NeoBus.post(new RegisterGuiLayersEvent());
        NeoBus.post(new RegisterClientExtensionsEvent());
        registerArmor();

        ClientTickEvents.START_CLIENT_TICK.register(mc -> NeoBus.post(new ClientTickEvent.Pre()));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> NeoBus.post(new ClientTickEvent.Post()));
        ClientTickEvents.END_CLIENT_TICK.register(com.steelstorm.arsenal.client.TrailerDirector::tick);
        ClientTickEvents.START_WORLD_TICK.register(level -> NeoBus.post(new LevelTickEvent.Pre(level)));
        ClientTickEvents.END_WORLD_TICK.register(level -> NeoBus.post(new LevelTickEvent.Post(level)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> NeoBus.post(new ClientPlayerNetworkEvent.LoggingOut()));

        HudRenderCallback.EVENT.register((graphics, delta) -> {
            for (RegisterGuiLayersEvent.Layer layer : RegisterGuiLayersEvent.LAYERS) {
                layer.draw().render(graphics, delta);
            }
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            stage(ctx, RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS);
            stage(ctx, RenderLevelStageEvent.Stage.AFTER_PARTICLES);
        });
    }

    private static void stage(WorldRenderContext ctx, RenderLevelStageEvent.Stage stage) {
        PoseStack pose = new PoseStack();
        pose.mulPose(ctx.positionMatrix());
        NeoBus.post(new RenderLevelStageEvent(stage, pose, ctx.projectionMatrix(), ctx.camera(), ctx.tickCounter()));
    }

    private static <T extends CustomPacketPayload> void registerClientHandler(PayloadRegistrar.ClientEntry<T> entry) {
        IPayloadHandler<T> handler = entry.handler();
        ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, ctx) -> handler.handle(payload, ctx::player));
    }

    private static void registerArmor() {
        ArmorRenderer renderer = (pose, buffers, stack, entity, slot, light, contextModel) -> {
            IClientItemExtensions ext = RegisterClientExtensionsEvent.of(stack.getItem());
            if (ext == null) {
                return;
            }
            @SuppressWarnings("unchecked")
            HumanoidModel<net.minecraft.world.entity.LivingEntity> model =
                    (HumanoidModel<net.minecraft.world.entity.LivingEntity>) ext.getHumanoidArmorModel(entity, stack, slot, contextModel);
            contextModel.copyPropertiesTo(model);
            model.setAllVisible(false);
            switch (slot) {
                case HEAD -> {
                    model.head.visible = true;
                    model.hat.visible = true;
                }
                case CHEST -> {
                    model.body.visible = true;
                    model.rightArm.visible = true;
                    model.leftArm.visible = true;
                }
                case LEGS -> {
                    model.body.visible = true;
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
                case FEET -> {
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
                default -> {
                }
            }
            float partial = net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            float glow = com.steelstorm.arsenal.client.SuitGlow.strength(entity, partial);
            // During an ultimate the armour blazes: drawn full-bright, wrapped in a swirling aura of the ultimate's colour.
            ArmorRenderer.renderPart(pose, buffers, glow > 0 ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : light, stack, model,
                    texture(stack.getItem(), entity, stack, slot));
            if (glow > 0) {
                // The plate itself blazes: its own texture added on top, full bright, in the ultimate's colour...
                int gc = com.steelstorm.arsenal.client.SuitGlow.color(entity);
                float throb = 0.7F + 0.3F * net.minecraft.util.Mth.sin((entity.tickCount + partial) * 0.5F);
                int eyes = ((int) (255 * glow * throb) << 24) | (gc & 0xFFFFFF);
                model.renderToBuffer(pose, buffers.getBuffer(net.minecraft.client.renderer.RenderType.eyes(texture(stack.getItem(), entity, stack, slot))),
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, eyes);
                // ...inside a swirling shell of energy.
                float t = (entity.tickCount + partial) * 0.012F;
                com.mojang.blaze3d.vertex.VertexConsumer vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.energySwirl(SWIRL, t % 1.0F, (t * 0.7F) % 1.0F));
                int c = com.steelstorm.arsenal.client.SuitGlow.color(entity);
                float pulse = 0.75F + 0.25F * net.minecraft.util.Mth.sin((entity.tickCount + partial) * 0.4F);
                int argb = ((int) (255 * glow * pulse) << 24) | (c & 0xFFFFFF);
                pose.pushPose();
                pose.scale(1.1F, 1.05F, 1.1F);
                model.renderToBuffer(pose, vc, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, argb);
                pose.popPose();
            }
        };
        for (Item item : BuiltInRegistries.ITEM) {
            if (com.steelstorm.arsenal.client.StormsteelArmorRendering.isModArmor(item)) {
                ArmorRenderer.register(renderer, item);
            }
        }
    }

    private static ResourceLocation texture(Item item, Entity entity, net.minecraft.world.item.ItemStack stack, EquipmentSlot slot) {
        boolean inner = slot == EquipmentSlot.LEGS;
        if (item instanceof VoidwalkerArmorItem v) {
            return v.getArmorTexture(stack, entity, slot, null, inner);
        }
        if (item instanceof WarlordArmorItem w) {
            return w.getArmorTexture(stack, entity, slot, null, inner);
        }
        if (item instanceof com.steelstorm.arsenal.item.CelestialArmorItem c) {
            return c.getArmorTexture(stack, entity, slot, null, inner);
        }
        if (item instanceof com.steelstorm.arsenal.item.DragonscaleArmorItem d) {
            return d.getArmorTexture(stack, entity, slot, null, inner);
        }
        return ((StormsteelArmorItem) item).getArmorTexture(stack, entity, slot, null, inner);
    }

    private ClientHooks() {
    }
}
