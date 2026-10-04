package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;

public abstract class InputEvent extends Event {
    public static class InteractionKeyMappingTriggered extends InputEvent {
        private final int button;
        private final KeyMapping key;
        private final InteractionHand hand;
        private boolean swing = true;
        public InteractionKeyMappingTriggered(int button, KeyMapping key, InteractionHand hand) { this.button = button; this.key = key; this.hand = hand; }
        public boolean isAttack() { return button == 0; }
        public boolean isUseItem() { return button == 1; }
        public boolean isPickBlock() { return button == 2; }
        public KeyMapping getKeyMapping() { return key; }
        public InteractionHand getHand() { return hand; }
        public void setSwingHand(boolean swing) { this.swing = swing; }
        public boolean shouldSwingHand() { return swing; }
    }
}
