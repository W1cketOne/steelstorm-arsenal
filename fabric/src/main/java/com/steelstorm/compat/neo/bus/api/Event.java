package com.steelstorm.compat.neo.bus.api;

/** Base for compat events; cancellation stops later handlers, as in NeoForge. */
public abstract class Event {
    private boolean canceled;

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
