# Journal-MC

A high-performance voxel engine and 3D journaling environment built in native Java using the Lightweight Java Game Library (LWJGL 3.3.3) and OpenGL 3.3 Core Profile.

Journal-MC is developed directly on the JVM without external game engine dependencies. It implements custom voxel rendering pipelines, coordinate-based chunk streaming, 2D greedy meshing, binary region persistence, fixed-step 20 TPS physics, and comprehensive automated test suites.

---

## Feature Status Matrix

| Subsystem | Feature | Status | Details |
| :--- | :--- | :---: | :--- |
| **Streaming** | Chunk Streaming | **Implemented** | Prioritized spiral loading based on player distance and camera look angle. |
| **Streaming** | Spawn Neighborhood Loading | **Implemented** | Synchronous core chunk generation and GPU mesh upload at spawn. |
| **Streaming** | Distance-Based Unloading | **Implemented** | Automatic eviction beyond `renderDistance + 3` with GPU buffer deallocation. |
| **Meshing** | 2D Greedy Meshing | **Implemented** | Adjacent identical face merging with 4-level Ambient Occlusion. |
| **Meshing** | Packed Interleaved Geometry | **Implemented** | 52-byte interleaved VBO layout with 16-bit/32-bit index buffers (EBO). |
| **Rendering** | Budgeted GPU Uploads | **Implemented** | Per-frame upload count and byte budgets to prevent frame drops. |
| **Rendering** | Frustum & Distance Culling | **Implemented** | 6-plane view-frustum tests combined with radial distance checks. |
| **Rendering** | Water Fresnel & Absorption | **Implemented** | Stylized shoreline absorption (Beer-Lambert) and Schlick's Fresnel reflection. |
| **Atmosphere** | Continuous Solar Orbit | **Implemented** | 24,000-tick astronomical solar cycle with dynamic day/twilight/night grading. |
| **Block System** | Immutable BlockState Model | **Implemented** | Precomputed state permutations, property transitions (`with()`), and fast state IDs. |
| **Block System** | Procedural Texture Atlas | **Implemented** | Procedural 64x pixel-art atlas with mipmapping and anisotropic filtering. |
| **Physics** | Fixed-Step 20 TPS Physics | **Implemented** | Authoritative fixed timestep, AABB collision, gravity, jump impulse, and step-up. |
| **Physics** | Exploration Physics | **Implemented** | Peaceful exploration model without health/hunger depletion or fall damage. |
| **Fluids** | Batched Fluid Dynamics | **Implemented** | Level-based fluid states ($0..7$), scheduled deduplicated queue, and atomic batch mutations. |
| **Raycasting** | DDA Voxel Traversal | **Implemented** | Amanatides & Woo fast voxel traversal returning structured `RaycastHit` objects. |
| **Interaction** | Non-Mining World Protection | **Implemented** | World blocks exist as permanent environment; mining and breaking are disabled. |
| **Interaction** | Block Placement | **Implemented** | RMB block placement with axis orientation, player collision validation, and hand swing. |
| **Interaction** | Item Pickup & Inventory | **Implemented** | Floating item entities with buoyancy, bobbing, and automatic pickup into 9-slot hotbar. |
| **Persistence** | Binary `.jmc` Format | **Implemented** | Version 4 binary paletted chunk serialization inside 32x32 region files (2500x2500x256 finite world). |
| **Persistence** | Asynchronous World Saving | **Implemented** | Non-blocking dirty chunk flushing and background `world.jmc` metadata writes. |
| **Persistence** | Backward Compatibility | **Implemented** | Automatic migration from legacy JSON saves with obsolete tool ID sanitization. |
| **Journal Readiness** | Metadata, Clock & Context | **Implemented** | UUID world identity, persistent logical clock, decoupled event bus, isolated storage, landmarks. |
| **Instrumentation** | Real-Time Telemetry & F3 HUD | **Implemented** | Live FPS, TPS, chunk generation/meshing/upload times, draw calls, and vertex counts. |
| **GUI & HUD** | Scaled Logical GUI | **Implemented** | Virtual coordinate layout engine, pixel font rendering, world chronicle viewer, modal dialogs. |

---

## Architecture Overview

