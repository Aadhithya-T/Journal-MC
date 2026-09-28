package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkManager;
import com.mcjournal.ChunkPos;
import com.mcjournal.block.Blocks;
import com.mcjournal.client.Player;
import com.mcjournal.physics.PhysicsSystem;
import org.joml.Vector3f;

import java.io.File;

/**
 * P11 Unit Test: Physics Simulation
 * Covers: collision resolution, gravity acceleration, jump impulse, mid-air jump prevention,
 * fall damage calculation, and water damage negation.
 */
public class PhysicsSimulationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       RUNNING P11: PHYSICS SIMULATION TESTS     ");
        System.out.println("=================================================");

        testFloorAndWallCollision();
        testGravityAcceleration();
        testJumpMechanics();
        testFallDamageCalculation();
        testWaterFallDamageNegation();
        testPlayerFatalFallPermadeath();

        System.out.println(">>> ALL PHYSICS SIMULATION TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static ChunkManager createTestWorld() {
        ChunkManager cm = new ChunkManager(2, 42L, new File("saves/test_physics_p11"));
        Chunk c = new Chunk(0, 0);

        // Solid floor at Y=64
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                c.setBlockState(x, 64, z, Blocks.STONE.getDefaultState());
            }
        }
        cm.setChunk(c.getPos(), c);
        return cm;
    }

    private static void testFloorAndWallCollision() {
        System.out.print("[P11 - Physics] Floor collision & wall sliding resolution... ");

        ChunkManager world = createTestWorld();
        // Add a vertical wall at x=10, y=65..67, z=0..15
        Chunk c = world.getChunk(0, 0);
        for (int z = 0; z < 16; z++) {
            c.setBlockState(10, 65, z, Blocks.STONE.getDefaultState());
            c.setBlockState(10, 66, z, Blocks.STONE.getDefaultState());
        }

        PhysicsSystem physics = new PhysicsSystem();

        // 1. Landing on floor at Y=65.0
        Vector3f pos = new Vector3f(5.0f, 65.2f, 5.0f);
        Vector3f vel = new Vector3f(0.0f, -0.3f, 0.0f);
        PhysicsSystem.PhysicsState state = physics.updateEntity(
            world, pos, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
            false, 0.2f, 65.2f, 0, 0, 0, 0, false, false, false, false
        );

        assert state.onGround() : "Player must be on ground after landing on floor";
        assert Math.abs(state.pos().y - 65.0f) < 0.001f : "Player Y position must be resting exactly at 65.0f";
        assert state.velocity().y == 0.0f : "Downward velocity must be stopped by floor";

        // 2. Walking into wall at X=10 while trying to slide along +Z
        pos.set(9.6f, 65.0f, 5.0f);
        vel.set(0.5f, 0.0f, 0.3f); // Moving towards +X and +Z
        state = physics.updateEntity(
            world, pos, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
            true, 0.0f, 65.0f, 1.0f, 1.0f, 0.0f, 1.0f, true, false, false, false
        );

        assert state.pos().x < 10.0f - (Player.WIDTH / 2.0f) : "Player X must be stopped before entering the wall";
        assert state.pos().z > 5.0f : "Player should slide freely along unobstructed Z axis";

        System.out.println("PASSED");
    }

    private static void testGravityAcceleration() {
        System.out.print("[P11 - Physics] Gravity downward acceleration & curve... ");

        ChunkManager world = createTestWorld();
        PhysicsSystem physics = new PhysicsSystem();

        Vector3f pos = new Vector3f(5.0f, 120.0f, 5.0f); // High in air
        Vector3f vel = new Vector3f(0.0f, 0.0f, 0.0f);

        float prevY = pos.y;
        for (int tick = 0; tick < 10; tick++) {
            PhysicsSystem.PhysicsState state = physics.updateEntity(
                world, pos, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
                false, 0.0f, 120.0f, 0, 0, 0, 0, false, false, false, false
            );
            assert !state.onGround() : "Should be in free fall";
            assert state.pos().y < prevY : "Y position must decrease continuously under gravity";
            assert state.velocity().y < 0.0f : "Vertical velocity must be negative during fall";

            pos.set(state.pos());
            vel.set(state.velocity());
            prevY = pos.y;
        }

        System.out.println("PASSED");
    }

    private static void testJumpMechanics() {
        System.out.print("[P11 - Physics] Jump impulse and mid-air jump prevention... ");

        ChunkManager world = createTestWorld();
        PhysicsSystem physics = new PhysicsSystem();

        // 1. Jump from ground
        Vector3f pos = new Vector3f(5.0f, 65.0f, 5.0f);
        Vector3f vel = new Vector3f(0.0f, 0.0f, 0.0f);
        PhysicsSystem.PhysicsState groundJump = physics.updateEntity(
            world, pos, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
            true, 0.0f, 65.0f, 0, 0, 0, 0, false, true, false, false
        );

        assert groundJump.velocity().y > 0.30f : "Jump impulse must give positive upwards velocity";

        // 2. Jump in mid-air (onGround = false)
        Vector3f airPos = new Vector3f(5.0f, 75.0f, 5.0f);
        Vector3f airVel = new Vector3f(0.0f, -0.1f, 0.0f);
        PhysicsSystem.PhysicsState midAirJump = physics.updateEntity(
            world, airPos, airPos, airVel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
            false, 1.0f, 76.0f, 0, 0, 0, 0, false, true, false, false
        );

        assert midAirJump.velocity().y < 0.0f : "Mid-air jump must NOT trigger upwards velocity";

        System.out.println("PASSED");
    }

    private static void testFallDamageCalculation() {
        System.out.print("[P11 - Physics] Fall damage distance threshold calculation... ");

        ChunkManager world = createTestWorld();
        PhysicsSystem physics = new PhysicsSystem();

        // 1. Safe fall: 3.0 blocks
        Vector3f pos = new Vector3f(5.0f, 65.1f, 5.0f);
        Vector3f vel = new Vector3f(0.0f, -0.2f, 0.0f);
        PhysicsSystem.PhysicsState safeState = physics.updateEntity(
            world, pos, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
            false, 3.0f, 68.0f, 0, 0, 0, 0, false, false, false, false
        );
        assert safeState.onGround() && safeState.fallDamage() == 0 : "Fall <= 3.5 blocks must deal 0 damage";

        // 2. Damaging fall: 6.5 blocks -> floor(6.5 - 3.5) = 3 damage
        PhysicsSystem.PhysicsState damageState = physics.updateEntity(
            world, pos, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
            false, 6.5f, 71.5f, 0, 0, 0, 0, false, false, false, false
        );
        assert damageState.onGround() && damageState.fallDamage() == 3 : "Fall of 6.5 blocks must deal exactly 3 damage (got " + damageState.fallDamage() + ")";

        // 3. Heavy fall: 15.0 blocks -> floor(15.0 - 3.5) = 11 damage
        PhysicsSystem.PhysicsState heavyState = physics.updateEntity(
            world, pos, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
            false, 15.0f, 80.0f, 0, 0, 0, 0, false, false, false, false
        );
        assert heavyState.onGround() && heavyState.fallDamage() == 11 : "Fall of 15 blocks must deal 11 damage";

        System.out.println("PASSED");
    }

    private static void testWaterFallDamageNegation() {
        System.out.print("[P11 - Physics] Water fall damage negation... ");

        ChunkManager world = createTestWorld();
        // Place water pool at y=65
        Chunk c = world.getChunk(0, 0);
        c.setBlockState(5, 65, 5, Blocks.WATER.getDefaultState());

        PhysicsSystem physics = new PhysicsSystem();

        // Plunge into water from 50 blocks high
        Vector3f pos = new Vector3f(5.5f, 65.5f, 5.5f);
        Vector3f vel = new Vector3f(0.0f, -1.5f, 0.0f);

        PhysicsSystem.PhysicsState waterState = physics.updateEntity(
            world, pos, pos, vel, Player.WIDTH, Player.HEIGHT, Player.STEP_HEIGHT, Player.EYE_HEIGHT,
            false, 50.0f, 115.0f, 0, 0, 0, 0, false, false, false, false
        );

        assert waterState.inWater() : "Entity must detect inWater()";
        assert waterState.fallDamage() == 0 : "Water contact must completely negate fall damage";
        assert waterState.fallDistance() == 0.0f : "Fall distance must be reset to 0 upon entering water";

        System.out.println("PASSED");
    }

    private static void testPlayerFatalFallPermadeath() {
        System.out.print("[P11 - Physics] Player fatal damage and permadeath state... ");

        Player player = new Player();
        player.health = 20;
        player.isDead = false;

        // Take non-fatal damage
        player.takeDamage(10);
        assert player.health == 10 : "Health should be 10";
        assert !player.isDead : "Player should not be dead";

        // Take fatal damage
        player.takeDamage(15);
        assert player.health == 0 : "Health should be clamped to 0";
        assert player.isDead : "Player must be dead when health drops to 0";

        System.out.println("PASSED");
    }
}
