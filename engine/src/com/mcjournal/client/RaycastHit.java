package com.mcjournal.client;

import com.mcjournal.ChunkPos;
import com.mcjournal.WorldBlockPos;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockType;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.property.Direction;
import org.joml.Vector3f;

/**
 * Structured representation of a raycast intersection against the voxel world (P8.2).
 */
public class RaycastHit {
    private final ChunkPos chunk;
    private final WorldBlockPos block;
    private final Direction face;
    private final Vector3f hitPosition;
    public final float distance;
    public final BlockState state;

    // Backward-compatibility public fields
    public final int bx, by, bz;
    public final int normalX, normalY, normalZ;
    public final byte blockType;

    public RaycastHit(
        ChunkPos chunk,
        WorldBlockPos block,
        Direction face,
        Vector3f hitPosition,
        float distance,
        BlockState state
    ) {
        this.chunk = (chunk != null) ? chunk : new ChunkPos(Math.floorDiv(block.x(), 16), Math.floorDiv(block.z(), 16));
        this.block = block;
        this.face = (face != null) ? face : Direction.UP;
        this.hitPosition = (hitPosition != null) ? new Vector3f(hitPosition) : new Vector3f(block.x(), block.y(), block.z());
        this.distance = distance;
        this.state = (state != null) ? state : Blocks.AIR.getDefaultState();

        this.bx = block.x();
        this.by = block.y();
        this.bz = block.z();
        this.normalX = this.face.getOffsetX();
        this.normalY = this.face.getOffsetY();
        this.normalZ = this.face.getOffsetZ();
        this.blockType = this.state.getLegacyId();
    }

    public RaycastHit(int bx, int by, int bz, int normalX, int normalY, int normalZ, BlockState state, float distance) {
        this(
            new ChunkPos(Math.floorDiv(bx, 16), Math.floorDiv(bz, 16)),
            new WorldBlockPos(bx, by, bz),
            Direction.fromNormal(normalX, normalY, normalZ),
            new Vector3f(bx + 0.5f, by + 0.5f, bz + 0.5f),
            distance,
            state
        );
    }

    public ChunkPos getChunk() {
        return chunk;
    }

    public WorldBlockPos getBlock() {
        return block;
    }

    public WorldBlockPos getBlockPos() {
        return block;
    }

    public Direction getFace() {
        return face;
    }

    public Vector3f getHitPosition() {
        return new Vector3f(hitPosition);
    }

    public float getDistance() {
        return distance;
    }

    public BlockState getState() {
        return state;
    }

    public BlockType getBlockType() {
        return state.getBlock();
    }

    @Override
    public String toString() {
        return "RaycastHit{chunk=" + chunk +
               ", block=" + block +
               ", face=" + face +
               ", hitPos=" + String.format("(%.2f, %.2f, %.2f)", hitPosition.x, hitPosition.y, hitPosition.z) +
               ", dist=" + String.format("%.2f", distance) +
               ", state=" + state.getSerializedName() + "}";
    }
}
