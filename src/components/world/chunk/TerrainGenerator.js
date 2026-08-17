import { BLOCK } from './TextureAtlas';

// Simple Simplex Noise 2D Implementation
class SimplexNoise2D {
  constructor(seed = 4242) {
    this.p = new Uint8Array(256);
    this.perm = new Uint8Array(512);
    this.permMod12 = new Uint8Array(512);

    for (let i = 0; i < 256; i++) this.p[i] = i;
    for (let i = 255; i > 0; i--) {
      const j = Math.floor(Math.abs(Math.sin(seed + i) * 10000)) % (i + 1);
      const tmp = this.p[i];
      this.p[i] = this.p[j];
      this.p[j] = tmp;
    }
    for (let i = 0; i < 512; i++) {
      this.perm[i] = this.p[i & 255];
      this.permMod12[i] = this.perm[i] % 12;
    }
    this.G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
    this.F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    this.grad3 = [
      [1, 1, 0], [-1, 1, 0], [1, -1, 0], [-1, -1, 0],
      [1, 0, 1], [-1, 0, 1], [1, 0, -1], [-1, 0, -1],
      [0, 1, 1], [0, -1, 1], [0, 1, -1], [0, -1, -1]
    ];
  }

  noise(xin, yin) {
    const s = (xin + yin) * this.F2;
    const i = Math.floor(xin + s);
    const j = Math.floor(yin + s);
    const t = (i + j) * this.G2;
    const X0 = i - t;
    const Y0 = j - t;
    const x0 = xin - X0;
    const y0 = yin - Y0;

    let i1, j1;
    if (x0 > y0) { i1 = 1; j1 = 0; } else { i1 = 0; j1 = 1; }

    const x1 = x0 - i1 + this.G2;
    const y1 = y0 - j1 + this.G2;
    const x2 = x0 - 1.0 + 2.0 * this.G2;
    const y2 = y0 - 1.0 + 2.0 * this.G2;

    const ii = i & 255;
    const jj = j & 255;
    const gi0 = this.permMod12[ii + this.perm[jj]];
    const gi1 = this.permMod12[ii + i1 + this.perm[jj + j1]];
    const gi2 = this.permMod12[ii + 1 + this.perm[jj + 1]];

    let n0 = 0, n1 = 0, n2 = 0;
    let t0 = 0.5 - x0 * x0 - y0 * y0;
    if (t0 > 0) { t0 *= t0; n0 = t0 * t0 * (this.grad3[gi0][0] * x0 + this.grad3[gi0][1] * y0); }
    let t1 = 0.5 - x1 * x1 - y1 * y1;
    if (t1 > 0) { t1 *= t1; n1 = t1 * t1 * (this.grad3[gi1][0] * x1 + this.grad3[gi1][1] * y1); }
    let t2 = 0.5 - x2 * x2 - y2 * y2;
    if (t2 > 0) { t2 *= t2; n2 = t2 * t2 * (this.grad3[gi2][0] * x2 + this.grad3[gi2][1] * y2); }

    return 70.0 * (n0 + n1 + n2);
  }
}

export class TerrainGenerator {
  constructor(seed = 424242) {
    this.noiseBase = new SimplexNoise2D(seed);
    this.noiseDetail = new SimplexNoise2D(seed + 1337);
    this.noiseMountain = new SimplexNoise2D(seed + 9999);
    this.seaLevel = 4; // Water level at Y=4

    this.pois = [
      { id: 'spawn_lectern', name: "Adventurer's Lectern", subtitle: "River Clearing", icon: "📜", x: 6, z: -10, excerpt: "A hand-carved oak pedestal holding an open journal." },
      { id: 'crystal_lake', name: "Crystal Lake Study", subtitle: "Mirror Water's Edge", icon: "🌊", x: 14, z: 28, excerpt: "A tranquil research post set up beside clear spring waters." },
      { id: 'shrine', name: "Forgotten Stone Shrine", subtitle: "Eastern Overlook", icon: "🏛️", x: 22, z: 16, excerpt: "An ancient stone altar flanked by moss-weathered pillars." },
      { id: 'wanderer_cache', name: "Wanderer's Cache", subtitle: "Dense Birch Border", icon: "📦", x: -18, z: -24, excerpt: "A sturdy chest resting beside a cluster of oak logs." },
      { id: 'mineshaft_entrance', name: "Abandoned Mine Entrance", subtitle: "Deep Ravine Edge", icon: "⛏️", x: 32, z: -28, excerpt: "Weathered oak beam scaffolding leading into a dark mineral-rich tunnel." },
      { id: 'deep_ridge', name: "Deep Ridge Mineral Pocket", subtitle: "Southern Granite Crags", icon: "💎", x: -26, z: 32, excerpt: "Exposed veins of blue diamond ore glittering under the mountain sun." }
    ];
  }

  computeHeight(wx, wz) {
    // 1. Winding River Valley
    const riverPath = Math.sin(wz * 0.045) * 14 + Math.cos(wz * 0.02) * 5;
    const distToRiver = Math.abs(wx - riverPath);

    if (distToRiver < 5.0) {
      return 2; // River bed (covered with water up to Y=4)
    }

    if (distToRiver < 7.5) {
      const t = (distToRiver - 5.0) / 2.5;
      return Math.round(2 + t * 2);
    }

    // 2. Rolling Plains & Gentle Hills
    const n1 = this.noiseBase.noise(wx * 0.025, wz * 0.025) * 3.5;
    const n2 = this.noiseDetail.noise(wx * 0.08, wz * 0.08) * 1.5;
    const m = Math.max(0, this.noiseMountain.noise(wx * 0.012, wz * 0.012)) * 5.0;

    let h = Math.round(5.0 + n1 + n2 + m);
    return Math.max(3, Math.min(22, h));
  }

