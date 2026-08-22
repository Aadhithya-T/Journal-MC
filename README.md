# MC-Journal

A voxel sandbox game and 3D rendering engine written in native Java using Lightweight Java Game Library (LWJGL 3.3.3) and OpenGL 3.3 Core Profile. The project features procedural terrain generation, volumetric 3D cave networks, custom first-person viewmodel animation, voxel physics, dynamic lighting, fluid mechanics, dropped item entity physics, and persistent world state saves.

---

## Overview

MC-Journal is a standalone desktop application built directly on the JVM without external game engine frameworks. It implements custom voxel rendering, multi-threaded chunk generation, physical lighting calculations, and user interface systems directly through OpenGL shaders and GLFW window management.

---

## Core Systems & Features

### 1. Voxel Engine & Chunk Architecture
- **Chunk Geometry**: 16×16 horizontal blocks with a vertical height of 256 blocks (65,536 voxels per chunk).
- **Face Culling & Optimization**: Evaluates neighbor voxel transparency to generate and render only exposed surface geometry, minimizing draw calls.
- **Concurrent Chunk Meshing**: Asynchronous terrain generation and mesh building utilizing modern JVM multi-threading.
- **Complete Boundary Rebuilding**: Real-time mesh rebuilding and GPU buffer re-uploading across all 8 adjacent chunk neighbors and diagonal corners when blocks are placed or broken.
- **Dynamic Voxel Mutation**: Efficient runtime block modification tracking with delta-based save serialization.

### 2. Procedural World Generation
- **Terrain Elevation**: Multi-octave Simplex noise producing diverse topography including plains, rolling hills, mountains, and riverbeds.
- **Volumetric 3D Caves**: Continuous 3D noise fields carving winding underground tunnels, chambers, and submerged aquifers.
- **Stratified Geology**: Bedrock base layer, deep stone mantle embedded with mineral deposits (ores, cobblestone), and surface strata (dirt, sand, grass).
- **Vegetation & Scatter**: Procedural tree placement (oak and birch logs with clustered leaf canopies) and double-sided flora (flowers, tall grass).

### 3. Procedural Texture Atlas & Pixel Art Generation
- **In-Memory 8×8 Texture Atlas**: Runtime procedural texture synthesis creating individual 64×64 pixel tiles packed into an in-memory sRGB atlas buffer (`GL_SRGB8_ALPHA8`) with anisotropic filtering.
- **Pixel-Art Tool & Block Textures**: Exact procedural sprite matrices for terrain blocks, foliage, and tools (Iron Axe, Iron Shovel, Iron Pickaxe).
- **Texture De-Tiling**: Random deterministic face rotation on horizontal surfaces to break repetitive visual grid patterns.

### 4. Atmospheric Lighting & Shader Pipeline
- **Continuous Solar/Lunar Cycle**: 24,000-tick orbital cycle with dynamic directional sunlight/moonlight, ambient hemisphere lighting, and smooth horizon color grading.
- **Per-Vertex Ambient Occlusion**: 4-level baked vertex ambient occlusion curves computed during chunk mesh generation.
- **Optical Water Model**: Depth-based light absorption (Beer-Lambert transmission), surface Fresnel reflections, and shoreline depth attributes.
- **Tone Mapping & Atmospheric Fog**: Linear HDR color calculations passed through a Reinhard tone mapper with distance-based atmospheric fog blending.
- **Debug Shader Modes**: F1–F12 hotkeys for real-time visualization of normals, depth buffers, ambient occlusion, and lighting channels.

### 5. First-Person Viewmodel & Animation
- **Animated Viewmodel**: First-person character arm rendering with walking view-bobbing, breathing idle motion, and mining swing arcs.
- **Dynamic Held Items**: Renders held 3D miniature blocks, foliage quads, or full-scale 3D iron tools (Axe, Shovel, Pickaxe) directly in the character's hand.
- **Contextual Breaking Animations**: Downward chopping, digging, and mining swing trajectories when interacting with world blocks.

### 6. Item & Inventory Management
- **9-Slot Hotbar Inventory**: Supports stackable blocks (up to 64 units per slot) and unstackable tools.
- **Item Dropping & Throwing**: Dropping single items (`Q`) or entire held stacks (`Ctrl + Q`) with ballistic trajectories aligned to the camera look direction.
- **Ground Item Entities**: 3D floating and spinning dropped item entities with ground collision physics, pickup cooldowns, and automatic inventory collection.
- **Tool Harvest Rules**: Material-specific tool effectiveness and mining speed validation.

