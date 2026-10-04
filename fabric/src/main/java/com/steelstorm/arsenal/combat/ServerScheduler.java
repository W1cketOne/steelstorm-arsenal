package com.steelstorm.arsenal.combat;

import com.steelstorm.arsenal.SteelstormArsenal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.steelstorm.compat.neo.bus.api.SubscribeEvent;
import com.steelstorm.compat.neo.fml.common.EventBusSubscriber;
import com.steelstorm.compat.neo.neoforge.event.server.ServerStoppedEvent;
import com.steelstorm.compat.neo.neoforge.event.tick.ServerTickEvent;

/** Runs short delayed actions on the server thread (multi-hit specials, telegraphed attacks). */
@EventBusSubscriber(modid = SteelstormArsenal.MODID)
public final class ServerScheduler {
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private record Task(Runnable action, int[] remaining) {
    }

    public static void schedule(int delayTicks, Runnable action) {
        PENDING.add(new Task(action, new int[]{Math.max(0, delayTicks)}));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        TASKS.addAll(PENDING);
        PENDING.clear();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (--task.remaining[0] <= 0) {
                it.remove();
                try {
                    task.action.run();
                } catch (RuntimeException e) {
                    SteelstormArsenal.LOGGER.error("Scheduled combat task failed", e);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        TASKS.clear();
        PENDING.clear();
    }

    private ServerScheduler() {
    }
}
