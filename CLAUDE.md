# 🎮 Minecraft Journal (MC-Journal) — Complete System Architecture & Reference Manual

> **Purpose**: This document provides an exhaustive, line-by-line technical specification of every component, mathematical model, rendering algorithm, physics engine, and backend service in the **Minecraft Journal** codebase.

---

## 🏛️ 1. High-Level Architecture

```
                                  ┌──────────────────────────────────────────────────────────┐
                                  │                ELECTRON DESKTOP RUNTIME                  │
                                  │  (Chromium Window + Node.js Native Context + GPU Flags)  │
                                  └─────────────┬──────────────────────────────┬─────────────┘
                                                │                              │
                                                ▼                              ▼
                 ┌──────────────────────────────────────────────┐  ┌──────────────────────────────────────────┐
                 │       FRONTEND: REACT 19 + THREE.JS          │  │     BACKEND: JAVA 26 VOXEL ENGINE        │
                 │                                              │  │                                          │
                 │  • MinecraftWorldCanvas (Render Loop / FPP)  │  │  • ForkJoinPool Multi-Core Chunk Gen     │
                 │  • ChunkMeshBuilder (AO / Face Culling)      │  │  • Virtual-Threaded HTTP Server (:8088)  │
                 │  • TextureAtlas (16-Tile Procedural Canvas)  │  │  • 3D Strata Simplex Noise + Biomes      │
                 │  • FirstPersonHand (View Bobbing / Swing)    │  │  • Thread-Safe Voxel Modification Array  │
                 │  • WorldHUD (10 Hearts / Hunger / Hotbar)    │  │  • Winding River & Forest Generator      │
                 │  • BookAndQuillModal (Journaling UI)         │  └──────────────────────────────────────────┘
                 └──────────────────────────────────────────────┘
```

The system operates as a high-performance **hybrid local architecture**:
1. **Desktop Shell (`electron/`)**: Spawns the dedicated Java 26 virtual-threaded microservice, enables GPU rasterization, and hosts the React Three.js frontend with sub-millisecond local IPC.
2. **Java 26 Voxel Microservice (`engine/`)**: High-throughput multi-threaded chunk generation computing 3D simplex noise strata, river curves, and tree structures across a $10 \times 10$ chunk grid (160x160 blocks).
3. **Three.js Graphics & Physics Pipeline (`src/components/world/`)**: Executes exposed face culling, per-vertex ambient occlusion (smooth lighting), half-texel UV insetting, 18 FPS sinusoidal water animation, and axis-separated wall-sliding physics.

---

## 📂 2. File & Directory Breakdown

