package com.mcjournal.test;

import com.mcjournal.block.*;
import com.mcjournal.block.property.Property;

import java.util.Collection;

/**
 * P11 Unit Test: Block System
 * Covers: BlockState, BlockStateRegistry, Properties, and State Transitions.
 */
public class BlockSystemTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       RUNNING P11: BLOCK SYSTEM TESTS           ");
        System.out.println("=================================================");

        testBlockStateBasics();
        testBlockStateRegistry();
        testProperties();
        testStateTransitions();

        System.out.println(">>> ALL BLOCK SYSTEM TESTS PASSED SUCCESSFULLY! <<<\n");
    }

    private static void testBlockStateBasics() {
        System.out.print("[P11 - Block] BlockState basic attributes & immutability... ");

        BlockState airState = Blocks.AIR.getDefaultState();
        assert airState.isAir() : "AIR default state must report isAir() true";
        assert !airState.isSolid() : "AIR must not be solid";
        assert !airState.isWater() : "AIR must not be water";

        BlockState stoneState = Blocks.STONE.getDefaultState();
        assert !stoneState.isAir() : "STONE must not be air";
        assert stoneState.isSolid() : "STONE must be solid";
        assert !stoneState.isWater() : "STONE must not be water";

        BlockState waterState = Blocks.WATER.getDefaultState();
        assert waterState.isWater() : "WATER must report isWater() true";
        assert !waterState.isSolid() : "WATER must not be solid";

        // Equality & Hashcode
        BlockState stoneState2 = Blocks.STONE.getDefaultState();
        assert stoneState.equals(stoneState2) : "Identical states must be equal";
        assert stoneState.hashCode() == stoneState2.hashCode() : "Equal states must have identical hashCode";
        assert !stoneState.equals(airState) : "Different block states must not be equal";

        System.out.println("PASSED");
    }

    private static void testBlockStateRegistry() {
        System.out.print("[P11 - Block] BlockStateRegistry state resolution & fallbacks... ");

        int totalStates = BlockStateRegistry.getTotalStates();
        assert totalStates > 0 : "Registry must contain registered states";

        for (int i = 0; i < totalStates; i++) {
            BlockState state = BlockStateRegistry.getStateById(i);
            assert state != null : "Registry must not have null state at ID " + i;
            assert state.getStateId() == i : "State ID mismatch: expected " + i + " got " + state.getStateId();

            BlockType blockType = state.getBlock();
            assert blockType != null : "BlockType must not be null for valid state ID " + i;
            assert BlockStateRegistry.getBlockType(blockType.getLegacyId()) == blockType : "BlockType lookup mismatch";
        }

        // Unknown state ID fallback to AIR
        BlockState fallback = BlockStateRegistry.getStateById(999999);
        assert fallback != null && fallback.isAir() : "Unknown state ID must fall back to AIR default state";

        System.out.println("PASSED (" + totalStates + " states verified)");
    }

    private static void testProperties() {
        System.out.print("[P11 - Block] Property definitions & range validation... ");

        Property<Integer> levelProp = BlockProperties.LEVEL;
        assert levelProp.getName().equals("level") : "Property name must match";
        Collection<Integer> levelValues = levelProp.getAllowedValues();
        assert levelValues.size() == 8 : "Level 0..7 must contain 8 values";
        assert levelValues.contains(0) : "Must contain 0";
        assert levelValues.contains(7) : "Must contain 7";
        assert !levelValues.contains(8) : "Must not contain 8";
        assert !levelValues.contains(-1) : "Must not contain -1";

        Property<Boolean> snowyProp = BlockProperties.SNOWY;
        assert snowyProp.getName().equals("snowy") : "Property name must match";
        Collection<Boolean> snowyValues = snowyProp.getAllowedValues();
        assert snowyValues.size() == 2 : "Boolean property must have 2 values";
        assert snowyValues.contains(true) && snowyValues.contains(false) : "Must contain true and false";

        System.out.println("PASSED");
    }

    private static void testStateTransitions() {
        System.out.print("[P11 - Block] State transitions & immutability guarantee... ");

        BlockState defaultWater = Blocks.WATER.getDefaultState();
        int initialLevel = defaultWater.get(BlockProperties.LEVEL);
        assert initialLevel == 0 : "Default water should have level 0";

        // Transition water to level 1..7
        BlockState prev = defaultWater;
        for (int lvl = 1; lvl <= 7; lvl++) {
            BlockState transitioned = prev.with(BlockProperties.LEVEL, lvl);
            assert transitioned != null : "Transitioned state must not be null";
            assert transitioned != prev : "Transition must return a distinct BlockState instance";
            assert transitioned.get(BlockProperties.LEVEL) == lvl : "Transitioned state must have level " + lvl;
            assert transitioned.isWater() : "Transitioned water must remain water";
            assert transitioned.getStateId() != prev.getStateId() : "Distinct state must have distinct state ID";

            // Verify original state was NOT mutated
            assert defaultWater.get(BlockProperties.LEVEL) == 0 : "Original water state must remain level 0";
            prev = transitioned;
        }

        // Transition back to 0
        BlockState backToZero = prev.with(BlockProperties.LEVEL, 0);
        assert backToZero == defaultWater : "Transitioning back to level 0 should resolve to default water state";

        System.out.println("PASSED");
    }
}
