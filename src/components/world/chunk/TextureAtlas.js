import * as THREE from 'three';

export const BLOCK = {
  AIR: 0,
  GRASS: 1,
  DIRT: 2,
  STONE: 3,
  COBBLESTONE: 4,
  SAND: 5,
  BEDROCK: 6,
  OAK_LOG: 7,
  OAK_LEAVES: 8,
  DIAMOND_ORE: 9,
  WATER: 10,
  BIRCH_LOG: 11,
  BIRCH_LEAVES: 12,
  TALL_GRASS: 13,
  POPPY: 14,
  DANDELION: 15
};

export function isFloraBlock(blockType) {
  return blockType === BLOCK.TALL_GRASS || blockType === BLOCK.POPPY || blockType === BLOCK.DANDELION;
}

export function isSolidBlock(blockType) {
  return blockType !== BLOCK.AIR && blockType !== BLOCK.WATER && !isFloraBlock(blockType) && blockType > 0;
}

export const BLOCK_NAMES = {
  [BLOCK.GRASS]: "Grass Block",
  [BLOCK.DIRT]: "Dirt",
  [BLOCK.STONE]: "Stone",
  [BLOCK.COBBLESTONE]: "Cobblestone",
  [BLOCK.SAND]: "Sand",
  [BLOCK.BEDROCK]: "Bedrock",
  [BLOCK.OAK_LOG]: "Oak Log",
  [BLOCK.OAK_LEAVES]: "Oak Leaves",
  [BLOCK.DIAMOND_ORE]: "Diamond Ore",
  [BLOCK.WATER]: "Water",
  [BLOCK.BIRCH_LOG]: "Birch Log",
  [BLOCK.BIRCH_LEAVES]: "Birch Leaves",
  [BLOCK.TALL_GRASS]: "Tall Grass",
  [BLOCK.POPPY]: "Poppy",
  [BLOCK.DANDELION]: "Dandelion"
};

export const BLOCK_COLORS = {
  [BLOCK.GRASS]: "#5fa832",
  [BLOCK.DIRT]: "#866043",
  [BLOCK.STONE]: "#787878",
  [BLOCK.COBBLESTONE]: "#555555",
  [BLOCK.SAND]: "#dbd3a0",
  [BLOCK.BEDROCK]: "#222222",
  [BLOCK.OAK_LOG]: "#674a27",
  [BLOCK.OAK_LEAVES]: "#4ca028",
  [BLOCK.DIAMOND_ORE]: "#55ffff",
  [BLOCK.WATER]: "#2762d6",
  [BLOCK.BIRCH_LOG]: "#eaeaea",
  [BLOCK.BIRCH_LEAVES]: "#5db532",
  [BLOCK.TALL_GRASS]: "#5fa832",
  [BLOCK.POPPY]: "#dd2222",
  [BLOCK.DANDELION]: "#ffdd00"
};

// Texture Slots in 4x4 Atlas Grid (16 slots total)
export const ATLAS_SLOTS = {
  GRASS_TOP: 0,
  GRASS_SIDE: 1,
  DIRT: 2,
  STONE: 3,
  COBBLESTONE: 4,
  SAND: 5,
  BEDROCK: 6,
  OAK_LOG_SIDE: 7,
  OAK_LOG_TOP: 8,
  OAK_LEAVES: 9,
  DIAMOND_ORE: 10,
  WATER: 11,
  TALL_GRASS: 12,
  POPPY: 13,
  DANDELION: 14,
  BIRCH_LOG_SIDE: 15
};

const TEXTURE_FILE_MAP = {
  [ATLAS_SLOTS.GRASS_TOP]: 'grass_block_top.png',
  [ATLAS_SLOTS.GRASS_SIDE]: 'grass_block_side.png',
  [ATLAS_SLOTS.DIRT]: 'dirt.png',
  [ATLAS_SLOTS.STONE]: 'stone.png',
  [ATLAS_SLOTS.COBBLESTONE]: 'cobblestone.png',
  [ATLAS_SLOTS.SAND]: 'sand.png',
  [ATLAS_SLOTS.BEDROCK]: 'bedrock.png',
  [ATLAS_SLOTS.OAK_LOG_SIDE]: 'oak_log.png',
  [ATLAS_SLOTS.OAK_LOG_TOP]: 'oak_log_top.png',
  [ATLAS_SLOTS.OAK_LEAVES]: 'oak_leaves.png',
  [ATLAS_SLOTS.DIAMOND_ORE]: 'diamond_ore.png',
  [ATLAS_SLOTS.TALL_GRASS]: 'short_grass.png',
  [ATLAS_SLOTS.POPPY]: 'poppy.png',
  [ATLAS_SLOTS.DANDELION]: 'dandelion.png',
  [ATLAS_SLOTS.BIRCH_LOG_SIDE]: 'birch_log.png'
};

