import * as THREE from 'three';
import { TEXTURE_PACKS } from './texturePacks';

export class VoxelTerrain {
  constructor(size = 80, texturePackId = 'faithful64') {
    this.size = size;
    this.half = Math.floor(size / 2);
    this.currentTexturePack = texturePackId;
    this.group = new THREE.Group();
    this.group.name = 'MultiChunkVoxelTerrain';

    this.heightMap = new Map();
    this.blockTypeMap = new Map();
    this.pois = [];
    this.solidBlocks = new Set(); // Key: "x,y,z"
    this.instancedMeshes = {};
    this.waterMeshes = [];

    this.initTerrain();
  }

  computeTerrainHeight(x, z) {
    const dCenter = Math.sqrt(x * x + z * z);
    if (dCenter < 4) return 0; // Flat spawn

    const wave1 = Math.sin(x * 0.1) * Math.cos(z * 0.1) * 2.5;
    const wave2 = Math.sin(x * 0.04 + 1.2) * Math.cos(z * 0.04 - 0.8) * 3.8;
    const wave3 = Math.sin(x * 0.22) * Math.sin(z * 0.22) * 0.9;

    let h = Math.round(wave1 + wave2 + wave3);

    // Lake 1 (Crystal Lake)
    const dLake1 = Math.sqrt((x - 12) ** 2 + (z - 26) ** 2);
    if (dLake1 < 6.5) h = -1;

    // Lake 2 (Misty Hollow)
    const dLake2 = Math.sqrt((x + 18) ** 2 + (z + 14) ** 2);
    if (dLake2 < 5.5) h = -1;

    return THREE.MathUtils.clamp(h, -1, 7);
  }

  isNearWater(x, z) {
    const dLake1 = Math.sqrt((x - 12) ** 2 + (z - 26) ** 2);
    const dLake2 = Math.sqrt((x + 18) ** 2 + (z + 14) ** 2);
    return (dLake1 >= 6.5 && dLake1 <= 8.5) || (dLake2 >= 5.5 && dLake2 <= 7.2);
  }

  createCrossGeometry() {
    const geo = new THREE.BufferGeometry();
    const w = 0.45;
    const h = 0.85;

    // 2 intersecting quads at 45° and 135° (X-shape)
    const vertices = new Float32Array([
      // Quad 1
      -w, 0, -w,   w, 0,  w,   w, h,  w,
      -w, 0, -w,   w, h,  w,  -w, h, -w,
       w, 0,  w,  -w, 0, -w,  -w, h, -w,
       w, 0,  w,  -w, h, -w,   w, h,  w,
      // Quad 2
      -w, 0,  w,   w, 0, -w,   w, h, -w,
      -w, 0,  w,   w, h, -w,  -w, h,  w,
       w, 0, -w,  -w, 0,  w,  -w, h,  w,
       w, 0, -w,  -w, h,  w,   w, h, -w
    ]);

    const uvs = new Float32Array([
      0, 0,  1, 0,  1, 1,
      0, 0,  1, 1,  0, 1,
      1, 0,  0, 0,  0, 1,
      1, 0,  0, 1,  1, 1,

      0, 0,  1, 0,  1, 1,
      0, 0,  1, 1,  0, 1,
      1, 0,  0, 0,  0, 1,
      1, 0,  0, 1,  1, 1
    ]);

    geo.setAttribute('position', new THREE.BufferAttribute(vertices, 3));
    geo.setAttribute('uv', new THREE.BufferAttribute(uvs, 2));
    geo.computeVertexNormals();
    return geo;
  }

