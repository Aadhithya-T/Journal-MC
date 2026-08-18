import * as THREE from 'three';
import { TextureAtlas, BLOCK, BLOCK_NAMES, BLOCK_COLORS, isSolidBlock } from './TextureAtlas';
import { TerrainGenerator } from './TerrainGenerator';
import { Chunk } from './Chunk';
import { JavaEngineClient } from './JavaEngineClient';

export class ChunkManager {
  constructor(renderDistanceChunks = 5, texturePackId = 'faithful64') {
    this.renderRadius = renderDistanceChunks; // Radius 5 = 10x10 = 100 chunks
    this.chunkSize = 16;
    this.group = new THREE.Group();
    this.group.name = 'VoxelWorldChunkManager';

    this.atlas = new TextureAtlas(texturePackId);
    this.generator = new TerrainGenerator();

    this.chunks = new Map(); // Key: "cx,cz" => Chunk instance
    this.solidObstacles = new Set(); // Key: "wx,wy,wz" for POI structures
    this.pois = this.generator.pois;

    this.initWorld();
    this.initJavaEngine();
  }

  async initJavaEngine() {
    try {
      const status = await JavaEngineClient.checkStatus();
      if (status) {
        const javaChunks = await JavaEngineClient.fetchAllChunks();
        if (javaChunks && javaChunks.size > 0) {
          for (const [key, bytes] of javaChunks.entries()) {
            const [cxStr, czStr] = key.split(',');
            const cx = parseInt(cxStr, 10);
            const cz = parseInt(czStr, 10);
            const existing = this.chunks.get(key);
            if (existing) {
              existing.blocks = bytes;
              if (existing.mesh) this.group.remove(existing.mesh);
              const newMesh = existing.rebuildMesh(this.atlas);
              if (newMesh) this.group.add(newMesh);
            } else {
              const chunk = new Chunk(cx, cz, bytes, this);
              this.chunks.set(key, chunk);
              const mesh = chunk.rebuildMesh(this.atlas);
              if (mesh) this.group.add(mesh);
            }
          }
          console.log(`[ChunkManager] Synced ${javaChunks.size} Chunks from Java 26 Engine!`);
        }
      }
    } catch (e) {
      console.warn('[ChunkManager] Java Engine sync error:', e);
    }
  }

  getChunkKey(cx, cz) {
    return `${cx},${cz}`;
  }

  initWorld() {
    // 1. Generate Data for all 100 Chunks (10x10 Grid from cx=-5..4, cz=-5..4)
    const minC = -this.renderRadius;
    const maxC = this.renderRadius - 1;

    for (let cx = minC; cx <= maxC; cx++) {
      for (let cz = minC; cz <= maxC; cz++) {
        const blocks = this.generator.generateChunkData(cx, cz);
        const chunk = new Chunk(cx, cz, blocks, this);
        this.chunks.set(this.getChunkKey(cx, cz), chunk);
      }
    }

    // 2. Build Meshes for all 100 Chunks
    for (const chunk of this.chunks.values()) {
      const mesh = chunk.rebuildMesh(this.atlas);
      if (mesh) {
        this.group.add(mesh);
      }
    }

    // 3. Build Lore POI 3D Structures
    this.buildLoreStructures();
  }

