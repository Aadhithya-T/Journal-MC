package com.mcjournal.test;

import com.mcjournal.client.gui.*;

import java.util.List;

/**
 * Phase 4 verification test: rigorously tests WorldSelectScreen and WorldCreateScreen
 * across resolutions and GUI scale settings to guarantee zero out-of-bounds or overlap violations.
 */
public class GuiPhase4OverlapDiagnosticTest {

    public static void main(String[] args) {
        System.out.println("=== RUNNING GUI PHASE 4 OVERLAP DIAGNOSTIC TEST ===");
        runTests();
        System.out.println(">>> ALL GUI PHASE 4 OVERLAP TESTS PASSED SUCCESSFULLY! <<<");
    }

    public static void runTests() {
        int[][] resolutions = {
                {854, 480},
                {1280, 720},
                {1920, 1080},
                {2560, 1440}
        };

        int[] scales = {0, 1, 2}; // 0 = Auto

        GuiScaleManager scaleManager = new GuiScaleManager();

        for (int[] res : resolutions) {
            int resW = res[0];
            int resH = res[1];

            for (int scale : scales) {
                scaleManager.setRequestedScale(scale);
                scaleManager.update(resW, resH, resW, resH);
                int guiW = scaleManager.getGuiWidth();
                int guiH = scaleManager.getGuiHeight();

                // Test WorldSelectScreen
                WorldSelectScreen selectScreen = new WorldSelectScreen(null);
                selectScreen.init(guiW, guiH);
                List<GuiDiagnostics.Violation> selectViolations = GuiDiagnostics.checkScreen(selectScreen, null);
                if (!selectViolations.isEmpty()) {
                    for (GuiDiagnostics.Violation v : selectViolations) {
                        System.err.println("VIOLATION in SelectScreen (" + resW + "x" + resH + " scale=" + scale + "): " + v);
                    }
                    throw new AssertionError("WorldSelectScreen has " + selectViolations.size() + " layout violations at "
                            + resW + "x" + resH + " scale=" + scale);
                }

                // Test WorldCreateScreen
                WorldCreateScreen createScreen = new WorldCreateScreen(null);
                createScreen.init(guiW, guiH);
                List<GuiDiagnostics.Violation> createViolations = GuiDiagnostics.checkScreen(createScreen, null);
                if (!createViolations.isEmpty()) {
                    for (GuiDiagnostics.Violation v : createViolations) {
                        System.err.println("VIOLATION in CreateScreen (" + resW + "x" + resH + " scale=" + scale + "): " + v);
                    }
                    throw new AssertionError("WorldCreateScreen has " + createViolations.size() + " layout violations at "
                            + resW + "x" + resH + " scale=" + scale);
                }

                // Test WorldEditScreen
                com.mcjournal.client.WorldSaveManager.SavedWorld mockWorld =
                        new com.mcjournal.client.WorldSaveManager.SavedWorld();
                mockWorld.name = "Test World";
                mockWorld.biome = "Plains";
                mockWorld.seed = 12345L;
                mockWorld.worldWidth = 2500;
                mockWorld.worldDepth = 2500;
                mockWorld.worldHeight = 256;
                mockWorld.createdAt = "2026-11-02";
                WorldEditScreen editScreen = new WorldEditScreen(null, mockWorld);
                editScreen.init(guiW, guiH);
                List<GuiDiagnostics.Violation> editViolations = GuiDiagnostics.checkScreen(editScreen, null);
                if (!editViolations.isEmpty()) {
                    for (GuiDiagnostics.Violation v : editViolations) {
                        System.err.println("VIOLATION in EditScreen (" + resW + "x" + resH + " scale=" + scale + "): " + v);
                    }
                    throw new AssertionError("WorldEditScreen has " + editViolations.size() + " layout violations at "
                            + resW + "x" + resH + " scale=" + scale);
                }
            }
        }

        System.out.println("[Phase 4 Diagnostics] Verified WorldSelectScreen, WorldCreateScreen, and WorldEditScreen across 12 resolution/scale configurations: 0 violations.");
    }
}

