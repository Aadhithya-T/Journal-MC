package com.mcjournal.test;

import com.mcjournal.block.BlockProperties;
import com.mcjournal.block.BlockState;
import com.mcjournal.block.BlockStateRegistry;
import com.mcjournal.block.BlockType;
import com.mcjournal.block.Blocks;
import com.mcjournal.block.property.Axis;
import com.mcjournal.block.property.Property;

import java.util.Collection;

public class BlockStateRoundTripTest {
    public static void main(String[] args) {
        System.out.println("=== Running BlockState Round-Trip & Registry Tests ===");

        testAllRegisteredBlocks();
        testPropertyTransitions();
        testSerializedNameRoundTrip();
        testLegacyByteMapping();
        testLegacyStringParsing();
        testImmutabilityEnforcement();

        System.out.println("\n🎉 ALL BLOCK STATE ROUND-TRIP TESTS PASSED!");
    }

    private static void testAllRegisteredBlocks() {
        System.out.print("Testing state ID resolution for all registered blocks...");
        Collection<BlockType> allBlocks = Blocks.getAll();
        assert !allBlocks.isEmpty() : "Blocks registry should not be empty";

        int totalStates = BlockStateRegistry.getTotalStates();
        assert totalStates > 0 : "Total states should be greater than 0";

        for (int i = 0; i < totalStates; i++) {
            BlockState state = BlockStateRegistry.getStateById(i);
            assert state != null : "State at id " + i + " must not be null";
            assert state.getStateId() == i : "State ID mismatch: expected " + i + " but got " + state.getStateId();
            assert state.getBlock() != null : "Owner block must not be null for state " + i;
        }
        System.out.println(" PASSED (" + totalStates + " states verified)");
    }

    private static void testPropertyTransitions() {
        System.out.print("Testing property transitions and state identity...");

        // Axis property on Oak Log
        BlockState logY = Blocks.OAK_LOG.getDefaultState();
        assert logY.get(BlockProperties.AXIS) == Axis.Y : "Default oak log axis should be Y";

        BlockState logX = logY.with(BlockProperties.AXIS, Axis.X);
        assert logX.get(BlockProperties.AXIS) == Axis.X : "Log axis should be X";
        assert logX != logY : "State with axis=X should be a distinct instance";

        BlockState logZ = logX.with(BlockProperties.AXIS, Axis.Z);
        assert logZ.get(BlockProperties.AXIS) == Axis.Z : "Log axis should be Z";

        BlockState logYAgain = logZ.with(BlockProperties.AXIS, Axis.Y);
        assert logYAgain == logY : "Returning to axis=Y should return exact cached BlockState instance";

        // Level property on Water
        BlockState water0 = Blocks.WATER.getDefaultState();
        assert water0.get(BlockProperties.LEVEL) == 0 : "Default water level should be 0";
        BlockState water3 = water0.with(BlockProperties.LEVEL, 3);
        assert water3.get(BlockProperties.LEVEL) == 3 : "Water level should be 3";
        assert water3.getStateId() != water0.getStateId() : "Water level 3 should have distinct stateId";
        assert water3.with(BlockProperties.LEVEL, 0) == water0 : "Water level 0 should return default state";

        // Snowy property on Grass
        BlockState grass = Blocks.GRASS.getDefaultState();
        assert !grass.get(BlockProperties.SNOWY) : "Default grass snowy should be false";
        BlockState snowyGrass = grass.with(BlockProperties.SNOWY, true);
        assert snowyGrass.get(BlockProperties.SNOWY) : "Snowy grass snowy should be true";
        assert snowyGrass.with(BlockProperties.SNOWY, false) == grass : "Toggling snowy back should match";

        System.out.println(" PASSED");
    }

    private static void testSerializedNameRoundTrip() {
        System.out.print("Testing getSerializedName() -> parse() round-trip...");
        int totalStates = BlockStateRegistry.getTotalStates();

        for (int i = 0; i < totalStates; i++) {
            BlockState original = BlockStateRegistry.getStateById(i);
            String serialized = original.getSerializedName();
            assert serialized != null && !serialized.isEmpty() : "Serialized name empty for state " + i;

            BlockState parsed = BlockStateRegistry.parse(serialized);
            assert parsed == original : "Round-trip mismatch for '" + serialized + "': expected id " 
                    + original.getStateId() + " (" + original + "), got id " + parsed.getStateId() + " (" + parsed + ")";
        }
        System.out.println(" PASSED (" + totalStates + " serialized states verified)");
    }

    private static void testLegacyByteMapping() {
        System.out.print("Testing legacy byte mapping...");
        for (BlockType block : Blocks.getAll()) {
            byte legacyId = block.getLegacyId();
            BlockType mappedType = BlockStateRegistry.getBlockType(legacyId);
            assert mappedType == block : "Legacy ID " + legacyId + " mapped to " + mappedType + ", expected " + block;

            BlockState defaultState = BlockStateRegistry.getDefaultState(legacyId);
            assert defaultState.getBlock() == block : "Legacy ID " + legacyId + " default state owner mismatch";
        }
        System.out.println(" PASSED");
    }

    private static void testLegacyStringParsing() {
        System.out.print("Testing legacy string parsing fallback...");
        BlockState stone = BlockStateRegistry.parse("stone");
        assert stone.is(Blocks.STONE) : "Failed to parse 'stone'";

        BlockState bedrock = BlockStateRegistry.parse("6"); // Legacy ID 6 is bedrock
        assert bedrock.is(Blocks.BEDROCK) : "Failed to parse legacy numeric string '6' to BEDROCK";

        BlockState empty = BlockStateRegistry.parse("");
        assert empty.isAir() : "Empty string should parse to AIR";

        BlockState invalid = BlockStateRegistry.parse("nonexistent_block_xyz");
        assert invalid.isAir() : "Unknown block should parse to AIR";

        System.out.println(" PASSED");
    }

    private static void testImmutabilityEnforcement() {
        System.out.print("Testing strict immutability enforcement...");
        BlockState stone = Blocks.STONE.getDefaultState();
        assert stone.isSealed() : "BlockState should be sealed after initialization";

        // 1. Verify setDefaultState cannot be overwritten
        boolean threw = false;
        try {
            Blocks.STONE.setDefaultState(stone);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assert threw : "setDefaultState must throw IllegalStateException on re-assignment";

        // 2. Verify values map cannot be mutated
        threw = false;
        try {
            stone.getValues().clear();
        } catch (UnsupportedOperationException e) {
            threw = true;
        }
        assert threw : "BlockState values map must be unmodifiable";

        System.out.println(" PASSED");
    }
}