### 1. Application Coordination (`MCJournalApp`)
The top-level application coordinator is decoupled and centered on three primary lifecycle methods:
```java
public void tick()       // Authoritative 20 TPS game logic
public void render()     // Interpolated multi-pass frame rendering
public void shutdown()   // Safe persistence flush and GPU resource cleanup
```

Specific responsibilities are delegated to dedicated subsystems:
- `GameInputSystem`: Mouse look, movement kinematics, hotbar selection, and interaction routing.
- `WorldSession`: Finite world lifecycle, chunk streaming, fluid simulation, item entities, and context provider.
- `BlockPlacementManager`: Raycast block targeting, orientation solving, player collision rejection, and placement.
- `ItemEntityManager`: Floating item entities, buoyant water physics, bobbing animation, and player pickup.
- `GameRenderer`: 3D solid/cutout/water passes, sky dome, HUD, viewmodel hand, and selection outline.
- `ScreenManager`: GUI menus, cursor capture, modal dialogs, and window resize events.
- `AtmosphericTimeSystem`: Astronomical solar kinematics and dynamic lighting colors.
- `WorldClock`: Persistent logical in-game clock tracking solar cycles, days, and formatted time.
- `EventBus`: Lightweight, type-safe event dispatcher for lifecycle, movement, block placement, and day changes.
- `EngineContext`: Decoupled world location, biome, time, and landmark query facade for application layers.
- `WorldStorageDirectory`: Path-traversal-safe, world-isolated storage with atomic file writing.
- `LandmarkRegistry`: Discoverable world landmarks with proximity queries and persistent progression.
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
| **Right Mouse Button** | Place Selected Block |
| **1 – 9 / Scroll Wheel** | Select Hotbar Slot |
| **J** | Open Journal & World Chronicle Screen |
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
npm run build
```
*(or via javac directly)*
```powershell
npm run java:build
```

### 2. Run the Automated Test Suite
```powershell
npm run java:test
```
*(or via java directly)*
```powershell
java -ea -cp "engine/bin;engine/resources;engine/lib/*" com.mcjournal.test.MasterTestSuite
```

### 3. Launch the Application
```powershell
npm run start
```
*(or via java directly)*
```powershell
java -cp "engine/bin;engine/resources;engine/lib/*" com.mcjournal.client.MCJournalApp
```

---

## Project Structure

```
mc-journal/
├── CONCURRENCY.md                 # Thread safety rules and synchronization invariants
├── FINITE_WORLD_IMPLEMENTATION.md # Finite world boundary architecture & region grid specs
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
│       ├── ChunkSerializer.java   # Binary paletted Deflate serialization
│       ├── RegionManager.java     # 32x32 chunk region disk manager (.jmc)
│       ├── RegionPos.java         # Region coordinate value record
│       ├── block/                 # Immutable BlockState & Registry system
│       ├── event/                 # Decoupled EventBus and engine event types
│       ├── physics/               # PhysicsSystem and CollisionDetector
│       ├── storage/               # WorldStorageDirectory and atomic file persistence
│       ├── world/                 # World bounds, configuration, biomes, terrain, structures
│       │   ├── context/           # WorldLocation, WorldContext, and EngineContext
│       │   ├── landmark/          # WorldLandmark and LandmarkRegistry
│       │   └── time/              # Persistent WorldClock
│       ├── client/                # MCJournalApp, GameRenderer, WorldSession, BlockPlacementManager
│       └── test/                  # 34 automated unit & integration test suites
└── saves/                         # Binary .jmc region files & world metadata
```

---

## Typography & Third-Party Font Credits

The GUI subsystem utilizes open-source pixel typefaces under the [SIL Open Font License 1.1](http://scripts.sil.org/OFL):
- **Monocraft**: Created by [Idrees Hassan (IdreesInc)](https://github.com/IdreesInc/Monocraft). Used as the primary pixel typeface across Journal-MC menus, HUD, buttons, and world listings.
- **Minecraft-Font**: Created by [Idrees Hassan (IdreesInc)](https://github.com/IdreesInc/Minecraft-Font). Proportional pixel typeface available as an alternate profile via FontRenderer.FontConfig.

---

## License

This project is open-source and available under the [MIT License](LICENSE).
