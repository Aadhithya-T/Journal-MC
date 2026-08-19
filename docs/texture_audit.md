# Texture Pipeline Audit Report

## 1. Executive Summary
This document provides a comprehensive audit of the texture subsystem in the Minecraft Journal engine.
All texture assets in this engine are generated in memory mathematically at application startup via [`ProceduralTextureGenerator.java`](file:///c:/Users/aadhi/OneDrive/Desktop/mc-journal/engine/src/com/mcjournal/client/ProceduralTextureGenerator.java) and packed into a single dynamic OpenGL texture atlas by [`TextureAtlas.java`](file:///c:/Users/aadhi/OneDrive/Desktop/mc-journal/engine/src/com/mcjournal/client/TextureAtlas.java). Zero external PNG/image files are loaded from disk.

---

## 2. Technical Audit Questions & Answers

### 1. What resolution are the current block textures?
- **64x64 pixels** per individual tile (`TILE_SIZE = 64` in `ProceduralTextureGenerator.java`).

### 2. Are they individual images or packed into an atlas?
- They are procedurally painted into a single packed in-memory atlas buffer and uploaded as a single 2D OpenGL texture. No individual image files exist on disk (`engine/resources/textures/` is empty).

### 3. What is the atlas layout?
- **4x4 Grid** containing **16 slots**.
- Total atlas dimensions: **256x256 pixels** (`ATLAS_SIZE = 4 * 64 = 256`).
- Each slot occupies $0.25 \times 0.25$ in normalized UV coordinates (`TILE_UV_SIZE = 0.25`).

### 4. What texel density does each block use?
- Uniform **64 texels per world block** on all standard 1m x 1m voxel faces across the entire terrain.

### 5. Are textures being filtered with `GL_NEAREST`?
- Magnification filter: `GL_TEXTURE_MAG_FILTER = GL_NEAREST` (crisp pixel art up close).
- Minification filter: `GL_TEXTURE_MIN_FILTER = GL_NEAREST_MIPMAP_LINEAR` (currently interpolates mipmap levels, softening distant pixels).
- Target calibration: Upgrade minification filter to `GL_NEAREST_MIPMAP_NEAREST` with 8x anisotropic filtering to preserve hard, crisp pixel boundaries at distance without high-frequency aliasing shimmer.

### 6. Is mipmapping enabled?
- Yes, `glGenerateMipmap(GL_TEXTURE_2D)` is invoked after uploading `GL_SRGB8_ALPHA8` data.

### 7. Are texture coordinates bleeding between atlas tiles?
- Mitigated via half-texel insetting in [`ChunkMeshBuilder.java`](file:///c:/Users/aadhi/OneDrive/Desktop/mc-journal/engine/src/com/mcjournal/ChunkMeshBuilder.java):
  $$\text{eps} = \frac{0.5}{256.0} \approx 0.001953$$
  $$u \in [u_{\min} + \text{eps}, u_{\max} - \text{eps}], \quad v \in [v_{\min} + \text{eps}, v_{\max} - \text{eps}]$$
  This prevents texture bleeding across neighboring tiles.

### 8. Are different blocks using inconsistent resolutions?
- No. All 16 slots are strictly $64 \times 64$ pixels.

### 9. Are textures being color-corrected correctly?
- Yes. The atlas is uploaded to GPU VRAM with internal format `GL_SRGB8_ALPHA8`. Hardware texture samplers in OpenGL automatically perform hardware gamma decoding ($C^{2.2}$) into physical linear HDR space during shader sampling.
- Procedural color assignments in Java are written in standard sRGB hex/byte values ($0 \to 255$).

### 10. Are any textures being modified by vertex colors before reaching the shader?
- For solid terrain blocks: `vColor` carries pure vertex Ambient Occlusion (values between $0.42$ and $1.0$). It modulates incident light in [`chunk_fragment.glsl`](file:///c:/Users/aadhi/OneDrive/Desktop/mc-journal/engine/resources/shaders/chunk_fragment.glsl), leaving raw texture albedo un-tinted.
- For water: `vColor.r` encodes physical column depth, `vColor.g` encodes shoreline adjacency.
- Cross-foliage (Tall grass, Poppy, Dandelion): `vColor` is $(1.0, 1.0, 1.0)$.

### 11. Are any blocks currently using placeholder/generated colors instead of actual textures?
- **All textures currently use naive mathematical functions** (`sin()`, `cos()`, simple XOR patterns, rectangular pixel fills) rather than authentic pixel-art material structures.
- For example:
  - Grass top used broad $\sin \times \cos$ wave sweeps that looked like green plastic blobs.
  - Dirt used identical $\sin \times \cos$ noise with no earthen grain or pebble clustering.
  - Sand had only 1D horizontal sine stripes.
  - Stone used identical $\sin \times \cos$ noise with no rock cleavage or mineral flecks.
  - Oak Log used 3 flat vertical stripes.
  - Bedrock used an XOR math pattern `((x/4) ^ (y/4)) % 3`.

---

## 3. Atlas Slot Assignment Table

| Slot Index | Block / Feature | Mapping in `ChunkMeshBuilder.java` | Current Generator Method |
| :--- | :--- | :--- | :--- |
| **0** | Grass Top | `Block.GRASS` (+Y Top Face) | `generateGrassTop()` |
| **1** | Grass Side | `Block.GRASS` (Sides: +X, -X, +Z, -Z) | `generateGrassSide()` |
| **2** | Dirt | `Block.DIRT` (All Faces) & `Block.GRASS` (-Y Bottom) | `generateDirt()` |
| **3** | Stone | `Block.STONE` (All Faces) | `generateStone()` |
| **4** | Cobblestone | `Block.COBBLESTONE` (All Faces) | `generateCobblestone()` |
| **5** | Sand | `Block.SAND` (All Faces) | `generateSand()` |
| **6** | Bedrock | `Block.BEDROCK` (All Faces) | `generateBedrock()` |
| **7** | Oak Log Side | `Block.OAK_LOG` (Sides: +X, -X, +Z, -Z) | `generateOakLogSide()` |
| **8** | Oak Log Top | `Block.OAK_LOG` (+Y, -Y) & `Block.BIRCH_LOG` (+Y, -Y) | `generateOakLogTop()` |
| **9** | Oak Leaves | `Block.OAK_LEAVES` & `Block.BIRCH_LEAVES` (All Faces) | `generateOakLeaves()` |
| **10** | Diamond Ore | `Block.DIAMOND_ORE` (All Faces) | `generateDiamondOre()` |
| **11** | Water | `Block.WATER` (Shader-shaded) | `generateWater()` |
| **12** | Tall Grass | `Block.TALL_GRASS` (Cross-Quad Foliage) | `generateTallGrass()` |
| **13** | Poppy | `Block.POPPY` (Cross-Quad Foliage) | `generatePoppy()` |
| **14** | Dandelion | `Block.DANDELION` (Cross-Quad Foliage) | `generateDandelion()` |
| **15** | Birch Log Side | `Block.BIRCH_LOG` (Sides: +X, -X, +Z, -Z) | `generateBirchLogSide()` |