```
mc-journal/
├── electron/
│   ├── main.cjs                   # Desktop app entry, GPU flags, Java engine spawning, F11 fullscreen
│   └── preload.cjs                # Secure desktop context bridge (IPC)
├── engine/                        # Java 26 Multithreaded Voxel Engine
│   └── src/com/mcjournal/
│       ├── Block.java             # Block IDs (0..15), names, hex colors, solidity flags
│       ├── SimplexNoise.java      # 2D/3D Simplex noise generator with permutation tables
│       ├── Chunk.java             # 16x16x32 flat byte[] voxel buffer & coordinate packing
│       ├── TerrainGenerator.java  # Winding river valley math, strata noise, Oak/Birch canopies
│       ├── ChunkManager.java      # ForkJoinPool parallel generator, spatial indexing, raycasting
│       └── ChunkServer.java       # Embedded virtual-threaded HTTP microservice on 127.0.0.1:8088
├── src/
│   ├── components/
│   │   ├── background/            # Ambient background selectors & video canvas
│   │   ├── journal/               # Web/Desktop journal editors, cards, and export tools
│   │   ├── landing/               # Menu screens, navigation bars, and world selectors
│   │   ├── ui/                    # Authentic Minecraft styled UI buttons and panels
│   │   └── world/                 # 3D Voxel World Core
│   │       ├── BookAndQuillModal.jsx # In-game 2-page leather book journal editor
│   │       ├── EscapeMenuModal.jsx   # Game pause menu, texture pack selector, options
│   │       ├── FirstPersonHand.js    # Bare Steve fist viewmodel, view-bobbing, swing arc
│   │       ├── MinecraftWorldCanvas.jsx # Main 60-144 FPS game loop, DDA raycaster, camera
│   │       ├── SteveCharacter.js     # 3D third-person Steve avatar mesh & limb animations
│   │       ├── WorldHUD.jsx          # 10 Hearts, 10 Hunger, XP Bar, 9-Slot Hotbar, Crosshair
│   │       ├── texturePacks.js       # Texture pack presets (Faithful 64x, Bare Bones, etc.)
│   │       └── chunk/
│   │           ├── Chunk.js          # JS-side Chunk container & neighbor mesh links
│   │           ├── ChunkManager.js   # 100-chunk spatial manager, DDA raycaster, AABB collision
│   │           ├── ChunkMeshBuilder.js # Face culling, AO, anisotropic diagonal flip, de-tiling
│   │           ├── JavaEngineClient.js # Async HTTP client bridge to 127.0.0.1:8088
│   │           ├── TerrainGenerator.js # JS fallback terrain generator (identical to Java)
│   │           └── TextureAtlas.js   # 4x4 procedural atlas, water animation, transparent alpha
│   ├── context/
│   │   ├── BackgroundContext.jsx  # Background video/theme provider
│   │   └── WorldContext.jsx       # Worlds state, active world, entries, Supabase sync
│   ├── lib/
│   │   └── supabase.js            # Supabase cloud database client & offline local fallback
│   ├── App.jsx                    # Root view switcher (Landing -> 3D World -> Journal)
│   ├── index.css                  # Global Minecraft pixelated UI stylesheets and fonts
│   └── main.jsx                   # React 19 root bootstrap
├── package.json                   # Desktop scripts (npm start -> Java + Electron)
└── vite.config.js                 # Relative base path for native desktop asset resolution
```

---

## ☕ 3. Java 26 Voxel Engine (`engine/`)

### `Block.java`
Defines the byte IDs for every block in the world:
- `AIR = 0`, `GRASS = 1`, `DIRT = 2`, `STONE = 3`, `COBBLESTONE = 4`, `SAND = 5`, `BEDROCK = 6`, `OAK_LOG = 7`, `OAK_LEAVES = 8`, `DIAMOND_ORE = 9`, `WATER = 10`, `TALL_GRASS = 11`, `POPPY = 12`, `DANDELION = 13`, `BIRCH_LOG = 14`, `BIRCH_LEAVES = 15`.
- `isSolid(type)`: Returns `true` if `type != AIR && type != WATER && type < TALL_GRASS`.

### `Chunk.java`
Stores a $16 \times 16 \times 32$ voxel section ($8,192$ bytes total):
- Internal flat array: `byte[] blocks = new byte[16 * 16 * 32]`.
- Spatial Index Formula: $\text{Index}(x, y, z) = (y \times 256) + (z \times 16) + x$.
- Coordinate Bitwise Masking: local coordinates are accessed via `x & 15`, `z & 15`, `y & 31`.

### `TerrainGenerator.java`
1. **Winding River Valley Algorithm**:
   $$\text{RiverPath}(z) = 14 \cdot \sin(0.045 \cdot z) + 5 \cdot \cos(0.02 \cdot z)$$
   $$\text{DistanceToRiver}(x, z) = |x - \text{RiverPath}(z)|$$
   - If $\text{Distance} < 5.0 \implies \text{Elevation} = 2$ (Sand river bed, water from $Y = 3$ to $Y = 4$).
   - If $\text{Distance} < 7.5 \implies \text{Shoreline slope transition}$ ($Y = 2 \dots 4$).
2. **Strata & Mountain Elevation**:
   $$\text{Elevation}(x, z) = 5.0 + 3.5 \cdot \text{Noise}_{\text{base}}(0.025x, 0.025z) + 1.5 \cdot \text{Noise}_{\text{detail}}(0.08x, 0.08z) + 5.0 \cdot \max(0, \text{Noise}_{\text{mountain}}(0.012x, 0.012z))$$
