package com.mcjournal.physics;

import com.mcjournal.ChunkManager;
import org.joml.Vector3f;

/**
 * Authoritative 20-TPS Physics simulation engine (P7.1 & P7.4).
 * Coordinates input acceleration, gravity, fluid drag, buoyancy, and collision resolution.
 */
public class PhysicsSystem {
    public static final float GRAVITY = 0.08f;
    public static final float DRAG_Y = 0.98f;
    public static final float JUMP_IMPULSE = 0.42f;

    private final CollisionDetector collisionDetector = new CollisionDetector();

    public CollisionDetector getCollisionDetector() {
        return collisionDetector;
    }

    public record PhysicsState(
        Vector3f pos,
        Vector3f prevPos,
        Vector3f velocity,
        boolean onGround,
        boolean inWater,
        float fallDistance,
        float highestY,
        int fallDamage
    ) {}

    /**
     * Executes one fixed-step simulation tick (0.05s = 20 TPS).
     */
    public PhysicsState updateEntity(
        ChunkManager world,
        Vector3f curPos,
        Vector3f prevPos,
        Vector3f curVel,
        float width,
        float height,
        float stepHeight,
        float eyeHeight,
        boolean onGround,
        float fallDistance,
        float highestY,
        float moveX,
        float moveZ,
        float forwardX,
        float forwardZ,
        boolean forward,
        boolean jump,
        boolean sprint,
        boolean sneak
    ) {
        Vector3f vel = new Vector3f(curVel);
        Vector3f pos = new Vector3f(curPos);

        // 1. Water Contact Detection (Full Body: Feet, Mid, Head)
        boolean inWater = world != null && (
            world.getBlockStateAt((int) Math.floor(pos.x), (int) Math.floor(pos.y + 0.1f), (int) Math.floor(pos.z)).isWater()
            || world.getBlockStateAt((int) Math.floor(pos.x), (int) Math.floor(pos.y + 0.8f), (int) Math.floor(pos.z)).isWater()
            || world.getBlockStateAt((int) Math.floor(pos.x), (int) Math.floor(pos.y + eyeHeight), (int) Math.floor(pos.z)).isWater()
        );

        float friction;
        float curFallDistance = fallDistance;
        float curHighestY = highestY;

        if (inWater) {
            curFallDistance = 0; // Water negates fall damage
            curHighestY = pos.y;
            friction = 0.80f; // Water fluid drag

            float baseSpeed = sprint ? 0.035f : (sneak ? 0.015f : 0.024f);
            vel.x += moveX * baseSpeed;
            vel.z += moveZ * baseSpeed;

            if (jump) {
                if (onGround) {
                    vel.y = Math.min(vel.y + 0.08f, 0.22f);
                    onGround = false;
                } else {
                    vel.y = Math.min(vel.y + 0.05f, 0.15f);
                }
            } else if (sneak) {
                vel.y = Math.max(vel.y - 0.03f, -0.20f);
            } else {
                if (vel.y < -0.06f) {
                    vel.y = (vel.y - 0.01f) * 0.80f;
                } else if (vel.y > 0.02f) {
                    vel.y *= 0.80f;
                } else {
                    vel.y = Math.max(-0.04f, (vel.y - 0.005f) * 0.80f);
                }
            }
        } else {
            float slipperiness = onGround ? 0.6f : 1.0f;
            friction = slipperiness * 0.91f;

            float baseSpeed = sprint ? 0.14f : (sneak ? 0.035f : 0.10f);
            if (!onGround) baseSpeed *= 0.35f; // Air control

            vel.x += moveX * baseSpeed;
            vel.z += moveZ * baseSpeed;

            // Jump Impulse (Vanilla 0.42 height impulse)
            if (jump && onGround) {
                vel.y = JUMP_IMPULSE;
                if (sprint) {
                    vel.x += forwardX * 0.20f;
                    vel.z += forwardZ * 0.20f;
                } else if (forward) {
                    vel.x += forwardX * 0.10f;
                    vel.z += forwardZ * 0.10f;
                }
                onGround = false;
            }

            // Gravity & Vertical Drag
            vel.y = (vel.y - GRAVITY) * DRAG_Y;
        }

        // 2. Resolve Movement against Voxel Collision
        CollisionDetector.MovementResult moveResult = collisionDetector.resolveMovement(
            world, pos, vel, width, height, stepHeight, onGround, inWater
        );

        pos.set(moveResult.resolvedPos());
        vel.set(moveResult.resolvedVel());
        boolean newOnGround = moveResult.onGround();

        // 3. Apply Horizontal Drag / Fluid Resistance
        vel.x *= friction;
        vel.z *= friction;

        // 4. Fall Damage Calculation
        int fallDamage = 0;
        if (newOnGround) {
            if (curFallDistance > 3.5f) {
                fallDamage = (int) Math.floor(curFallDistance - 3.5f);
            }
            curFallDistance = 0;
            curHighestY = pos.y;
        } else {
            if (pos.y < curHighestY) {
                curFallDistance = curHighestY - pos.y;
            } else {
                curHighestY = pos.y;
            }
        }

        return new PhysicsState(
            pos, prevPos, vel, newOnGround, inWater, curFallDistance, curHighestY, fallDamage
        );
    }
}
