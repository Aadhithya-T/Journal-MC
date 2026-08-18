export class JavaEngineClient {
  static baseUrl = 'http://127.0.0.1:8088';
  static isConnected = false;

  static base64ToFloat32Array(base64Str) {
    if (!base64Str || base64Str.length === 0) return new Float32Array(0);
    const binary = atob(base64Str);
    const len = binary.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return new Float32Array(bytes.buffer);
  }

  static async checkStatus() {
    try {
      const res = await fetch(`${this.baseUrl}/api/status`, { signal: AbortSignal.timeout(1500) });
      if (res.ok) {
        const json = await res.json();
        this.isConnected = true;
        console.log(`[Java Engine] Connected: ${json.engine}, Chunks: ${json.chunks}`);
        return json;
      }
    } catch {
      this.isConnected = false;
    }
    return null;
  }

  static async fetchPrecomputedMeshes() {
    try {
      const res = await fetch(`${this.baseUrl}/api/meshes`, { signal: AbortSignal.timeout(4000) });
      if (!res.ok) return null;
      const json = await res.json();

      const meshMap = new Map();
      for (const item of json.meshes) {
        const key = `${item.cx},${item.cz}`;
        const solid = item.solid ? {
          positions: this.base64ToFloat32Array(item.solid.pos),
          normals: this.base64ToFloat32Array(item.solid.norm),
          uvs: this.base64ToFloat32Array(item.solid.uv),
          colors: this.base64ToFloat32Array(item.solid.col)
        } : null;

        const water = item.water ? {
          positions: this.base64ToFloat32Array(item.water.pos),
          normals: this.base64ToFloat32Array(item.water.norm),
          uvs: this.base64ToFloat32Array(item.water.uv),
          colors: this.base64ToFloat32Array(item.water.col)
        } : null;

        meshMap.set(key, { cx: item.cx, cz: item.cz, solid, water });
      }
      return meshMap;
    } catch (e) {
      console.warn('[Java Engine] Could not stream precomputed meshes from Java server:', e.message);
      return null;
    }
  }

  static async fetchAllChunks() {
    try {
      const res = await fetch(`${this.baseUrl}/api/chunks`, { signal: AbortSignal.timeout(3000) });
      if (!res.ok) return null;
      const json = await res.json();

      const chunkMap = new Map();
      for (const item of json.chunks) {
        const rawBinary = atob(item.data);
        const len = rawBinary.length;
        const bytes = new Uint8Array(len);
        for (let i = 0; i < len; i++) {
          bytes[i] = rawBinary.charCodeAt(i);
        }
        chunkMap.set(`${item.cx},${item.cz}`, bytes);
      }
      return chunkMap;
    } catch (e) {
      console.warn('[Java Engine] Could not fetch raw chunks from Java server:', e.message);
      return null;
    }
  }

  static async breakBlock(wx, wy, wz) {
    if (!this.isConnected) return null;
    try {
      const res = await fetch(`${this.baseUrl}/api/break`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ x: wx, y: wy, z: wz })
      });
      if (res.ok) {
        const json = await res.json();
        if (json.updatedMesh) {
          const item = json.updatedMesh;
          json.parsedMesh = {
            cx: item.cx,
            cz: item.cz,
            solid: item.solid ? {
              positions: this.base64ToFloat32Array(item.solid.pos),
              normals: this.base64ToFloat32Array(item.solid.norm),
              uvs: this.base64ToFloat32Array(item.solid.uv),
              colors: this.base64ToFloat32Array(item.solid.col)
            } : null,
            water: item.water ? {
              positions: this.base64ToFloat32Array(item.water.pos),
              normals: this.base64ToFloat32Array(item.water.norm),
              uvs: this.base64ToFloat32Array(item.water.uv),
              colors: this.base64ToFloat32Array(item.water.col)
            } : null
          };
        }
        return json;
      }
    } catch (e) {
      console.warn('[Java Engine] Block break request error:', e.message);
    }
    return null;
  }
}
