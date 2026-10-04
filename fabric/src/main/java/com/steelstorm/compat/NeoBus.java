package com.steelstorm.compat;

import com.steelstorm.compat.neo.bus.api.Event;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny event bus standing in for NeoForge's: classes register their static @SubscribeEvent
 * methods, and the Fabric hooks post event objects to them in priority order.
 */
public final class NeoBus {
    private record Handler(int priority, Method method) {
    }

    private static final Map<Class<?>, List<Handler>> HANDLERS = new HashMap<>();

    public static void register(Class<?> type) {
        for (Method m : type.getDeclaredMethods()) {
            SubscribeEvent sub = m.getAnnotation(SubscribeEvent.class);
            if (sub == null || !Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 1) {
                continue;
            }
            m.setAccessible(true);
            List<Handler> list = HANDLERS.computeIfAbsent(m.getParameterTypes()[0], k -> new ArrayList<>());
            list.add(new Handler(sub.priority().ordinal(), m));
            list.sort(Comparator.comparingInt(Handler::priority));
        }
    }

    public static boolean hasHandlers(Class<?> eventType) {
        return HANDLERS.containsKey(eventType);
    }

    public static <E> E post(E event) {
        for (Class<?> c = event.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            List<Handler> list = HANDLERS.get(c);
            if (list == null) {
                continue;
            }
            for (Handler h : list) {
                if (event instanceof Event e && e.isCanceled()) {
                    return event;
                }
                try {
                    h.method().invoke(null, event);
                } catch (ReflectiveOperationException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof RuntimeException re) {
                        throw re;
                    }
                    throw new RuntimeException(cause);
                }
            }
        }
        return event;
    }

    private NeoBus() {
    }
}
