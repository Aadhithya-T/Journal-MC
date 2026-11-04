package com.mcjournal.test;

/**
 * MasterTestSuite orchestrates execution of all automated test suites across
 * all engine systems (P1 through P11) with timing and unified status reporting.
 */
public class MasterTestSuite {

    @FunctionalInterface
    interface TestRunner {
        void run() throws Exception;
    }

    private static int passedSuites = 0;
    private static int totalSuites = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("            MINECRAFT JOURNAL - ENGINE MASTER AUTOMATED TEST SUITE             ");
        System.out.println("===============================================================================\n");

        long suiteStartTime = System.currentTimeMillis();

        // Phase 1: Finite World Configuration & Bounds
        runSuite("Phase 1: Finite World Configuration & Bounds", () -> FiniteWorldBoundsTest.main(new String[0]));

        // Phase 2: Finite Chunk System, Streaming & Partial Edge Chunks
        runSuite("Phase 2: Finite Chunk System, Streaming & Partial Edge Chunks", () -> FiniteChunkSystemTest.main(new String[0]));

        // Phase 3: Physical World Containment, Raycasting & Boundary Systems
        runSuite("Phase 3: Physical World Containment, Raycasting & Boundary Systems", () -> FiniteBoundaryPhysicsTest.main(new String[0]));

        // Phase 4: World Metadata V4, Persistence & Legacy Save Compatibility
        runSuite("Phase 4: World Metadata V4, Persistence & Legacy Save Compatibility", () -> FiniteWorldPersistenceTest.main(new String[0]));

        // Phase 5: Terrain & Biome Architecture
        runSuite("Phase 5: Terrain & Biome Architecture", () -> FiniteTerrainGenerationTest.main(new String[0]));

        // Phase 6: Structure Generation & POI Framework
        runSuite("Phase 6: Structure Generation & POI Framework", () -> FiniteStructureGenerationTest.main(new String[0]));

        // Phase 7: Finite World Final Integration & Stability
        runSuite("Phase 7: Finite World Final Integration & Stability", () -> FiniteWorldIntegrationAndStabilityTest.main(new String[0]));

        // GUI Overhaul Phase 0: GuiRenderer Verification & Stability
        runSuite("GUI Overhaul Phase 0: GuiRenderer Verification & Stability", () -> GuiRendererVerificationTest.main(new String[0]));

        // GUI Overhaul Phase 1: Virtual Resolution & Foundation
        runSuite("GUI Overhaul Phase 1: Virtual Resolution & Foundation", () -> GuiPhase1VerificationTest.main(new String[0]));

        // GUI Overhaul Phase 2: Widget Toolkit & Screen Management
        runSuite("GUI Overhaul Phase 2: Widget Toolkit & Screen Management", () -> GuiPhase2VerificationTest.main(new String[0]));

        // GUI Overhaul Phase 3: Screen Redesign & Branding
        runSuite("GUI Overhaul Phase 3: Screen Redesign & Branding", () -> GuiPhase3VerificationTest.main(new String[0]));

        // GUI Overhaul Phase 4: Gameplay HUD
        runSuite("GUI Overhaul Phase 4: Gameplay HUD", () -> GuiPhase4VerificationTest.main(new String[0]));

        // GUI Typography and Layout Overhaul: Phase 4 Overlap Diagnostics
        runSuite("GUI Overhaul: Phase 4 Layout and Overlap Diagnostics", () -> GuiPhase4OverlapDiagnosticTest.main(new String[0]));

        // Non-Mining & Exploration: Block Protection, Pickup & Item Invariants
        runSuite("Non-Mining & Exploration: Block Protection, Pickup & Item Invariants", () -> NonMiningExplorationTest.main(new String[0]));

        // Journal Engine Readiness: World Metadata, Clock, Events, Storage, Landmarks & Context
        runSuite("Journal Engine Readiness: Metadata, Clock, Events, Storage & Context", () -> JournalEngineReadinessTest.main(new String[0]));

