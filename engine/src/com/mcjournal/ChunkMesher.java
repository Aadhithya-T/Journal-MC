package com.mcjournal;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * Handles chunk voxel mesh generation and neighbor mesh rebuilds.
 */
public class ChunkMesher {
    private final ConcurrentMap<ChunkPos, Chunk> chunks;
    private final ConcurrentMap<ChunkPos, ChunkMeshBuilder.MeshData> meshes;
    private final ConcurrentMap<ChunkPos, ChunkStatus> chunkStatuses;
    private final ChunkUploadQueue uploadQueue;

    public ChunkMesher(ConcurrentMap<ChunkPos, Chunk> chunks,
                       ConcurrentMap<ChunkPos, ChunkMeshBuilder.MeshData> meshes,
                       ConcurrentMap<ChunkPos, ChunkStatus> chunkStatuses,
                       ChunkUploadQueue uploadQueue) {
        this.chunks = chunks;
        this.meshes = meshes;
        this.chunkStatuses = chunkStatuses;
        this.uploadQueue = uploadQueue;
    }

    public void meshChunk(ChunkPos pos, ChunkManager world) {
        Chunk chunk = chunks.get(pos);
        if (chunk != null) {
            chunkStatuses.put(pos, ChunkStatus.MESHING);
            ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(chunk, world);
            meshes.put(pos, mesh);
            uploadQueue.queueUpload(pos);
            chunkStatuses.put(pos, ChunkStatus.MESHED);
        }
    }

    public void rebuildSingleMesh(ChunkPos pos, ChunkManager world) {
        Chunk chunk = chunks.get(pos);
        if (chunk != null) {
            ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(chunk, world);
            meshes.put(pos, mesh);
            uploadQueue.queueUpload(pos);
        }
    }

    public void rebuildNeighborIfLoaded(ChunkPos neighborPos, ChunkManager world, ExecutorService workers) {
        Chunk neighborChunk = chunks.get(neighborPos);
        ChunkStatus status = chunkStatuses.getOrDefault(neighborPos, ChunkStatus.UNLOADED);
        if (neighborChunk != null && status != ChunkStatus.MESHING && status != ChunkStatus.GENERATING && !status.isUnloading()) {
            if (workers != null && !workers.isShutdown()) {
                try {
                    workers.submit(() -> {
                        if (chunks.containsKey(neighborPos)) {
                            chunkStatuses.put(neighborPos, ChunkStatus.MESHING);
                            ChunkMeshBuilder.MeshData mesh = ChunkMeshBuilder.buildMesh(neighborChunk, world);
                            meshes.put(neighborPos, mesh);
                            uploadQueue.queueUpload(neighborPos);
                            chunkStatuses.put(neighborPos, ChunkStatus.MESHED);
                        }
                    });
                } catch (Exception ignored) {}
            }
        }
    }
}
