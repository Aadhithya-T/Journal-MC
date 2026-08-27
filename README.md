# MC-Journal

A high-performance voxel sandbox game and 3D rendering engine written in native Java using the Lightweight Java Game Library (LWJGL 3.3.3) and OpenGL 3.3 Core Profile. The project features infinite multi-threaded chunk streaming, region-based disk persistence, an immutable block state property system, 6-plane view-frustum culling, procedural terrain generation, volumetric 3D caves, dynamic lighting, fluid mechanics, dropped item physics, first-person viewmodel animations, and in-game video settings.

---

## Overview

MC-Journal is built directly on the JVM without external game engine frameworks. It implements custom voxel rendering pipelines, coordinate-based world streaming, physical lighting calculations, and user interface systems directly through OpenGL shaders, GLFW window management, and native multi-threading.

---

## Core Systems & Architecture

### 1. Infinite World Streaming & Chunk Manager
- **Coordinate-Based Chunk System**: Uses an immutable `ChunkPos` value record representing $(cx, cz)$ chunk coordinates for zero-allocation hashing and spatial arithmetic.
- **On-Demand Streaming**: Dynamically loads and generates chunks around the player as they explore in any direction without fixed world boundaries.
- **Multi-Threaded Worker Pool**: Offloads procedural terrain generation and greedy mesh building to a dedicated JVM background worker thread pool (`Executors.newFixedThreadPool`).
- **Closest-First Spiral Generation**: Queues chunks in an outward spiral sorted by Euclidean distance, with directional look-cone biasing to prioritize chunks in front of the camera.
- **Distance-Based Unloading & GPU Cleanup**: Automatically unloads chunks beyond $\text{renderDistance} + 3$, queueing GPU vertex buffer deallocations to prevent memory leaks and VRAM fragmentation.
- **Offline Voxel Delta Retention**: Stores block modifications made to unloaded or ungenerated chunks and applies them seamlessly when the chunks stream into memory.

### 2. BlockState & Property System (`com.mcjournal.block`)
- **BlockType & BlockState Abstraction**: Decouples physical block definitions from runtime state instances, enabling extensible block properties without inflating memory footprints.
- **Dynamic Property Permutations**:
  - **Axis Property**: Directional 3-axis alignment (`X`, `Y`, `Z`) for wood logs, dynamically swapping ring and bark textures based on face orientation.
  - **Level Property**: Fluid depth levels (`0` to `7`) for flowing and falling water.
  - **Snowy Property**: Binary top-cover states for snow-capped terrain.
- **State Transition Graph**: Precomputes and caches immutable state transitions (`state.with(property, value)`) for $O(1)$ lock-free state swaps.
- **Fast Registry & Parsing**: Supports serializing and parsing block states by unique integer IDs, human-readable strings (`oak_log[axis=x]`), and legacy numeric identifiers.

### 3. View-Frustum Culling & Dynamic Atmosphere
- **6-Plane View-Frustum Culling (`FrustumCuller`)**: Extracts normalized frustum planes from the camera's combined View-Projection matrix each frame, performing fast $O(1)$ AABB intersection tests on $16 \times 256 \times 16$ chunk bounding boxes to discard out-of-view geometry.
- **Render-Distance Calibrated Horizon Fog**: Atmospherically blends distant chunks into horizon haze calibrated directly to the active render distance:
  $$\text{uFogStart} = (\text{renderDistance} - 2.5) \times 16.0f, \quad \text{uFogEnd} = (\text{renderDistance} - 0.5) \times 16.0f$$
  This completely eliminates chunk generation pop-in at the horizon.
- **Continuous Solar & Atmospheric Cycle**: 24,000-tick continuous solar orbital cycle with dynamic directional sun/moonlight, ambient hemisphere lighting, and smooth day/twilight/night color grading.
- **Per-Vertex Ambient Occlusion**: 4-level baked vertex ambient occlusion curves computed during chunk mesh building.
- **Optical Water Model**: Depth-based light absorption (Beer-Lambert transmission), surface Fresnel reflections, and shoreline depth attributes.

