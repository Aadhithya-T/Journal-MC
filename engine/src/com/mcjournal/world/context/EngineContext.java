package com.mcjournal.world.context;

/**
 * Static engine facade providing global, decoupled access to the active world's context.
 * Enables future application modules (e.g. journaling) to retrieve player location,
 * time, biome, and landmark information without referencing engine internals.
 */
public final class EngineContext {

    private static volatile WorldContextProvider provider = new WorldContextProvider() {
        @Override
        public WorldContext getCurrentContext() {
            return WorldContext.UNAVAILABLE;
        }

        @Override
        public WorldLocation getPlayerLocation() {
            return WorldLocation.empty();
        }

        @Override
        public boolean isContextAvailable() {
            return false;
        }
    };

    private EngineContext() {}

    /**
     * Registers the authoritative context provider (typically WorldSession).
     */
    public static void setProvider(WorldContextProvider p) {
        provider = (p != null) ? p : new WorldContextProvider() {
            @Override
            public WorldContext getCurrentContext() {
                return WorldContext.UNAVAILABLE;
            }

            @Override
            public WorldLocation getPlayerLocation() {
                return WorldLocation.empty();
            }

            @Override
            public boolean isContextAvailable() {
                return false;
            }
        };
    }

    /**
     * Obtains the current WorldContext snapshot. Safe to call at any time.
     */
    public static WorldContext getCurrentContext() {
        return provider.getCurrentContext();
    }

    /**
     * Obtains the player's current WorldLocation. Safe to call at any time.
     */
    public static WorldLocation getPlayerLocation() {
        return provider.getPlayerLocation();
    }

    /**
     * Returns true if an active world context is currently available.
     */
    public static boolean isAvailable() {
        return provider.isContextAvailable();
    }
}

