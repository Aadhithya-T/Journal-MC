package com.mcjournal.block;

import com.mcjournal.block.property.Property;

import java.util.*;

public class BlockState {
    private final BlockType owner;
    private final Map<Property<?>, Comparable<?>> values;
    private int stateId = -1;
    private final Map<Property<?>, Map<Comparable<?>, BlockState>> transitions = new HashMap<>();

    public BlockState(BlockType owner, Map<Property<?>, Comparable<?>> values) {
        this.owner = owner;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public BlockType getBlock() {
        return owner;
    }

    public int getStateId() {
        return stateId;
    }

    private boolean sealed = false;

    void setStateId(int id) {
        if (this.stateId != -1) {
            throw new IllegalStateException("BlockState ID already initialized: " + this.stateId);
        }
        this.stateId = id;
    }

    void setTransition(Property<?> property, Comparable<?> value, BlockState target) {
        if (sealed) {
            throw new IllegalStateException("Cannot add transitions to a sealed BlockState");
        }
        transitions.computeIfAbsent(property, k -> new HashMap<>()).put(value, target);
    }

    void sealTransitions() {
        if (sealed) return;
        for (Map.Entry<Property<?>, Map<Comparable<?>, BlockState>> entry : transitions.entrySet()) {
            entry.setValue(Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
        }
        this.sealed = true;
    }

    public boolean isSealed() {
        return sealed;
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T get(Property<T> property) {
        Comparable<?> val = values.get(property);
        if (val == null) {
            throw new IllegalArgumentException("Property " + property.getName() + " does not exist on " + owner.getId());
        }
        return (T) val;
    }

    public boolean contains(Property<?> property) {
        return values.containsKey(property);
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>, V extends T> BlockState with(Property<T> property, V value) {
        Comparable<?> current = values.get(property);
        if (Objects.equals(current, value)) {
            return this;
        }

        Map<Comparable<?>, BlockState> propMap = transitions.get(property);
        if (propMap != null) {
            BlockState neighbor = propMap.get(value);
            if (neighbor != null) return neighbor;
        }

        // Fallback: Query registry
        Map<Property<?>, Comparable<?>> newValues = new HashMap<>(this.values);
        newValues.put(property, value);
        return BlockStateRegistry.getState(owner, newValues);
    }

    public Map<Property<?>, Comparable<?>> getValues() {
        return values;
    }

    public boolean is(BlockType block) {
        return this.owner == block;
    }

    public boolean isAir() {
        return this.owner == Blocks.AIR;
    }

    public boolean isSolid() {
        return this.owner.isSolid();
    }

    public boolean isTransparent() {
        return this.owner.isTransparent();
    }

    public boolean isPlant() {
        return this.owner.isPlant();
    }

    public boolean isFluid() {
        return this.owner.isFluid();
    }

    public boolean isWater() {
        return this.owner == Blocks.WATER;
    }

    public boolean isBedrock() {
        return this.owner == Blocks.BEDROCK;
    }

    public byte getLegacyId() {
        return this.owner.getLegacyId();
    }

    public int getFaceTextureSlot(int faceIndex) {
        return this.owner.getTextureSlot(this, faceIndex);
    }

    public int getDisplayFaceTile() {
        return this.owner.getDisplayFaceTile();
    }

    public float getHardness() {
        return this.owner.getHardness();
    }

    public BlockType getDrop() {
        return this.owner.getDrop(this);
    }

    public String getSerializedName() {
        if (values.isEmpty()) {
            return owner.getId();
        }

        StringBuilder sb = new StringBuilder(owner.getId()).append('[');
        boolean first = true;
        for (Map.Entry<Property<?>, Comparable<?>> entry : values.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(entry.getKey().getName()).append('=');
            sb.append(entry.getValue().toString().toLowerCase(Locale.ROOT));
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockState that)) return false;
        return stateId == that.stateId && Objects.equals(owner, that.owner) && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return (stateId >= 0) ? stateId : Objects.hash(owner, values);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }
}