3. **Flora Rarity**:
   - `POPPY`: Spawns if $\text{VegetationNoise} > 0.82$ (~1.5% rarity).
   - `DANDELION`: Spawns if $\text{VegetationNoise} > 0.74$ (~2.0% rarity).
   - `TALL_GRASS`: Spawns if $\text{VegetationNoise} > 0.58$ (~4.0% rarity).
4. **Oak & White Birch Forests**:
   - Automatically plants multi-tier spherical/box leaf canopies ($5 \times 5$ base with cut corners, $3 \times 3$ mid-tier, $+$ cap) with white birch trunks (`BIRCH_LOG`) and oak trunks (`OAK_LOG`).

### `ChunkManager.java` & `ChunkServer.java`
- **Parallel Generation**: Uses Java's `ForkJoinPool.commonPool()` to generate all 100 chunks across CPU hardware threads in under $15\text{ms}$.
- **HTTP Server**: Runs on `http://127.0.0.1:8088` using `Executors.newVirtualThreadPerTaskExecutor()`:
  - `GET /api/world/chunks`: Streams JSON payload of all loaded chunk voxel arrays.
  - `POST /api/world/break`: Modifies a voxel in the world array and notifies client.
  - `GET /api/world/pois`: Returns points of interest (Lecterns, Lakes, Shrines).

---

## 🎨 4. Three.js Graphics Pipeline (`src/components/world/`)

### `TextureAtlas.js`
- **Atlas Layout**: $4 \times 4$ grid (16 slots of $64 \times 64$ pixels) rendered onto an HTML5 `<canvas>` and wrapped in `THREE.CanvasTexture`.
- **Half-Texel UV Inset**: Eliminates texture bleeding and edge seams:
  $$u_{\min} = \frac{\text{col}}{4} + \frac{0.5}{256}, \quad u_{\max} = \frac{\text{col}+1}{4} - \frac{0.5}{256}$$
- **Mipmapping & Filtering**:
  - `magFilter = THREE.NearestFilter` (Crisp pixel art up close).
  - `minFilter = THREE.NearestMipmapLinearFilter` (Smooth anti-aliased textures at a distance).
  - `anisotropy = 4` (Sharp textures at grazing angles).
- **Dynamic 18 FPS Water Animation**:
  Sinusoidal traveling wave equation executed inside `paintWaterFrame(ctx, time)`:
  $$\text{Wave}(x, y, t) = \frac{1}{2}\Big(\sin(0.15x + 2.5t + 0.1y) + \cos(0.2y - 2.0t + 0.08x)\Big)$$
  - Crests ($\text{Wave} > 0.42$) draw light shimmer (`#689dff`).
  - Mid-tones draw river blue (`#3b78f0`).
  - Troughs ($\text{Wave} < -0.42$) draw deep water shadow (`#1e4bb8`).
- **Transparency Alpha**: Foliage tiles use `ctx.clearRect` with `alphaTest = 0.45` to guarantee zero black background boxes.

### `ChunkMeshBuilder.js`
- **Exposed Face Culling**: A face is only added to the geometry if the adjacent block in direction $\vec{d}$ is transparent (air, water, leaves, foliage).
- **Per-Vertex Smooth Lighting (Ambient Occlusion)**:
  For each vertex of a quad, samples 3 neighbor blocks ($S_1, S_2, \text{Corner}$).
  $$\text{AO Level} = \begin{cases} 1.00 & 0\text{ occluders} \\ 0.78 & 1\text{ occluder} \\ 0.58 & 2\text{ occluders} \\ 0.42 & 3\text{ occluders} \end{cases}$$
- **Anisotropic Diagonal Flip**:
  If $AO_0 + AO_2 > AO_1 + AO_3$, the quad splits along $(v_0, v_2)$ instead of $(v_1, v_3)$ to prevent harsh diagonal creasing.
- **Top-Face De-Tiling Rotation**:
  For top faces of stone, dirt, bedrock, sand, and cobblestone, UV coordinates rotate by $0^\circ, 90^\circ, 180^\circ, \text{ or } 270^\circ$ based on a spatial seed hash $(x \cdot 374761393 + z \cdot 668265263)$.