        // World Generation: Gravel, Andesite, Diorite & Stone
        runSuite("World Generation: Gravel, Andesite, Diorite & Stone", () -> NewBlocksWorldGenTest.main(new String[0]));

        // Tree Types & Foliage: Spruce, Dark Oak, Birch & Oak
        runSuite("Tree Types & Foliage: Spruce, Dark Oak, Birch & Oak", () -> TreeFoliageAndNewTreesTest.main(new String[0]));

        // Texture Redesign: Authentic Stone & Cobblestone
        runSuite("Texture Redesign: Authentic Stone & Cobblestone", () -> StoneAndCobblestoneRedesignTest.main(new String[0]));

        // P12 & P13 Performance Instrumentation & Constants
        runSuite("P12 & P13: Engine Instrumentation & Constants", () -> EngineInstrumentationTest.main(new String[0]));

        // P11 New Dedicated Test Suites
        runSuite("P11: Block System (BlockState, Registry, Properties, Transitions)", () -> BlockSystemTest.main(new String[0]));

        runSuite("P11: World Coordinates & Chunk Boundaries", () -> WorldCoordinatesTest.main(new String[0]));

        runSuite("P11: Terrain Determinism & Divergence", () -> TerrainGenerationTest.main(new String[0]));

        runSuite("P11: Raycast DDA Edge Cases", () -> RaycastEdgeCasesTest.main(new String[0]));

        runSuite("P11: Physics, Collision, Gravity & Fall Damage", () -> PhysicsSimulationTest.main(new String[0]));

        runSuite("P11: Persistence Save -> Load Bit-for-Bit Fidelity", () -> PersistenceRoundTripTest.main(new String[0]));

        // Engine Integration Suites
        runSuite("P10: MCJournalApp Modular Subsystems", () -> MCJournalAppSubsystemsTest.main(new String[0]));

        runSuite("P9: Persistence Migration & Async Saving", () -> PersistenceMigrationAndAsyncTest.main(new String[0]));

        runSuite("P8: Comprehensive Raycast DDA", () -> RaycastDDATest.main(new String[0]));

        runSuite("P6 & P7: Physics & Fluid Simulation", () -> PhysicsAndFluidTest.main(new String[0]));

        runSuite("P4 & P5: Mutation & GPU Upload Pipeline", () -> MutationAndGpuPipelineTest.main(new String[0]));

        runSuite("P3: Greedy Chunk Meshing & Neighborhoods", () -> ChunkMeshingTest.main(new String[0]));

        runSuite("P2: BlockState Registry & Round-Trip", () -> BlockStateRoundTripTest.main(new String[0]));

        runSuite("P2: ChunkSerializer Binary Round-Trip", () -> ChunkSerializerRoundTripTest.main(new String[0]));

        runSuite("P1: Chunk Streaming & Lifecycle", () -> ChunkStreamingTest.main(new String[0]));

        long totalTime = System.currentTimeMillis() - suiteStartTime;

        System.out.println("===============================================================================");
        System.out.println(String.format("TEST RESULTS: %d/%d SUITES PASSED (Total Time: %d ms)", passedSuites, totalSuites, totalTime));
        System.out.println("===============================================================================");

        if (passedSuites == totalSuites) {
            System.out.println("? ALL AUTOMATED TESTS COMPLETED WITH ZERO ERRORS!");
        } else {
            System.err.println("❌ SOME TESTS FAILED!");
            System.exit(1);
        }
    }

    private static void runSuite(String suiteName, TestRunner runner) {
        totalSuites++;
        long start = System.currentTimeMillis();
        try {
            runner.run();
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("✔ [" + suiteName + "] PASSED in " + elapsed + " ms\n");
            passedSuites++;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            System.err.println("❌ [" + suiteName + "] FAILED in " + elapsed + " ms");
            t.printStackTrace();
            System.err.println();
        }
    }
}