  createMaterials(packId = 'faithful64') {
    const pack = TEXTURE_PACKS[packId] || TEXTURE_PACKS.faithful64 || TEXTURE_PACKS.vanilla;
    const textures = pack.getTextures();

    const grassTopMat = new THREE.MeshLambertMaterial({ map: textures.grassTopTex });
    const grassSideMat = new THREE.MeshLambertMaterial({ map: textures.grassSideTex });
    const dirtMat = new THREE.MeshLambertMaterial({ map: textures.dirtTex });
    const stoneMat = new THREE.MeshLambertMaterial({ map: textures.stoneTex });
    const cobbleMat = new THREE.MeshLambertMaterial({ map: textures.cobbleTex });
    const sandMat = new THREE.MeshLambertMaterial({ map: textures.sandTex });
    const bedrockMat = new THREE.MeshLambertMaterial({ map: textures.bedrockTex });
    const oakLogMat = new THREE.MeshLambertMaterial({ map: textures.oakLogTex });
    const leavesMat = new THREE.MeshLambertMaterial({
      map: textures.leavesTex,
      color: 0x5fa832,
      transparent: true,
      opacity: 0.96,
      alphaTest: 0.2
    });
    const diamondMat = new THREE.MeshLambertMaterial({ map: textures.diamondOreTex });
    
    // Animated semi-translucent Minecraft Water
    const waterMat = new THREE.MeshLambertMaterial({
      color: 0x2563eb,
      transparent: true,
      opacity: 0.72,
      depthWrite: false
    });

    // Foliage materials
    const tallGrassMat = new THREE.MeshLambertMaterial({
      map: textures.tallGrassTex,
      transparent: true,
      alphaTest: 0.3,
      side: THREE.DoubleSide
    });

    const poppyMat = new THREE.MeshLambertMaterial({
      map: textures.poppyTex,
      transparent: true,
      alphaTest: 0.3,
      side: THREE.DoubleSide
    });

    const dandelionMat = new THREE.MeshLambertMaterial({
      map: textures.dandelionTex,
      transparent: true,
      alphaTest: 0.3,
      side: THREE.DoubleSide
    });

    const grassBlockMats = [grassSideMat, grassSideMat, grassTopMat, dirtMat, grassSideMat, grassSideMat];

    return {
      grassBlockMats,
      dirtMat,
      stoneMat,
      cobbleMat,
      sandMat,
      bedrockMat,
      oakLogMat,
      leavesMat,
      diamondMat,
      waterMat,
      tallGrassMat,
      poppyMat,
      dandelionMat
    };
  }

  switchTexturePack(packId) {
    if (!TEXTURE_PACKS[packId]) return;
    this.currentTexturePack = packId;
    const mats = this.createMaterials(packId);

    if (this.instancedMeshes.grass) this.instancedMeshes.grass.material = mats.grassBlockMats;
    if (this.instancedMeshes.dirt) this.instancedMeshes.dirt.material = mats.dirtMat;
    if (this.instancedMeshes.stone) this.instancedMeshes.stone.material = mats.stoneMat;
    if (this.instancedMeshes.sand) this.instancedMeshes.sand.material = mats.sandMat;
    if (this.instancedMeshes.bedrock) this.instancedMeshes.bedrock.material = mats.bedrockMat;
    if (this.instancedMeshes.water) this.instancedMeshes.water.material = mats.waterMat;
    if (this.instancedMeshes.logs) this.instancedMeshes.logs.material = mats.oakLogMat;
    if (this.instancedMeshes.leaves) this.instancedMeshes.leaves.material = mats.leavesMat;
    if (this.instancedMeshes.tallGrass) this.instancedMeshes.tallGrass.material = mats.tallGrassMat;
    if (this.instancedMeshes.poppy) this.instancedMeshes.poppy.material = mats.poppyMat;
    if (this.instancedMeshes.dandelion) this.instancedMeshes.dandelion.material = mats.dandelionMat;
  }

