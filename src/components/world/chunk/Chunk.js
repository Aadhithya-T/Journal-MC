import { ChunkMeshBuilder } from './ChunkMeshBuilder';
import { BLOCK } from './TextureAtlas';

export class Chunk {
  constructor(cx, cz, blocksData, manager) {
    this.cx = cx;
    this.cz = cz;
    this.manager = manager;
    this.size = 16;
    this.height = 32;
    this.blocks = blocksData;
    this.mesh = null;
    this.isDirty = false;
  }

  getIndex(x, y, z) {
    return (y * this.size + z) * this.size + x;
  }

  getBlock(x, y, z) {
    if (x < 0 || x >= this.size || z < 0 || z >= this.size || y < 0 || y >= this.height) {
      return BLOCK.AIR;
    }
    return this.blocks[this.getIndex(x, y, z)];
  }

  setBlock(x, y, z, type) {
    if (x < 0 || x >= this.size || z < 0 || z >= this.size || y < 0 || y >= this.height) {
      return;
    }
    this.blocks[this.getIndex(x, y, z)] = type;
    this.isDirty = true;
  }

  getNeighborBlock(wx, y, wz) {
    if (this.manager) {
      const globalX = this.cx * this.size + wx;
      const globalZ = this.cz * this.size + wz;
      return this.manager.getBlockAt(globalX, y, globalZ);
    }
    return BLOCK.AIR;
  }

  rebuildMesh(atlas) {
    this.dispose();
    this.mesh = ChunkMeshBuilder.buildChunkMesh(this, atlas);
    this.isDirty = false;
    return this.mesh;
  }

  dispose() {
    if (this.mesh) {
      if (this.mesh.isGroup) {
        this.mesh.traverse((child) => {
          if (child.geometry) child.geometry.dispose();
        });
      } else if (this.mesh.geometry) {
        this.mesh.geometry.dispose();
      }
      this.mesh = null;
    }
  }
}