### 7. Voxel Physics & Player Controller
- **Player Movement**: AABB collision detection with gravity, acceleration, ground friction, jumping, sprinting, sneaking, and swimming.
- **Water Dynamics & Step-Up**: Fluid buoyancy drag, velocity deceleration, water exit jumping, and shoreline step-up ledge assistance.
- **DDA Raycast Traversal**: High-precision voxel raycasting for targeted block selection and placement.
- **Block Fracture & Particle Systems**: Multi-stage fracture crack overlay decals and 3D debris fragment particles with velocity dispersion and gravity.
- **Fluid Propagation**: Automatic horizontal and downward water spreading when adjacent voxels are removed.

### 8. User Interface & World Persistence
- **Menu Systems**: Title screen with GPU-accelerated video background playback, World Selection, World Creation, In-Game HUD with health/hunger bars, Pause Menu, and Death/Game-Over screens.
- **Font Rendering**: Crisp TrueType Font (TTF) text rasterization via STB Truetype with dynamic scaling and drop shadows.
- **JSON World Persistence**: Local save management serializing player position, health, hunger, hotbar inventory, world time, seed, and all voxel modifications.

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
| **Escape** | Pause Game / Return to Menu |
| **F1** | Standard Game Rendering |
| **F2 – F12** | Shader & Buffer Debug Visualization Modes |

---

## Project Structure

```
mc-journal/
├── engine/
│   ├── lib/                  # LWJGL 3.3.3 & JOML JAR dependencies
│   ├── resources/
│   │   ├── backgrounds/      # Menu video assets
│   │   ├── fonts/            # TTF font files
│   │   └── shaders/          # GLSL vertex & fragment shaders
│   └── src/com/mcjournal/
│       ├── Block.java               # Block definitions & physical attributes
│       ├── Chunk.java               # Voxel chunk data container
│       ├── ChunkManager.java        # Chunk storage & multi-threaded coordinator
│       ├── ChunkMeshBuilder.java    # Voxel mesh generation & AO computation
│       ├── FluidPhysicsManager.java # Fluid propagation logic
│       ├── Item.java                # Tool definitions & harvest rules
│       ├── Raycast.java             # DDA ray-traversal algorithm
│       ├── TerrainGenerator.java    # Multi-octave Simplex noise generation
│       └── client/
│           ├── BlockBreakingManager.java # Block destruction & interaction logic
│           ├── BlockSelectionRenderer.java# Wireframe outline & fracture decals
│           ├── Camera.java          # 3D view & projection matrix management
│           ├── ChunkRenderer.java   # OpenGL VAO/VBO chunk buffer management
│           ├── FirstPersonHandRenderer.java # Viewmodel, held items & animations
│           ├── ItemEntity.java      # Ground dropped item entity physics
│           ├── ItemEntityManager.java# Dropped items manager & rendering
│           ├── MCJournalApp.java    # Application entrypoint & main loop
│           ├── ParticleManager.java # Voxel debris particle system
│           ├── Player.java          # Player physics, collision & inventory
│           ├── ProceduralTextureGenerator.java # In-memory procedural texture generator
│           ├── TextureAtlas.java    # OpenGL texture atlas loader
│           ├── VideoBackgroundManager.java # Menu video playback renderer
│           ├── Window.java          # GLFW window & OpenGL context setup
│           ├── WorldSaveManager.java# World save JSON serialization
│           └── gui/                 # UI screens, HUD, fonts, and button widgets
├── docs/                     # Technical specifications & design docs
└── saves/                    # Local JSON world save files
```

---

## System Requirements

- **Operating System**: Windows 10 / 11 (64-bit), macOS, or Linux
- **Java Runtime**: JDK 21 or newer (Java 26 supported)
- **Graphics**: GPU supporting OpenGL 3.3 Core Profile
- **Build Tools**: Node.js & npm (optional helper scripts) or standard `javac`

---

## Building and Running

### 1. Compile the Engine
```bash
npm run java:build
```

### 2. Launch the Application
```bash
npm start
```

---

## License

This project is open-source and available under the [MIT License](LICENSE).