  initTerrain() {
    this.solidBlocks.clear();
    const mats = this.createMaterials(this.currentTexturePack);
    const boxGeo = new THREE.BoxGeometry(1, 1, 1);
    const crossGeo = this.createCrossGeometry();

    const grassTransforms = [];
    const dirtTransforms = [];
    const stoneTransforms = [];
    const sandTransforms = [];
    const bedrockTransforms = [];
    const waterTransforms = [];

    const tallGrassTransforms = [];
    const poppyTransforms = [];
    const dandelionTransforms = [];

    const dummy = new THREE.Object3D();
    const BEDROCK_Y = -4;

    for (let x = -this.half; x <= this.half; x++) {
      for (let z = -this.half; z <= this.half; z++) {
        const surfaceH = this.computeTerrainHeight(x, z);
        this.heightMap.set(`${x},${z}`, surfaceH);

        const isWater = surfaceH < 0;
        const isSand = !isWater && this.isNearWater(x, z);

        // Bedrock Bottom Layer
        dummy.position.set(x, BEDROCK_Y, z);
        dummy.updateMatrix();
        bedrockTransforms.push(dummy.matrix.clone());

        // Stone Core Column (from BEDROCK_Y + 1 up to surfaceH - 2)
        const stoneTop = isWater ? -2 : surfaceH - 2;
        for (let y = BEDROCK_Y + 1; y <= stoneTop; y++) {
          dummy.position.set(x, y, z);
          dummy.updateMatrix();
          stoneTransforms.push(dummy.matrix.clone());
        }

        if (isWater) {
          // Water surface block at y = 0
          dummy.position.set(x, 0, z);
          dummy.updateMatrix();
          waterTransforms.push(dummy.matrix.clone());

          // Dirt beneath water
          dummy.position.set(x, -1, z);
          dummy.updateMatrix();
          dirtTransforms.push(dummy.matrix.clone());
        } else if (isSand) {
          // Sand Shoreline
          dummy.position.set(x, surfaceH, z);
          dummy.updateMatrix();
          sandTransforms.push(dummy.matrix.clone());

          if (surfaceH - 1 > BEDROCK_Y) {
            dummy.position.set(x, surfaceH - 1, z);
            dummy.updateMatrix();
            sandTransforms.push(dummy.matrix.clone());
          }
        } else {
          // Top Grass Block
          dummy.position.set(x, surfaceH, z);
          dummy.updateMatrix();
          grassTransforms.push(dummy.matrix.clone());

          // Sub-surface Dirt Layer
          if (surfaceH - 1 > BEDROCK_Y) {
            dummy.position.set(x, surfaceH - 1, z);
            dummy.updateMatrix();
            dirtTransforms.push(dummy.matrix.clone());
          }

          // Foliage & Flower Placement (Pseudo-random based on coords)
          if (Math.abs(x) > 2 || Math.abs(z) > 2) {
            const seed = Math.abs(Math.sin(x * 12.9898 + z * 78.233) * 43758.5453) % 1;
            dummy.position.set(x, surfaceH + 0.5, z);
            dummy.updateMatrix();

            if (seed < 0.18) {
              tallGrassTransforms.push(dummy.matrix.clone());
            } else if (seed >= 0.18 && seed < 0.22) {
              poppyTransforms.push(dummy.matrix.clone());
            } else if (seed >= 0.22 && seed < 0.25) {
              dandelionTransforms.push(dummy.matrix.clone());
            }
          }
        }
      }
    }

    const createInstanced = (geo, mat, matrices, name) => {
      const mesh = new THREE.InstancedMesh(geo, mat, matrices.length);
      mesh.name = name;
      mesh.receiveShadow = true;
      mesh.castShadow = true;
      for (let i = 0; i < matrices.length; i++) {
        mesh.setMatrixAt(i, matrices[i]);
      }
      mesh.instanceMatrix.needsUpdate = true;
      return mesh;
    };

    if (grassTransforms.length > 0) {
      const gMesh = createInstanced(boxGeo, mats.grassBlockMats, grassTransforms, 'InstancedGrass');
      this.instancedMeshes.grass = gMesh;
      this.group.add(gMesh);
    }
    if (dirtTransforms.length > 0) {
      const dMesh = createInstanced(boxGeo, mats.dirtMat, dirtTransforms, 'InstancedDirt');
      this.instancedMeshes.dirt = dMesh;
      this.group.add(dMesh);
    }
    if (stoneTransforms.length > 0) {
      const sMesh = createInstanced(boxGeo, mats.stoneMat, stoneTransforms, 'InstancedStone');
      this.instancedMeshes.stone = sMesh;
      this.group.add(sMesh);
    }
    if (sandTransforms.length > 0) {
      const sndMesh = createInstanced(boxGeo, mats.sandMat, sandTransforms, 'InstancedSand');
      this.instancedMeshes.sand = sndMesh;
      this.group.add(sndMesh);
    }
    if (bedrockTransforms.length > 0) {
      const bMesh = createInstanced(boxGeo, mats.bedrockMat, bedrockTransforms, 'InstancedBedrock');
      this.instancedMeshes.bedrock = bMesh;
      this.group.add(bMesh);
    }
    if (waterTransforms.length > 0) {
      const wMesh = createInstanced(boxGeo, mats.waterMat, waterTransforms, 'InstancedWater');
      this.instancedMeshes.water = wMesh;
      this.group.add(wMesh);
    }

    // Foliage Cross-Quads
    if (tallGrassTransforms.length > 0) {
      const tgMesh = createInstanced(crossGeo, mats.tallGrassMat, tallGrassTransforms, 'InstancedTallGrass');
      this.instancedMeshes.tallGrass = tgMesh;
      this.group.add(tgMesh);
    }
    if (poppyTransforms.length > 0) {
      const pMesh = createInstanced(crossGeo, mats.poppyMat, poppyTransforms, 'InstancedPoppy');
      this.instancedMeshes.poppy = pMesh;
      this.group.add(pMesh);
    }
    if (dandelionTransforms.length > 0) {
      const dMesh = createInstanced(crossGeo, mats.dandelionMat, dandelionTransforms, 'InstancedDandelion');
      this.instancedMeshes.dandelion = dMesh;
      this.group.add(dMesh);
    }

    // Trees (Solid Trunks that block player movement)
    const treeTransformsLogs = [];
    const treeTransformsLeaves = [];

    const treePositions = [
      { x: -6, z: -5 }, { x: 7, z: -7 }, { x: -8, z: 6 }, { x: 8, z: 8 }, { x: 3, z: -8 },
      { x: -18, z: -14 }, { x: -22, z: -18 }, { x: -16, z: -25 }, { x: -28, z: -12 },
      { x: 18, z: -16 }, { x: 24, z: -10 }, { x: 28, z: -22 }, { x: 15, z: -28 },
      { x: -14, z: 22 }, { x: -24, z: 14 }, { x: -30, z: 24 }, { x: -20, z: 32 },
      { x: 22, z: 18 }, { x: 26, z: 28 }, { x: 32, z: 12 }, { x: 18, z: 34 },
      { x: 0, z: 24 }, { x: -4, z: 30 }, { x: 32, z: -4 }, { x: -32, z: 0 }
    ];

    treePositions.forEach(({ x, z }) => {
      const groundH = this.heightMap.get(`${x},${z}`);
      if (groundH === undefined || groundH < 0) return;

      // Tree Trunk (4 solid log blocks)
      for (let ty = 1; ty <= 4; ty++) {
        const logY = groundH + ty;
        dummy.position.set(x, logY, z);
        dummy.updateMatrix();
        treeTransformsLogs.push(dummy.matrix.clone());
        this.solidBlocks.add(`${x},${logY},${z}`);
      }

      // Leaves Canopy
      for (let lx = -2; lx <= 2; lx++) {
        for (let lz = -2; lz <= 2; lz++) {
          for (let ly = 3; ly <= 5; ly++) {
            if (Math.abs(lx) === 2 && Math.abs(lz) === 2 && ly === 5) continue;
            if (lx === 0 && lz === 0 && ly <= 4) continue;
            const leafY = groundH + ly;
            dummy.position.set(x + lx, leafY, z + lz);
            dummy.updateMatrix();
            treeTransformsLeaves.push(dummy.matrix.clone());
          }
        }
      }
    });

    if (treeTransformsLogs.length > 0) {
      const lMesh = createInstanced(boxGeo, mats.oakLogMat, treeTransformsLogs, 'InstancedLogs');
      this.instancedMeshes.logs = lMesh;
      this.group.add(lMesh);
    }
    if (treeTransformsLeaves.length > 0) {
      const lvMesh = createInstanced(boxGeo, mats.leavesMat, treeTransformsLeaves, 'InstancedLeaves');
      this.instancedMeshes.leaves = lvMesh;
      this.group.add(lvMesh);
    }

    // POIs & Lore Structures
    this.createLecternPOI(0, -3, '📜 Adventurer Lectern', 'Ancient Lectern for recording journey starts.');
    this.createLecternPOI(10, 30, '🌊 Crystal Lake Study', 'A peaceful lakeside lectern with ancient reflection notes.');
    this.createShrinePOI(18, 15, mats.cobbleMat, '🏛️ Forgotten Stone Shrine', 'Mysterious ancient altar dedicated to world builders.');
    this.createChestPOI(-15, -20, '📦 Wanderer\'s Supply Cache', 'Explorer\'s chest containing preserved survival tools and maps.');
    this.createMinePOI(25, -22, mats.cobbleMat, mats.oakLogMat, '⛏️ Abandoned Mine Entrance', 'A stone shaft leading deep into the earth.');

    const diamondCoords = [
      { x: 4, z: 3, name: '💎 Plains Diamond Node' },
      { x: -24, z: 18, name: '💎 Deep Ridge Diamond Vein' },
      { x: -28, z: -28, name: '💎 Sanctuary Diamond Cache' }
    ];

    diamondCoords.forEach(({ x, z, name }) => {
      const groundH = this.heightMap.get(`${x},${z}`) || 0;
      const oreY = groundH + 1;
      const diamondMesh = new THREE.Mesh(boxGeo, mats.diamondMat);
      diamondMesh.position.set(x, oreY, z);
      diamondMesh.castShadow = true;
      this.group.add(diamondMesh);

      this.solidBlocks.add(`${x},${oreY},${z}`);

      this.pois.push({
        id: `poi-diamond-${x}-${z}`,
        type: 'diamond',
        name,
        x,
        y: oreY,
        z,
        description: 'Mined rare sparkling diamonds from the earth.'
      });
    });
  }