  buildLoreStructures() {
    const boxGeo = new THREE.BoxGeometry(1, 1, 1);
    const stoneMat = new THREE.MeshLambertMaterial({ color: 0x6b7280 });
    const woodMat = new THREE.MeshLambertMaterial({ color: 0x854d0e });
    const goldMat = new THREE.MeshLambertMaterial({ color: 0xfacc15 });

    // 1. Spawn Lectern (0, 4, -3)
    const lecternStand = new THREE.Mesh(new THREE.BoxGeometry(0.6, 0.9, 0.6), woodMat);
    lecternStand.position.set(0, 4.45, -3);
    lecternStand.castShadow = true;
    this.group.add(lecternStand);

    const bookMesh = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.1, 0.4), goldMat);
    bookMesh.position.set(0, 4.95, -3);
    bookMesh.rotation.x = 0.2;
    this.group.add(bookMesh);
    this.solidObstacles.add('0,4,-3');
    this.solidObstacles.add('0,5,-3');

    // 2. Crystal Lake Study (14, 4, 28)
    const lakeStand = new THREE.Mesh(new THREE.BoxGeometry(0.6, 0.9, 0.6), woodMat);
    lakeStand.position.set(14, 4.45, 28);
    this.group.add(lakeStand);
    const lakeBook = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.1, 0.4), goldMat);
    lakeBook.position.set(14, 4.95, 28);
    lakeBook.rotation.x = 0.2;
    this.group.add(lakeBook);
    this.solidObstacles.add('14,4,28');

    // 3. Forgotten Shrine (22, 6, 16)
    for (let dx = -1; dx <= 1; dx++) {
      for (let dz = -1; dz <= 1; dz++) {
        const base = new THREE.Mesh(boxGeo, stoneMat);
        base.position.set(22 + dx, 6.5, 16 + dz);
        this.group.add(base);
        this.solidObstacles.add(`${22 + dx},7,${16 + dz}`);
      }
    }
    [[-1, -1], [1, -1], [-1, 1], [1, 1]].forEach(([dx, dz]) => {
      const pillar = new THREE.Mesh(boxGeo, stoneMat);
      pillar.position.set(22 + dx, 7.5, 16 + dz);
      this.group.add(pillar);
      this.solidObstacles.add(`${22 + dx},8,${16 + dz}`);
    });
    const shrineRelic = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.5, 0.5), goldMat);
    shrineRelic.position.set(22, 7.5, 16);
    this.group.add(shrineRelic);

    // 4. Wanderer Cache (-18, 5, -24)
    const chest = new THREE.Mesh(new THREE.BoxGeometry(0.7, 0.7, 0.7), new THREE.MeshLambertMaterial({ color: 0xb45309 }));
    chest.position.set(-18, 5.35, -24);
    this.group.add(chest);
    this.solidObstacles.add('-18,5,-24');

    // 5. Abandoned Mine Entrance (32, 5, -28)
    [-1, 1].forEach((dx) => {
      for (let y = 1; y <= 3; y++) {
        const p = new THREE.Mesh(boxGeo, woodMat);
        p.position.set(32 + dx, 5.5 + y, -28);
        this.group.add(p);
        this.solidObstacles.add(`${32 + dx},${6 + y},-28`);
      }
    });
    for (let dx = -1; dx <= 1; dx++) {
      const b = new THREE.Mesh(boxGeo, woodMat);
      b.position.set(32 + dx, 9.5, -28);
      this.group.add(b);
      this.solidObstacles.add(`${32 + dx},10,-28`);
    }
  }

  // Global Voxel Query
  getBlockAt(wx, wy, wz) {
    const cx = Math.floor(wx / this.chunkSize);
    const cz = Math.floor(wz / this.chunkSize);
    const chunk = this.chunks.get(this.getChunkKey(cx, cz));
    if (!chunk) return BLOCK.AIR;

    const lx = ((wx % this.chunkSize) + this.chunkSize) % this.chunkSize;
    const lz = ((wz % this.chunkSize) + this.chunkSize) % this.chunkSize;

    return chunk.getBlock(lx, wy, lz);
  }

  // Set block and immediately rebuild chunk mesh + affected neighbor meshes
  setBlockAt(wx, wy, wz, blockType) {
    const cx = Math.floor(wx / this.chunkSize);
    const cz = Math.floor(wz / this.chunkSize);
    const chunk = this.chunks.get(this.getChunkKey(cx, cz));
    if (!chunk) return false;

    const lx = ((wx % this.chunkSize) + this.chunkSize) % this.chunkSize;
    const lz = ((wz % this.chunkSize) + this.chunkSize) % this.chunkSize;

    chunk.setBlock(lx, wy, lz, blockType);

    // Rebuild main chunk mesh
    const oldMesh = chunk.mesh;
    if (oldMesh) this.group.remove(oldMesh);
    const newMesh = chunk.rebuildMesh(this.atlas);
    if (newMesh) this.group.add(newMesh);

    // If on chunk boundary, also update neighbor chunk to show newly exposed faces
    if (lx === 0) this.rebuildSingleChunk(cx - 1, cz);
    if (lx === this.chunkSize - 1) this.rebuildSingleChunk(cx + 1, cz);
    if (lz === 0) this.rebuildSingleChunk(cx, cz - 1);
    if (lz === this.chunkSize - 1) this.rebuildSingleChunk(cx, cz + 1);

    // Remove from solidObstacles if clearing
    if (blockType === BLOCK.AIR) {
      this.solidObstacles.delete(`${wx},${wy},${wz}`);
    }

    return true;
  }

  rebuildSingleChunk(cx, cz) {
    const neighbor = this.chunks.get(this.getChunkKey(cx, cz));
    if (neighbor) {
      if (neighbor.mesh) this.group.remove(neighbor.mesh);
      const mesh = neighbor.rebuildMesh(this.atlas);
      if (mesh) this.group.add(mesh);
    }
  }

  // Instant block break (Minecraft Java Edition)
  breakBlock(wx, wy, wz) {
    const currentBlock = this.getBlockAt(wx, wy, wz);
    // Air and Bedrock cannot be broken
    if (currentBlock === BLOCK.AIR || currentBlock === BLOCK.BEDROCK || currentBlock === BLOCK.WATER) {
      return null;
    }

    const blockName = BLOCK_NAMES[currentBlock] || 'Block';
    const blockColor = BLOCK_COLORS[currentBlock] || '#888888';

    this.setBlockAt(wx, wy, wz, BLOCK.AIR);
    JavaEngineClient.breakBlock(wx, wy, wz);

    return {
      type: currentBlock,
      name: blockName,
      color: blockColor,
      x: wx,
      y: wy,
      z: wz
    };
  }

  // Fast 3D Voxel DDA Raycast Traversal (Amanatides & Woo)
  raycastBlock(origin, direction, maxDistance = 5.0) {
    let x = Math.floor(origin.x);
    let y = Math.floor(origin.y);
    let z = Math.floor(origin.z);

    const dx = direction.x;
    const dy = direction.y;
    const dz = direction.z;

    const stepX = Math.sign(dx);
    const stepY = Math.sign(dy);
    const stepZ = Math.sign(dz);

    const tDeltaX = stepX !== 0 ? Math.abs(1 / dx) : Infinity;
    const tDeltaY = stepY !== 0 ? Math.abs(1 / dy) : Infinity;
    const tDeltaZ = stepZ !== 0 ? Math.abs(1 / dz) : Infinity;

    let tMaxX = stepX > 0 ? (Math.floor(origin.x) + 1 - origin.x) * tDeltaX : (origin.x - Math.floor(origin.x)) * tDeltaX;
    let tMaxY = stepY > 0 ? (Math.floor(origin.y) + 1 - origin.y) * tDeltaY : (origin.y - Math.floor(origin.y)) * tDeltaY;
    let tMaxZ = stepZ > 0 ? (Math.floor(origin.z) + 1 - origin.z) * tDeltaZ : (origin.z - Math.floor(origin.z)) * tDeltaZ;

    let dist = 0;
    let normal = [0, 0, 0];

    while (dist < maxDistance) {
      const block = this.getBlockAt(x, y, z);
      if (block !== BLOCK.AIR && block !== BLOCK.WATER) {
        return {
          block,
          blockX: x,
          blockY: y,
          blockZ: z,
          normal,
          dist
        };
      }

      if (tMaxX < tMaxY) {
        if (tMaxX < tMaxZ) {
          dist = tMaxX;
          x += stepX;
          tMaxX += tDeltaX;
          normal = [-stepX, 0, 0];
        } else {
          dist = tMaxZ;
          z += stepZ;
          tMaxZ += tDeltaZ;
          normal = [0, 0, -stepZ];
        }
      } else {
        if (tMaxY < tMaxZ) {
          dist = tMaxY;
          y += stepY;
          tMaxY += tDeltaY;
          normal = [0, -stepY, 0];
        } else {
          dist = tMaxZ;
          z += stepZ;
          tMaxZ += tDeltaZ;
          normal = [0, 0, -stepZ];
        }
      }
    }

    return null;
  }

  // Ground height lookup under player position or from top of world
  getGroundHeight(wx, wz, currentY = null) {
    const rx = Math.floor(wx);
    const rz = Math.floor(wz);

    const startY = currentY !== null ? Math.min(28, Math.floor(currentY + 0.6)) : 28;

    // Search downwards from startY for the first solid surface block under player
    for (let y = startY; y >= 0; y--) {
      const block = this.getBlockAt(rx, y, rz);
      if (isSolidBlock(block)) {
        return y + 1.0;
      }
    }

    return 2.0;
  }

  // 3D Bounding AABB Collision check against solid world blocks and obstacles
  isCollidingWithSolid(px, py, pz, radius = 0.28, height = 1.8) {
    const minBX = Math.floor(px - radius);
    const maxBX = Math.floor(px + radius);
    const minBZ = Math.floor(pz - radius);
    const maxBZ = Math.floor(pz + radius);
    
    // Check vertical overlap with player torso/head (above 0.15m to avoid floor collisions)
    const minBY = Math.floor(py + 0.15);
    const maxBY = Math.floor(py + height - 0.05);

    for (let bx = minBX; bx <= maxBX; bx++) {
      for (let bz = minBZ; bz <= maxBZ; bz++) {
        for (let by = minBY; by <= maxBY; by++) {
          if (this.solidObstacles.has(`${bx},${by},${bz}`)) {
            return true;
          }

          const block = this.getBlockAt(bx, by, bz);
          // Solid obstacle blocks (blocks that are solid: grass, dirt, stone, logs, leaves, ores)
          if (isSolidBlock(block)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  getNearbyPOI(stevePos, threshold = 2.8) {
    for (const poi of this.pois) {
      const dx = stevePos.x - poi.x;
      const dz = stevePos.z - poi.z;
      const dist = Math.sqrt(dx * dx + dz * dz);
      if (dist <= threshold) {
        return poi;
      }
    }
    return null;
  }

  switchTexturePack(packId) {
    this.atlas.buildAtlas(packId);
  }
}
