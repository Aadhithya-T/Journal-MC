package com.mcjournal.block;

import com.mcjournal.block.property.Axis;
import com.mcjournal.block.property.Property;

import java.util.*;

public final class BlockStateRegistry {
    private static final List<BlockState> STATES_BY_ID = new ArrayList<>();
    private static final Map<String, BlockState> STATES_BY_NAME = new HashMap<>();
    private static final Map<BlockType, Map<Map<Property<?>, Comparable<?>>, BlockState>> STATES_BY_BLOCK = new HashMap<>();
    private static final BlockType[] BLOCKS_BY_LEGACY_ID = new BlockType[256];
    private static final BlockState[] DEFAULT_STATES_BY_LEGACY_ID = new BlockState[256];

    static {
        initialize();
    }

    private static void initialize() {
        for (BlockType block : Blocks.getAll()) {
            BLOCKS_BY_LEGACY_ID[block.getLegacyId() & 0xFF] = block;

            List<Property<?>> properties = block.getProperties();
            List<Map<Property<?>, Comparable<?>>> combinations = generatePropertyCombinations(properties);

            Map<Map<Property<?>, Comparable<?>>, BlockState> stateMap = new HashMap<>();

            for (Map<Property<?>, Comparable<?>> valMap : combinations) {
                BlockState state = new BlockState(block, valMap);
                int id = STATES_BY_ID.size();
                state.setStateId(id);
                STATES_BY_ID.add(state);
                STATES_BY_NAME.put(state.getSerializedName(), state);
                stateMap.put(valMap, state);
            }

            STATES_BY_BLOCK.put(block, stateMap);

            // Establish default state
            Map<Property<?>, Comparable<?>> defaultProps = new HashMap<>();
            for (Property<?> prop : properties) {
                if (prop == BlockProperties.AXIS) {
                    defaultProps.put(prop, Axis.Y);
                } else if (prop == BlockProperties.LEVEL) {
                    defaultProps.put(prop, 0);
                } else if (prop == BlockProperties.SNOWY) {
                    defaultProps.put(prop, false);
                } else {
                    defaultProps.put(prop, prop.getAllowedValues().iterator().next());
                }
            }

            BlockState defaultState = stateMap.get(defaultProps);
            if (defaultState == null && !stateMap.isEmpty()) {
                defaultState = stateMap.values().iterator().next();
            }
            block.setDefaultState(defaultState);
            DEFAULT_STATES_BY_LEGACY_ID[block.getLegacyId() & 0xFF] = defaultState;
        }

        // Link state transitions for instant with(property, value) lookups
        for (BlockType block : Blocks.getAll()) {
            Map<Map<Property<?>, Comparable<?>>, BlockState> stateMap = STATES_BY_BLOCK.get(block);
            if (stateMap == null) continue;

            for (BlockState state : stateMap.values()) {
                for (Property<?> prop : block.getProperties()) {
                    linkTransitions(state, prop, stateMap);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> void linkTransitions(
            BlockState state, Property<T> prop, Map<Map<Property<?>, Comparable<?>>, BlockState> stateMap) {
        for (T val : prop.getAllowedValues()) {
            Map<Property<?>, Comparable<?>> targetProps = new HashMap<>(state.getValues());
            targetProps.put(prop, val);
            BlockState target = stateMap.get(targetProps);
            if (target != null) {
                state.setTransition(prop, val, target);
            }
        }
    }

    private static List<Map<Property<?>, Comparable<?>>> generatePropertyCombinations(List<Property<?>> properties) {
        List<Map<Property<?>, Comparable<?>>> result = new ArrayList<>();
        if (properties.isEmpty()) {
            result.add(Collections.emptyMap());
            return result;
        }

        generateRecursive(properties, 0, new LinkedHashMap<>(), result);
        return result;
    }

    private static void generateRecursive(
            List<Property<?>> properties, int index,
            Map<Property<?>, Comparable<?>> current,
            List<Map<Property<?>, Comparable<?>>> result) {
        if (index == properties.size()) {
            result.add(new LinkedHashMap<>(current));
            return;
        }

        Property<?> prop = properties.get(index);
        for (Comparable<?> val : prop.getAllowedValues()) {
            current.put(prop, val);
            generateRecursive(properties, index + 1, current, result);
            current.remove(prop);
        }
    }

    public static BlockState getStateById(int id) {
        if (id >= 0 && id < STATES_BY_ID.size()) {
            return STATES_BY_ID.get(id);
        }
        return Blocks.AIR.getDefaultState();
    }

    public static BlockState getDefaultState(byte legacyId) {
        BlockState state = DEFAULT_STATES_BY_LEGACY_ID[legacyId & 0xFF];
        return state != null ? state : Blocks.AIR.getDefaultState();
    }

    public static BlockType getBlockType(byte legacyId) {
        BlockType type = BLOCKS_BY_LEGACY_ID[legacyId & 0xFF];
        return type != null ? type : Blocks.AIR;
    }

    public static BlockState getState(BlockType block, Map<Property<?>, Comparable<?>> values) {
        Map<Map<Property<?>, Comparable<?>>, BlockState> map = STATES_BY_BLOCK.get(block);
        if (map != null) {
            BlockState state = map.get(values);
            if (state != null) return state;
        }
        return block.getDefaultState();
    }

    /**
     * Parses a block state from string (e.g. "oak_log[axis=x]" or "stone" or legacy numeric string "7").
     */
    public static BlockState parse(String input) {
        if (input == null || input.isEmpty()) return Blocks.AIR.getDefaultState();

        // 1. Direct match
        BlockState exact = STATES_BY_NAME.get(input);
        if (exact != null) return exact;

        // 2. Numeric legacy fallback
        try {
            byte num = Byte.parseByte(input);
            return getDefaultState(num);
        } catch (NumberFormatException ignored) {}

        // 3. Parse block_name[prop1=val1,prop2=val2]
        int bracketIndex = input.indexOf('[');
        if (bracketIndex < 0) {
            for (BlockType block : Blocks.getAll()) {
                if (block.getId().equalsIgnoreCase(input)) {
                    return block.getDefaultState();
                }
            }
            return Blocks.AIR.getDefaultState();
        }

        String blockId = input.substring(0, bracketIndex).trim();
        String propsStr = input.substring(bracketIndex + 1, input.endsWith("]") ? input.length() - 1 : input.length());

        BlockType targetBlock = null;
        for (BlockType block : Blocks.getAll()) {
            if (block.getId().equalsIgnoreCase(blockId)) {
                targetBlock = block;
                break;
            }
        }
        if (targetBlock == null) return Blocks.AIR.getDefaultState();

        Map<Property<?>, Comparable<?>> parsedProps = new HashMap<>();
        String[] pairs = propsStr.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                String key = kv[0].trim();
                String val = kv[1].trim();
                for (Property<?> prop : targetBlock.getProperties()) {
                    if (prop.getName().equalsIgnoreCase(key)) {
                        Optional<?> parsedVal = prop.parseValue(val);
                        parsedVal.ifPresent(v -> parsedProps.put(prop, (Comparable<?>) v));
                    }
                }
            }
        }

        return getState(targetBlock, parsedProps);
    }

    public static int getTotalStates() {
        return STATES_BY_ID.size();
    }
}
