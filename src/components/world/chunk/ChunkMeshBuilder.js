import * as THREE from 'three';
import { BLOCK } from './TextureAtlas';

// 6 Cube Faces with Tangent Vectors for Per-Vertex Ambient Occlusion
const FACES = [
  // 0: Right (+X)
  {
    dir: [1, 0, 0],
    norm: [1, 0, 0],
    baseShade: 0.75,
    uAxis: [0, 0, 1],
    vAxis: [0, 1, 0],
    corners: [[1, 0, 1], [1, 0, 0], [1, 1, 0], [1, 1, 1]],
    cornerOffsets: [[1, -1], [-1, -1], [-1, 1], [1, 1]]
  },
  // 1: Left (-X)
  {
    dir: [-1, 0, 0],
    norm: [-1, 0, 0],
    baseShade: 0.75,
    uAxis: [0, 0, -1],
    vAxis: [0, 1, 0],
    corners: [[0, 0, 0], [0, 0, 1], [0, 1, 1], [0, 1, 0]],
    cornerOffsets: [[-1, -1], [1, -1], [1, 1], [-1, 1]]
  },
  // 2: Top (+Y)
  {
    dir: [0, 1, 0],
    norm: [0, 1, 0],
    baseShade: 1.0,
    uAxis: [1, 0, 0],
    vAxis: [0, 0, 1],
    corners: [[0, 1, 1], [1, 1, 1], [1, 1, 0], [0, 1, 0]],
    cornerOffsets: [[-1, 1], [1, 1], [1, -1], [-1, -1]]
  },
  // 3: Bottom (-Y)
  {
    dir: [0, -1, 0],
    norm: [0, -1, 0],
    baseShade: 0.55,
    uAxis: [1, 0, 0],
    vAxis: [0, 0, -1],
    corners: [[0, 0, 0], [1, 0, 0], [1, 0, 1], [0, 0, 1]],
    cornerOffsets: [[-1, -1], [1, -1], [1, 1], [-1, 1]]
  },
  // 4: Front (+Z)
  {
    dir: [0, 0, 1],
    norm: [0, 0, 1],
    baseShade: 0.85,
    uAxis: [-1, 0, 0],
    vAxis: [0, 1, 0],
    corners: [[0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]],
    cornerOffsets: [[1, -1], [-1, -1], [-1, 1], [1, 1]]
  },
  // 5: Back (-Z)
  {
    dir: [0, 0, -1],
    norm: [0, 0, -1],
    baseShade: 0.85,
    uAxis: [1, 0, 0],
    vAxis: [0, 1, 0],
    corners: [[1, 0, 0], [0, 0, 0], [0, 1, 0], [1, 1, 0]],
    cornerOffsets: [[1, -1], [-1, -1], [-1, 1], [1, 1]]
  }
];

const AO_CURVE = [1.0, 0.78, 0.58, 0.42];

export class ChunkMeshBuilder {
  static computeVertexAO(isSolidFn, x, y, z, face, uSign, vSign) {
    const fx = x + face.dir[0];
    const fy = y + face.dir[1];
    const fz = z + face.dir[2];

    const u1 = face.uAxis[0] * uSign;
    const u2 = face.uAxis[1] * uSign;
    const u3 = face.uAxis[2] * uSign;

    const v1 = face.vAxis[0] * vSign;
    const v2 = face.vAxis[1] * vSign;
    const v3 = face.vAxis[2] * vSign;

    const s1 = isSolidFn(fx + u1, fy + u2, fz + u3);
    const s2 = isSolidFn(fx + v1, fy + v2, fz + v3);
    const corner = isSolidFn(fx + u1 + v1, fy + u2 + v2, fz + u3 + v3);

    let occlusion = 0;
    if (s1) occlusion++;
    if (s2) occlusion++;
    if (s1 && s2) {
      occlusion++;
    } else if (corner) {
      occlusion++;
    }

    return AO_CURVE[Math.min(3, occlusion)];
  }

