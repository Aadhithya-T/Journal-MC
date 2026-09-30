# MC-Journal

A high-performance voxel sandbox game and 3D rendering engine built in native Java using the Lightweight Java Game Library (LWJGL 3.3.3) and OpenGL 3.3 Core Profile.

MC-Journal is written directly on the JVM without external game engine dependencies. It implements custom voxel rendering pipelines, continuous coordinate-based world streaming, 2D greedy meshing, binary region persistence, fixed-step 20 TPS physics, and comprehensive automated test suites.

---

## Feature Status Matrix

| Subsystem | Feature | Status | Details |
| :--- | :--- | :---: | :--- |
| **Streaming** | Infinite Chunk Streaming | **Implemented** | Prioritized spiral loading based on player distance and camera look angle. |
| **Streaming** | Spawn Neighborhood Loading | **Implemented** | Synchronous 7x7 core chunk generation and GPU mesh upload at spawn. |
| **Streaming** | Distance-Based Unloading | **Implemented** | Automatic eviction beyond `renderDistance + 3` with GPU buffer deallocation. |
| **Meshing** | 2D Greedy Meshing | **Implemented** | Adjacent identical face merging with 4-level Ambient Occlusion. |
| **Meshing** | Packed Interleaved Geometry | **Implemented** | 52-byte interleaved VBO layout with 16-bit/32-bit index buffers (EBO). |
| **Rendering** | Budgeted GPU Uploads | **Implemented** | Per-frame upload count and byte budgets to prevent frame drops. |
| **Rendering** | Frustum & Distance Culling | **Implemented** | 6-plane view-frustum tests combined with radial distance checks. |
| **Rendering** | Occlusion Culling | *In Progress* | Hierarchical Z-Buffer depth testing for complex caves and mountains. |
| **Rendering** | Water Fresnel & Absorption | **Implemented** | Stylized shoreline absorption (Beer-Lambert) and Schlick's Fresnel reflection. |
| **Atmosphere** | Continuous Solar Orbit | **Implemented** | 24,000-tick astronomical solar cycle with dynamic day/twilight/night grading. |
| **Block System** | Immutable BlockState Model | **Implemented** | Precomputed state permutations, property transitions (`with()`), and fast state IDs. |
| **Physics** | Fixed-Step 20 TPS Physics | **Implemented** | Authoritative fixed timestep, AABB collision, gravity, jump impulse, and step-up. |
| **Physics** | Fall Damage Calculation | **Implemented** | Threshold-based fall damage ($>3.5$ blocks) and water landing damage negation. |
| **Fluids** | Batched Fluid Dynamics | **Implemented** | Level-based fluid states ($0..7$), scheduled deduplicated queue, and atomic batch mutations. |
| **Fluids** | Full Cellular Automata | *In Progress* | Multi-source infinite horizontal pressure propagation. |
| **Raycasting** | DDA Voxel Traversal | **Implemented** | Amanatides & Woo fast voxel traversal returning structured `RaycastHit` objects. |
| **Persistence** | Binary `.jmc` Format | **Implemented** | Version 3 binary paletted chunk serialization inside 32x32 region files. |
| **Persistence** | Asynchronous World Saving | **Implemented** | Non-blocking dirty chunk flushing and background `world.jmc` metadata writes. |
| **Persistence** | Legacy Migration | **Implemented** | Automatic detection and migration from legacy JSON saves to version 3 binary `.jmc`. |
| **Instrumentation** | Real-Time Telemetry & F3 HUD | **Implemented** | Live FPS, TPS, chunk generation/meshing/upload times, draw calls, and vertex counts. |
| **Gameplay** | Survival & Inventory | **Implemented** | Hotbar selection, tool harvesting efficiencies, Q key item throwing with ballistics. |
| **Gameplay** | Entity AI & Mobs | *Planned* | Pathfinding, hostility states, and mob entity simulation. |
| **Gameplay** | Crafting Grid | *Planned* | 2x2 player crafting inventory and 3x3 crafting workbench. |

---

## Architecture Overview

