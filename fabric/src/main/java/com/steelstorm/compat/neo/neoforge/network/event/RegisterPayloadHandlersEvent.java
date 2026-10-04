package com.steelstorm.compat.neo.neoforge.network.event;

import com.steelstorm.compat.neo.bus.api.Event;
import com.steelstorm.compat.neo.neoforge.network.registration.PayloadRegistrar;

public class RegisterPayloadHandlersEvent extends Event {
    public PayloadRegistrar registrar(String version) {
        return new PayloadRegistrar();
    }
}
