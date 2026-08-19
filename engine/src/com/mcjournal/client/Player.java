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
    public static final float STEP_HEIGHT = 0.6f; // Vanilla 0.6 block step-up

    // Movement constants
    public static final float GRAVITY = 0.08f;       // blocks per tick^2
    public static final float DRAG_Y = 0.98f;        // vertical air drag
    public static final float JUMP_IMPULSE = 0.42f;  // vanilla jump impulse (~1.25 block height)

    public final Vector3f pos = new Vector3f(8.0f, 16.0f, 8.0f);
    public final Vector3f prevPos = new Vector3f(8.0f, 16.0f, 8.0f);
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
    public float highestY = 16.0f;
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

        float baseSpeed = isSprinting ? 0.14f : (isSneaking ? 0.035f : 0.10f);
        if (!onGround) baseSpeed *= 0.35f; // Air control

        velocity.x += moveX * baseSpeed;
        velocity.z += moveZ * baseSpeed;

        // 2. Jump Impulse (Vanilla 0.42 height impulse)
        if (jump && onGround) {
            velocity.y = JUMP_IMPULSE;
            if (isSprinting) {
                velocity.x += forwardX * 0.20f;
                velocity.z += forwardZ * 0.20f;
            } else if (forward) {
                velocity.x += forwardX * 0.10f;
                velocity.z += forwardZ * 0.10f;
            }
            onGround = false;
        }

        // 2.5 Water Physics & Buoyancy (Proportional Entry Plunge, Fluid Drag & Sprint-Jump Water Exit)
        boolean inWater = world.getBlockAt((int) Math.floor(pos.x), (int) Math.floor(pos.y + 0.35f), (int) Math.floor(pos.z)) == Block.WATER
                       || world.getBlockAt((int) Math.floor(pos.x), (int) Math.floor(pos.y + EYE_HEIGHT), (int) Math.floor(pos.z)) == Block.WATER;
        if (inWater) {
            fallDistance = 0; // Water completely breaks fall damage
            highestY = pos.y;
            friction = 0.82f;

            if (jump) {
                if (isSprinting) {
                    // Sprint-jump dolphin leap / water exit boost (breaches water surface to jump out onto land)
                    velocity.y = Math.min(velocity.y + 0.10f, 0.38f);
                    velocity.x += forwardX * 0.08f;
                    velocity.z += forwardZ * 0.08f;
                } else {
                    // Standard upward swimming
                    velocity.y = Math.min(velocity.y + 0.05f, 0.20f);
                }
            } else if (sneak) {
                // Dive downwards faster
                velocity.y = Math.max(velocity.y - 0.03f, -0.25f);
            } else {
                // Natural fluid drag & momentum deceleration
                // High downward entry velocity decelerates smoothly through the water column in proportion to fall height
                if (velocity.y < -0.08f) {
                    velocity.y = (velocity.y - 0.015f) * 0.82f;
                } else if (velocity.y > 0.02f) {
                    velocity.y *= 0.82f;
                } else {
                    // Gentle terminal buoyancy sinking
                    velocity.y = Math.max(-0.05f, (velocity.y - 0.005f) * 0.85f);
                }
            }
        } else {
            // 3. Gravity & Vertical Drag (Air/Ground)
            velocity.y = (velocity.y - GRAVITY) * DRAG_Y;
        }

        // 4. Move & Collide with Voxel Terrain (Y first, then X and Z with continuous step-up)
        moveWithCollision(world, velocity.x, velocity.y, velocity.z, inWater);

        // Apply Horizontal Friction
        velocity.x *= friction;
        velocity.z *= friction;

        // 5. Fall Damage Calculation (Vanilla Hardcore formula)
        if (onGround) {
            if (fallDistance > 3.5f) {
                int damage = (int) Math.floor(fallDistance - 3.5f);
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

    private void moveWithCollision(ChunkManager world, float dx, float dy, float dz, boolean inWater) {
        // Step 1: Move Y (Vertical) FIRST so jump lifts player before checking horizontal walls
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
            // Jumping / Rising up (Check ceiling collision at head level)
            if (!checkBlockCollision(world, pos.x, targetY, pos.z)) {
                pos.y = targetY;
                onGround = false;
            } else {
                velocity.y = 0;
            }
        }

        // Step 2: Move X (Horizontal with 0.6-block step-up and jump-clearing)
        if (dx != 0) {
            float targetX = pos.x + dx;
            if (!checkBlockCollision(world, targetX, pos.y, pos.z)) {
                pos.x = targetX;
            } else {
                // Auto Step-Up 0.6-block ledge
                if (!checkBlockCollision(world, targetX, pos.y + STEP_HEIGHT, pos.z)) {
                    pos.x = targetX;
                    if (onGround || inWater) pos.y += STEP_HEIGHT;
                } else if ((!onGround || inWater) && (velocity.y > 0 || inWater) && !checkBlockCollision(world, targetX, pos.y + 1.05f, pos.z)) {
                    // Mid-jump or swimming out of water: allow forward traversal and lift onto 1-block ledge
                    pos.x = targetX;
                    pos.y += STEP_HEIGHT;
                } else {
                    velocity.x = 0;
                }
            }
        }

        // Step 3: Move Z (Horizontal with 0.6-block step-up and jump-clearing)
        if (dz != 0) {
            float targetZ = pos.z + dz;
            if (!checkBlockCollision(world, pos.x, pos.y, targetZ)) {
                pos.z = targetZ;
            } else {
                // Auto Step-Up 0.6-block ledge
                if (!checkBlockCollision(world, pos.x, pos.y + STEP_HEIGHT, targetZ)) {
                    pos.z = targetZ;
                    if (onGround || inWater) pos.y += STEP_HEIGHT;
                } else if ((!onGround || inWater) && (velocity.y > 0 || inWater) && !checkBlockCollision(world, pos.x, pos.y + 1.05f, targetZ)) {
                    // Mid-jump or swimming out of water: allow forward traversal and lift onto 1-block ledge
                    pos.z = targetZ;
                    pos.y += STEP_HEIGHT;
                } else {
                    velocity.z = 0;
                }
            }
        }
    }

    private boolean checkBlockCollision(ChunkManager world, float px, float py, float pz) {
        float halfW = (WIDTH / 2.0f) - 0.04f; // 0.26 half-width for smooth traversal
        int minX = (int) Math.floor(px - halfW + 0.001f);
        int maxX = (int) Math.floor(px + halfW - 0.001f);
        int minY = (int) Math.floor(py + 0.01f);
        int maxY = (int) Math.floor(py + HEIGHT - 0.05f);
        int minZ = (int) Math.floor(pz - halfW + 0.001f);
        int maxZ = (int) Math.floor(pz + halfW - 0.001f);

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
