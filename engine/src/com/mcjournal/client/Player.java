package com.mcjournal.client;

import com.mcjournal.ChunkManager;
import com.mcjournal.physics.PhysicsSystem;
import org.joml.Vector3f;

public class Player {
    // Physics and Collision Engine Subsystem (P7.1)
    private final PhysicsSystem physicsSystem = new PhysicsSystem();

    // Physical dimensions (Vanilla Minecraft: 0.6 x 1.8 x 0.6)
    public static final float WIDTH = com.mcjournal.EngineConstants.PLAYER_WIDTH;
    public static final float HEIGHT = com.mcjournal.EngineConstants.PLAYER_HEIGHT;
    public static final float EYE_HEIGHT = com.mcjournal.EngineConstants.PLAYER_EYE_HEIGHT;
    public static final float SNEAK_EYE_HEIGHT = 1.27f;
    public static final float STEP_HEIGHT = com.mcjournal.EngineConstants.PLAYER_STEP_HEIGHT;

    // Movement constants
    public static final float GRAVITY = com.mcjournal.EngineConstants.GRAVITY;       // blocks per tick^2
    public static final float DRAG_Y = com.mcjournal.EngineConstants.DRAG_Y;        // vertical air drag
    public static final float JUMP_IMPULSE = com.mcjournal.EngineConstants.JUMP_IMPULSE;  // vanilla jump impulse (~1.25 block height)

    public final Vector3f pos = new Vector3f(8.0f, 16.0f, 8.0f);
    public final Vector3f prevPos = new Vector3f(8.0f, 16.0f, 8.0f);
    public final Vector3f velocity = new Vector3f(0, 0, 0);

    public float yaw = 0;   // In degrees
    public float pitch = 0; // In degrees
    public boolean onGround = false;
    public boolean isSprinting = false;
    public boolean isSneaking = false;

    // Selected hotbar slot (0..8)
    public int selectedSlot = 0;

    // 9-Slot Hotbar Inventory (initialized empty: exploration & journaling mode)
    public final byte[] hotbarBlocks = new byte[9];
    public final int[] hotbarCounts = new int[9];

    public float getX() { return pos.x; }
    public float getY() { return pos.y; }
    public float getZ() { return pos.z; }

    public byte getSelectedBlock() {
        int slot = Math.clamp(selectedSlot, 0, 8);
        return (hotbarCounts[slot] > 0) ? hotbarBlocks[slot] : 0;
    }

    public int getSelectedCount() {
        int slot = Math.clamp(selectedSlot, 0, 8);
        return hotbarCounts[slot];
    }

    public void consumeSelected() {
        int slot = Math.clamp(selectedSlot, 0, 8);
        if (hotbarCounts[slot] > 0) {
            hotbarCounts[slot]--;
            if (hotbarCounts[slot] == 0) {
                hotbarBlocks[slot] = 0;
            }
        }
    }

    /**
     * Checks whether the player's hotbar has capacity to receive at least one item of the specified block type.
     *
     * @param blockType The block byte ID to check.
     * @return true if there is an existing non-full stack for this item or an empty hotbar slot available.
     */
    public boolean canAddItem(byte blockType) {
        if (blockType == 0) return false;

        // 1. Check if it can merge into an existing non-full stack
        for (int i = 0; i < 9; i++) {
            if (hotbarBlocks[i] == blockType && hotbarCounts[i] > 0 && hotbarCounts[i] < 64) {
                return true;
            }
        }

        // 2. Check for an empty slot
        for (int i = 0; i < 9; i++) {
            if (hotbarBlocks[i] == 0 || hotbarCounts[i] <= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds items to the player's hotbar inventory.
     * First attempts to merge with existing stacks (max 64), then populates empty slots.
     *
     * @param blockType The block byte ID to add.
     * @param count The number of items to add.
     * @return Remaining items that could not fit (0 if entire stack was collected).
     */
    public int addItem(byte blockType, int count) {
        if (blockType == 0 || count <= 0) return 0;
        int remaining = count;

        // 1. Fill existing matching stacks
        for (int i = 0; i < 9; i++) {
            if (hotbarBlocks[i] == blockType && hotbarCounts[i] > 0 && hotbarCounts[i] < 64) {
                int space = 64 - hotbarCounts[i];
                int toAdd = Math.min(remaining, space);
                hotbarCounts[i] += toAdd;
                remaining -= toAdd;
                if (remaining == 0) return 0;
            }
        }

        // 2. Place into first empty slots
        for (int i = 0; i < 9; i++) {
            if (hotbarBlocks[i] == 0 || hotbarCounts[i] <= 0) {
                hotbarBlocks[i] = blockType;
                int toAdd = Math.min(remaining, 64);
                hotbarCounts[i] = toAdd;
                remaining -= toAdd;
                if (remaining == 0) return 0;
            }
        }

        return remaining;
    }

    public PhysicsSystem getPhysicsSystem() {
        return physicsSystem;
    }

    public void updateTick(ChunkManager world, boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sprint, boolean sneak) {
        prevPos.set(pos);
        this.isSprinting = sprint && !sneak;
        this.isSneaking = sneak;

        // 1. Calculate Input Direction Vectors
        float yawRad = (float) Math.toRadians(yaw);
        float forwardX = (float) Math.sin(yawRad);
        float forwardZ = (float) -Math.cos(yawRad);
        float rightX = -forwardZ;
        float rightZ = forwardX;

        float moveX = 0;
        float moveZ = 0;

        if (forward) { moveX += forwardX; moveZ += forwardZ; }
        if (backward) { moveX -= forwardX; moveZ -= forwardZ; }
        if (left) { moveX -= rightX; moveZ -= rightZ; }
        if (right) { moveX += rightX; moveZ += rightZ; }

        float inputLen = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (inputLen > 0.001f) {
            moveX /= inputLen;
            moveZ /= inputLen;
        }

        // 2. Authoritative Fixed-Step Physics Simulation (P7.1, P7.4)
        PhysicsSystem.PhysicsState state = physicsSystem.updateEntity(
            world,
            pos,
            prevPos,
            velocity,
            WIDTH,
            HEIGHT,
            STEP_HEIGHT,
            EYE_HEIGHT,
            onGround,
            moveX,
            moveZ,
            forwardX,
            forwardZ,
            forward,
            jump,
            this.isSprinting,
            this.isSneaking
        );

        pos.set(state.pos());
        velocity.set(state.velocity());
        onGround = state.onGround();

        // 3. Void Containment: prevent player from falling into an infinite abyss
        if (pos.y < -10.0f) {
            pos.set(1250.0f, 66.0f, 1250.0f);
            prevPos.set(pos);
            velocity.set(0.0f, 0.0f, 0.0f);
            onGround = true;
        }
    }

    /**
     * Broad-phase voxel collision check delegated to CollisionDetector (P7.1 & P7.2).
     */
    public boolean checkBlockCollision(ChunkManager world, float px, float py, float pz) {
        return physicsSystem.getCollisionDetector().hasBlockCollisionAt(world, px, py, pz, WIDTH, HEIGHT);
    }

    public Vector3f getEyePosition(float partialTick) {
        float eyeY = isSneaking ? SNEAK_EYE_HEIGHT : EYE_HEIGHT;
        float rx = prevPos.x + (pos.x - prevPos.x) * partialTick;
        float ry = prevPos.y + (pos.y - prevPos.y) * partialTick + eyeY;
        float rz = prevPos.z + (pos.z - prevPos.z) * partialTick;
        return new Vector3f(rx, ry, rz);
    }
}