  createLecternPOI(x, z, name, description) {
    const groundH = this.heightMap.get(`${x},${z}`) || 0;
    const standY = groundH + 1;

    const lecternStandGeo = new THREE.BoxGeometry(0.6, 0.9, 0.6);
    const lecternStandMat = new THREE.MeshLambertMaterial({ color: 0x8b5a2b });
    const lecternStand = new THREE.Mesh(lecternStandGeo, lecternStandMat);
    lecternStand.position.set(x, standY - 0.05, z);
    lecternStand.castShadow = true;
    this.group.add(lecternStand);

    const bookGeo = new THREE.BoxGeometry(0.5, 0.1, 0.4);
    const bookMat = new THREE.MeshLambertMaterial({ color: 0xffd700 });
    const bookMesh = new THREE.Mesh(bookGeo, bookMat);
    bookMesh.position.set(x, standY + 0.45, z);
    bookMesh.rotation.x = 0.2;
    this.group.add(bookMesh);

    this.solidBlocks.add(`${x},${standY},${z}`);

    this.pois.push({ id: `poi-lectern-${x}-${z}`, type: 'lectern', name, x, y: standY, z, description });
  }

  createChestPOI(x, z, name, description) {
    const groundH = this.heightMap.get(`${x},${z}`) || 0;
    const chestY = groundH + 1;

    const chestGeo = new THREE.BoxGeometry(0.7, 0.7, 0.7);
    const chestMat = new THREE.MeshLambertMaterial({ color: 0xb87333 });
    const chestMesh = new THREE.Mesh(chestGeo, chestMat);
    chestMesh.position.set(x, chestY - 0.15, z);
    chestMesh.castShadow = true;
    this.group.add(chestMesh);

    this.solidBlocks.add(`${x},${chestY},${z}`);

    this.pois.push({ id: `poi-chest-${x}-${z}`, type: 'chest', name, x, y: chestY, z, description });
  }