### 1. Application Coordination (`MCJournalApp`)
The top-level application coordinator is aggressively decoupled and centered on three primary lifecycle methods:
```java
public void tick()       // Authoritative 20 TPS game logic
public void render()     // Interpolated multi-pass frame rendering
public void shutdown()   // Safe persistence flush and GPU resource cleanup
```
Specific responsibilities are delegated to dedicated subsystems:
- `GameInputSystem`: Mouse look, hotbar controls, item drop throwing.
- `WorldSession`: Chunk streaming, fluid physics, block breaking, item entities.
- `GameRenderer`: 3D solid/cutout/water passes, sky dome, HUD, viewmodel hand.
- `ScreenManager`: GUI menus, cursor capture, and window resize events.
- `AtmosphericTimeSystem`: Astronomical solar kinematics and dynamic lighting colors.
- `DebugController`: Shader inspection modes (F1–F12) and F3 telemetry overlay.

### 2. Thread Safety & Concurrency
The engine maintains strict thread separation to guarantee high performance and avoid race conditions:
- **Game Thread (Main)**: Authoritative 20 TPS simulation, input processing, and world state mutations.
- **Worker Pool (`ChunkWorker-N`)**: Parallel procedural terrain generation and greedy voxel meshing.
- **Render Thread (Main)**: Sole owner of the OpenGL 3.3 context; executes budgeted GPU buffer uploads and draw calls.
- **Persistence Worker (`WorldSave-Worker`)**: Single-threaded background I/O for binary chunk region writes.

See [CONCURRENCY.md](CONCURRENCY.md) for detailed invariants and thread safety contracts.

---

## Controls

| Key / Input | Action |
| :--- | :--- |
| **W / A / S / D** | Move Forward / Left / Backward / Right |
| **Space** | Jump / Swim Upward |
| **Left Shift** | Sneak |
| **Left Ctrl** | Sprint |
| **Mouse Move** | First-Person Camera Look |
| **Left Mouse Button** | Mine / Break Targeted Block |
| **Right Mouse Button** | Place Selected Block |
| **1 – 9 / Scroll Wheel** | Select Hotbar Slot |
| **Q** | Drop 1 Item from Hand |
| **Ctrl + Q** | Drop Entire Held Stack |
| **F3** | Toggle Engine Performance Telemetry Overlay |
| **F1 – F12** | Shader & Buffer Debug Visualization Modes |
| **Escape** | Pause Game / Return to Menu |

---

## Performance Telemetry Overlay (F3)

Pressing **F3** activates the live engine telemetry card:

```text
--- ENGINE ---
FPS: 144
TPS: 20
Chunks: 441
Visible: 87
Generating: 4
Meshing: 2
Vertices: 1.2M
GPU uploads: 3
Fluid updates: 18
```

---

## Building, Running, and Testing

### 1. Build All Sources
```powershell
$files = Get-ChildItem -Path engine/src -Filter *.java -Recurse | Select-Object -ExpandProperty FullName
javac -cp "engine/lib/*" -d engine/bin $files
```

### 2. Run the Master Automated Test Suite
```powershell
java -ea -cp "engine/bin;engine/lib/*" com.mcjournal.test.MasterTestSuite
```

### 3. Launch the Game
```powershell
java -cp "engine/bin;engine/lib/*" com.mcjournal.client.MCJournalApp
```

---

## Project Structure

```
mc-journal/
├── CONCURRENCY.md                 # Thread safety rules and synchronization invariants
├── engine/
│   ├── lib/                       # LWJGL 3.3.3 & JOML dependencies
│   ├── resources/                 # Shaders, fonts, textures
│   └── src/com/mcjournal/
│       ├── EngineConstants.java   # Centralized engine geometry, physics & timing constants
│       ├── Chunk.java             # Voxel chunk container (16x256x16)
│       ├── ChunkManager.java      # Multi-threaded streaming coordinator
│       ├── ChunkMesher.java       # Asynchronous 2D greedy mesh builder
│       ├── ChunkNeighborhood.java # Direct 3x3 array voxel access for meshing
│       ├── ChunkPos.java          # Immutable chunk coordinate value record
│       ├── ChunkSerializer.java   # Binary paletted Deflate serialization (v3)
│       ├── RegionManager.java     # 32x32 chunk region disk manager (.jmc)
│       ├── RegionPos.java         # Region coordinate value record
│       ├── block/                 # Immutable BlockState & Registry system
│       ├── physics/               # PhysicsSystem and CollisionDetector
│       ├── client/                # MCJournalApp, GameRenderer, WorldSession, Input
│       └── test/                  # 15 automated unit & integration test suites
└── saves/                         # Binary .jmc region files & world metadata
```

---

## License

This project is open-source and available under the [MIT License](LICENSE).