export class TextureAtlas {
  constructor(texturePackId = 'faithful64') {
    this.packId = texturePackId;
    this.gridSize = 4; // 4x4 tiles
    this.tileSize = 64;
    this.atlasCanvas = document.createElement('canvas');
    this.atlasCanvas.width = this.gridSize * this.tileSize;
    this.atlasCanvas.height = this.gridSize * this.tileSize;
    this.ctx = this.atlasCanvas.getContext('2d');
    this.ctx.imageSmoothingEnabled = false;

    this.threeTexture = new THREE.CanvasTexture(this.atlasCanvas);
    this.threeTexture.generateMipmaps = true;
    this.threeTexture.magFilter = THREE.NearestFilter;
    this.threeTexture.minFilter = THREE.NearestMipmapLinearFilter;
    this.threeTexture.anisotropy = 4;
    this.threeTexture.colorSpace = THREE.SRGBColorSpace;

    // 1. Solid Blocks & Foliage Material (with per-vertex AO & shading)
    this.material = new THREE.MeshLambertMaterial({
      map: this.threeTexture,
      vertexColors: true,
      transparent: true,
      alphaTest: 0.35,
      side: THREE.DoubleSide
    });

    // 2. Crystal Translucent Water Material
    this.waterMaterial = new THREE.MeshLambertMaterial({
      color: new THREE.Color(0x2762d6),
      transparent: true,
      opacity: 0.62,
      depthWrite: false,
      side: THREE.DoubleSide
    });

    // 3. Foliage Material
    this.foliageMaterial = new THREE.MeshLambertMaterial({
      map: this.threeTexture,
      vertexColors: true,
      transparent: true,
      alphaTest: 0.35,
      side: THREE.DoubleSide
    });

    this.buildAtlas(this.packId);
    this.loadRealTextures();
  }

  getUVs(slotIndex) {
    const col = slotIndex % this.gridSize;
    const row = Math.floor(slotIndex / this.gridSize);

    const totalWidth = this.gridSize * this.tileSize;
    const totalHeight = this.gridSize * this.tileSize;
    const epsX = 0.5 / totalWidth;
    const epsY = 0.5 / totalHeight;

    const uMin = col / this.gridSize + epsX;
    const uMax = (col + 1) / this.gridSize - epsX;
    const vMin = 1 - (row + 1) / this.gridSize + epsY;
    const vMax = 1 - row / this.gridSize - epsY;

    return { uMin, uMax, vMin, vMax };
  }

  getBlockFaceSlot(blockType, faceIndex) {
    switch (blockType) {
      case BLOCK.GRASS:
        if (faceIndex === 2) return ATLAS_SLOTS.GRASS_TOP;
        if (faceIndex === 3) return ATLAS_SLOTS.DIRT;
        return ATLAS_SLOTS.GRASS_SIDE;
      case BLOCK.DIRT:
        return ATLAS_SLOTS.DIRT;
      case BLOCK.STONE:
        return ATLAS_SLOTS.STONE;
      case BLOCK.COBBLESTONE:
        return ATLAS_SLOTS.COBBLESTONE;
      case BLOCK.SAND:
        return ATLAS_SLOTS.SAND;
      case BLOCK.BEDROCK:
        return ATLAS_SLOTS.BEDROCK;
      case BLOCK.OAK_LOG:
        if (faceIndex === 2 || faceIndex === 3) return ATLAS_SLOTS.OAK_LOG_TOP;
        return ATLAS_SLOTS.OAK_LOG_SIDE;
      case BLOCK.BIRCH_LOG:
        if (faceIndex === 2 || faceIndex === 3) return ATLAS_SLOTS.OAK_LOG_TOP;
        return ATLAS_SLOTS.BIRCH_LOG_SIDE;
      case BLOCK.OAK_LEAVES:
      case BLOCK.BIRCH_LEAVES:
        return ATLAS_SLOTS.OAK_LEAVES;
      case BLOCK.DIAMOND_ORE:
        return ATLAS_SLOTS.DIAMOND_ORE;
      case BLOCK.WATER:
        return ATLAS_SLOTS.WATER;
      case BLOCK.TALL_GRASS:
        return ATLAS_SLOTS.TALL_GRASS;
      case BLOCK.POPPY:
        return ATLAS_SLOTS.POPPY;
      case BLOCK.DANDELION:
        return ATLAS_SLOTS.DANDELION;
      default:
        return ATLAS_SLOTS.DIRT;
    }
  }

