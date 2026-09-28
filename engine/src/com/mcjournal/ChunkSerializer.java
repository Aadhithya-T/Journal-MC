package com.mcjournal;

import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;

import java.io.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Compact binary serializer for Chunk voxel data.
 * Version 1: Raw 128 KB block state arrays compressed with Deflate.
 * Version 2: Paletted chunk encoding (palette table + packed byte/short indices compressed with Deflate).
 */
public class ChunkSerializer {
    public static final byte FORMAT_VERSION_V1 = 1;
    public static final byte FORMAT_VERSION_V2 = 2;
    public static final byte FORMAT_VERSION_V3 = 3;
    public static final byte CURRENT_FORMAT_VERSION = FORMAT_VERSION_V3;

    public static byte[] serialize(Chunk chunk) throws IOException {
        return serialize(chunk, CURRENT_FORMAT_VERSION);
    }

    public static byte[] serialize(Chunk chunk, byte version) throws IOException {
        if (version == FORMAT_VERSION_V1) {
            return serializeV1(chunk);
        } else if (version == FORMAT_VERSION_V2 || version == FORMAT_VERSION_V3) {
            return serializePaletted(chunk, version);
        } else {
            throw new IllegalArgumentException("Unsupported chunk serialization version: " + version);
        }
    }

    private static byte[] serializeV1(Chunk chunk) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(FORMAT_VERSION_V1);
        dos.writeInt(chunk.getCx());
        dos.writeInt(chunk.getCz());

        byte[] compressedData;
        ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream(4096);
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(chunkBuffer);
             DataOutputStream chunkDos = new DataOutputStream(deflater)) {
            short[] states = chunk.getStateIds();
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

    private static byte[] serializePaletted(Chunk chunk, byte version) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(version);
        dos.writeInt(chunk.getCx());
        dos.writeInt(chunk.getCz());

        short[] globalStates = chunk.getStateIds();

        // 1. Build local palette of unique BlockStates in this chunk
        Map<Integer, Integer> globalToLocal = new HashMap<>();
        List<BlockState> palette = new ArrayList<>();

        for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
            int gId = globalStates[i] & 0xFFFF;
            if (!globalToLocal.containsKey(gId)) {
                globalToLocal.put(gId, palette.size());
                palette.add(BlockStateRegistry.getStateById(gId));
            }
        }

        int paletteSize = palette.size();
        dos.writeShort(paletteSize);
        for (BlockState state : palette) {
            dos.writeUTF(state.getSerializedName());
        }

        // 2. Compress local palette indices (1 byte per voxel if <= 256 entries, else 2 bytes)
        byte[] compressedData;
        ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream(4096);
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(chunkBuffer);
             DataOutputStream chunkDos = new DataOutputStream(deflater)) {
            boolean useByteIndices = (paletteSize <= 256);
            chunkDos.writeBoolean(useByteIndices);

            if (useByteIndices) {
                for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
                    int gId = globalStates[i] & 0xFFFF;
                    int localIdx = globalToLocal.get(gId);
                    chunkDos.writeByte(localIdx);
                }
            } else {
                for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
                    int gId = globalStates[i] & 0xFFFF;
                    int localIdx = globalToLocal.get(gId);
                    chunkDos.writeShort(localIdx);
                }
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

        if (version == FORMAT_VERSION_V1) {
            return deserializeV1(dis);
        } else if (version == FORMAT_VERSION_V2 || version == FORMAT_VERSION_V3) {
            return deserializeV2(dis);
        } else {
            throw new IOException("Unsupported chunk format version: " + version);
        }
    }

    private static Chunk deserializeV1(DataInputStream dis) throws IOException {
        int cx = dis.readInt();
        int cz = dis.readInt();
        int compressedLen = dis.readInt();

        byte[] compressedData = new byte[compressedLen];
        dis.readFully(compressedData);

        short[] stateIds = new short[Chunk.TOTAL_VOXELS];
        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressedData));
             DataInputStream chunkDis = new DataInputStream(inflater)) {
            for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
                stateIds[i] = chunkDis.readShort();
            }
        }

        return new Chunk(cx, cz, stateIds);
    }

    private static Chunk deserializeV2(DataInputStream dis) throws IOException {
        int cx = dis.readInt();
        int cz = dis.readInt();

        int paletteSize = dis.readUnsignedShort();
        short[] localToGlobal = new short[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            String stateName = dis.readUTF();
            BlockState state = BlockStateRegistry.parse(stateName);
            localToGlobal[i] = (short) state.getStateId();
        }

        int compressedLen = dis.readInt();
        byte[] compressedData = new byte[compressedLen];
        dis.readFully(compressedData);

        short[] stateIds = new short[Chunk.TOTAL_VOXELS];
        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressedData));
             DataInputStream chunkDis = new DataInputStream(inflater)) {
            boolean useByteIndices = chunkDis.readBoolean();
            if (useByteIndices) {
                for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
                    int localIdx = chunkDis.readUnsignedByte();
                    stateIds[i] = localToGlobal[localIdx];
                }
            } else {
                for (int i = 0; i < Chunk.TOTAL_VOXELS; i++) {
                    int localIdx = chunkDis.readUnsignedShort();
                    stateIds[i] = localToGlobal[localIdx];
                }
            }
        }

        return new Chunk(cx, cz, stateIds);
    }
}
