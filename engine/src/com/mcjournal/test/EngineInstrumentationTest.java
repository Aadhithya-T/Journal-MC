package com.mcjournal.test;

import com.mcjournal.EngineConstants;
import com.mcjournal.RegionPos;
import com.mcjournal.client.EngineMetrics;

/**
 * P12 & P13 Unit Test Suite:
 * - EngineMetrics telemetry and debug overlay functionality (P12)
 * - EngineConstants centralization and magic number elimination (P13.1)
 * - RegionPos and record value object conversions (P13.2)
 */
public class EngineInstrumentationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   RUNNING P12 & P13: INSTRUMENTATION & QUALITY  ");
        System.out.println("=================================================");

        testEngineConstants();
        testEngineMetricsTelemetry();
        testRegionPosCalculations();

        System.out.println(">>> ALL INSTRUMENTATION & QUALITY TESTS PASSED! <<<\n");
    }

    private static void testEngineConstants() {
        System.out.print("[P13.1] Centralized EngineConstants validation... ");

        assert EngineConstants.CHUNK_SIZE == 16 : "CHUNK_SIZE must be 16";
        assert EngineConstants.CHUNK_HEIGHT == 256 : "CHUNK_HEIGHT must be 256";
        assert EngineConstants.CHUNK_TOTAL_VOXELS == 16 * 16 * 256 : "CHUNK_TOTAL_VOXELS must be 65,536";
        assert EngineConstants.REGION_SIZE_CHUNKS == 32 : "REGION_SIZE_CHUNKS must be 32";
        assert EngineConstants.REGION_TOTAL_CHUNKS == 32 * 32 : "REGION_TOTAL_CHUNKS must be 1,024";
        assert EngineConstants.TICKS_PER_SECOND == 20 : "TICKS_PER_SECOND must be 20";
        assert Math.abs(EngineConstants.TICK_DURATION_SECONDS - 0.050) < 0.0001 : "TICK_DURATION must be 0.050s";
        assert EngineConstants.GRAVITY == 0.08f : "GRAVITY must be 0.08";
        assert EngineConstants.JUMP_IMPULSE == 0.42f : "JUMP_IMPULSE must be 0.42";
        assert EngineConstants.FALL_DAMAGE_THRESHOLD == 3.5f : "FALL_DAMAGE_THRESHOLD must be 3.5";

        System.out.println("PASSED");
    }

    private static void testEngineMetricsTelemetry() {
        System.out.print("[P12] EngineMetrics telemetry capture and latching... ");

        EngineMetrics metrics = EngineMetrics.getInstance();
        metrics.updateRates(144, 20);
        assert metrics.getFps() == 144 : "FPS should be 144";
        assert metrics.getTps() == 20 : "TPS should be 20";

        metrics.updateChunkCounts(441, 87, 4, 2, 5);
        assert metrics.getChunksLoaded() == 441 : "Chunks loaded mismatch";
        assert metrics.getChunksVisible() == 87 : "Chunks visible mismatch";
        assert metrics.getChunksGenerating() == 4 : "Chunks generating mismatch";
        assert metrics.getChunksMeshing() == 2 : "Chunks meshing mismatch";
        assert metrics.getDirtyChunks() == 5 : "Dirty chunks mismatch";

        // Record frame activities
        metrics.recordGpuUpload();
        metrics.recordGpuUpload();
        metrics.recordGpuUpload();
        metrics.recordFluidUpdates(18);
        metrics.recordDrawCall(500_000);
        metrics.recordDrawCall(700_000);

        // Record pipeline timings
        metrics.recordChunkGenTime(1.2);
        metrics.recordMeshGenTime(0.8);
        metrics.recordGpuUploadTime(0.4);

        assert metrics.getChunkGenTimeMs() > 0.0 : "Chunk gen time should be recorded";
        assert metrics.getMeshGenTimeMs() > 0.0 : "Mesh gen time should be recorded";
        assert metrics.getGpuUploadTimeMs() > 0.0 : "GPU upload time should be recorded";

        // Latch frame
        metrics.endFrame();
        assert metrics.getLastGpuUploads() == 3 : "Should have 3 GPU uploads";
        assert metrics.getLastFluidUpdates() == 18 : "Should have 18 fluid updates";
        assert metrics.getLastDrawCalls() == 2 : "Should have 2 draw calls";
        assert metrics.getLastVerticesRendered() == 1_200_000L : "Should have 1.2M vertices rendered";

        // Format formatting helper
        assert EngineMetrics.formatVertices(1_200_000L).equals("1.2M") : "Format 1.2M mismatch";
        assert EngineMetrics.formatVertices(45_600L).equals("45.6k") : "Format 45.6k mismatch";
        assert EngineMetrics.formatVertices(850L).equals("850") : "Format 850 mismatch";

        // Overlay toggle
        boolean initialVisible = metrics.isDebugOverlayVisible();
        metrics.toggleDebugOverlay();
        assert metrics.isDebugOverlayVisible() == !initialVisible : "Toggle should invert visibility";
        metrics.setDebugOverlayVisible(false);
        assert !metrics.isDebugOverlayVisible() : "setDebugOverlayVisible(false) mismatch";

        System.out.println("PASSED");
    }

    private static void testRegionPosCalculations() {
        System.out.print("[P13.2] RegionPos coordinate conversions with EngineConstants... ");

        RegionPos pos = RegionPos.fromChunkCoords(35, -5);
        assert pos.rx() == 1 && pos.rz() == -1 : "Region coordinates mismatch";
        assert pos.getLocalChunkX(35) == 3 : "Local chunk X mismatch";
        assert pos.getLocalChunkZ(-5) == 27 : "Local chunk Z mismatch for negative coordinate (-5 mod 32 = 27)";
        assert pos.getFileName().equals("r.1.-1.jmc") : "File name mismatch";

        RegionPos fromWorld = RegionPos.fromWorldCoords(512, -16);
        assert fromWorld.rx() == 1 && fromWorld.rz() == -1 : "World to region coordinate mismatch";

        System.out.println("PASSED");
    }
}
