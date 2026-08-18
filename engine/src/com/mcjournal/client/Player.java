package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.ChunkManager;
import org.joml.Vector3f;

public class Player {
    // Physical dimensions (Vanilla Minecraft: 0.6 x 1.8 x 0.6)
    public static final float WIDTH = 0.6f;
    public static final float HEIGHT = 1.8f;
    public static final float EYE_HEIGHT = 1.62f;
    public static final float SNEAK_EYE_HEIGHT = 1.27f;

    // Movement constants
    public static final float GRAVITY = 0.08f;       // blocks per tick^2
    public static final float DRAG_Y = 0.98f;        // vertical air drag
    public static final float JUMP_IMPULSE = 0.42f;  // vanilla jump impulse (~1.25 block height)

    public final Vector3f pos = new Vector3f(6.0f, 12.0f, 6.0f);
    public final Vector3f prevPos = new Vector3f(6.0f, 12.0f, 6.0f);
    public final Vector3f velocity = new Vector3f(0, 0, 0);

    public float yaw = 0;   // In degrees
    public float pitch = 0; // In degrees
    public boolean onGround = false;
    public boolean isSprinting = false;
    public boolean isSneaking = false;

    // Hardcore Survival Stats
    public int health = 20; // 10 Hardcore Hearts
    public int maxHealth = 20;
    public int hunger = 20; // 10 Drumsticks
    public float fallDistance = 0;
    public float highestY = 12.0f;
    public boolean isDead = false;

    // Selected hotbar slot (0..8)
    public int selectedSlot = 0;

    public void updateTick(ChunkManager world, boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sprint, boolean sneak) {
        if (isDead) return;

        prevPos.set(pos);
        this.isSprinting = sprint && !sneak;
        this.isSneaking = sneak;

        // 1. Calculate Input Direction
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

        // Vanilla Movement Slipperiness & Acceleration
        float slipperiness = onGround ? 0.6f : 1.0f;
        float friction = slipperiness * 0.91f;

        float baseSpeed = isSprinting ? 0.13f : (isSneaking ? 0.03f : 0.098f);
        if (!onGround) baseSpeed *= 0.2f; // Air control penalty

        velocity.x += moveX * baseSpeed;
        velocity.z += moveZ * baseSpeed;

        // 2. Jump Impulse
        if (jump && onGround) {
            velocity.y = JUMP_IMPULSE;
            if (isSprinting) {
                velocity.x += forwardX * 0.15f;
                velocity.z += forwardZ * 0.15f;
            }
            onGround = false;
        }

        // 3. Gravity & Vertical Drag
        velocity.y = (velocity.y - GRAVITY) * DRAG_Y;

        // 4. Move & Collide with Voxel Terrain (X, then Z, then Y with step-up)
        moveWithCollision(world, velocity.x, velocity.y, velocity.z);

        // Apply Horizontal Friction
        velocity.x *= friction;
        velocity.z *= friction;

        // 5. Fall Damage Calculation (Vanilla Hardcore formula)
        if (onGround) {
            if (fallDistance > 3.0f) {
                int damage = (int) Math.floor(fallDistance - 3.0f);
                if (damage > 0) {
                    takeDamage(damage);
                    System.out.println("[Hardcore] Player took " + damage + " fall damage! (HP: " + health + "/20)");
                }
            }
            fallDistance = 0;
            highestY = pos.y;
        } else {
            if (pos.y < highestY) {
                fallDistance = highestY - pos.y;
            } else {
                highestY = pos.y;
            }
        }

        // Void Damage
        if (pos.y < -10) {
            takeDamage(20);
        }
    }

    public void takeDamage(int amount) {
        health -= amount;
        if (health <= 0) {
            health = 0;
            isDead = true;
            System.out.println("[Hardcore] ☠ PLAYER HAS DIED! PERMADEATH TRIGGERED.");
        }
    }

    private void moveWithCollision(ChunkManager world, float dx, float dy, float dz) {
        float halfW = WIDTH / 2.0f;

        // Step 1: Move X
        if (dx != 0) {
            float targetX = pos.x + dx;
            if (!checkBlockCollision(world, targetX, pos.y, pos.z)) {
                pos.x = targetX;
            } else {
                // Auto Step-Up 0.5-block ledge
                if (onGround && !checkBlockCollision(world, targetX, pos.y + 0.6f, pos.z)) {
                    pos.x = targetX;
                    pos.y += 0.55f;
                } else {
                    velocity.x = 0;
                }
            }
        }

        // Step 2: Move Z
        if (dz != 0) {
            float targetZ = pos.z + dz;
            if (!checkBlockCollision(world, pos.x, pos.y, targetZ)) {
                pos.z = targetZ;
            } else {
                // Auto Step-Up 0.5-block ledge
                if (onGround && !checkBlockCollision(world, pos.x, pos.y + 0.6f, targetZ)) {
                    pos.z = targetZ;
                    pos.y += 0.55f;
                } else {
                    velocity.z = 0;
                }
            }
        }

        // Step 3: Move Y (Vertical)
        float targetY = pos.y + dy;
        if (dy < 0) {
            // Falling down
            if (!checkBlockCollision(world, pos.x, targetY, pos.z)) {
                pos.y = targetY;
                onGround = false;
            } else {
                // Landed on block
                pos.y = (float) Math.floor(targetY) + 1.0f;
                velocity.y = 0;
                onGround = true;
            }
        } else if (dy > 0) {
            // Jumping / Rising up (Bonking ceiling)
            if (!checkBlockCollision(world, pos.x, targetY + HEIGHT, pos.z)) {
                pos.y = targetY;
            } else {
                velocity.y = 0;
            }
        }
    }

    private boolean checkBlockCollision(ChunkManager world, float px, float py, float pz) {
        float halfW = WIDTH / 2.0f;
        int minX = (int) Math.floor(px - halfW);
        int maxX = (int) Math.floor(px + halfW);
        int minY = (int) Math.floor(py);
        int maxY = (int) Math.floor(py + HEIGHT);
        int minZ = (int) Math.floor(pz - halfW);
        int maxZ = (int) Math.floor(pz + halfW);

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    byte block = world.getBlockAt(x, y, z);
                    if (Block.isSolid(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Vector3f getEyePosition(float partialTick) {
        float eyeY = isSneaking ? SNEAK_EYE_HEIGHT : EYE_HEIGHT;
        float rx = prevPos.x + (pos.x - prevPos.x) * partialTick;
        float ry = prevPos.y + (pos.y - prevPos.y) * partialTick + eyeY;
        float rz = prevPos.z + (pos.z - prevPos.z) * partialTick;
        return new Vector3f(rx, ry, rz);
    }
}
