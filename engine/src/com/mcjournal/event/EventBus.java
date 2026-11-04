package com.mcjournal.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Lightweight, type-safe, decoupled engine event bus.
 * Supports multiple subscribers, duplicate subscription prevention,
 * exception isolation, and re-entrant recursion guards.
 */
public class EventBus {

    private static final EventBus INSTANCE = new EventBus();
    private static final int MAX_RECURSION_DEPTH = 16;

    private final Map<Class<?>, Set<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    private final ThreadLocal<Integer> dispatchDepth = ThreadLocal.withInitial(() -> 0);

    public EventBus() {}

    public static EventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Subscribes a listener to events of the specified type.
     * Prevents duplicate registrations.
     *
     * @param eventType The class of the event to listen for.
     * @param listener The consumer to invoke when the event is posted.
     * @param <T> The event type.
     * @return A Subscription token that can be used to unsubscribe.
     */
    @SuppressWarnings("unchecked")
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
        if (eventType == null || listener == null) {
            throw new IllegalArgumentException("EventType and listener cannot be null");
        }

        Set<Consumer<?>> set = listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>());
        set.add(listener);

        return () -> unsubscribe(eventType, listener);
    }

    /**
     * Unsubscribes a previously registered listener.
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        if (eventType == null || listener == null) return;

        Set<Consumer<?>> set = listeners.get(eventType);
        if (set != null) {
            set.remove(listener);
            if (set.isEmpty()) {
                listeners.remove(eventType, Collections.emptySet());
            }
        }
    }

    /**
     * Posts an event to all registered listeners.
     * Dispatches safely with exception isolation: if one listener throws an exception,
     * it is logged and subsequent listeners still receive the event.
     * Guards against runaway recursion beyond MAX_RECURSION_DEPTH.
     *
     * @param event The event object to dispatch.
     */
    @SuppressWarnings("unchecked")
    public void post(Object event) {
        if (event == null) return;

        int currentDepth = dispatchDepth.get();
        if (currentDepth >= MAX_RECURSION_DEPTH) {
            System.err.println("[EventBus] Recursion limit reached (" + MAX_RECURSION_DEPTH + ") for event: " + event.getClass().getSimpleName() + " - skipping dispatch.");
            return;
        }

        dispatchDepth.set(currentDepth + 1);
        try {
            Class<?> eventClass = event.getClass();

            // Dispatch to direct class listeners and assignable interface/supertype listeners
            for (Map.Entry<Class<?>, Set<Consumer<?>>> entry : listeners.entrySet()) {
                if (entry.getKey().isAssignableFrom(eventClass)) {
                    for (Consumer<?> rawListener : entry.getValue()) {
                        try {
                            ((Consumer<Object>) rawListener).accept(event);
                        } catch (Throwable t) {
                            System.err.println("[EventBus] Exception caught in listener for " + eventClass.getSimpleName() + ": " + t.getMessage());
                        }
                    }
                }
            }
        } finally {
            dispatchDepth.set(currentDepth);
        }
    }

    /**
     * Clears all registered subscriptions. Useful for testing and world lifecycle transitions.
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * Returns the number of distinct event types currently listened to.
     */
    public int getRegisteredEventTypeCount() {
        return listeners.size();
    }

    /**
     * Returns the number of subscribers for a specific event type.
     */
    public int getSubscriberCount(Class<?> eventType) {
        Set<Consumer<?>> set = listeners.get(eventType);
        return (set != null) ? set.size() : 0;
    }
}