- **Foliage Cross-Quads**:
  Tall grass, poppies, and dandelions are built as two intersecting diagonal planes ($X$-cross) with deterministic horizontal jitter ($\pm 0.22\text{ blocks}$).

---

## ✋ 5. First-Person Hand & HUD (`FirstPersonHand.js` & `WorldHUD.jsx`)

### `FirstPersonHand.js`
- **Hierarchy**: Anchored directly to `camera.add(hand.group)` at $(0.46, -0.42, -0.62)$.
- **View Bobbing**:
  $$X_{\text{bob}} = \sin(\text{timer}) \cdot \text{amp}_X$$
  $$Y_{\text{bob}} = -|\cos(\text{timer})| \cdot \text{amp}_Y$$
  $$Z_{\text{rot}} = \sin(\text{timer}) \cdot 0.04$$
- **Arm Swing Arc**:
  On left click, evaluates downward chopping arc $\sin(\text{progress} \cdot \pi)$ on pitch ($X = -1.15$), yaw ($Y = 0.65$), and roll ($Z = -0.35$).

### `WorldHUD.jsx`
- **Top Bar**: World name, biome name, game mode (Survival/Hardcore), coordinates XYZ, movement badges (SPRINT / SNEAK), and action buttons.
- **Crosshair**: Vanilla Minecraft pixel crosshair centered on viewport.
- **Bottom Bar**:
  - **10 Red Hearts** (Health).
  - **10 Drumsticks** (Hunger).
  - **Green XP Progress Bar** (Level 0).
  - **9-Slot Hotbar** with white selection border on active item.

---

## 🏃 6. Physics, Raycasting & Collisions

### Axis-Separated Wall Sliding
Movement is evaluated in two independent phases:
1. **$X$-Axis Phase**: Computes $X_{\text{next}} = X_{\text{cur}} + V_x \cdot \Delta t$. If no obstacle or if ground height step $\le 0.55\text{m}$, moves $X$.
2. **$Z$-Axis Phase**: Computes $Z_{\text{next}} = Z_{\text{cur}} + V_z \cdot \Delta t$. If no obstacle or if ground height step $\le 0.55\text{m}$, moves $Z$.
*Result*: Steve slides smoothly along walls and corners without sticking.

### Voxel 3D DDA Raycaster
`raycastBlock(origin, direction, maxDistance = 5.0)` executes an exact 3D Digital Differential Analyzer (DDA) grid traversal:
- Steps voxel-by-voxel along $\Delta t_x, \Delta t_y, \Delta t_z$.
- Identifies the exact targeted voxel coordinate and face normal.
- Projects the black wireframe selection box on the targeted block.

### Progressive Block Cracking System
- Holding Left-Click increments `miningProgress` over $0.38\text{ seconds}$.
- Updates a dynamic 6-stage fracture crack overlay canvas box on the block.
- Upon reaching $1.0$, triggers `breakBlock()`, spawns 18 physics shard cubes with gravity, and plays the break toast!

---

## 🚀 7. Build, Run & Development Workflow

### Starting the Desktop App:
```bash
npm start
# or
npm run app
```
*(Automatically compiles Java classes to `engine/bin`, starts the Vite bundler, boots the Java 26 HTTP server, and launches the Electron desktop window)*

### Building Production Native Bundle:
```bash
npm run build
```

### Keyboard & Mouse Controls:
- **`W / A / S / D`**: Move Forward / Left / Back / Right.
- **`Mouse Look`**: Sub-frame smooth first-person camera look.
- **`Spacebar`**: Jump.
- **`Left Shift`**: Sneak (prevents falling off cliff edges, lowers eye height to $1.27\text{m}$).
- **`Left Control` or `Double-Tap W`**: Sprint (increases speed to $5.61\text{ m/s}$ and widens FOV to $79^\circ$).
- **`Left Click` (Hold/Tap)**: Mine / break targeted block with progressive crack decals and arm swing.
- **`Right Click` or `E`**: Interact / open in-game Book & Quill journal.
- **`Escape`**: Toggle Pause Menu / release mouse lock.
- **`F11`**: Toggle borderless true fullscreen gaming mode.