### 4. Region-Based World Persistence (`.jmc`)
- **$32 \times 32$ Chunk Region Files**: Stores up to 1,024 chunks ($512 \times 512$ blocks) per region file in `saves/<world>/regions/r.X.Z.jmc`, preventing directory bloat and OS file handle limits.
- **4 KB Sector Allocation**: Employs an 8 KB header lookup table (4 KB sector offsets + 4 KB payload lengths) for fast $O(1)$ random-access chunk seek, read, and write operations.
- **Deflate Binary Chunk Compression**: Compresses raw 128 KB chunk voxel state arrays down to **180–4,000 bytes** per chunk payload using zlib Deflate streams.
- **Non-Blocking Dirty Flushing**: Asynchronously serializes and flushes modified chunks to disk when evicted by distance or upon world save.
- **Disk-First Loading Pipeline**: Loads existing chunks from region files on disk before falling back to procedural generation.

### 5. In-Game Video Settings & Framerate Limiting
- **Options Screen**: Accessible from the Title Screen and the in-game Pause (Escape) Menu.
- **Framerate Limiter**: Supports `VSync (60 FPS)`, `30 FPS`, `60 FPS`, `90 FPS`, `120 FPS`, `144 FPS`, `240 FPS`, or `Unlimited` with sub-millisecond thread sleep throttling.
- **Live Render Distance Scaling**: Dynamically adjusts render distance ($4$ to $24$ Chunks) and resizes chunk streaming buffers in real-time.
- **Field of View (FOV)**: Configurable from $60^\circ$ (Normal) up to $110^\circ$ (Quake Pro).
- **HUD Performance Metrics**: Real-time color-coded FPS counter and active chunk metrics (`C: rendered/loaded`) displayed in the top-left HUD.
- **Settings Persistence**: Saves user preferences to `options.json`.

### 6. Procedural World Generation
- **Terrain Elevation**: Multi-octave Simplex noise producing diverse topography including plains, rolling hills, mountains, and ocean basins.
- **Volumetric 3D Caves**: Continuous 3D noise fields carving winding underground tunnels, chambers, and submerged aquifers.
- **Stratified Geology**: Bedrock base layer ($Y = 0$), deep stone mantle embedded with mineral deposits (Diamond, Cobblestone), and surface strata (dirt, sand, grass).
- **Vegetation & Scatter**: Procedural tree placement (Oak and Birch logs with clustered leaf canopies) and double-sided flora (flowers, tall grass).

### 7. First-Person Viewmodel, Camera Shake & Player Controller
- **Player Physics**: AABB collision detection with gravity, acceleration, ground friction, jumping, sprinting, sneaking, and swimming.
- **Damage Camera Tilt & Screen Shake**: Taking damage triggers a 10-tick hurt timer that tilts the camera view matrix via a smooth sine pulse curve ($\text{roll} = \sin(\text{hurtFraction} \cdot \pi) \cdot \text{hurtAngle}$).
- **Red Damage Vignette Overlay**: Renders a dynamic crimson damage flash overlay that fades smoothly as health regenerates.
- **HUD Heart Jitter**: Hardcore hearts rapidly jitter when taking damage or on critical health ($\le 2$ hearts).
- **Animated Viewmodel**: First-person character arm rendering with walking view-bobbing, breathing idle motion, and mining swing trajectories.
- **Ground Item Entities**: 3D floating and spinning dropped item entities with ground collision physics, pickup cooldowns, and automatic magnet collection.
- **Fluid Mechanics**: Automatic horizontal and downward water spreading when adjacent voxels are removed.

---

## Controls

| Key / Input | Action |
| :--- | :--- |
| **W / A / S / D** | Move Forward / Left / Backward / Right |
| **Space** | Jump / Swim Upward |
| **Left Shift** | Sneak |
| **Left Ctrl** | Sprint |
| **Mouse Move** | First-Person Camera Look |
| **Left Mouse Button (Hold)** | Mine / Break Targeted Block |
| **Right Mouse Button** | Place Selected Block |
| **1 – 9 / Scroll Wheel** | Select Hotbar Slot |
| **Q** | Drop 1 Item from Hand |
| **Ctrl + Q** | Drop Entire Held Stack |
| **Escape** | Pause Game / Options / Return to Menu |
| **F1** | Standard Game Rendering |
| **F2 – F9** | Shader & Buffer Debug Visualization Modes (Normals, Albedo, AO, Fog) |

---

## Project Structure

