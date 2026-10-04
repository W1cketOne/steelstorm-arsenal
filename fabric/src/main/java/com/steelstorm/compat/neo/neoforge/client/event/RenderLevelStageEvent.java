package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;

public class RenderLevelStageEvent extends Event {
    public enum Stage { AFTER_TRANSLUCENT_BLOCKS, AFTER_PARTICLES, AFTER_WEATHER, AFTER_LEVEL }
    private final Stage stage;
    private final PoseStack pose;
    private final Matrix4f projection;
    private final Camera camera;
    private final DeltaTracker delta;
    public RenderLevelStageEvent(Stage stage, PoseStack pose, Matrix4f projection, Camera camera, DeltaTracker delta) {
        this.stage = stage; this.pose = pose; this.projection = projection; this.camera = camera; this.delta = delta;
    }
    public Stage getStage() { return stage; }
    public PoseStack getPoseStack() { return pose; }
    public Matrix4f getProjectionMatrix() { return projection; }
    public Camera getCamera() { return camera; }
    public DeltaTracker getPartialTick() { return delta; }
}
