# Minecraft Journal (MC-Journal)

[![Java 26](https://img.shields.io/badge/Java-26%20Loom%20Virtual%20Threads-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://jdk.java.net/)
[![OpenGL 3.3 Core](https://img.shields.io/badge/OpenGL-3.3%20Core%20Profile-5586A4?style=for-the-badge&logo=opengl&logoColor=white)](https://www.opengl.org/)
[![LWJGL 3.3.3](https://img.shields.io/badge/LWJGL-3.3.3-FF6600?style=for-the-badge)](https://www.lwjgl.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20x64-0078D6?style=for-the-badge&logo=windows&logoColor=white)](https://www.microsoft.com/windows)

An authentic, standalone singleplayer Minecraft voxel sandbox and journaling desktop game. Built as a 100% pure native Java 26 and hardware-accelerated OpenGL 3.3 Core Profile engine utilizing LWJGL 3.3.3 bindings, featuring authentic Minecraft Java Edition 1.17 world generation, 3D volumetric cave carving, survival hand mining, dynamic fluid mechanics, continuous 24-hour astronomical lighting, physical water optics, and persistent world state management.

---

## Core Engine Architecture

### Native Java 26 and OpenGL 3.3 Core Profile
- Pure native desktop application running directly on the JVM with zero web browser, Electron, or JavaScript runtime overhead.
- Direct hardware interaction via GLFW window management, OpenGL 3.3 Core Profile shaders, and STB native bindings.
- Multi-threaded chunk generation and mesh construction utilizing Java 26 virtual threads to achieve sub-second world generation across 500+ chunks.

### Procedural 64x Pixel-Art Texture Engine & sRGB Pipeline
- In-memory procedural texture generation: All block textures (Grass, Dirt, Stone, Cobblestone, Sand, Bedrock, Oak/Birch Logs, Leaves, Diamond Ore, Water, Foliage) are computed mathematically in RAM in under 1 millisecond on launch without requiring external PNG files.
- Color space management: Texture atlas is uploaded with GL_SRGB8_ALPHA8 for automatic hardware sRGB-to-linear decoding during sampling.
- Per-vertex ambient occlusion with 4-level light curves and anisotropic diagonal flip correction.
- Top-face de-tiling with pseudo-random 90-degree UV rotations on stone, dirt, sand, and cobblestone surfaces.

---

## Lighting, Atmosphere, and Rendering Pipeline

### Continuous 24-Hour Astronomical Solar Cycle
- Real-time solar orbital engine calculating dynamic celestial coordinates across standard 24,000-tick Minecraft days (20 minutes real-time cycle).
- Day Phase: Crisp, warm directional sunlight with vibrant azure sky dome and square Minecraft Sun.
- Sunset & Sunrise (Golden Hour): Warm golden-amber low-angle direct light, fiery crimson/amber horizon haze, and twilight purple zenith.
- Night Phase: Directional cool moonlight from the opposing lunar orbit vector, glowing square Minecraft Moon disc, and procedural twinkling starfield.
- Continuous Smooth Transitions: Hermite interpolation (smoothstep) across solar elevation angles eliminates abrupt lighting snaps.

### Principled Shading and Color Management
- Linear Scene Radiance: All direct sunlight, hemisphere ambient sky fill, and ground bounce are evaluated in physical linear HDR space.
- Filmic Soft-Shoulder Tone Mapping: Extended Reinhard operator with white point W = 1.8 prevents sunlight highlight blowout while preserving deep shadow detail.
- Atmospheric Perspective Fog: Linear distance fog blended prior to tone mapping for seamless horizon integration between terrain and the sky dome.
- Strict Binary Alpha Foliage: 100% binary cutout testing (discard at alpha < 0.5) eliminates transparent depth-sorting sorting bugs.

### Physical Water Optical Model
- Per-Column Physical Depth: Vertical depth scanning encodes true water column depth (1 to 16 blocks) and shoreline adjacency into vertex attributes.
- Beer-Lambert Optical Transmission: Dynamic transmission curve (T = exp(-mu * depth)) delivers crystal-clear turquoise shorelines with visible submerged sand/gravel and rich, saturated deep navy absorption in oceans.
- Schlick Dielectric Fresnel Optics: Surface reflection (F0 = 0.02) transitions smoothly from 98% underwater transmission at normal incidence to sky ambient reflection at grazing angles.
- Continuous World-Space Wave Normals: Dual-frequency wave perturbations computed in world coordinates eliminate chunk UV boundary seams and grid artifacts.
- Distant Water Preservation: Preserves blue chroma in distant water fog, preventing distant lakes from bleaching to white/gray horizon haze.

---

## World Generation (Minecraft Java 1.17 Specification)

### Full 256 World Height
- World coordinate bounds from Y = 0 to Y = 256 (65,536 voxels per chunk).
- Active world grid spanning 529 chunks (23 x 23 chunk grid, 368 x 368 blocks horizontally), encompassing over 34.6 million voxels.

### Continental Elevation and Sea Level
- Sea Level: Standard Y = 62.
- Plains and Forest Ground Level: Y = 64 to 75.
- Rolling Hills and Valleys: Y = 75 to 90.
- Mountain Peaks and Exposed Cliffs: Y = 90 to 125+.
- River Valleys: Carved down to Y = 48 to 56 with sandy shores and riverbeds.

### Underground Strata and Mineral Distribution
- Y = 0: Solid Bedrock floor with random bedrock layers up to Y = 3.
- Y = 1 to 16: Deep Stone core with natural Diamond Ore vein distribution.
- Y = 16 to Surface - 4: Stone mantle with Cobblestone vein clusters.
- Surface - 3 to Surface - 1: Subsurface Dirt and Sand layers.
- Surface: Lush Grass Blocks above sea level, Sand along shorelines, and Stone on high mountain peaks.
- Vegetation: 3D Oak and Birch tree canopies and wild flower scatter (Poppies, Dandelions, Tall Grass).

### 3D Volumetric Cave and Cavern Generation
- Dual 3D Simplex noise samplers evaluate continuous 3D noise fields across X, Y, and Z coordinates.
- Noodle and Worm Caves: Winding 3D tunnels carved where noise field intersections satisfy c1^2 + c2^2 < 0.016 between Y = 5 and Y = surface - 4.
- Large Caverns: Volumetric cavern chambers carved where 3D noise density exceeds 0.60.
- Deep Spring Pools: Natural underground water pools generated in deep caverns between Y = 9 and Y = 11.
- Exposed Ores: Diamond and stone veins exposed naturally along cave walls, floors, and ceilings.

---

## Survival Gameplay and Physics

### Voxel DDA Raycasting and Hand Mining
- Amanatides-Woo Fast Voxel Traversal algorithm: Casts a continuous 3D ray from the player eye position along the camera look direction up to 4.5 blocks (standard survival reach).
- Block Hardness and Mining Formula: Hand mining rate follows the authentic survival equation: damagePerTick = 1.0 / (Hardness * 30.0).
  - Foliage (Tall Grass, Flowers): Instant 1-tick break.
  - Leaves: Approximately 0.3s (6 ticks).
  - Dirt and Sand: Approximately 0.75s (15 ticks).
  - Grass Block: Approximately 0.9s (18 ticks).
  - Logs: Approximately 3.0s (60 ticks).
  - Stone and Cobblestone: Approximately 7.5s to 10.0s (150 to 200 ticks).
  - Bedrock: Unbreakable.
- 10-Stage Minecraft Cracking Decals: Overlays procedural progressive fracture patterns (stages 0 through 9) across all block faces as mining progress advances from 0% to 100%.
- Wireframe Targeting Outline: 1px black bounding box rendered around the currently targeted voxel.
- Real-Time GPU Mesh Rebuilding: Broken blocks immediately update local and boundary chunk meshes and re-upload to OpenGL buffers.

### 3D Block Disintegration and Particle Physics
- Striking a block chips off 2 to 3 small debris particles oriented along the hit face normal.
- Breaking a block spawns a bursting cloud of approximately 28 3D debris fragments matching the block color palette, simulating gravity acceleration (-18.0 m/s^2), drag, and ground bounce physics.

### Dynamic Fluid Simulation and Cavity Filling
- Neighbor Block Triggers: Breaking or placing blocks adjacent to water notifies surrounding voxels.
- Downward Flow Priority: Water cascades downward vertically when air exists below.
- Horizontal Spreading: Water flows outward horizontally into adjacent empty trenches and excavated cavities up to 6 blocks from the source.
- Full 3D Water Volume Meshing: Renders all visible top, side, and bottom water faces against air.
- Swimming and Buoyancy: Player buoyancy with gentle sinking, upward swimming on Space input, and complete fall damage negation upon landing in water.

---

## Menu Interface and Video Background Engine

### Hardware GPU Video Array
- High-definition 720p (1280x720) video sequences decoded directly from MP4 files.
- Random session-persistent selection among 3 themes: Aurora Night, Coral Reef, and Cozy Campfire.
- Pre-allocated hardware 2D Texture Array (GL_TEXTURE_2D_ARRAY): All frames are resident in GPU VRAM on launch, eliminating runtime CPU-to-GPU PCI-e transfer stalls.
- Sub-Frame Temporal Crossfading: Dual-texture shader blending (mix(uFrame0, uFrame1, uBlend)) delivers fluid 60+ FPS playback.
- Dynamic Aspect Ratio Cover Scaling: UV mapping automatically adjusts to prevent distortion across any window dimension.

### Authentic Minecraft UI
- 9-slice stone button rendering matching the authentic Minecraft Java Edition user interface.
- Bold bitmap typography with drop shadows.
- World management interface with JSON-based world save persistence, slot loading, and world deletion.
- In-game Escape menu with game pause, resume, and world save & quit functionality.

---

## Controls Reference

| Input | Action |
| :--- | :--- |
| **W / A / S / D** | Move Forward / Left / Backward / Right |
| **Space** | Jump / Swim Upward in Water |
| **Left Shift** | Sneak |
| **Left Ctrl** | Sprint |
| **Mouse Move** | First-Person Camera Look |
| **Left Mouse Button (Hold)** | Mine Targeted Block by Hand |
| **Right Mouse Button** | Place Selected Hotbar Block |
| **1 - 9** | Select Hotbar Slot |
| **Mouse Scroll Wheel** | Cycle Hotbar Slots |
| **Escape** | Pause Game / Open Escape Menu |
| **F1** | Normal Full Rendering Pipeline |
| **F2** | Debug Mode: Albedo Only |
| **F3** | Debug Mode: Surface Normals |
| **F4** | Debug Mode: Direct Sunlight/Moonlight |
| **F5** | Debug Mode: Ambient Hemisphere Light |
| **F6** | Debug Mode: Baked Vertex AO |
| **F7** | Debug Mode: Total Pre-Albedo Radiance |
| **F8** | Debug Mode: Linear Pre-Tonemap HDR |
| **F9** | Debug Mode: Atmospheric Fog Factor |
| **F10** | Debug Mode: Water Base Color Gradient |
| **F11** | Debug Mode: Normalized Water Depth Map |
| **F12** | Debug Mode: Beer-Lambert Transmission (T) |

---

## System Requirements and Dependencies

- **Operating System**: Windows 10 / 11 (64-bit)
- **Java Runtime**: JDK 21+ / OpenJDK 26 (x64)
- **Graphics Hardware**: OpenGL 3.3 Core Profile compatible GPU
- **Bundled Libraries (engine/lib/)**:
  - LWJGL 3.3.3 (Core, GLFW, OpenGL, STB)
  - JOML (Java OpenGL Math Library) v1.10.8

---

## Build and Execution

### Compile Java Engine
```bash
npm run java:build
```

### Launch Application
```bash
npm start
```

---

## License

This project is open-source and available under the **MIT License**.
All Minecraft-inspired concepts, aesthetics, and audio mechanics adhere to community fair-use standards.
