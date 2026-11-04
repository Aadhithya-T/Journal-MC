package com.mcjournal.test;

import com.mcjournal.event.*;
import com.mcjournal.storage.WorldStorageDirectory;
import com.mcjournal.world.WorldConfiguration;
import com.mcjournal.world.context.*;
import com.mcjournal.world.landmark.*;
import com.mcjournal.world.time.WorldClock;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive verification suite for the Journal Engine Readiness systems:
 * 1. World Identity and Metadata (UUID persistence, renaming stability, malformed JSON recovery)
 * 2. Persistent In-Game Clock (deterministic solar time, day transitions, formatted time)
 * 3. Lightweight Engine Event System (type safety, exception isolation, duplicate prevention)
 * 4. Independent Storage Foundation (world directory isolation, path traversal security, atomic writes)
 * 5. Landmark & POI Registry (registration, world isolation, proximity discovery, event firing)
 * 6. World Location and Context API (negative chunk coordinate math, context snapshots, safe fallbacks)
 */
public class JournalEngineReadinessTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   JOURNAL ENGINE READINESS TEST SUITE           ");
        System.out.println("=================================================");

        testWorldMetadata();
        testWorldClock();
        testEventBus();
        testWorldStorageDirectory();
        testLandmarkRegistry();
        testWorldLocationAndContext();
        testEngineContextFacade();

        System.out.println("\n>>> ALL JOURNAL ENGINE READINESS TESTS PASSED! <<<\n");
    }

    private static void testWorldMetadata() {
        System.out.print("[JER.1] World identity and metadata lifecycle... ");

        WorldConfiguration config = WorldConfiguration.createDefault(987654321L);
        WorldMetadata meta = WorldMetadata.createNew("Chronicle Expedition", 987654321L, config);

        assert meta.getWorldId() != null && !meta.getWorldId().isBlank() : "World ID must be generated";
        assert "Chronicle Expedition".equals(meta.getWorldName()) : "World name mismatch";
        assert meta.getSeed() == 987654321L : "Seed mismatch";
        assert meta.getWorldWidth() == 2500 : "Width mismatch";

        // JSON Serialization Round-Trip
        String json = meta.toJson();
        assert json != null && json.contains(meta.getWorldId()) : "JSON must contain worldId";
        WorldMetadata reloaded = WorldMetadata.fromJson(json);
        assert meta.getWorldId().equals(reloaded.getWorldId()) : "WorldId must be preserved across round-trip";
        assert meta.getWorldName().equals(reloaded.getWorldName()) : "WorldName must match after reload";
        assert meta.getSeed() == reloaded.getSeed() : "Seed must match after reload";

        // Renaming must NEVER change worldId
        String originalId = meta.getWorldId();
        meta.setWorldName("Updated Chronicle Name");
        assert "Updated Chronicle Name".equals(meta.getWorldName()) : "World name should update";
        assert originalId.equals(meta.getWorldId()) : "WorldId must remain invariant across renames";

        // Malformed and Missing JSON Recovery
        WorldMetadata malformed = WorldMetadata.fromJson("Not valid json {{{");
        assert malformed != null && malformed.getWorldId() != null : "Malformed JSON must not throw, must return safe defaults";

        WorldMetadata emptyJson = WorldMetadata.fromJson("{}");
        assert emptyJson != null && emptyJson.getWorldId() != null : "Empty JSON must supply safe defaults";

        // Legacy Migration Determinism
        WorldMetadata legacy1 = WorldMetadata.fromLegacySave("Old World", 12345L, config, 12000.0);
        WorldMetadata legacy2 = WorldMetadata.fromLegacySave("Old World", 12345L, config, 12000.0);
        assert legacy1.getWorldId().equals(legacy2.getWorldId()) : "Legacy migration must produce deterministic stable UUID";

        System.out.println("PASSED");
    }

    private static void testWorldClock() {
        System.out.print("[JER.2] Persistent in-game clock & day rollover... ");

        WorldClock clock = new WorldClock(6000.0);
        clock.setWorldId("clock-test-world");

        // Tick 6000: Day 1 Midday (12:00)
        assert clock.getDay() == 1 : "Initial day must be 1";
        assert clock.getHour() == 12 : "Tick 6000 must correspond to 12:00 noon";
        assert clock.getMinute() == 0 : "Tick 6000 minute must be 0";
        assert clock.isDay() : "Tick 6000 must be daytime";
        assert "Day 1 — 12:00".equals(clock.getFormattedTime()) : "Formatted time mismatch: " + clock.getFormattedTime();

        // Advance 12,000 ticks -> 18,000 ticks: Midnight (00:00)
        clock.advanceTicks(12000.0);
        assert clock.getDay() == 1 : "Should still be Day 1";
        assert clock.getHour() == 0 : "Tick 18000 must correspond to 00:00 midnight";
        assert !clock.isDay() : "Tick 18000 must be nighttime";
        assert "Day 1 — 00:00".equals(clock.getFormattedTime()) : "Formatted time mismatch: " + clock.getFormattedTime();

        // Day rollover check with event subscription
        AtomicBoolean dayChangedFired = new AtomicBoolean(false);
        AtomicInteger eventCount = new AtomicInteger(0);

        Subscription sub = EventBus.getInstance().subscribe(DayChangedEvent.class, event -> {
            dayChangedFired.set(true);
            eventCount.incrementAndGet();
            assert event.previousDay() == 1 : "Previous day must be 1";
            assert event.newDay() == 2 : "New day must be 2";
            assert "clock-test-world".equals(event.worldId()) : "World ID mismatch in event";
        });

        // Advance past 24,000 ticks into Day 2 Dawn (Tick 0 of day 2 -> total 24,000 ticks)
        clock.advanceTicks(6000.0);
        assert clock.getTotalTicks() == 24000.0 : "Total ticks must be 24000";
        assert clock.getDay() == 2 : "Day must advance to 2";
        assert clock.getHour() == 6 : "Tick 24000 (day 2 dawn) must be 06:00";
        assert dayChangedFired.get() : "DayChangedEvent must fire on day rollover";
        assert eventCount.get() == 1 : "Exactly one DayChangedEvent should have fired";

        sub.unsubscribe();

        // Test advanceTime at 20 TPS (1 second = 20 ticks)
        double before = clock.getTotalTicks();
        clock.advanceTime(1.5); // 1.5 seconds = 30 ticks
        assert Math.abs(clock.getTotalTicks() - (before + 30.0)) < 0.001 : "advanceTime(1.5s) must advance 30 ticks";

        System.out.println("PASSED");
    }

    private static void testEventBus() {
        System.out.print("[JER.3] Lightweight engine event system... ");

        EventBus bus = new EventBus();
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);

        // Multiple subscribers
        Subscription sub1 = bus.subscribe(WorldLoadedEvent.class, e -> counter1.incrementAndGet());
        Subscription sub2 = bus.subscribe(WorldLoadedEvent.class, e -> counter2.incrementAndGet());

        bus.post(new WorldLoadedEvent("test-id", "Test World", 42L, 6000.0));
        assert counter1.get() == 1 : "Subscriber 1 must receive event";
        assert counter2.get() == 1 : "Subscriber 2 must receive event";

        // Unsubscribe
        sub1.unsubscribe();
        bus.post(new WorldLoadedEvent("test-id", "Test World", 42L, 6000.0));
        assert counter1.get() == 1 : "Subscriber 1 must not receive event after unsubscribe";
        assert counter2.get() == 2 : "Subscriber 2 must receive second event";

        // Duplicate subscription prevention
        java.util.function.Consumer<WorldUnloadedEvent> singleListener = e -> counter1.incrementAndGet();
        bus.subscribe(WorldUnloadedEvent.class, singleListener);
        bus.subscribe(WorldUnloadedEvent.class, singleListener); // Duplicate
        assert bus.getSubscriberCount(WorldUnloadedEvent.class) == 1 : "Duplicate listener should not register twice";

        // Exception isolation
        bus.subscribe(BlockPlacedEvent.class, e -> {
            throw new RuntimeException("Simulated listener exception");
        });
        AtomicBoolean healthyListenerReceived = new AtomicBoolean(false);
        bus.subscribe(BlockPlacedEvent.class, e -> healthyListenerReceived.set(true));

        // Posting should NOT throw even when one listener fails
        try {
            bus.post(new BlockPlacedEvent("test-id", (byte) 1, 10, 20, 30));
        } catch (Throwable t) {
            assert false : "EventBus.post must isolate listener exceptions";
        }
        assert healthyListenerReceived.get() : "Subsequent listener must still execute despite earlier failure";

        System.out.println("PASSED");
    }

    private static void testWorldStorageDirectory() {
        System.out.print("[JER.4] Independent persistence storage & security... ");

        File testBaseDir = new File("build/test_saves");
        String testWorldId = "test_world_storage_" + UUID.randomUUID();
        WorldStorageDirectory storage = new WorldStorageDirectory(testBaseDir, testWorldId);

        assert storage.getRootDir().exists() : "World storage directory must exist";
        assert storage.getWorldId().equals(testWorldId) : "World ID mismatch";

        // Standard Directory Layout
        assert storage.getJournalDir().exists() : "journal/ dir must exist";
        assert storage.getJournalEntriesDir().exists() : "journal/entries/ dir must exist";
        assert storage.getJournalAttachmentsDir().exists() : "journal/attachments/ dir must exist";
        assert storage.getRegionsDir().exists() : "regions/ dir must exist";
        assert storage.getBackupsDir().exists() : "backups/ dir must exist";

        // Path Traversal Security Checks
        try {
            storage.resolvePath("../outside_file.txt");
            assert false : "Should throw SecurityException on parent path traversal";
        } catch (SecurityException expected) {
            // Success
        }

        try {
            storage.resolvePath("journal/../../escaped.txt");
            assert false : "Should throw SecurityException on nested path traversal";
        } catch (SecurityException expected) {
            // Success
        }

        // Atomic File Writes & Safe Reading
        File testFile = storage.resolvePath("test_atomic.txt");
        String sampleContent = "Journal-MC Atomic Persistence Test Content";
        try {
            WorldStorageDirectory.writeStringAtomic(testFile, sampleContent);
            assert testFile.exists() : "Target file must exist after atomic write";
            File tempFile = new File(testFile.getParentFile(), testFile.getName() + ".tmp");
            assert !tempFile.exists() : "Temporary file must be cleaned up after atomic write";

            String readBack = WorldStorageDirectory.readStringSafe(testFile);
            assert sampleContent.equals(readBack) : "Read content mismatch";
        } catch (IOException e) {
            assert false : "Atomic write failed: " + e.getMessage();
        }

        // Safe reading of non-existent file
        assert WorldStorageDirectory.readStringSafe(storage.resolvePath("does_not_exist.json")) == null;

        // Cleanup test directory
        deleteDirectory(storage.getRootDir());
        System.out.println("PASSED");
    }

    private static void testLandmarkRegistry() {
        System.out.print("[JER.5] Landmark & POI registry with discovery events... ");

        String worldId = "landmark_test_world";
        LandmarkRegistry registry = new LandmarkRegistry(worldId);

        WorldLandmark shrine = new WorldLandmark(
            "shrine_ancient_1",
            worldId,
            "Sunken Shrine",
            "An overgrown stone monolith beside the marsh.",
            1200.0, 68.0, 1300.0,
            LandmarkType.RUIN,
            DiscoveryState.UNDISCOVERED,
            System.currentTimeMillis()
        );

        WorldLandmark overlook = new WorldLandmark(
            "overlook_pine_2",
            worldId,
            "Pine Ridge Overlook",
            "A scenic peak offering views across the taiga valley.",
            1250.0, 95.0, 1250.0,
            LandmarkType.VIEWPOINT,
            DiscoveryState.UNDISCOVERED,
            System.currentTimeMillis()
        );

        registry.register(shrine);
        registry.register(overlook);
        assert registry.size() == 2 : "Registry should contain 2 landmarks";

        // World Isolation Enforced
        try {
            registry.register(new WorldLandmark("alien_1", "other_world", "Alien Site", "", 0, 0, 0, LandmarkType.CUSTOM, DiscoveryState.UNDISCOVERED, 0));
            assert false : "Registry must reject landmark belonging to another world";
        } catch (IllegalArgumentException expected) {
            // Success
        }

        // Query by ID
        Optional<WorldLandmark> retrieved = registry.get("shrine_ancient_1");
        assert retrieved.isPresent() && "Sunken Shrine".equals(retrieved.get().getName()) : "Failed to retrieve shrine";

        // Proximity Query
        List<WorldLandmark> nearby = registry.getNearbyLandmarks(1240.0, 90.0, 1245.0, 50.0);
        assert nearby.size() == 1 : "Expected 1 landmark within 50m radius";
        assert "overlook_pine_2".equals(nearby.get(0).getLandmarkId()) : "Overlook should be closest";

        // Discovery State Transition and Event Verification
        AtomicBoolean discoveryEventFired = new AtomicBoolean(false);
        Subscription sub = EventBus.getInstance().subscribe(LandmarkDiscoveredEvent.class, e -> {
            discoveryEventFired.set(true);
            assert "shrine_ancient_1".equals(e.landmarkId());
            assert "Sunken Shrine".equals(e.landmarkName());
        });

        boolean stateChanged = registry.markDiscovered("shrine_ancient_1");
        assert stateChanged : "First discovery mark should return true";
        assert discoveryEventFired.get() : "LandmarkDiscoveredEvent must fire";

        // Second call should be idempotent and not re-fire
        discoveryEventFired.set(false);
        boolean secondCall = registry.markDiscovered("shrine_ancient_1");
        assert !secondCall : "Second discovery mark on already discovered landmark must return false";
        assert !discoveryEventFired.get() : "Event should not fire again";

        sub.unsubscribe();

        // JSON Serialization Round-Trip
        String json = registry.toJson();
        assert json != null && json.contains("Sunken Shrine");

        LandmarkRegistry loadedRegistry = new LandmarkRegistry(worldId);
        loadedRegistry.loadFromJson(json);
        assert loadedRegistry.size() == 2 : "Loaded registry must have 2 landmarks";
        assert loadedRegistry.get("shrine_ancient_1").get().isDiscovered() : "Shrine discovery state must be preserved";

        System.out.println("PASSED");
    }

    private static void testWorldLocationAndContext() {
        System.out.print("[JER.6] World location, negative chunk math & context snapshots... ");

        // Negative coordinate math verification
        WorldLocation locPositive = WorldLocation.of("world_1", 35.5, 72.0, 45.2, "plains", "Plains");
        assert locPositive.chunkX() == 2 : "35.5 should map to chunkX = 2 (floorDiv(35, 16))";
        assert locPositive.chunkZ() == 2 : "45.2 should map to chunkZ = 2 (floorDiv(45, 16))";
        assert locPositive.getBlockX() == 35;
        assert locPositive.getBlockY() == 72;
        assert locPositive.getBlockZ() == 45;

        WorldLocation locNegative = WorldLocation.of("world_1", -15.5, 64.0, -33.2, "desert", "Desert");
        assert locNegative.chunkX() == -1 : "-15.5 should map to chunkX = -1 (floorDiv(-16, 16))";
        assert locNegative.chunkZ() == -3 : "-33.2 should map to chunkZ = -3 (floorDiv(-34, 16))";
        assert locNegative.getBlockX() == -16;
        assert locNegative.getBlockZ() == -34;

        // WorldContext snapshot creation
        WorldContext context = new WorldContext(
            "world_abc",
            "Expedition Alpha",
            locPositive,
            5,
            8000.0,
            "Day 5 — 14:00",
            "Plains",
            null,
            true
        );

        assert "world_abc".equals(context.worldId());
        assert context.available();
        assert "Day 5 — 14:00".equals(context.formattedTime());
        assert context.getNearbyLandmark().isEmpty();

        // Safe unavailable fallback
        WorldContext unavailable = WorldContext.UNAVAILABLE;
        assert !unavailable.available() : "UNAVAILABLE context must have available=false";
        assert unavailable.playerLocation() != null : "playerLocation must never be null";

        System.out.println("PASSED");
    }

    private static void testEngineContextFacade() {
        System.out.print("[JER.7] Global EngineContext facade... ");

        // Default before provider is set
        assert !EngineContext.isAvailable() : "EngineContext should be unavailable initially";
        assert EngineContext.getCurrentContext() != null : "EngineContext.getCurrentContext() must not return null";
        assert EngineContext.getPlayerLocation() != null : "EngineContext.getPlayerLocation() must not return null";

        // Register custom provider
        WorldLocation mockLoc = WorldLocation.of("custom_world", 100.0, 70.0, 200.0, "taiga", "Taiga");
        WorldContext mockCtx = new WorldContext("custom_world", "Mock World", mockLoc, 3, 12000.0, "Day 3 — 18:00", "Taiga", null, true);

        EngineContext.setProvider(new WorldContextProvider() {
            @Override
            public WorldContext getCurrentContext() { return mockCtx; }
            @Override
            public WorldLocation getPlayerLocation() { return mockLoc; }
            @Override
            public boolean isContextAvailable() { return true; }
        });

        assert EngineContext.isAvailable() : "EngineContext must now report available";
        assert "Mock World".equals(EngineContext.getCurrentContext().worldName());
        assert "taiga".equals(EngineContext.getPlayerLocation().biomeId());

        // Reset provider
        EngineContext.setProvider(null);
        assert !EngineContext.isAvailable() : "Resetting provider must fall back to safe unavailable default";

        System.out.println("PASSED");
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                f.delete();
            }
        }
        dir.delete();
    }
}

