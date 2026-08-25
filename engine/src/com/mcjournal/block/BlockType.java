package com.mcjournal.block;

import com.mcjournal.block.property.Axis;
import com.mcjournal.block.property.Property;

import java.util.*;

public class BlockType {
    private final String id;
    private final String name;
    private final String colorHex;
    private final float hardness;
    private final byte legacyId;
    private final boolean solid;
    private final boolean transparent;
    private final boolean plant;
    private final boolean fluid;
    private final List<Property<?>> properties;
    private BlockState defaultState;

    public BlockType(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.colorHex = builder.colorHex;
        this.hardness = builder.hardness;
        this.legacyId = builder.legacyId;
        this.solid = builder.solid;
        this.transparent = builder.transparent;
        this.plant = builder.plant;
        this.fluid = builder.fluid;
        this.properties = List.copyOf(builder.properties);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColorHex() {
        return colorHex;
    }

    public float getHardness() {
        return hardness;
    }

    public byte getLegacyId() {
        return legacyId;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public boolean isPlant() {
        return plant;
    }

    public boolean isFluid() {
        return fluid;
    }

    public List<Property<?>> getProperties() {
        return properties;
    }

    public BlockState getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(BlockState state) {
        this.defaultState = state;
    }

    /**
     * Calculates the texture atlas slot index for a given face on this block state.
     *
     * @param state The specific BlockState instance.
     * @param faceIndex 0: East (+X), 1: West (-X), 2: Top (+Y), 3: Bottom (-Y), 4: South (+Z), 5: North (-Z).
     * @return The 0-based TextureAtlas slot index.
     */
    public int getTextureSlot(BlockState state, int faceIndex) {
        // Dynamic orientation handling for directional logs
        if (state.contains(BlockProperties.AXIS)) {
            Axis axis = state.get(BlockProperties.AXIS);
            boolean isBirch = (this.legacyId == 11);
            int barkSlot = isBirch ? 15 : 7;
            int ringSlot = 8;

            switch (axis) {
                case Y -> {
                    return (faceIndex == 2 || faceIndex == 3) ? ringSlot : barkSlot;
                }
                case X -> {
                    return (faceIndex == 0 || faceIndex == 1) ? ringSlot : barkSlot;
                }
                case Z -> {
                    return (faceIndex == 4 || faceIndex == 5) ? ringSlot : barkSlot;
                }
            }
        }

        return switch (legacyId) {
            case 1 -> (faceIndex == 2) ? 0 : (faceIndex == 3 ? 2 : 1); // Grass Block
            case 2 -> 2; // Dirt
            case 3 -> 3; // Stone
            case 4 -> 4; // Cobblestone
            case 5 -> 5; // Sand
            case 6 -> 6; // Bedrock
            case 8, 12 -> 9; // Leaves
            case 9 -> 10; // Diamond Ore
            case 10 -> 11; // Water
            case 13 -> 12; // Tall Grass
            case 14 -> 13; // Poppy
            case 15 -> 14; // Dandelion
            default -> 2;
        };
    }

    public int getDisplayFaceTile() {
        return switch (legacyId) {
            case 1 -> 1; // Grass side
            case 2 -> 2; // Dirt
            case 3 -> 3; // Stone
            case 4 -> 4; // Cobblestone
            case 5 -> 5; // Sand
            case 6 -> 6; // Bedrock
            case 7 -> 7; // Oak Log
            case 8, 12 -> 9; // Leaves
            case 9 -> 10; // Diamond Ore
            case 10 -> 11; // Water
            case 11 -> 15; // Birch Log
            case 13 -> 12; // Tall Grass
            case 14 -> 13; // Poppy
            case 15 -> 14; // Dandelion
            default -> 0;
        };
    }

    public BlockType getDrop(BlockState state) {
        return switch (legacyId) {
            case 1, 2 -> Blocks.DIRT;
            case 3 -> Blocks.COBBLESTONE;
            case 4 -> Blocks.COBBLESTONE;
            case 5 -> Blocks.SAND;
            case 7 -> Blocks.OAK_LOG;
            case 8 -> Blocks.OAK_LEAVES;
            case 9 -> Blocks.DIAMOND_ORE;
            case 11 -> Blocks.BIRCH_LOG;
            case 12 -> Blocks.BIRCH_LEAVES;
            case 13 -> Blocks.TALL_GRASS;
            case 14 -> Blocks.POPPY;
            case 15 -> Blocks.DANDELION;
            default -> Blocks.AIR;
        };
    }

    public static class Builder {
        private final String id;
        private final byte legacyId;
        private String name = "";
        private String colorHex = "#888888";
        private float hardness = 1.0f;
        private boolean solid = true;
        private boolean transparent = false;
        private boolean plant = false;
        private boolean fluid = false;
        private final List<Property<?>> properties = new ArrayList<>();

        public Builder(String id, byte legacyId) {
            this.id = id;
            this.legacyId = legacyId;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder color(String hex) {
            this.colorHex = hex;
            return this;
        }

        public Builder hardness(float hardness) {
            this.hardness = hardness;
            return this;
        }

        public Builder solid(boolean solid) {
            this.solid = solid;
            return this;
        }

        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }

        public Builder plant(boolean plant) {
            this.plant = plant;
            this.solid = false;
            this.transparent = true;
            this.hardness = 0.0f;
            return this;
        }

        public Builder fluid(boolean fluid) {
            this.fluid = fluid;
            this.solid = false;
            this.transparent = true;
            this.hardness = -1.0f;
            return this;
        }

        public Builder properties(Property<?>... props) {
            Collections.addAll(this.properties, props);
            return this;
        }

        public BlockType build() {
            return new BlockType(this);
        }
    }

    @Override
    public String toString() {
        return "BlockType{" + id + "}";
    }
}
