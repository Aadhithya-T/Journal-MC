import * as THREE from 'three';

/**
 * Procedural texture pack generators for iconic Minecraft styles.
 */
function createTexture(width, height, drawFn) {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  ctx.imageSmoothingEnabled = false;
  drawFn(ctx, width, height);
  const texture = new THREE.CanvasTexture(canvas);
  texture.magFilter = THREE.NearestFilter;
  texture.minFilter = THREE.NearestFilter;
  texture.colorSpace = THREE.SRGBColorSpace;
  return texture;
}

function loadFileTexture(path) {
  const loader = new THREE.TextureLoader();
  const tex = loader.load(path);
  tex.magFilter = THREE.NearestFilter;
  tex.minFilter = THREE.NearestFilter;
  tex.colorSpace = THREE.SRGBColorSpace;
  return tex;
}

export const TEXTURE_PACKS = {
  faithful64: {
    id: 'faithful64',
    name: 'Faithful 64x (Real Minecraft Pack)',
    description: 'Real authentic Faithful 64x Minecraft textures',
    getTextures: () => {
      const grassTopTex = loadFileTexture('/texturepacks/faithful64x/grass_block_top.png');
      const grassSideTex = loadFileTexture('/texturepacks/faithful64x/grass_block_side.png');
      const dirtTex = loadFileTexture('/texturepacks/faithful64x/dirt.png');
      const stoneTex = loadFileTexture('/texturepacks/faithful64x/stone.png');
      const oakLogTex = loadFileTexture('/texturepacks/faithful64x/oak_log.png');
      const leavesTex = loadFileTexture('/texturepacks/faithful64x/oak_leaves.png');
      const diamondOreTex = loadFileTexture('/texturepacks/faithful64x/diamond_ore.png');
      const cobbleTex = loadFileTexture('/texturepacks/faithful64x/cobblestone.png');

      const sandTex = createTexture(64, 64, (ctx) => {
        ctx.fillStyle = '#dbd3a0';
        ctx.fillRect(0, 0, 64, 64);
        ctx.fillStyle = '#cfc58f';
        for (let i = 0; i < 90; i++) ctx.fillRect(Math.floor(Math.random() * 64), Math.floor(Math.random() * 64), 2, 2);
        ctx.fillStyle = '#e8e0b5';
        for (let i = 0; i < 70; i++) ctx.fillRect(Math.floor(Math.random() * 64), Math.floor(Math.random() * 64), 2, 2);
      });

      const bedrockTex = createTexture(64, 64, (ctx) => {
        ctx.fillStyle = '#222222';
        ctx.fillRect(0, 0, 64, 64);
        ctx.fillStyle = '#111111';
        for (let i = 0; i < 80; i++) ctx.fillRect(Math.floor(Math.random() * 64), Math.floor(Math.random() * 64), 4, 4);
        ctx.fillStyle = '#444444';
        for (let i = 0; i < 40; i++) ctx.fillRect(Math.floor(Math.random() * 64), Math.floor(Math.random() * 64), 2, 2);
      });

      const tallGrassTex = createTexture(32, 32, (ctx) => {
        ctx.clearRect(0, 0, 32, 32);
        ctx.fillStyle = '#5fa832';
        ctx.fillRect(10, 8, 3, 24);
        ctx.fillRect(14, 4, 4, 28);
        ctx.fillRect(19, 12, 3, 20);
        ctx.fillRect(7, 16, 3, 16);
        ctx.fillRect(23, 14, 3, 18);
        ctx.fillStyle = '#7ebd42';
        ctx.fillRect(11, 8, 1, 20);
        ctx.fillRect(15, 4, 2, 24);
      });

      const poppyTex = createTexture(32, 32, (ctx) => {
        ctx.clearRect(0, 0, 32, 32);
        ctx.fillStyle = '#498834';
        ctx.fillRect(14, 12, 4, 20);
        ctx.fillStyle = '#dd2222';
        ctx.fillRect(8, 4, 16, 10);
        ctx.fillStyle = '#aa0000';
        ctx.fillRect(10, 2, 12, 14);
        ctx.fillStyle = '#111111';
        ctx.fillRect(14, 6, 4, 4);
      });

      const dandelionTex = createTexture(32, 32, (ctx) => {
        ctx.clearRect(0, 0, 32, 32);
        ctx.fillStyle = '#498834';
        ctx.fillRect(14, 14, 4, 18);
        ctx.fillStyle = '#ffdd00';
        ctx.fillRect(10, 6, 12, 10);
        ctx.fillStyle = '#ffaa00';
        ctx.fillRect(12, 8, 8, 6);
      });

      return { grassTopTex, grassSideTex, dirtTex, stoneTex, oakLogTex, leavesTex, diamondOreTex, cobbleTex, sandTex, bedrockTex, tallGrassTex, poppyTex, dandelionTex };
    }
  },

  vanilla: {
    id: 'vanilla',
    name: 'Default 16x16 (Vanilla)',
    description: 'Iconic authentic Minecraft pixel art',
    getTextures: () => {
      const grassTopTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#5c8e32';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#7ebd42';
        for (let i = 0; i < 40; i++) ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 2, 2);
        ctx.fillStyle = '#3c641d';
        for (let i = 0; i < 25; i++) ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 2, 2);
      });

      const grassSideTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#866043';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#573d26';
        for (let i = 0; i < 30; i++) ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 2, 2);
        ctx.fillStyle = '#5c8e32';
        ctx.fillRect(0, 0, 32, 8);
        for (let x = 0; x < 32; x += 4) ctx.fillRect(x, 8, 4, Math.floor(Math.random() * 6));
      });

      const dirtTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#866043';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#573d26';
        for (let i = 0; i < 45; i++) ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 2, 2);
      });

      const stoneTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#737373';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#404040';
        for (let i = 0; i < 40; i++) ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 2, 2);
        ctx.fillStyle = '#a3a3a3';
        for (let i = 0; i < 20; i++) ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 2, 2);
      });

      const oakLogTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#674a27';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#3c2912';
        for (let y = 0; y < 32; y += 4) ctx.fillRect(0, y, 32, 2);
      });

      const leavesTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#2d5a1e';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#498834';
        for (let i = 0; i < 50; i++) ctx.fillRect(Math.floor(Math.random() * 30), Math.floor(Math.random() * 30), 3, 3);
      });

      const diamondOreTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#737373';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#55ffff';
        ctx.fillRect(6, 6, 6, 6);
        ctx.fillRect(20, 10, 8, 6);
        ctx.fillRect(10, 22, 6, 6);
        ctx.fillRect(22, 22, 6, 6);
      });

      const cobbleTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#555555';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#777777';
        for (let i = 0; i < 30; i++) ctx.fillRect(Math.floor(Math.random() * 28), Math.floor(Math.random() * 28), 4, 4);
        ctx.fillStyle = '#333333';
        for (let i = 0; i < 25; i++) ctx.fillRect(Math.floor(Math.random() * 30), Math.floor(Math.random() * 30), 2, 2);
      });

      const sandTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#dcd3a2';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#c5b980';
        for (let i = 0; i < 30; i++) ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 2, 2);
      });

      const bedrockTex = createTexture(32, 32, (ctx) => {
        ctx.fillStyle = '#2b2b2b';
        ctx.fillRect(0, 0, 32, 32);
        ctx.fillStyle = '#111111';
        for (let i = 0; i < 35; i++) ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 3, 3);
      });

      const tallGrassTex = createTexture(32, 32, (ctx) => {
        ctx.clearRect(0, 0, 32, 32);
        ctx.fillStyle = '#5c8e32';
        ctx.fillRect(10, 8, 3, 24);
        ctx.fillRect(14, 4, 4, 28);
        ctx.fillRect(19, 12, 3, 20);
        ctx.fillRect(7, 16, 3, 16);
      });

      const poppyTex = createTexture(32, 32, (ctx) => {
        ctx.clearRect(0, 0, 32, 32);
        ctx.fillStyle = '#498834';
        ctx.fillRect(14, 12, 4, 20);
        ctx.fillStyle = '#dd2222';
        ctx.fillRect(8, 4, 16, 10);
      });

      const dandelionTex = createTexture(32, 32, (ctx) => {
        ctx.clearRect(0, 0, 32, 32);
        ctx.fillStyle = '#498834';
        ctx.fillRect(14, 14, 4, 18);
        ctx.fillStyle = '#ffdd00';
        ctx.fillRect(10, 6, 12, 10);
      });

      return { grassTopTex, grassSideTex, dirtTex, stoneTex, oakLogTex, leavesTex, diamondOreTex, cobbleTex, sandTex, bedrockTex, tallGrassTex, poppyTex, dandelionTex };
    }
  },

  barebones: {
    id: 'barebones',
    name: 'Bare Bones (Official Promo Style)',
    description: 'Clean vibrant flat-shaded minimalist trailer style',
    getTextures: () => {
      const grassTopTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#65a832';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#78c43b';
        ctx.fillRect(2, 2, 12, 12);
      });

      const grassSideTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#835432';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#65a832';
        ctx.fillRect(0, 0, 16, 5);
        ctx.fillRect(3, 5, 3, 3);
        ctx.fillRect(10, 5, 4, 4);
      });

      const dirtTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#835432';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#6a4325';
        ctx.fillRect(2, 2, 6, 6);
        ctx.fillRect(9, 9, 5, 5);
      });

      const stoneTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#8a8a8a';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#707070';
        ctx.fillRect(2, 2, 6, 5);
        ctx.fillRect(9, 8, 5, 6);
      });

      const oakLogTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#785329';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#543818';
        ctx.fillRect(4, 0, 8, 16);
      });

      const leavesTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#3f7c22';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#5aa632';
        ctx.fillRect(2, 2, 6, 6);
        ctx.fillRect(9, 8, 5, 6);
      });

      const diamondOreTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#8a8a8a';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#40e0d0';
        ctx.fillRect(4, 4, 4, 4);
        ctx.fillRect(10, 9, 3, 3);
      });

      const cobbleTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#7a7a7a';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#555555';
        ctx.fillRect(1, 1, 6, 6);
        ctx.fillRect(9, 1, 6, 6);
        ctx.fillRect(1, 9, 6, 6);
        ctx.fillRect(9, 9, 6, 6);
      });

      const sandTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#dbd3a0';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#c8bf8c';
        ctx.fillRect(3, 3, 10, 10);
      });

      const bedrockTex = createTexture(16, 16, (ctx) => {
        ctx.fillStyle = '#222222';
        ctx.fillRect(0, 0, 16, 16);
        ctx.fillStyle = '#111111';
        ctx.fillRect(2, 2, 12, 12);
      });

      const tallGrassTex = createTexture(16, 16, (ctx) => {
        ctx.clearRect(0, 0, 16, 16);
        ctx.fillStyle = '#65a832';
        ctx.fillRect(6, 4, 4, 12);
      });

      const poppyTex = createTexture(16, 16, (ctx) => {
        ctx.clearRect(0, 0, 16, 16);
        ctx.fillStyle = '#5aa632';
        ctx.fillRect(7, 8, 2, 8);
        ctx.fillStyle = '#dd2222';
        ctx.fillRect(5, 3, 6, 6);
      });

      const dandelionTex = createTexture(16, 16, (ctx) => {
        ctx.clearRect(0, 0, 16, 16);
        ctx.fillStyle = '#5aa632';
        ctx.fillRect(7, 8, 2, 8);
        ctx.fillStyle = '#ffdd00';
        ctx.fillRect(5, 3, 6, 6);
      });

      return { grassTopTex, grassSideTex, dirtTex, stoneTex, oakLogTex, leavesTex, diamondOreTex, cobbleTex, sandTex, bedrockTex, tallGrassTex, poppyTex, dandelionTex };
    }
  }
};
