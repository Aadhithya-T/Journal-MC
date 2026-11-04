package com.mcjournal.event;

/**
 * Handle representing an active event subscription that can be cancelled.
 */
@FunctionalInterface
public interface Subscription {
    /**
     * Unsubscribes the associated listener from the EventBus.
     */
    void unsubscribe();
}

