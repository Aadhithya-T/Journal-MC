package com.mcjournal;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Compact binary serializer for Chunk voxel data.
 * Compresses raw 128 KB block state arrays down to 1-4 KB payloads using Deflate compression.
 */
public class ChunkSerializer {
    private static final byte FORMAT_VERSION = 1;

    public static byte[] serialize(Chunk chunk) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(FORMAT_VERSION);
        dos.writeInt(chunk.getCx());
        dos.writeInt(chunk.getCz());

        // Compress short[] blockStates
        byte[] compressedData;
        ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream(4096);
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(chunkBuffer);
             DataOutputStream chunkDos = new DataOutputStream(deflater)) {
            short[] states = chunk.getBlockStates();
            for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
                chunkDos.writeShort(states[i]);
            }
            chunkDos.flush();
            deflater.finish();
        }
        compressedData = chunkBuffer.toByteArray();

        dos.writeInt(compressedData.length);
        dos.write(compressedData);
        dos.flush();

        return baos.toByteArray();
    }

    public static Chunk deserialize(byte[] data) throws IOException {
        if (data == null || data.length < 9) {
            throw new IOException("Corrupted chunk data: payload too short");
        }

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        byte version = dis.readByte();
        if (version != FORMAT_VERSION) {
            throw new IOException("Unsupported chunk format version: " + version);
        }

        int cx = dis.readInt();
        int cz = dis.readInt();
        int compressedLen = dis.readInt();

        byte[] compressedData = new byte[compressedLen];
        dis.readFully(compressedData);

        short[] blockStates = new short[Chunk.TOTAL_VOXELS];
        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressedData));
             DataInputStream chunkDis = new DataInputStream(inflater)) {
            for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
                blockStates[i] = chunkDis.readShort();
            }
        }

        return new Chunk(cx, cz, blockStates);
    }
}
