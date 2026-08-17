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
  return texture;
}

function loadFileTexture(path) {
  const loader = new THREE.TextureLoader();
  const tex = loader.load(path);
  tex.magFilter = THREE.NearestFilter;
  tex.minFilter = THREE.NearestFilter;
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

      return { grassTopTex, grassSideTex, dirtTex, stoneTex, oakLogTex, leavesTex, diamondOreTex, cobbleTex };
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

      return { grassTopTex, grassSideTex, dirtTex, stoneTex, oakLogTex, leavesTex, diamondOreTex, cobbleTex };
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

      return { grassTopTex, grassSideTex, dirtTex, stoneTex, oakLogTex, leavesTex, diamondOreTex, cobbleTex };
    }
  }
};
