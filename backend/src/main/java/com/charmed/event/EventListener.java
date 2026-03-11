package com.charmed.event;

/**
 * Functional interface for type-safe event handling.
 * Enables lambda subscriptions: eventBus.subscribe(XxxEvent.class, e -> handle(e))
 */
@FunctionalInterface
public interface EventListener<T extends Event> {
    void onEvent(T event);
}