  static buildChunkMesh(chunk, atlas) {
    const CHUNK_SIZE = 16;
    const CHUNK_HEIGHT = 32;

    // Solid geometry arrays
    const solidPositions = [];
    const solidNormals = [];
    const solidUvs = [];
    const solidColors = [];

    // Water geometry arrays
    const waterPositions = [];
    const waterNormals = [];
    const waterUvs = [];
    const waterColors = [];

    const getBlockAt = (x, y, z) => {
      if (x < 0 || x >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT) {
        return chunk.getNeighborBlock(x, y, z);
      }
      return chunk.getBlock(x, y, z);
    };

    const isSolid = (x, y, z) => {
      const b = getBlockAt(x, y, z);
      return b !== BLOCK.AIR && b !== BLOCK.WATER && b < BLOCK.TALL_GRASS && b !== BLOCK.OAK_LEAVES && b !== BLOCK.BIRCH_LEAVES;
    };

    for (let y = 0; y < CHUNK_HEIGHT; y++) {
      for (let z = 0; z < CHUNK_SIZE; z++) {
        for (let x = 0; x < CHUNK_SIZE; x++) {
          const block = chunk.getBlock(x, y, z);
          if (block === BLOCK.AIR) continue;

          // 1. Foliage Cross-Quads (Tall grass, flowers with natural scatter jitter)
          if (block >= BLOCK.TALL_GRASS) {
            this.buildCrossFoliage(chunk, x, y, z, block, atlas, solidPositions, solidNormals, solidUvs, solidColors);
            continue;
          }

          // 2. Voxel Cube Faces with Exposed Face Culling & Smooth Ambient Occlusion
          for (let f = 0; f < 6; f++) {
            const face = FACES[f];
            const nx = x + face.dir[0];
            const ny = y + face.dir[1];
            const nz = z + face.dir[2];

            const neighborBlock = getBlockAt(nx, ny, nz);

            let shouldDrawFace = false;
            const isWater = (block === BLOCK.WATER);

            if (isWater) {
              // Only draw top face of water when neighbor is air
              shouldDrawFace = (neighborBlock === BLOCK.AIR && f === 2);
            } else if (block === BLOCK.OAK_LEAVES || block === BLOCK.BIRCH_LEAVES) {
              shouldDrawFace = (neighborBlock !== block && atlas.isTransparent(neighborBlock));
            } else {
              shouldDrawFace = atlas.isTransparent(neighborBlock);
            }

            if (shouldDrawFace) {
              const slot = atlas.getBlockFaceSlot(block, f);
              const { uMin, uMax, vMin, vMax } = atlas.getUVs(slot);

              // Vanilla 1.19 Texture De-tiling: Random 90-degree rotations on stone, dirt, bedrock, sand top faces
              let uv0 = [uMin, vMin], uv1 = [uMax, vMin], uv2 = [uMax, vMax], uv3 = [uMin, vMax];
              if (f === 2 && (block === BLOCK.STONE || block === BLOCK.DIRT || block === BLOCK.SAND || block === BLOCK.BEDROCK || block === BLOCK.COBBLESTONE)) {
                const rot = Math.abs((x * 374761393 + z * 668265263) ^ (chunk.cx * 31 + chunk.cz)) % 4;
                if (rot === 1) {
                  uv0 = [uMax, vMin]; uv1 = [uMax, vMax]; uv2 = [uMin, vMax]; uv3 = [uMin, vMin];
                } else if (rot === 2) {
                  uv0 = [uMax, vMax]; uv1 = [uMin, vMax]; uv2 = [uMin, vMin]; uv3 = [uMax, vMin];
                } else if (rot === 3) {
                  uv0 = [uMin, vMax]; uv1 = [uMin, vMin]; uv2 = [uMax, vMin]; uv3 = [uMax, vMax];
                }
              }

              // 4 corners of face quad
              const c = face.corners;
              const v0 = [x + c[0][0], y + c[0][1], z + c[0][2]];
              const v1 = [x + c[1][0], y + c[1][1], z + c[1][2]];
              const v2 = [x + c[2][0], y + c[2][1], z + c[2][2]];
              const v3 = [x + c[3][0], y + c[3][1], z + c[3][2]];

              // Compute Per-Vertex Smooth Lighting (Ambient Occlusion)
              const ao0 = isWater ? 1.0 : this.computeVertexAO(isSolid, x, y, z, face, face.cornerOffsets[0][0], face.cornerOffsets[0][1]);
              const ao1 = isWater ? 1.0 : this.computeVertexAO(isSolid, x, y, z, face, face.cornerOffsets[1][0], face.cornerOffsets[1][1]);
              const ao2 = isWater ? 1.0 : this.computeVertexAO(isSolid, x, y, z, face, face.cornerOffsets[2][0], face.cornerOffsets[2][1]);
              const ao3 = isWater ? 1.0 : this.computeVertexAO(isSolid, x, y, z, face, face.cornerOffsets[3][0], face.cornerOffsets[3][1]);

              const s0 = face.baseShade * ao0;
              const s1 = face.baseShade * ao1;
              const s2 = face.baseShade * ao2;
              const s3 = face.baseShade * ao3;

              const targetPositions = isWater ? waterPositions : solidPositions;
              const targetNormals = isWater ? waterNormals : solidNormals;
              const targetUvs = isWater ? waterUvs : solidUvs;
              const targetColors = isWater ? waterColors : solidColors;

              // Anisotropic Triangulation Flip
              if (ao0 + ao2 > ao1 + ao3) {
                // Triangle 1 (v0, v1, v2)
                targetPositions.push(...v0, ...v1, ...v2);
                targetNormals.push(...face.norm, ...face.norm, ...face.norm);
                targetUvs.push(...uv0, ...uv1, ...uv2);
                targetColors.push(s0, s0, s0, s1, s1, s1, s2, s2, s2);

                // Triangle 2 (v0, v2, v3)
                targetPositions.push(...v0, ...v2, ...v3);
                targetNormals.push(...face.norm, ...face.norm, ...face.norm);
                targetUvs.push(...uv0, ...uv2, ...uv3);
                targetColors.push(s0, s0, s0, s2, s2, s2, s3, s3, s3);
              } else {
                // Triangle 1 (v1, v2, v3)
                targetPositions.push(...v1, ...v2, ...v3);
                targetNormals.push(...face.norm, ...face.norm, ...face.norm);
                targetUvs.push(...uv1, ...uv2, ...uv3);
                targetColors.push(s1, s1, s1, s2, s2, s2, s3, s3, s3);

                // Triangle 2 (v1, v3, v0)
                targetPositions.push(...v1, ...v3, ...v0);
                targetNormals.push(...face.norm, ...face.norm, ...face.norm);
                targetUvs.push(...uv1, ...uv3, ...uv0);
                targetColors.push(s1, s1, s1, s3, s3, s3, s0, s0, s0);
              }
            }
          }
        }
      }
    }

    if (solidPositions.length === 0 && waterPositions.length === 0) return null;

    const chunkGroup = new THREE.Group();
    chunkGroup.name = `ChunkGroup_${chunk.cx}_${chunk.cz}`;
    chunkGroup.position.set(chunk.cx * CHUNK_SIZE, 0, chunk.cz * CHUNK_SIZE);

    // 1. Solid Mesh (Terrain, stone, dirt, leaves, flowers)
    if (solidPositions.length > 0) {
      const solidGeo = new THREE.BufferGeometry();
      solidGeo.setAttribute('position', new THREE.Float32BufferAttribute(solidPositions, 3));
      solidGeo.setAttribute('normal', new THREE.Float32BufferAttribute(solidNormals, 3));
      solidGeo.setAttribute('uv', new THREE.Float32BufferAttribute(solidUvs, 2));
      solidGeo.setAttribute('color', new THREE.Float32BufferAttribute(solidColors, 3));

      const solidMesh = new THREE.Mesh(solidGeo, atlas.material);
      solidMesh.name = `SolidMesh_${chunk.cx}_${chunk.cz}`;
      solidMesh.receiveShadow = true;
      solidMesh.castShadow = true;
      chunkGroup.add(solidMesh);
    }

    // 2. Translucent Water Mesh (Shimmering river surface over sandy riverbed)
    if (waterPositions.length > 0) {
      const waterGeo = new THREE.BufferGeometry();
      waterGeo.setAttribute('position', new THREE.Float32BufferAttribute(waterPositions, 3));
      waterGeo.setAttribute('normal', new THREE.Float32BufferAttribute(waterNormals, 3));
      waterGeo.setAttribute('uv', new THREE.Float32BufferAttribute(waterUvs, 2));
      waterGeo.setAttribute('color', new THREE.Float32BufferAttribute(waterColors, 3));

      const waterMesh = new THREE.Mesh(waterGeo, atlas.waterMaterial);
      waterMesh.name = `WaterMesh_${chunk.cx}_${chunk.cz}`;
      waterMesh.renderOrder = 1;
      chunkGroup.add(waterMesh);
    }

    return chunkGroup;
  }

