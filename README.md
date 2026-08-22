# Minecraft Journal (MC-Journal)

A voxel sandbox game and engine written in native Java and OpenGL 3.3 Core Profile. Features procedural terrain generation, 3D cave systems, block interaction, fluid mechanics, a continuous day/night solar cycle, procedural textures, and persistent world state saves.

---

## Overview

Minecraft Journal is a standalone desktop application built directly on the JVM using Lightweight Java Game Library (LWJGL 3.3.3) bindings. The engine implements custom voxel rendering, multi-threaded chunk generation, physical lighting calculations, and user interface systems without external game engine frameworks.

---

## Features

### Voxel Engine & Chunk Management
- **Chunk Geometry**: 16x16 horizontal blocks with a vertical height of 256 blocks per chunk.
- **Mesh Generation**: Face culling that renders only exposed voxel surfaces to optimize draw calls.
- **Multi-Threading**: Asynchronous chunk generation and mesh building using modern JVM concurrency.
- **Dynamic Chunk Updates**: Real-time mesh rebuilding and GPU buffer re-uploading upon block modifications.

### Procedural World Generation
- **Terrain Elevation**: Multi-octave Simplex noise producing plains, rolling hills, mountains, and riverbeds.
- **Volumetric Caves**: 3D noise fields carving winding tunnels, chambers, and underground pools.
- **Stratified Geology**: Bedrock base, deep stone mantle with mineral deposits (diamond ore, cobblestone), and surface layers (dirt, sand, grass).
- **Surface Scatter**: Procedural tree placement (oak, birch) and double-sided wild foliage (flowers, tall grass).

### Rendering & Atmospheric Pipeline
- **Procedural 64x Texture Atlas**: Runtime-generated pixel-art textures for all block types packed into an in-memory sRGB atlas buffer (`GL_SRGB8_ALPHA8`) with anisotropic filtering.
- **Day/Night Cycle**: Continuous 24,000-tick solar orbital progression with dynamic directional sun/moonlight, ambient hemisphere lighting, and horizon color grading.
- **Tone Mapping & Fog**: Linear HDR color calculations passed through a Reinhard tone mapper with distance-based atmospheric fog blending.
- **Water Optical Model**: Depth-based light absorption (Beer-Lambert transmission), surface Fresnel reflections, and shoreline depth attributes.
- **Ambient Occlusion**: Baked per-vertex 4-level ambient occlusion curves.

### Voxel Physics & Player Mechanics
- **First-Person Controller**: Standard movement physics including walking, sprinting, sneaking, jumping, and swimming.
- **Water Dynamics & Step-Up**: Buoyancy drag, fluid entry deceleration, sprint-jumping out of water, and shoreline ledge step-up assistance.
- **Raycast Interaction**: Fast voxel traversal (DDA) for targeted block selection, hand mining with progressive fracture overlay decals, and block placement.
- **Particle System**: 3D debris fragment particles with gravity and velocity dispersion spawned during block destruction.
- **Fluid Mechanics**: Automatic horizontal and downward flow propagation when neighboring voxels are removed.

### User Interface & Persistence
- **Menu Screens**: Title screen, world selection screen, world creation screen, in-game pause menu, and game-over screen.
- **GPU Video Backgrounds**: Hardware-accelerated 2D texture array video playback on menu screens.
- **Save Management**: JSON-based world persistence storing player position, health, inventory selection, time of day, seed, and all modified blocks.

---

## Controls

| Input | Action |
| :--- | :--- |
| **W / A / S / D** | Move Forward / Left / Backward / Right |
| **Space** | Jump / Swim Upward |
| **Left Shift** | Sneak |
| **Left Ctrl** | Sprint |
| **Mouse Move** | First-Person Camera Rotation |
| **Left Mouse Button (Hold)** | Mine Targeted Block |
| **Right Mouse Button** | Place Selected Block |
| **1 - 9 / Scroll Wheel** | Select Hotbar Slot |
| **Q** | Throw / Drop 1 Item from Hand |
| **Ctrl + Q** | Drop Entire Held Stack |
| **Escape** | Pause Game / Open Game Menu |
| **F1** | Standard Rendering Mode |
| **F2 - F12** | Shader & Buffer Debug Visualization Modes |

---

## Project Structure

```
mc-journal/
├── engine/
│   ├── lib/                  # LWJGL 3.3.3 & JOML JAR dependencies
│   ├── resources/
│   │   ├── backgrounds/      # Menu video assets
│   │   └── shaders/          # GLSL vertex & fragment shaders
│   └── src/com/mcjournal/
│       ├── Block.java               # Block definitions & physical properties
│       ├── Chunk.java               # Voxel chunk data container
│       ├── ChunkManager.java        # World chunk storage & generation coordinator
│       ├── ChunkMeshBuilder.java    # Voxel mesh extraction & AO computation
│       ├── FluidPhysicsManager.java # Fluid propagation logic
│       ├── Raycast.java             # DDA ray-traversal algorithm
│       ├── TerrainGenerator.java    # Multi-octave Simplex noise generation
│       └── client/
│           ├── Camera.java          # 3D view & projection matrices
│           ├── ChunkRenderer.java   # OpenGL chunk buffer management
│           ├── MCJournalApp.java    # Main game loop & window management
│           ├── Player.java          # Player physics & collision detection
│           ├── ProceduralTextureGenerator.java # In-memory 64x texture generator
│           ├── TextureAtlas.java    # OpenGL texture atlas loader
│           ├── WorldSaveManager.java# Save/load JSON serialization
│           └── gui/                 # Screen implementations & GUI rendering
├── docs/                     # Technical specifications & calibration reports
└── saves/                    # Local world save files
```

---

## System Requirements

- **Operating System**: Windows 10 / 11 (64-bit)
- **Java Runtime**: JDK 21 or newer (Java 26 supported)
- **Graphics**: GPU supporting OpenGL 3.3 Core Profile
- **Node.js / npm**: For build script execution (optional; standard `javac` commands can also be used)

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
