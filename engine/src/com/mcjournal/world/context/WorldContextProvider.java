package com.mcjournal.world.context;

/**
 * Stable, high-level interface exposing world and player context to application layers
 * without coupling them to Player, ChunkManager, or rendering pipeline internals.
 */
public interface WorldContextProvider {

    /**
     * Obtains the current snapshot context of the active world.
     * Guaranteed to return a valid WorldContext object (e.g. WorldContext.UNAVAILABLE)
     * even when a world is loading, unloading, or unavailable.
     */
    WorldContext getCurrentContext();

    /**
     * Obtains the player's current location, or WorldLocation.empty() if inactive.
     */
    WorldLocation getPlayerLocation();

    /**
     * Returns true if an active world session is fully loaded and ready for querying.
     */
    boolean isContextAvailable();
}