  static buildCrossFoliage(chunk, x, y, z, block, atlas, positions, normals, uvs, colors) {
    const slot = atlas.getBlockFaceSlot(block, 0);
    const { uMin, uMax, vMin, vMax } = atlas.getUVs(slot);

    // Vanilla 1.19 Natural Positional Jitter
    const hash = Math.abs((x * 127 + z * 311) ^ (chunk.cx * 53 + chunk.cz * 17));
    const jitterX = (((hash % 100) / 100) - 0.5) * 0.38;
    const jitterZ = ((((hash >> 3) % 100) / 100) - 0.5) * 0.38;

    const w = 0.5;
    const h = 0.92;
    const cx = x + 0.5 + jitterX;
    const cz = z + 0.5 + jitterZ;

    // Plane 1 (Diagonal 1)
    const p1_0 = [cx - w, y, cz - w];
    const p1_1 = [cx + w, y, cz + w];
    const p1_2 = [cx + w, y + h, cz + w];
    const p1_3 = [cx - w, y + h, cz - w];

    // Plane 2 (Diagonal 2)
    const p2_0 = [cx - w, y, cz + w];
    const p2_1 = [cx + w, y, cz - w];
    const p2_2 = [cx + w, y + h, cz - w];
    const p2_3 = [cx - w, y + h, cz + w];

    const addQuad = (v0, v1, v2, v3) => {
      positions.push(...v0, ...v1, ...v2, ...v0, ...v2, ...v3);
      normals.push(0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0);
      uvs.push(uMin, vMin, uMax, vMin, uMax, vMax, uMin, vMin, uMax, vMax, uMin, vMax);
      for (let i = 0; i < 6; i++) colors.push(0.92, 0.92, 0.92);
    };

    addQuad(p1_0, p1_1, p1_2, p1_3);
    addQuad(p2_0, p2_1, p2_2, p2_3);
  }
}
