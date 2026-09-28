package com.mcjournal.test;

import com.mcjournal.Chunk;
import com.mcjournal.ChunkSerializer;
import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.property.Axis;

import java.io.IOException;

public class ChunkSerializerRoundTripTest {
    public static void main(String[] args) {
        System.out.println("=== Running ChunkSerializer Round-Trip Tests ===");

        testEmptyChunk();
        testComplexBlockStates();
        testBoundaryPositions();
        testCorruptedDataHandling();
        testVersion1BackwardCompatibility();

        System.out.println("\n🎉 ALL CHUNK SERIALIZER TESTS PASSED!");
    }

    private static void testEmptyChunk() {
        System.out.print("Testing empty/air chunk round-trip...");
        try {
            Chunk emptyChunk = new Chunk(10, -20);
            byte[] bytes = ChunkSerializer.serialize(emptyChunk);
            assert bytes != null && bytes.length > 0 : "Empty chunk serialization should produce bytes";

            Chunk deserialized = ChunkSerializer.deserialize(bytes);
            assert deserialized.getCx() == 10 && deserialized.getCz() == -20 : "Chunk coordinates mismatch";

            for (int y = 0; y < Chunk.HEIGHT; y += 32) {
                for (int z = 0; z < Chunk.SIZE; z += 4) {
                    for (int x = 0; x < Chunk.SIZE; x += 4) {
                        assert deserialized.getBlockState(x, y, z).isAir() : "Expected AIR at (" + x + "," + y + "," + z + ")";
                    }
                }
            }
            System.out.println(" PASSED (" + bytes.length + " bytes)");
        } catch (IOException e) {
            throw new RuntimeException("Empty chunk test failed", e);
        }
    }

    private static void testComplexBlockStates() {
        System.out.print("Testing chunk with parameterized BlockStates...");
        try {
            Chunk chunk = new Chunk(-3, 7);

            BlockState logX = Blocks.OAK_LOG.getDefaultState().with(BlockProperties.AXIS, Axis.X);
            BlockState logZ = Blocks.OAK_LOG.getDefaultState().with(BlockProperties.AXIS, Axis.Z);
            BlockState water3 = Blocks.WATER.getDefaultState().with(BlockProperties.LEVEL, 3);
            BlockState snowyGrass = Blocks.GRASS.getDefaultState().with(BlockProperties.SNOWY, true);
            BlockState birchLog = Blocks.BIRCH_LOG.getDefaultState().with(BlockProperties.AXIS, Axis.Z);
            BlockState poppy = Blocks.POPPY.getDefaultState();

            chunk.setBlockState(0, 0, 0, Blocks.BEDROCK.getDefaultState());
            chunk.setBlockState(5, 64, 5, logX);
            chunk.setBlockState(6, 64, 5, logZ);
            chunk.setBlockState(7, 60, 7, water3);
            chunk.setBlockState(8, 70, 8, snowyGrass);
            chunk.setBlockState(2, 30, 2, birchLog);
            chunk.setBlockState(14, 10, 14, poppy);

            byte[] serialized = ChunkSerializer.serialize(chunk);
            Chunk deserialized = ChunkSerializer.deserialize(serialized);

            assert deserialized.getCx() == -3 && deserialized.getCz() == 7 : "Coordinates mismatch";
            assert deserialized.getBlockState(0, 0, 0).is(Blocks.BEDROCK) : "Expected Bedrock at (0,0,0)";
            assert deserialized.getBlockState(5, 64, 5).equals(logX) : "Expected oak_log[axis=x] at (5,64,5)";
            assert deserialized.getBlockState(5, 64, 5).get(BlockProperties.AXIS) == Axis.X : "Axis property not preserved";
            assert deserialized.getBlockState(6, 64, 5).get(BlockProperties.AXIS) == Axis.Z : "Axis property not preserved";
            assert deserialized.getBlockState(7, 60, 7).equals(water3) : "Expected water[level=3]";
            assert deserialized.getBlockState(7, 60, 7).get(BlockProperties.LEVEL) == 3 : "Water level property not preserved";
            assert deserialized.getBlockState(8, 70, 8).equals(snowyGrass) : "Expected grass[snowy=true]";
            assert deserialized.getBlockState(8, 70, 8).get(BlockProperties.SNOWY) : "Snowy property not preserved";
            assert deserialized.getBlockState(2, 30, 2).is(Blocks.BIRCH_LOG) : "Expected birch log";
            assert deserialized.getBlockState(14, 10, 14).is(Blocks.POPPY) : "Expected poppy";

            System.out.println(" PASSED (Serialized size: " + serialized.length + " bytes)");
        } catch (IOException e) {
            throw new RuntimeException("Complex states test failed", e);
        }
    }

