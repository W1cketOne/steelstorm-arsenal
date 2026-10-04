package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

public abstract class ClientPlayerNetworkEvent extends Event {
    public static class LoggingOut extends ClientPlayerNetworkEvent { }
    public static class LoggingIn extends ClientPlayerNetworkEvent { }
}