  createShrinePOI(x, z, cobbleMat, name, description) {
    const groundH = this.heightMap.get(`${x},${z}`) || 0;
    const boxGeo = new THREE.BoxGeometry(1, 1, 1);

    for (let dx = -1; dx <= 1; dx++) {
      for (let dz = -1; dz <= 1; dz++) {
        const by = groundH + 1;
        const block = new THREE.Mesh(boxGeo, cobbleMat);
        block.position.set(x + dx, by, z + dz);
        block.castShadow = true;
        this.group.add(block);
        this.solidBlocks.add(`${x + dx},${by},${z + dz}`);
      }
    }
    [[-1, -1], [1, -1], [-1, 1], [1, 1]].forEach(([dx, dz]) => {
      const by = groundH + 2;
      const pillar = new THREE.Mesh(boxGeo, cobbleMat);
      pillar.position.set(x + dx, by, z + dz);
      pillar.castShadow = true;
      this.group.add(pillar);
      this.solidBlocks.add(`${x + dx},${by},${z + dz}`);
    });

    const relicGeo = new THREE.BoxGeometry(0.5, 0.5, 0.5);
    const relicMat = new THREE.MeshLambertMaterial({ color: 0xffd700 });
    const relic = new THREE.Mesh(relicGeo, relicMat);
    relic.position.set(x, groundH + 2, z);
    this.group.add(relic);

    this.pois.push({ id: `poi-shrine-${x}-${z}`, type: 'shrine', name, x, y: groundH + 1, z, description });
  }

