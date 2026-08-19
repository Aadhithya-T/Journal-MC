# Minecraft Journal: Texture Art Direction Specification

## 1. Core Visual Philosophy
- **Authentic Minecraft Voxel Aesthetic**: Pixel-art textures authored with clear material purpose, hard pixel edges, and crisp definition at both close-up inspection and distant panoramic viewing.
- **No Naive Mathematical Sweeps**: Replace smooth `sin() * cos()` continuous noise with discrete, clustered pixel structures and deterministic hash noise.
- **Material Differentiation**: Every block must communicate its physical substance (organic grass blades, granular earth, mineral grain, sedimentary stratification, fibrous bark) through pixel architecture, not just solid tinting.
- **No Lighting Hacks**: All textural depth must originate purely within the texture albedo itself. The physical lighting model, tone mapper, and exposure remain strictly locked.

---

## 2. Global Texture Specifications

| Dimension / Property | Specification | Rationale |
| :--- | :--- | :--- |
| **Grid Resolution** | $64 \times 64$ pixels per tile | Preserves uniform high-detail pixel art fidelity across all blocks |
| **Texture Filtering** | `GL_NEAREST` Mag & `GL_NEAREST_MIPMAP_NEAREST` Min | Ensures hard pixel edges up close, prevents blurry texture mud at distance |
| **Anisotropic Filtering** | 8x Max Anisotropy | Sharp mipmap transitions at glancing camera angles across flat terrain |
| **Texel Density** | Exactly 64 texels / meter | Uniform across all blocks (Grass, Dirt, Sand, Stone, Wood, Leaves) |
| **Color Space** | sRGB authored $\to$ `GL_SRGB8_ALPHA8` hardware decoded | Natural linear color blending under sunlight and ambient skies |

---

## 3. Core Terrain Blocks: Detailed Art Direction

### 🌿 1. Grass (Top & Side)
- **Top Face (`slot 0`)**:
  - Base: Rich, temperate meadow green ($[82, 142, 42]$).
  - Variation: Clustered micro-blades, subtle dark green accents ($[64, 118, 32]$), and bright leafy highlights ($[102, 168, 54]$).
  - Structure: Micro-blade clusters (2x2 to 3x4 pixels) distributed across a subtle macro-tone map to avoid repetitive tiling gridlines.
- **Side Face (`slot 1`)**:
  - Top edge: Organic, irregular drooping grass fringe extending 12 to 24 pixels down from the upper edge with natural varying root tips.
  - Lower body: Deep rich earthen soil matching the Dirt texture.

### 🌰 2. Dirt (`slot 2`)
- **Base**: Medium warm earthen brown ($[134, 96, 62]$).
- **Variation**: Darker loam patches ($[108, 76, 48]$), embedded rounded pebble clusters ($[158, 122, 88]$), and subtle granular specks ($[92, 64, 40]$).
- **Avoid**: Checkerboard grids, periodic waves, and uniform white-noise TV static.

### 🏖️ 3. Sand (`slot 5`)
- **Base**: Pale warm beige ($[224, 214, 166]$).
- **Variation**: Gentle sedimentary drifts, sparse darker sandstone grains ($[198, 186, 138]$), and warm cream highlights ($[238, 230, 188]$).
- **Avoid**: Pure yellow/orange tint, gray mud tones, and excessive contrast.

### 🪨 4. Stone (`slot 3`)
- **Base**: Balanced neutral slate gray ($[126, 126, 128]$).
- **Variation**: Subtle cool-gray fissures ($[104, 104, 108]$), mineral flecks ($[148, 148, 152]$), and organic basalt patches ($[92, 92, 96]$).
- **Avoid**: Sharp black cracks, high-contrast digital noise, and photographic marble smears.

---

## 4. Deterministic Hash Architecture
All procedural texture generation uses a fast, deterministic integer bit-permutation hash function:
```java
private static int hash2D(int x, int y, int seed) {
    int h = x * 374761393 + y * 668265263 + seed;
    h = (h ^ (h >> 13)) * 1274126177;
    return h ^ (h >> 16);
}
```
This guarantees:
1. 100% deterministic pixel reproducibility on every engine launch.
2. Complete absence of periodic trigonometric wave artifacts.
3. Natural, clustered pixel-art material formation.