```
mc-journal/
├── engine/
│   ├── lib/                       # LWJGL 3.3.3 & JOML JAR dependencies
│   ├── resources/
│   │   ├── backgrounds/           # Menu video background assets
│   │   ├── fonts/                 # TTF font files
│   │   └── shaders/               # GLSL vertex & fragment shaders
│   └── src/com/mcjournal/
│       ├── block/                 # BlockState & Property System
│       │   ├── property/          # Property abstractions (Axis, Property)
│       │   ├── BlockProperties.java # Property constants (AXIS, LEVEL, SNOWY)
│       │   ├── BlockState.java    # Immutable block state container
│       │   ├── BlockStateRegistry.java # State lookup & permutation registry
│       │   ├── BlockType.java     # Block physical attributes & atlas mapping
│       │   └── Blocks.java        # Block registration catalog
│       ├── test/
│       │   └── ChunkStreamingTest.java # Automated streaming & persistence test suite
│       ├── Block.java             # Legacy block definitions
│       ├── Chunk.java             # Voxel chunk container (16x256x16)
│       ├── ChunkManager.java      # Infinite chunk streaming coordinator
│       ├── ChunkMeshBuilder.java  # Voxel mesh building & AO computation
│       ├── ChunkPos.java          # Immutable chunk coordinate record (cx, cz)
│       ├── ChunkSerializer.java   # Binary Deflate chunk compression
│       ├── FluidPhysicsManager.java # Fluid propagation logic
│       ├── Item.java              # Tool definitions & harvest rules
│       ├── Raycast.java           # DDA voxel ray-traversal algorithm
│       ├── RegionFile.java        # 32x32 chunk region file manager (.jmc)
│       ├── RegionManager.java     # Open region file cache & coordinator
│       ├── RegionPos.java         # Region coordinate record (rx, rz)
│       ├── TerrainGenerator.java  # Multi-octave Simplex noise generator
│       └── client/
│           ├── BlockBreakingManager.java # Block destruction & interaction logic
│           ├── BlockSelectionRenderer.java # Wireframe outline & fracture decals
│           ├── Camera.java        # 3D view, roll tilt, and projection matrices
│           ├── ChunkRenderer.java # OpenGL VAO/VBO chunk buffer management
│           ├── FirstPersonHandRenderer.java # Viewmodel & held item animations
│           ├── FrustumCuller.java # 6-plane view-frustum AABB culler
│           ├── GameSettings.java  # Video settings & framerate limiter config
│           ├── ItemEntity.java    # Ground dropped item entity physics
│           ├── ItemEntityManager.java # Dropped items manager & rendering
│           ├── MCJournalApp.java  # Application entrypoint & main loop
│           ├── ParticleManager.java # Voxel debris particle system
│           ├── Player.java        # Player physics, collision, damage & inventory
│           ├── ProceduralTextureGenerator.java # In-memory procedural texture synthesis
│           ├── TextureAtlas.java  # OpenGL texture atlas loader
│           ├── VideoBackgroundManager.java # Menu video playback renderer
│           ├── Window.java        # GLFW window, VSync & OpenGL context setup
│           ├── WorldSaveManager.java # World save JSON serialization
│           └── gui/               # UI screens, HUD, fonts, and widgets
│               ├── Button.java
│               ├── EscapeMenuScreen.java
│               ├── FontRenderer.java
│               ├── GameOverScreen.java
│               ├── GuiRenderer.java
│               ├── HardcoreHUD.java
│               ├── MinecraftLogoRenderer.java
│               ├── OptionsScreen.java
│               ├── Screen.java
│               ├── TitleScreen.java
│               ├── WorldCreateScreen.java
│               ├── WorldEditScreen.java
│               └── WorldSelectScreen.java
├── docs/                          # Technical specifications & design docs
└── saves/                         # Local world region files & JSON saves
```

---

## System Requirements

- **Operating System**: Windows 10 / 11 (64-bit), macOS, or Linux
- **Java Runtime**: JDK 21 or newer (Java 26 supported)
- **Graphics**: GPU supporting OpenGL 3.3 Core Profile
- **Build Tools**: Node.js & npm (optional helper scripts) or standard `javac`

---

## Building, Testing, and Running

### 1. Compile the Engine
```bash
npm run java:build
```

### 2. Run the Automated Test Suite
```bash
npm run java:test
```

### 3. Launch the Application
```bash
npm start
# or
npm run java:run
```

---

## License

This project is open-source and available under the [MIT License](LICENSE).
