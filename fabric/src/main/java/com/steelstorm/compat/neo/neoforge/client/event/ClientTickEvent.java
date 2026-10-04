package com.steelstorm.compat.neo.neoforge.client.event;

import com.steelstorm.compat.neo.bus.api.Event;

public abstract class ClientTickEvent extends Event {
    public static class Pre extends ClientTickEvent { }
    public static class Post extends ClientTickEvent { }
}
