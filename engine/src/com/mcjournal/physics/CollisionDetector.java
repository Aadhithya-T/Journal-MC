package com.mcjournal.physics;

import com.mcjournal.ChunkManager;
import com.mcjournal.block.BlockState;
import org.joml.Vector3f;

/**
 * Dedicated collision detection and movement resolution subsystem (P7.1 & P7.2).
 * Implements bounding box broad-phase voxel extraction and multi-axis movement resolution
 * with auto step-up over obstacles.
 */
public class CollisionDetector {

    public record MovementResult(
        Vector3f resolvedPos,
        Vector3f resolvedVel,
        boolean onGround,
        boolean collidedX,
        boolean collidedY,
        boolean collidedZ
    ) {}

    /**
     * Broad-phase voxel extraction and solid block collision query.
     */
    public boolean hasBlockCollision(ChunkManager world, AABB box) {
        if (world == null || box == null) return false;

        int minX = (int) Math.floor(box.minX + 0.001f);
        int maxX = (int) Math.floor(box.maxX - 0.001f);
        int minY = (int) Math.floor(box.minY + 0.001f);
        int maxY = (int) Math.floor(box.maxY - 0.001f);
        int minZ = (int) Math.floor(box.minZ + 0.001f);
        int maxZ = (int) Math.floor(box.maxZ - 0.001f);

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockState block = world.getBlockStateAt(x, y, z);
                    if (block.isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasBlockCollisionAt(ChunkManager world, float px, float py, float pz, float width, float height) {
        float halfW = (width / 2.0f) - 0.04f; // Inner tolerance margin for smooth corridor traversal
        AABB box = new AABB(px - halfW, py + 0.01f, pz - halfW, px + halfW, py + height - 0.05f, pz + halfW);
        return hasBlockCollision(world, box);
    }

    /**
     * Resolves continuous movement against voxel obstacles.
     * Evaluates vertical axis first (to resolve steps/slopes/landings) followed by horizontal axes with step-up.
     */
    public MovementResult resolveMovement(
        ChunkManager world,
        Vector3f pos,
        Vector3f vel,
        float width,
        float height,
        float stepHeight,
        boolean onGround,
        boolean inWater
    ) {
        Vector3f resPos = new Vector3f(pos);
        Vector3f resVel = new Vector3f(vel);

        boolean ground = onGround;
        boolean colX = false;
        boolean colY = false;
        boolean colZ = false;

        // Step 1: Move Y (Vertical) FIRST
        if (resVel.y != 0) {
            float targetY = resPos.y + resVel.y;
            if (resVel.y < 0) {
                // Falling down
                if (!hasBlockCollisionAt(world, resPos.x, targetY, resPos.z, width, height)) {
                    resPos.y = targetY;
                    ground = false;
                } else {
                    // Landed on block floor
                    resPos.y = (float) Math.floor(targetY) + 1.0f;
                    resVel.y = 0;
                    ground = true;
                    colY = true;
                }
            } else {
                // Jumping / Rising (Ceiling collision check)
                if (!hasBlockCollisionAt(world, resPos.x, targetY, resPos.z, width, height)) {
                    resPos.y = targetY;
                    ground = false;
                } else {
                    resVel.y = 0;
                    colY = true;
                }
            }
        }

        // Step 2: Move X (Horizontal with 0.6-block step-up)
        if (resVel.x != 0) {
            float targetX = resPos.x + resVel.x;
            if (!hasBlockCollisionAt(world, targetX, resPos.y, resPos.z, width, height)) {
                resPos.x = targetX;
            } else {
                // Auto Step-Up 0.6-block ledge
                if (!hasBlockCollisionAt(world, targetX, resPos.y + stepHeight, resPos.z, width, height)) {
                    resPos.x = targetX;
                    if (ground || inWater) resPos.y += stepHeight;
                } else if ((!ground || inWater) && (resVel.y > 0 || inWater)
                    && !hasBlockCollisionAt(world, targetX, resPos.y + 1.05f, resPos.z, width, height)) {
                    // Mid-jump or swimming out of water: climb onto 1-block ledge
                    resPos.x = targetX;
                    resPos.y += stepHeight;
                } else {
                    resVel.x = 0;
                    colX = true;
                }
            }
        }

        // Step 3: Move Z (Horizontal with 0.6-block step-up)
        if (resVel.z != 0) {
            float targetZ = resPos.z + resVel.z;
            if (!hasBlockCollisionAt(world, resPos.x, resPos.y, targetZ, width, height)) {
                resPos.z = targetZ;
            } else {
                // Auto Step-Up 0.6-block ledge
                if (!hasBlockCollisionAt(world, resPos.x, resPos.y + stepHeight, targetZ, width, height)) {
                    resPos.z = targetZ;
                    if (ground || inWater) resPos.y += stepHeight;
                } else if ((!ground || inWater) && (resVel.y > 0 || inWater)
                    && !hasBlockCollisionAt(world, resPos.x, resPos.y + 1.05f, targetZ, width, height)) {
                    // Mid-jump or swimming out of water: climb onto 1-block ledge
                    resPos.z = targetZ;
                    resPos.y += stepHeight;
                } else {
                    resVel.z = 0;
                    colZ = true;
                }
            }
        }

        return new MovementResult(resPos, resVel, ground, colX, colY, colZ);
    }
}
