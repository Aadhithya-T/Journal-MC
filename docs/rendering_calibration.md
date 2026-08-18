# Minecraft Journal: Rendering Engine Architecture & Calibration Report

## 1. Executive Summary
This report establishes the technical foundation, color pipeline, lighting equations, and calibration methodology for the **Minecraft Journal Native Hardcore Edition** voxel renderer. 

The primary objective is to achieve a technically defensible, visually coherent, stylized Minecraft voxel aesthetic across all times of day (**Day**, **Sunset**, **Night**, **Morning**) without ad-hoc magic numbers or screenshot-driven guessing.

---

## 2. Rendering Pipeline Audit & Color Space Flow

### A. Current Color Pipeline Flow
```
[Procedural Texture Generation (sRGB)]
                 │
                 ▼
[Texture Atlas Upload: GL_SRGB8_ALPHA8] ─── Hardware automatic sRGB -> Linear decoding
                 │
                 ▼
[Fragment Shader Texture Fetch: Linear Albedo]
                 │
                 ├── Directional Sunlight (Linear Cosine Diffuse)
                 ├── Hemisphere Ambient Sky & Ground Fill (Linear Space)
                 ├── Vertex Ambient Occlusion (Linear Clamped AO)
                 └── Fresnel Specular & Optical Depth Absorption (Water)
                 │
                 ▼
[Linear Scene Radiance (HDR range: 0.0 -> 2.5+)]
                 │
                 ▼
[Atmospheric Distance Fog Blending (Linear Space)]
                 │
                 ▼
[Filmic / Soft-Shoulder Exposure & Tone Mapping (HDR -> [0, 1])]
                 │
                 ▼
[Gamma Correction (Linear -> sRGB: pow(c, 1.0 / 2.2))]
                 │
                 ▼
[Display Output (sRGB Monitor Framebuffer)]
```

### B. Identified Technical Root Causes of Previous Visual Issues

1. **Uncalibrated Color Space (Gamma/sRGB Misalignment)**:
   - *Cause*: Textures authored in sRGB were uploaded as linear `GL_RGBA8`. Shaders performed lighting calculations on non-linear colors, which distorted falloff curves, blew out highlights, and crushed darks.
   - *Fix*: Upload atlas with `GL_SRGB8_ALPHA8` for free hardware linear decoding and apply explicit gamma conversion ($E = C^{1/2.2}$) before fragment output.

2. **Double Directional Shading in Vertex & Fragment Stages**:
   - *Cause*: `ChunkMeshBuilder.java` multiplied per-face `baseShade` (Top=1.0, Side=0.75, Bottom=0.55) into vertex colors ($v\text{Color}$). In `chunk_fragment.glsl`, $N \cdot L$ was computed *and* multiplied by $v\text{Color}$, effectively applying directional falloff twice ($0.75 \times 0.0 \times \text{AO} \approx 0.0$).
   - *Fix*: Store pure Ambient Occlusion ($v\text{Color} = \text{AO}$) in vertex colors and let the fragment shader calculate directional lighting.

3. **Pipeline Inversion (Fog Applied After Tone Mapping)**:
   - *Cause*: Applying fog after tone mapping broke atmospheric perspective because tone-mapped terrain was blended with non-tone-mapped horizon colors.
   - *Fix*: Blend atmospheric fog in linear HDR space *prior* to tone mapping and exposure.

4. **Hard-Cutoff Time Transitions**:
   - *Cause*: Hard `if/else` branches at discrete sun elevation thresholds caused visual popping.
   - *Fix*: Continuous solar orbital equations with smooth Hermite interpolation (`smoothstep`) for seamless Day $\to$ Sunset $\to$ Night transitions.

5. **Scattered Magic Numbers**:
   - *Cause*: Lighting constants were hardcoded across multiple files.
   - *Fix*: Centralize all physical and stylized constants into `RenderingConfig.java`.

---

## 3. Physical vs. Stylized Parameter Separation

| Parameter Category | Physical / Theoretical Basis | Stylized Adaptations |
| :--- | :--- | :--- |
| **Color Space** | sRGB $\gamma = 2.2$, Linear Lighting Math | Texture art pixelation & color palette |
| **Exposure / Tone Mapping** | Extended Reinhard / Filmic Curve ($W = 1.8$) | High-saturation preservation for vibrant voxel look |
| **Water Optics** | Schlick's Fresnel ($F_0 = 0.04$), Beer-Lambert Absorption | Vibrant cyan-to-sapphire depth gradient |
| **Directional Lighting** | Half-Lambert Diffuse, Astronomical Orbit | Minecraft square sun disc, soft golden corona |
| **Ambient Occlusion** | Geometric contact shadowing | 4-step discrete corner voxel AO curve |
| **Atmospheric Fog** | Rayleigh/Mie scattering approximation | Seamless horizon dome color matching |

---

## 4. Debug Visualization Suite (F1 - F9)

| Key | Debug Mode | Purpose |
| :--- | :--- | :--- |
| **F1** | **Normal Rendering** | Complete calibrated pipeline |
| **F2** | **Albedo Only** | Verifies base texture colors without lighting or shading |
| **F3** | **World Normals** | Inspects normal vector orientations ($N \cdot 0.5 + 0.5$) |
| **F4** | **Direct Sunlight** | Isolates directional sun/moon illumination |
| **F5** | **Ambient Sky Fill** | Isolates environmental hemisphere light |
| **F6** | **Vertex AO** | Isolates baked block corner ambient occlusion |
| **F7** | **Total Radiance (Pre-Albedo)** | Isolates total incident light before texture multiplication |
| **F8** | **Linear HDR Pre-Tonemap** | Visualizes unclamped HDR dynamic range |
| **F9** | **Fog Factor** | Visualizes atmospheric depth attenuation |