  createMinePOI(x, z, cobbleMat, oakLogMat, name, description) {
    const groundH = this.heightMap.get(`${x},${z}`) || 0;
    const boxGeo = new THREE.BoxGeometry(1, 1, 1);

    [-1, 1].forEach((dx) => {
      for (let y = 1; y <= 3; y++) {
        const by = groundH + y;
        const pillar = new THREE.Mesh(boxGeo, oakLogMat);
        pillar.position.set(x + dx, by, z);
        pillar.castShadow = true;
        this.group.add(pillar);
        this.solidBlocks.add(`${x + dx},${by},${z}`);
      }
    });

    for (let dx = -1; dx <= 1; dx++) {
      const by = groundH + 4;
      const beam = new THREE.Mesh(boxGeo, oakLogMat);
      beam.position.set(x + dx, by, z);
      beam.castShadow = true;
      this.group.add(beam);
      this.solidBlocks.add(`${x + dx},${by},${z}`);
    }

    this.pois.push({ id: `poi-mine-${x}-${z}`, type: 'mine', name, x, y: groundH + 1, z, description });
  }

  // Returns the exact top surface Y of the ground block (h + 0.5)
  getGroundHeight(x, z) {
    const rx = Math.round(x);
    const rz = Math.round(z);
    const h = this.heightMap.get(`${rx},${rz}`);
    return h !== undefined ? h + 0.5 : 0.5;
  }

  // Checks if Steve's 3D bounding cylinder overlaps any solid obstacle block
  isCollidingWithSolid(x, y, z, radius = 0.35, height = 1.8) {
    const minX = Math.floor(x - radius);
    const maxX = Math.ceil(x + radius);
    const minZ = Math.floor(z - radius);
    const maxZ = Math.ceil(z + radius);
    const minY = Math.floor(y);
    const maxY = Math.ceil(y + height);

    for (let bx = minX; bx <= maxX; bx++) {
      for (let bz = minZ; bz <= maxZ; bz++) {
        for (let by = minY; by <= maxY; by++) {
          if (this.solidBlocks.has(`${bx},${by},${bz}`)) {
            const dx = Math.max(Math.abs(x - bx) - 0.5, 0);
            const dz = Math.max(Math.abs(z - bz) - 0.5, 0);
            if (dx * dx + dz * dz < radius * radius) {
              return true; // Collision!
            }
          }
        }
      }
    }
    return false;
  }

  getNearbyPOI(stevePos, threshold = 2.4) {
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
}
