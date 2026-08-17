export class JavaEngineClient {
  static baseUrl = 'http://127.0.0.1:8088';
  static isConnected = false;

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
      console.warn('[Java Engine] Could not fetch chunks from Java server:', e.message);
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
        return await res.json();
      }
    } catch (e) {
      console.warn('[Java Engine] Block break request error:', e.message);
    }
    return null;
  }
}