  isTransparent(blockType) {
    return (
      blockType === BLOCK.AIR ||
      blockType === BLOCK.WATER ||
      isFloraBlock(blockType)
    );
  }

  drawTile(slotIndex, drawFn) {
    const col = slotIndex % this.gridSize;
    const row = Math.floor(slotIndex / this.gridSize);
    const x = col * this.tileSize;
    const y = row * this.tileSize;

    this.ctx.save();
    this.ctx.translate(x, y);
    this.ctx.clearRect(0, 0, this.tileSize, this.tileSize);
    drawFn(this.ctx);
    this.ctx.restore();
  }

  loadRealTextures() {
    const basePath = './texturepacks/faithful64x/';

    for (const [slotStr, fileName] of Object.entries(TEXTURE_FILE_MAP)) {
      const slotIndex = parseInt(slotStr, 10);
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.src = `${basePath}${fileName}`;

      img.onload = () => {
        const col = slotIndex % this.gridSize;
        const row = Math.floor(slotIndex / this.gridSize);
        const x = col * this.tileSize;
        const y = row * this.tileSize;

        this.ctx.clearRect(x, y, this.tileSize, this.tileSize);
        this.ctx.drawImage(img, x, y, this.tileSize, this.tileSize);
        this.threeTexture.needsUpdate = true;
      };
    }
  }

