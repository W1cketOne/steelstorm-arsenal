package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

import com.steelstorm.compat.neo.neoforge.client.gui.VanillaGuiLayers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class RegisterGuiLayersEvent extends Event {
    public interface LayeredDraw { void render(GuiGraphics graphics, DeltaTracker delta); }
    public record Layer(ResourceLocation anchor, ResourceLocation id, LayeredDraw draw) { }
    public static final List<Layer> LAYERS = new ArrayList<>();
    public void registerAbove(ResourceLocation anchor, ResourceLocation id, LayeredDraw draw) { LAYERS.add(new Layer(anchor, id, draw)); }
}
