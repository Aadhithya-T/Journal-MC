package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkPos;
import com.mcjournal.Item;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.BlockBreakingManager;
import com.mcjournal.client.Camera;
import com.mcjournal.client.Player;
import org.joml.Vector3f;

/**
 * Verification test for BlockBreakingManager inventory slot / number change behavior.
 * Ensures that if the player's active inventory slot or held item changes while mining,
 * the mining progress is immediately stopped and restarted from scratch.
 */
public class BlockBreakingInventorySwitchTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   BLOCK BREAKING INVENTORY SWITCH TEST SUITE    ");
        System.out.println("=================================================");

        testInventoryNumberChangeResetsMining();
        testSwitchingBackAndForthAlwaysRestarts();
        testSlotChangeWithoutLmbRemainsZero();
        testHeldItemCountChangeRestartsMining();

        System.out.println("\n>>> ALL BLOCK BREAKING INVENTORY SWITCH TESTS PASSED! <<<\n");
    }

    private static ChunkManager createTestWorld() {
        ChunkManager world = new ChunkManager(4, 12345L, null, com.mcjournal.world.WorldBounds.UNBOUNDED);
        Chunk chunk = new Chunk(0, 0);
        // Place stone block at (5, 10, 5) with hardness 1.5
        chunk.setBlockState(5, 10, 5, Blocks.STONE.getDefaultState());
        world.setChunk(new ChunkPos(0, 0), chunk);
        return world;
    }

    private static void setupPlayerAndCamera(Player player, Camera camera) {
        // Position player at (2.0, 10.5, 5.5) looking along +X (yaw = 90) at stone block (5, 10, 5)
        player.pos.set(2.0f, 10.5f - Player.EYE_HEIGHT, 5.5f);
        player.prevPos.set(player.pos);
        camera.setPosition(2.0f, 10.5f, 5.5f);
        camera.setYaw(90.0f);
        camera.setPitch(0.0f);
    }

    private static void testInventoryNumberChangeResetsMining() {
        System.out.print("[BBM.1] Inventory slot change while mining stops and restarts from scratch... ");

        ChunkManager world = createTestWorld();
        Player player = new Player();
        Camera camera = new Camera();
        setupPlayerAndCamera(player, camera);

        BlockBreakingManager bbm = new BlockBreakingManager();

        // Slot 2 = Iron Pickaxe (Tool speed multiplier = 6.0, canHarvest = true)
        // Hardness = 1.5 -> damagePerTick = 6.0 / (1.5 * 30) = 0.1333f per tick
        player.selectedSlot = 2;

        // Tick 1
        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        float p1 = bbm.getBreakProgress();
        assert p1 > 0.16f && p1 < 0.20f : "Expected ~0.133 progress on tick 1, got: " + p1;

        // Tick 2
        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        float p2 = bbm.getBreakProgress();
        assert p2 > 0.34f && p2 < 0.38f : "Expected ~0.266 progress on tick 2, got: " + p2;

        // Tick 3: Switch inventory slot from 2 to 0 (Iron Axe: wrong tool for stone!)
        // Wrong tool damagePerTick = 1.0 / (1.5 * 100) = 0.00667f
        player.selectedSlot = 0;
        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        float p3 = bbm.getBreakProgress();

        // The progress must NOT be 0.266 + 0.0067 (~0.273).
        // It MUST be stopped and restarted from scratch (approx 0.0067f)!
        assert p3 < 0.02f : "Mining should have restarted from scratch on slot change! Got: " + p3;
        assert p3 > 0.005f : "Mining should have started from scratch with new tool! Got: " + p3;

        System.out.println("PASSED (Progress reset from " + String.format("%.2f", p2) + " -> " + String.format("%.4f", p3) + ")");
    }

    private static void testSwitchingBackAndForthAlwaysRestarts() {
        System.out.print("[BBM.2] Repeated slot switching always restarts mining from scratch... ");

        ChunkManager world = createTestWorld();
        Player player = new Player();
        Camera camera = new Camera();
        setupPlayerAndCamera(player, camera);

        BlockBreakingManager bbm = new BlockBreakingManager();

        // Mine with Pickaxe (Slot 2) for 3 ticks
        player.selectedSlot = 2;
        for (int i = 0; i < 3; i++) {
            bbm.update(world, null, null, null, null, null, player, camera, true, false);
        }
        assert bbm.getBreakProgress() > 0.50f : "Progress should be > 0.35f after 3 ticks";

        // Switch to Slot 1 (Shovel) -> restarts
        player.selectedSlot = 1;
        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        assert bbm.getBreakProgress() < 0.02f : "Progress must restart from scratch on switch to Slot 1";

        // Mine with Shovel for 5 ticks
        for (int i = 0; i < 4; i++) {
            bbm.update(world, null, null, null, null, null, player, camera, true, false);
        }
        float shovelProgress = bbm.getBreakProgress();
        assert shovelProgress > 0.02f && shovelProgress < 0.05f : "Shovel progress should accumulate slowly";

        // Switch back to Slot 2 (Pickaxe) -> restarts again from scratch!
        player.selectedSlot = 2;
        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        float pickaxeRestart = bbm.getBreakProgress();
        assert pickaxeRestart > 0.16f && pickaxeRestart < 0.20f : "Expected single-tick pickaxe progress (~0.133), got: " + pickaxeRestart;

        System.out.println("PASSED (Seamless re-start on each switch)");
    }

    private static void testSlotChangeWithoutLmbRemainsZero() {
        System.out.print("[BBM.3] Slot change without mouse click clears progress cleanly... ");

        ChunkManager world = createTestWorld();
        Player player = new Player();
        Camera camera = new Camera();
        setupPlayerAndCamera(player, camera);

        BlockBreakingManager bbm = new BlockBreakingManager();

        // Mine with pickaxe
        player.selectedSlot = 2;
        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        assert bbm.getBreakProgress() > 0.1f;

        // Switch slot while releasing LMB
        player.selectedSlot = 0;
        bbm.update(world, null, null, null, null, null, player, camera, false, false);
        assert bbm.getBreakProgress() == 0.0f : "Break progress must be 0 when LMB released";

        System.out.println("PASSED");
    }

    private static void testHeldItemCountChangeRestartsMining() {
        System.out.print("[BBM.4] Item change in same slot resets mining from scratch... ");

        ChunkManager world = createTestWorld();
        Player player = new Player();
        Camera camera = new Camera();
        setupPlayerAndCamera(player, camera);

        BlockBreakingManager bbm = new BlockBreakingManager();

        player.selectedSlot = 2;
        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        assert bbm.getBreakProgress() > 0.25f;

        // Simulate item dropping / breaking in slot 2
        player.hotbarCounts[2] = 0;
        player.hotbarBlocks[2] = 0;

        bbm.update(world, null, null, null, null, null, player, camera, true, false);
        float bareHandProgress = bbm.getBreakProgress();
        assert bareHandProgress < 0.02f : "Progress should reset to bare hand scratch rate, got: " + bareHandProgress;

        System.out.println("PASSED");
    }
}
