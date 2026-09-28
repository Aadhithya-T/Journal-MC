package com.mcjournal.test;

import com.mcjournal.client.AtmosphericTimeSystem;
import com.mcjournal.client.RenderingConfig;
import com.mcjournal.client.ScreenManager;
import com.mcjournal.client.WorldSession;
import org.joml.Vector3f;

/**
 * Unit test suite for P10 modular subsystems extracted from MCJournalApp.
 */
public class MCJournalAppSubsystemsTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("      RUNNING P10 (APP CLEANUP) TEST SUITE       ");
        System.out.println("=================================================");

        testAtmosphericTimeSystem();
        testScreenManager();
        testWorldSessionLifecycle();

        System.out.println("\n>>> ALL P10 APP CLEANUP TESTS PASSED SUCCESSFULLY! <<<");
    }

    private static void testAtmosphericTimeSystem() {
        System.out.print("[P10.1] AtmosphericTimeSystem solar cycle & lighting... ");
        AtmosphericTimeSystem timeSystem = new AtmosphericTimeSystem();
        // Solar orbit: tick 12,000 corresponds to peak elevation
        timeSystem.setWorldTimeTicks(12000.0);
        timeSystem.update(0.0, true, false);

        Vector3f sunDirHigh = timeSystem.getSunDir();
        assert sunDirHigh.y > 0.0f : "Sun elevation should be high at tick 12000";
        assert timeSystem.getDirectLightColor().length() > 0.5f : "Direct light color should be bright during peak day";
        float dayExposure = RenderingConfig.exposure;

        // Advance to night (tick 0.0 / 24,000.0)
        timeSystem.setWorldTimeTicks(0.0);
        timeSystem.update(0.0, true, false);
        Vector3f sunDirNight = timeSystem.getSunDir();
        assert sunDirNight.y < 0.0f : "Sun elevation should be negative at tick 0";
        assert timeSystem.getUnderwaterFogColor() != null : "Underwater fog color should be populated";
        assert RenderingConfig.exposure >= dayExposure : "Night exposure adaptation should boost visibility";

        // Advance by delta time
        double startTicks = timeSystem.getWorldTimeTicks();
        timeSystem.update(1.0, true, false); // 1.0 second = 20 ticks
        assert Math.abs(timeSystem.getWorldTimeTicks() - ((startTicks + 20.0) % 24000.0)) < 0.001 : "Tick rate must be 20 ticks per second";

        System.out.println("PASSED");
    }

    private static void testScreenManager() {
        System.out.print("[P10.2] ScreenManager state and lifecycle... ");
        ScreenManager sm = new ScreenManager();
        assert !sm.hasScreen() : "Should start without active screen";
        assert sm.getCurrentScreen() == null : "Initial screen must be null";
        System.out.println("PASSED");
    }

    private static void testWorldSessionLifecycle() {
        System.out.print("[P10.3] WorldSession encapsulation & state... ");
        WorldSession session = new WorldSession();
        assert !session.isInWorld() : "Session should start not in world";
        assert session.getCurrentBiome() != null : "Biome must not be null";
        assert session.getCurrentWorldName() != null : "World name must not be null";
        assert session.getBlockBreakingManager() != null : "BlockBreakingManager must be initialized";
        assert session.getFluidPhysicsManager() != null : "FluidPhysicsManager must be initialized";
        assert session.getItemEntityManager() != null : "ItemEntityManager must be initialized";
        assert session.getParticleManager() != null : "ParticleManager must be initialized";

        session.shutdown(null, 6000.0);
        assert !session.isInWorld() : "Session should remain closed after shutdown";
        System.out.println("PASSED");
    }
}
