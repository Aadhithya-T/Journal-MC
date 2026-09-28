package com.mcjournal;

/**
 * Centralized engine constants to eliminate magic numbers across the codebase (P13.1).
 */
public final class EngineConstants {

    private EngineConstants() {}

    // Chunk Geometry & Spatial Dimensions
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int CHUNK_TOTAL_VOXELS = CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT; // 65,536

    // Region Persistence Dimensions
    public static final int REGION_SIZE_CHUNKS = 32; // 32x32 chunks per region
    public static final int REGION_TOTAL_CHUNKS = REGION_SIZE_CHUNKS * REGION_SIZE_CHUNKS; // 1,024

    // Meshing & Geometry
    public static final int VERTICES_PER_QUAD = 4;
    public static final int INDICES_PER_QUAD = 6;
    public static final int BLOCK_FACES_COUNT = 6;
    public static final int FLOATS_PER_VERTEX = 13; // pos(3), uv(4), color/ao(3), normal(3)
    public static final int BYTES_PER_VERTEX = FLOATS_PER_VERTEX * Float.BYTES; // 52 bytes

    // Time & Fixed-Step Simulation Timing
    public static final int TICKS_PER_SECOND = 20;
    public static final double TICK_DURATION_SECONDS = 1.0 / TICKS_PER_SECOND; // 0.050s = 50ms
    public static final double WORLD_DAY_TICKS = 24000.0; // Minecraft 24,000 tick solar cycle

    // Inventory & Player Attributes
    public static final int HOTBAR_SLOTS = 9;
    public static final int MAX_STACK_SIZE = 64;
    public static final float PLAYER_WIDTH = 0.6f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float PLAYER_EYE_HEIGHT = 1.62f;
    public static final float PLAYER_SNEAK_EYE_HEIGHT = 1.40f;
    public static final float PLAYER_STEP_HEIGHT = 0.60f;

    // Physics & Kinematics
    public static final float GRAVITY = 0.08f;
    public static final float DRAG_Y = 0.98f;
    public static final float JUMP_IMPULSE = 0.42f;
    public static final float FALL_DAMAGE_THRESHOLD = 3.5f;

    // Fluid Dynamics
    public static final int MAX_FLUID_LEVEL = 7; // Level 0 = source, 1..7 = flowing decay

    // World & Rendering Defaults
    public static final int DEFAULT_RENDER_DISTANCE = 12; // 12 chunks = 192 blocks radius
    public static final int UNLOAD_PADDING_CHUNKS = 3;     // 15 chunks radius for unloading
}
