package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public class RegisterKeyMappingsEvent extends Event {
    public void register(KeyMapping key) { KeyBindingHelper.registerKeyBinding(key); }
}