    private static void testBoundaryPositions() {
        System.out.print("Testing chunk boundary positions (corners & height limits)...");
        try {
            Chunk chunk = new Chunk(0, 0);
            chunk.setBlockState(0, 0, 0, Blocks.BEDROCK.getDefaultState());
            chunk.setBlockState(15, 0, 0, Blocks.STONE.getDefaultState());
            chunk.setBlockState(0, 0, 15, Blocks.DIRT.getDefaultState());
            chunk.setBlockState(15, 0, 15, Blocks.SAND.getDefaultState());

            chunk.setBlockState(0, 255, 0, Blocks.OAK_LEAVES.getDefaultState());
            chunk.setBlockState(15, 255, 0, Blocks.COBBLESTONE.getDefaultState());
            chunk.setBlockState(0, 255, 15, Blocks.BIRCH_LEAVES.getDefaultState());
            chunk.setBlockState(15, 255, 15, Blocks.DIAMOND_ORE.getDefaultState());

            byte[] serialized = ChunkSerializer.serialize(chunk);
            Chunk deserialized = ChunkSerializer.deserialize(serialized);

            assert deserialized.getBlockState(0, 0, 0).is(Blocks.BEDROCK);
            assert deserialized.getBlockState(15, 0, 0).is(Blocks.STONE);
            assert deserialized.getBlockState(0, 0, 15).is(Blocks.DIRT);
            assert deserialized.getBlockState(15, 0, 15).is(Blocks.SAND);

            assert deserialized.getBlockState(0, 255, 0).is(Blocks.OAK_LEAVES);
            assert deserialized.getBlockState(15, 255, 0).is(Blocks.COBBLESTONE);
            assert deserialized.getBlockState(0, 255, 15).is(Blocks.BIRCH_LEAVES);
            assert deserialized.getBlockState(15, 255, 15).is(Blocks.DIAMOND_ORE);

            System.out.println(" PASSED");
        } catch (IOException e) {
            throw new RuntimeException("Boundary test failed", e);
        }
    }

    private static void testCorruptedDataHandling() {
        System.out.print("Testing corrupted data error handling...");
        try {
            ChunkSerializer.deserialize(null);
            assert false : "Expected IOException for null payload";
        } catch (IOException ignored) {}

        try {
            ChunkSerializer.deserialize(new byte[]{1, 2, 3});
            assert false : "Expected IOException for truncated payload";
        } catch (IOException ignored) {}

        try {
            // Invalid version byte (e.g. version 99)
            byte[] invalidVersion = new byte[20];
            invalidVersion[0] = 99;
            ChunkSerializer.deserialize(invalidVersion);
            assert false : "Expected IOException for invalid version";
        } catch (IOException ignored) {}

        System.out.println(" PASSED");
    }

    private static void testVersion1BackwardCompatibility() {
        System.out.print("Testing Version 1 format backward compatibility...");
        try {
            Chunk chunk = new Chunk(42, -99);
            chunk.setBlockState(3, 64, 3, Blocks.DIAMOND_ORE.getDefaultState());
            chunk.setBlockState(4, 64, 4, Blocks.WATER.getDefaultState());
            chunk.setBlockState(5, 64, 5, Blocks.OAK_LOG.getDefaultState().with(BlockProperties.AXIS, Axis.Z));

            // Force serialize with Version 1
            byte[] v1Bytes = ChunkSerializer.serialize(chunk, ChunkSerializer.FORMAT_VERSION_V1);
            assert v1Bytes[0] == 1 : "Expected format version 1 in header";

            // Deserialize with standard deserialize method
            Chunk deserialized = ChunkSerializer.deserialize(v1Bytes);
            assert deserialized.getCx() == 42 && deserialized.getCz() == -99 : "Coordinates mismatch";
            assert deserialized.getBlockState(3, 64, 3).is(Blocks.DIAMOND_ORE) : "Expected DIAMOND_ORE";
            assert deserialized.getBlockState(4, 64, 4).is(Blocks.WATER) : "Expected WATER";
            assert deserialized.getBlockState(5, 64, 5).get(BlockProperties.AXIS) == Axis.Z : "Expected axis=Z";

            // Verify Version 2 and Version 3 (current) paletted serialization
            byte[] v2Bytes = ChunkSerializer.serialize(chunk, ChunkSerializer.FORMAT_VERSION_V2);
            assert v2Bytes[0] == 2 : "Expected format version 2 in header";
            Chunk v2Deserialized = ChunkSerializer.deserialize(v2Bytes);
            assert v2Deserialized.getBlockState(3, 64, 3).is(Blocks.DIAMOND_ORE);
            assert v2Deserialized.getBlockState(5, 64, 5).get(BlockProperties.AXIS) == Axis.Z;

            byte[] currentBytes = ChunkSerializer.serialize(chunk);
            assert currentBytes[0] == ChunkSerializer.CURRENT_FORMAT_VERSION : "Expected current format version in header";
            Chunk currentDeserialized = ChunkSerializer.deserialize(currentBytes);
            assert currentDeserialized.getBlockState(3, 64, 3).is(Blocks.DIAMOND_ORE);

            System.out.println(" PASSED (V1: " + v1Bytes.length + " bytes, V2/V3 Paletted: " + currentBytes.length + " bytes)");
        } catch (IOException e) {
            throw new RuntimeException("V1 backward compatibility test failed", e);
        }
    }
}
