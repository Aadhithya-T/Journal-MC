package com.mcjournal;

public record WorldBlockPos(int x, int y, int z) {
    public ChunkPos toChunkPos() {
        return new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    }

    public int toLocalX() {
        return Math.floorMod(x, 16);
    }

    public int toLocalZ() {
        return Math.floorMod(z, 16);
    }

    public int toChunkIndex() {
        return Chunk.getIndex(toLocalX(), y, toLocalZ());
    }

    public String toStringKey() {
        return x + "," + y + "," + z;
    }

    public static WorldBlockPos fromStringKey(String key) {
        if (key == null) return null;
        String[] parts = key.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid WorldBlockPos key: " + key);
        }
        return new WorldBlockPos(
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        );
    }
}
