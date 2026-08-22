package com.mcjournal.client;

import com.mcjournal.Block;
import com.mcjournal.ChunkManager;
import org.joml.Vector3f;

import java.util.Random;

public class ItemEntity {
    public static final float MAX_LIFETIME = 300.0f; // 5 minutes in seconds
    public static final float PICKUP_DELAY = 0.5f;   // 0.5s initial immunity
    public static final float MAGNET_RADIUS = 2.0f;  // Proximity attraction distance
    public static final float PICKUP_RADIUS = 0.45f; // Touch pickup distance
    public static final float SIZE = 0.28f;          // Miniature voxel item scale

    private static final Random RANDOM = new Random();

    public final Vector3f pos = new Vector3f();
    public final Vector3f prevPos = new Vector3f();
    public final Vector3f velocity = new Vector3f();

    public byte blockType;
    public int count;
    public float age = 0.0f;
    public float pickupDelay = PICKUP_DELAY;
    public float rotation = 0.0f; // In degrees
    public boolean onGround = false;

    public ItemEntity(byte blockType, int count, float x, float y, float z) {
        this.blockType = blockType;
        this.count = count;
        this.pos.set(x, y, z);
        this.prevPos.set(x, y, z);

        // Initial upward and slight randomized radial pop velocity
        float vx = (RANDOM.nextFloat() - 0.5f) * 1.6f;
        float vy = 2.2f + RANDOM.nextFloat() * 1.0f;
        float vz = (RANDOM.nextFloat() - 0.5f) * 1.6f;
        this.velocity.set(vx, vy, vz);
        this.rotation = RANDOM.nextFloat() * 360.0f;
    }

    public ItemEntity(byte blockType, int count, float x, float y, float z, float vx, float vy, float vz, float pickupDelay) {
        this.blockType = blockType;
        this.count = count;
        this.pos.set(x, y, z);
        this.prevPos.set(x, y, z);
        this.velocity.set(vx, vy, vz);
        this.pickupDelay = pickupDelay;
        this.rotation = RANDOM.nextFloat() * 360.0f;
    }

    public void update(float dt, ChunkManager world, Player player) {
        prevPos.set(pos);
        age += dt;
        if (pickupDelay > 0.0f) {
            pickupDelay -= dt;
        }

        // Smooth continuous 90 deg/s rotation
        rotation = (rotation + 90.0f * dt) % 360.0f;

        // Check if item is inside water
        int bX = (int) Math.floor(pos.x);
        int bY = (int) Math.floor(pos.y);
        int bZ = (int) Math.floor(pos.z);
        boolean inWater = (world != null && world.getBlockAt(bX, bY, bZ) == Block.WATER);

        if (inWater) {
            // Gentle water buoyancy & fluid resistance
            velocity.y = Math.min(velocity.y + 3.5f * dt, 0.6f);
            velocity.x *= 0.88f;
            velocity.z *= 0.88f;
            onGround = false;
        } else {
            // Standard air gravity & drag
            velocity.y -= 13.0f * dt;
            velocity.x *= 0.96f;
            velocity.z *= 0.96f;
        }

        // Magnet attraction toward player (only if player is alive and can accept this item in inventory)
        if (player != null && !player.isDead && pickupDelay <= 0.0f && player.canAddItem(blockType)) {
            float playerCenterX = player.pos.x;
            float playerCenterY = player.pos.y + 0.8f;
            float playerCenterZ = player.pos.z;

            float dx = playerCenterX - pos.x;
            float dy = playerCenterY - pos.y;
            float dz = playerCenterZ - pos.z;
            float distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < MAGNET_RADIUS * MAGNET_RADIUS) {
                float dist = (float) Math.sqrt(distSq);
                if (dist > 0.001f) {
                    float pullSpeed = 7.5f;
                    velocity.x = (dx / dist) * pullSpeed;
                    velocity.y = (dy / dist) * pullSpeed;
                    velocity.z = (dz / dist) * pullSpeed;
                    onGround = false;
                }
            }
        }

        // Step Y physics & voxel terrain floor collision
        float nextY = pos.y + velocity.y * dt;
        if (velocity.y < 0.0f) {
            int floorX = (int) Math.floor(pos.x);
            int floorY = (int) Math.floor(nextY);
            int floorZ = (int) Math.floor(pos.z);

            if (world != null && Block.isSolid(world.getBlockAt(floorX, floorY, floorZ))) {
                pos.y = floorY + 1.0f + 0.02f;
                velocity.y = 0.0f;
                onGround = true;
            } else {
                pos.y = nextY;
                onGround = false;
            }
        } else if (velocity.y > 0.0f) {
            int ceilX = (int) Math.floor(pos.x);
            int ceilY = (int) Math.floor(nextY + SIZE);
            int ceilZ = (int) Math.floor(pos.z);

            if (world != null && Block.isSolid(world.getBlockAt(ceilX, ceilY, ceilZ))) {
                velocity.y = 0.0f;
            } else {
                pos.y = nextY;
            }
        }

        // Step X physics & voxel terrain wall collision
        float nextX = pos.x + velocity.x * dt;
        int checkX = (int) Math.floor(nextX + Math.signum(velocity.x) * (SIZE / 2.0f));
        int checkY = (int) Math.floor(pos.y + 0.05f);
        int checkZ = (int) Math.floor(pos.z);
        if (world != null && Block.isSolid(world.getBlockAt(checkX, checkY, checkZ))) {
            velocity.x = 0.0f;
        } else {
            pos.x = nextX;
        }

        // Step Z physics & voxel terrain wall collision
        float nextZ = pos.z + velocity.z * dt;
        checkX = (int) Math.floor(pos.x);
        checkZ = (int) Math.floor(nextZ + Math.signum(velocity.z) * (SIZE / 2.0f));
        if (world != null && Block.isSolid(world.getBlockAt(checkX, checkY, checkZ))) {
            velocity.z = 0.0f;
        } else {
            pos.z = nextZ;
        }

        // Ground friction
        if (onGround) {
            velocity.x *= 0.72f;
            velocity.z *= 0.72f;
        }
    }

    public float getVisualY(float partialTick) {
        float interpY = prevPos.y + (pos.y - prevPos.y) * partialTick;
        if (onGround) {
            // Authentic Minecraft hover bobbing
            interpY += (float) Math.sin(age * 3.5f) * 0.04f + 0.06f;
        }
        return interpY;
    }

    public boolean isBlinking() {
        // Last 15 seconds before 5-minute despawn: rapid warning blink
        if (age > MAX_LIFETIME - 15.0f) {
            return ((int) (age * 12.0f) % 2) == 0;
        }
        return false;
    }

    public boolean isExpired() {
        return age >= MAX_LIFETIME;
    }
}