  buildAtlas(packId = 'faithful64') {
    this.packId = packId;
    const S = this.tileSize;

    this.ctx.clearRect(0, 0, this.atlasCanvas.width, this.atlasCanvas.height);

    // 0: GRASS TOP
    this.drawTile(ATLAS_SLOTS.GRASS_TOP, (ctx) => {
      ctx.fillStyle = '#5fa832';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#4f9129';
      for (let i = 0; i < 90; i++) ctx.fillRect(Math.floor(Math.random() * (S - 3)), Math.floor(Math.random() * (S - 3)), 4, 4);
      ctx.fillStyle = '#6cb53b';
      for (let i = 0; i < 60; i++) ctx.fillRect(Math.floor(Math.random() * (S - 3)), Math.floor(Math.random() * (S - 3)), 3, 3);
      ctx.fillStyle = '#427822';
      for (let i = 0; i < 30; i++) ctx.fillRect(Math.floor(Math.random() * (S - 2)), Math.floor(Math.random() * (S - 2)), 2, 2);
    });

    // 1: GRASS SIDE
    this.drawTile(ATLAS_SLOTS.GRASS_SIDE, (ctx) => {
      ctx.fillStyle = '#866043';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#5a3d28';
      for (let i = 0; i < 70; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 4, 4);
      ctx.fillStyle = '#a17855';
      for (let i = 0; i < 40; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 3, 3);

      ctx.fillStyle = '#5fa832';
      ctx.fillRect(0, 0, S, 14);

      for (let x = 0; x < S; x += 4) {
        const fringeLen = 14 + (Math.abs(Math.sin(x * 0.45) * 14) % 12);
        ctx.fillStyle = '#5fa832';
        ctx.fillRect(x, 14, 4, fringeLen);
        ctx.fillStyle = '#4f9129';
        ctx.fillRect(x, 14, 2, fringeLen - 2);
      }
    });

    // 2: DIRT
    this.drawTile(ATLAS_SLOTS.DIRT, (ctx) => {
      ctx.fillStyle = '#866043';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#5a3d28';
      for (let i = 0; i < 80; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 4, 4);
      ctx.fillStyle = '#a17855';
      for (let i = 0; i < 50; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 3, 3);
    });

    // 3: STONE
    this.drawTile(ATLAS_SLOTS.STONE, (ctx) => {
      ctx.fillStyle = '#787878';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#525252';
      for (let i = 0; i < 90; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 3, 3);
      ctx.fillStyle = '#9e9e9e';
      for (let i = 0; i < 60; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 3, 3);
    });

    // 4: COBBLESTONE
    this.drawTile(ATLAS_SLOTS.COBBLESTONE, (ctx) => {
      ctx.fillStyle = '#555555';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#777777';
      for (let i = 0; i < 70; i++) ctx.fillRect(Math.floor(Math.random() * (S - 8)), Math.floor(Math.random() * (S - 8)), 8, 8);
      ctx.fillStyle = '#333333';
      for (let i = 0; i < 50; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 4, 4);
    });

    // 5: SAND
    this.drawTile(ATLAS_SLOTS.SAND, (ctx) => {
      ctx.fillStyle = '#dbd3a0';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#cfc58f';
      for (let i = 0; i < 90; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 3, 3);
      ctx.fillStyle = '#e8e0b5';
      for (let i = 0; i < 60; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 3, 3);
    });

    // 6: BEDROCK
    this.drawTile(ATLAS_SLOTS.BEDROCK, (ctx) => {
      ctx.fillStyle = '#222222';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#111111';
      for (let i = 0; i < 90; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 6, 6);
      ctx.fillStyle = '#444444';
      for (let i = 0; i < 50; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 3, 3);
    });

    // 7: OAK LOG SIDE
    this.drawTile(ATLAS_SLOTS.OAK_LOG_SIDE, (ctx) => {
      ctx.fillStyle = '#674a27';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#3c2912';
      for (let y = 0; y < S; y += 8) ctx.fillRect(0, y, S, 4);
      ctx.fillStyle = '#835f35';
      for (let y = 4; y < S; y += 8) ctx.fillRect(0, y, S, 2);
    });

    // 8: OAK LOG TOP
    this.drawTile(ATLAS_SLOTS.OAK_LOG_TOP, (ctx) => {
      ctx.fillStyle = '#674a27';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#a6855b';
      ctx.fillRect(6, 6, S - 12, S - 12);
      ctx.fillStyle = '#7a5d38';
      ctx.fillRect(14, 14, S - 28, S - 28);
    });

    // 9: OAK LEAVES
    this.drawTile(ATLAS_SLOTS.OAK_LEAVES, (ctx) => {
      ctx.fillStyle = '#2d631d';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#4ca028';
      for (let i = 0; i < 110; i++) ctx.fillRect(Math.floor(Math.random() * (S - 4)), Math.floor(Math.random() * (S - 4)), 5, 5);
      ctx.fillStyle = '#5cb832';
      for (let i = 0; i < 60; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 3, 3);
    });

    // 10: DIAMOND ORE
    this.drawTile(ATLAS_SLOTS.DIAMOND_ORE, (ctx) => {
      ctx.fillStyle = '#787878';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#525252';
      for (let i = 0; i < 50; i++) ctx.fillRect(Math.floor(Math.random() * S), Math.floor(Math.random() * S), 4, 4);
      ctx.fillStyle = '#55ffff';
      ctx.fillRect(10, 10, 10, 10);
      ctx.fillRect(36, 16, 12, 10);
      ctx.fillRect(16, 38, 10, 10);
      ctx.fillRect(40, 40, 12, 10);
    });

    // 11: WATER
    this.drawTile(ATLAS_SLOTS.WATER, (ctx) => {
      ctx.fillStyle = '#2762d6';
      ctx.fillRect(0, 0, S, S);
    });

    // 12: TALL GRASS
    this.drawTile(ATLAS_SLOTS.TALL_GRASS, (ctx) => {
      ctx.fillStyle = '#3e7520';
      ctx.fillRect(18, 16, 4, 48);
      ctx.fillRect(42, 22, 4, 42);
      ctx.fillStyle = '#4f9129';
      ctx.fillRect(24, 8, 4, 56);
      ctx.fillRect(36, 12, 4, 52);
    });

    // 13: POPPY
    this.drawTile(ATLAS_SLOTS.POPPY, (ctx) => {
      ctx.fillStyle = '#3e7520';
      ctx.fillRect(30, 24, 4, 40);
      ctx.fillStyle = '#ee2222';
      ctx.fillRect(22, 10, 20, 14);
      ctx.fillStyle = '#111111';
      ctx.fillRect(28, 12, 8, 8);
    });

    // 14: DANDELION
    this.drawTile(ATLAS_SLOTS.DANDELION, (ctx) => {
      ctx.fillStyle = '#3e7520';
      ctx.fillRect(30, 26, 4, 38);
      ctx.fillStyle = '#ffcc00';
      ctx.fillRect(20, 10, 24, 18);
      ctx.fillStyle = '#cc8800';
      ctx.fillRect(28, 14, 8, 6);
    });

    // 15: BIRCH LOG SIDE
    this.drawTile(ATLAS_SLOTS.BIRCH_LOG_SIDE, (ctx) => {
      ctx.fillStyle = '#e8e8e8';
      ctx.fillRect(0, 0, S, S);
      ctx.fillStyle = '#1a1a1a';
      ctx.fillRect(4, 10, 16, 4);
      ctx.fillRect(34, 20, 18, 5);
      ctx.fillRect(10, 36, 20, 5);
      ctx.fillRect(40, 48, 16, 4);
    });

    this.threeTexture.needsUpdate = true;
  }

  updateWaterAnimation(time) {
    const waveSin = (Math.sin(time * 2.0) + 1.0) * 0.5;
    if (this.waterMaterial) {
      this.waterMaterial.opacity = 0.60 + waveSin * 0.08;
    }
  }
}