  generateChunkData(cx, cz) {
    const size = 16;
    const height = 32;
    const blocks = new Uint8Array(size * size * height);

    const setBlock = (x, y, z, blockType) => {
      if (x >= 0 && x < size && z >= 0 && z < size && y >= 0 && y < height) {
        blocks[(y * size + z) * size + x] = blockType;
      }
    };

    // 1. Terrain Strata Generation
    for (let lz = 0; lz < size; lz++) {
      for (let lx = 0; lx < size; lx++) {
        const wx = cx * size + lx;
        const wz = cz * size + lz;
        const surfaceY = this.computeHeight(wx, wz);

        // Bedrock
        setBlock(lx, 0, lz, BLOCK.BEDROCK);

        // Underground Strata
        for (let y = 1; y < surfaceY; y++) {
          if (y < surfaceY - 3) {
            const oreNoise = this.noiseDetail.noise(wx * 0.25, wz * 0.25 + y * 0.15);
            if (oreNoise > 0.62 && y <= 4) {
              setBlock(lx, y, lz, BLOCK.DIAMOND_ORE);
            } else {
              setBlock(lx, y, lz, BLOCK.STONE);
            }
          } else {
            setBlock(lx, y, lz, BLOCK.DIRT);
          }
        }

        // River Bed, Shoreline & Surface
        if (surfaceY <= 2) {
          // River Sand Bed
          setBlock(lx, surfaceY, lz, BLOCK.SAND);
          for (let wy = surfaceY + 1; wy <= this.seaLevel; wy++) {
            setBlock(lx, wy, lz, BLOCK.WATER);
          }
        } else if (surfaceY <= 3) {
          // River Shoreline Sand
          setBlock(lx, surfaceY, lz, BLOCK.SAND);
          if (surfaceY < this.seaLevel) {
            for (let wy = surfaceY + 1; wy <= this.seaLevel; wy++) {
              setBlock(lx, wy, lz, BLOCK.WATER);
            }
          }
        } else if (surfaceY > 11) {
          setBlock(lx, surfaceY, lz, BLOCK.STONE);
        } else {
          // Lush Green Grass Block
          setBlock(lx, surfaceY, lz, BLOCK.GRASS);

          // Subtle Wildflower & Grass Scatter (Vanilla 1.19 Rarity)
          const vegNoise = this.noiseDetail.noise(wx * 0.35, wz * 0.35);
          if (surfaceY + 1 < height) {
            if (vegNoise > 0.84) {
              setBlock(lx, surfaceY + 1, lz, BLOCK.POPPY);
            } else if (vegNoise > 0.76) {
              setBlock(lx, surfaceY + 1, lz, BLOCK.DANDELION);
            } else if (vegNoise > 0.58) {
              setBlock(lx, surfaceY + 1, lz, BLOCK.TALL_GRASS);
            }
          }
        }
      }
    }

    // 2. Dense Volumetric Oak & White Birch Forest Canopies
    const plantTree = (tx, tz, isBirch) => {
      const wx = cx * size + tx;
      const wz = cz * size + tz;
      const ty = this.computeHeight(wx, wz);

      if (ty >= 4 && ty <= 10) {
        const logType = isBirch ? BLOCK.BIRCH_LOG : BLOCK.OAK_LOG;
        const leafType = isBirch ? BLOCK.BIRCH_LEAVES : BLOCK.OAK_LEAVES;
        const trunkHeight = isBirch ? 5 : 4;

        setBlock(tx, ty + 1, tz, BLOCK.AIR);

        // Trunk
        for (let y = 1; y <= trunkHeight; y++) {
          setBlock(tx, ty + y, tz, logType);
        }

        // Volumetric 5x5 Lower Canopy (Full solid leaves, clipped outer corners)
        const leafBase = ty + trunkHeight - 1;
        for (let dx = -2; dx <= 2; dx++) {
          for (let dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) === 2 && Math.abs(dz) === 2) continue;
            setBlock(tx + dx, leafBase, tz + dz, leafType);
            setBlock(tx + dx, leafBase + 1, tz + dz, leafType);
          }
        }

        // Volumetric 3x3 Upper Canopy
        for (let dx = -1; dx <= 1; dx++) {
          for (let dz = -1; dz <= 1; dz++) {
            setBlock(tx + dx, leafBase + 2, tz + dz, leafType);
          }
        }

        // Top Rounded Leaf Crown (+ shape)
        setBlock(tx, leafBase + 3, tz, leafType);
        setBlock(tx + 1, leafBase + 3, tz, leafType);
        setBlock(tx - 1, leafBase + 3, tz, leafType);
        setBlock(tx, leafBase + 3, tz + 1, leafType);
        setBlock(tx, leafBase + 3, tz - 1, leafType);
      }
    };

    const seedHash = Math.abs((cx * 31 + cz) ^ 0x5DEECE66);
    if (seedHash % 2 === 0) {
      const t1x = (seedHash >> 2) % 8 + 4;
      const t1z = (seedHash >> 5) % 8 + 4;
      plantTree(t1x, t1z, seedHash % 4 === 0);

      const t2x = ((seedHash >> 7) % 6) + 2;
      const t2z = ((seedHash >> 9) % 6) + 8;
      plantTree(t2x, t2z, (seedHash >> 3) % 4 === 0);
    }

    return blocks;
  }
}
