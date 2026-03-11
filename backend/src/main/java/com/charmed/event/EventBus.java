package com.charmed.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central publish/subscribe event bus. Type-safe subscriptions via generics.
 * Observer pattern — decouples publishers from subscribers.
 */
public class EventBus {

    private final Map<Class<? extends Event>, List<EventListener<? extends Event>>> listeners
            = new ConcurrentHashMap<>();

    /** Subscribe a listener for a specific event type. */
    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** Publish an event to all subscribers of its type. */
    @SuppressWarnings("unchecked")
    public <T extends Event> void publish(T event) {
        List<EventListener<? extends Event>> handlers = listeners.get(event.getClass());
        if (handlers != null) {
            for (EventListener<? extends Event> handler : handlers) {
                ((EventListener<T>) handler).onEvent(event);
            }
        }
    }
}
