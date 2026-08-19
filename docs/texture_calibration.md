# Minecraft Journal: Complete Block Texture Calibration Report

## 1. Executive Summary & Calibration Target
- **Scope**: Complete procedural pixel-art calibration of all 16 atlas block slots in [`ProceduralTextureGenerator.java`](file:///c:/Users/aadhi/OneDrive/Desktop/mc-journal/engine/src/com/mcjournal/client/ProceduralTextureGenerator.java).
- **Aesthetic**: Authentic Minecraft Java Edition voxel pixel-art character at crisp 64x64 resolution per face.
- **Strict Constraint**: **Lighting, exposure, tone mapping, atmospheric fog, and water optics are 100% UNTOUCHED**. All visual improvements stem strictly from authored procedural pixel art.
- **Filtering**: `GL_NEAREST` Mag + `GL_NEAREST_MIPMAP_NEAREST` Min with 8x anisotropic filtering in [`TextureAtlas.java`](file:///c:/Users/aadhi/OneDrive/Desktop/mc-journal/engine/src/com/mcjournal/client/TextureAtlas.java).

---

## 2. Complete Atlas Slot Calibration Breakdown

| Slot Index | Block / Feature | Previous Problem | Calibrated Pixel-Art Architecture |
| :--- | :--- | :--- | :--- |
| **0** | **Grass Top** | Naive $\sin \times \cos$ wave sweeps; looked like green plastic. | 5-color meadow green palette ($[64,116,32] \to [118,184,66]$), discrete micro-blade clusters ($1\times2, 2\times3$), blade tip highlights. |
| **1** | **Grass Side** | Flat dirt with periodic sine-wave fringe. | Calibrated dirt base with an organic 10-22px jagged grass fringe, root tips, and subtle contact drop shadows cast onto the soil underneath. |
| **2** | **Dirt** | Smooth $\sin \times \cos$ noise; no soil granularity. | Rich warm loam base ($[92,64,42] \to [154,116,80]$), 4x4 texel macro clusters, and scattered 2x2 & 3x2 rounded pebbles with drop shadows. |
| **3** | **Stone** | Featureless gray with no cleavage or rock grain. | Balanced neutral slate gray ($[88,88,92] \to [164,164,170]$), subtle diagonal cleavage lines, basalt patches, and quartz sparkle flecks. |
| **4** | **Cobblestone** | Grid-aligned 16px/32px mechanical lines. | Authentic irregular masonry cobblestones with 3D beveled sunlight highlights on top-left edges, shaded underside borders, and dark mortar grooves ($[48,48,52]$). |
| **5** | **Sand** | 1D horizontal sine stripes; visually flat. | Pale warm beige sand ($[196,184,136] \to [246,240,198]$), interlocking micro-grains, gentle wind-drift ripples, and sunlit silica highlights. |
| **6** | **Bedrock** | XOR math checkerboard pattern `((x/4)^(y/4))%3`. | Volcanic basalt fissures, void-black crevice lines ($[16,16,18]$), dark charcoal body, and ash/obsidian highlights ($[102,102,114]$). |
| **7** | **Oak Log Side** | 3 flat vertical stripes; no bark texture. | Vertical fibrous bark strips with organic wander, deep crevice fissure grooves ($[54,38,20]$), highlighted ridges ($[138,108,64]$), and bark knot whorls. |
| **8** | **Oak Log Top** | Simple mathematical circles. | Concentric annual growth rings ($[138,108,64] \to [216,182,124]$), dark pith heartwood core, radial wood grain rays, and outer bark boundary rim. |
| **9** | **Oak / Birch Leaves** | 4x4 rectangular grid banding artifacts. | Volumetric clustered foliage clumps with sunlit top highlights, canopy underside shadows, and organic 12% transparent cutout holes (STRICT alpha 0 or 255). |
| **10** | **Diamond Ore** | Low-contrast gem dots on flat stone. | Calibrated neutral Stone mantle embedded with 5 multi-facet diamond crystal clusters featuring brilliant white sparkles ($[220,255,255]$), top-left lit facets ($[110,240,255]$), rich cyan bodies, and deep border shadows. |
| **11** | **Water Base** | Unused base tile. | Crystalline water tile texture (surface optics are driven dynamically by the shader). |
| **12** | **Tall Grass** | Rough green lines. | Authentic multi-blade wild grass stems with bright sunlit tips ($[114,186,64]$), dark rooted bases ($[58,118,30]$), and gentle wind curves (STRICT alpha 0 or 255). |
| **13** | **Poppy** | Simple red circle. | Authentic red flower petals with lit top petals ($[236,48,48]$), base red petals ($[214,36,36]$), dark center core ($[44,28,22]$), and green stem (STRICT alpha 0 or 255). |
| **14** | **Dandelion** | Simple yellow circle. | Golden-yellow dandelion flower head with sunlit yellow petals ($[255,234,42]$), orange-gold core ($[248,172,18]$), and green stem (STRICT alpha 0 or 255). |
| **15** | **Birch Log Side** | 4 hardcoded gray rectangles. | Smooth pale cream paper birch bark ($[228,226,218]$) with authentic horizontal black/charcoal lenticel dashes ($[28,28,30]$) and shaded gray rims ($[68,68,72]$). |

---

## 3. Visual Validation Checklist Across All Environments

| Environment / View | Observation | Result |
| :--- | :--- | :--- |
| **1. Bright Daylight Grass/Dirt Plains** | Rich temperate meadow green with organic micro-blades; dirt borders blend seamlessly with root fringe. Zero repetitive wave banding. | **PASSED** |
| **2. Mountain Stone Cliffs & Caverns** | Slate gray stone reads as solid, cohesive bedrock cliffs at distance and detailed mineral cleavage up close. | **PASSED** |
| **3. Sandy Beaches & Shorelines** | Warm pale beige sand with fine granular ripples; crystal clear water transitions naturally onto beach sand. | **PASSED** |
| **4. Oak & Birch Forests** | Distinct fibrous oak bark vs. pale lenticel birch bark; volumetric leaves have natural depth without grid banding. | **PASSED** |
| **5. Underground Mining (Stone, Cobble, Diamond Ore, Bedrock)** | Cobblestone reads as authentic masonry blocks; Diamond Ore crystals sparkle cleanly against stone; Bedrock looks like dark volcanic rock. | **PASSED** |
| **6. Distant Panoramic Landscapes** | `GL_NEAREST_MIPMAP_NEAREST` with 8x anisotropic filtering preserves crisp pixel art without blurring into gray mud. | **PASSED** |
| **7. Lighting & Atmospheric Integrity** | 100% UNTOUCHED (Sunlight, Moonlight, Ambient, Tone Mapping, Fog, Water Optics remain locked). | **PASSED** |
