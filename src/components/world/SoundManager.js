// Zero-dependency Web Audio API Procedural Sound Synthesizer for Minecraft Audio
class SoundManager {
  constructor() {
    this.ctx = null;
    this.lastFootstepTime = 0;
  }

  init() {
    if (!this.ctx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (AudioContext) {
        this.ctx = new AudioContext();
      }
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  // Helper: Create a noise buffer
  createNoiseBuffer(duration = 0.15) {
    if (!this.ctx) return null;
    const bufferSize = this.ctx.sampleRate * duration;
    const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = Math.random() * 2 - 1;
    }
    return buffer;
  }

  // 1. Material Footstep Sound
  playFootstep(material = 'grass') {
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    if (now - this.lastFootstepTime < 0.18) return; // Prevent audio overlap
    this.lastFootstepTime = now;

    const noise = this.ctx.createBufferSource();
    noise.buffer = this.createNoiseBuffer(0.08);

    const filter = this.ctx.createBiquadFilter();
    const gain = this.ctx.createGain();

    switch (material) {
      case 'stone':
        filter.type = 'bandpass';
        filter.frequency.setValueAtTime(800 + Math.random() * 300, now);
        filter.Q.setValueAtTime(3.0, now);
        gain.gain.setValueAtTime(0.22, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.06);
        break;
      case 'wood':
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(450 + Math.random() * 100, now);
        gain.gain.setValueAtTime(0.25, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.07);
        break;
      case 'sand':
        filter.type = 'bandpass';
        filter.frequency.setValueAtTime(550 + Math.random() * 150, now);
        gain.gain.setValueAtTime(0.18, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.08);
        break;
      case 'water':
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(350 + Math.random() * 80, now);
        gain.gain.setValueAtTime(0.28, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.12);
        break;
      case 'grass':
      default:
        filter.type = 'bandpass';
        filter.frequency.setValueAtTime(600 + Math.random() * 200, now);
        filter.Q.setValueAtTime(1.8, now);
        gain.gain.setValueAtTime(0.20, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.06);
        break;
    }

    noise.connect(filter);
    filter.connect(gain);
    gain.connect(this.ctx.destination);

    noise.start(now);
    noise.stop(now + 0.09);
  }

  // 2. Mining Hit Tick Sound
  playMiningHit(material = 'stone') {
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    const noise = this.ctx.createBufferSource();
    noise.buffer = this.createNoiseBuffer(0.06);

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.setValueAtTime(material === 'wood' ? 500 : (material === 'grass' ? 400 : 1100), now);
    filter.Q.setValueAtTime(2.5, now);

    const gain = this.ctx.createGain();
    gain.gain.setValueAtTime(0.25, now);
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.05);

    noise.connect(filter);
    filter.connect(gain);
    gain.connect(this.ctx.destination);

    noise.start(now);
    noise.stop(now + 0.06);
  }

  // 3. Block Break Crunch Sound
  playBlockBreak(material = 'stone') {
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    const noise = this.ctx.createBufferSource();
    noise.buffer = this.createNoiseBuffer(0.22);

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.setValueAtTime(material === 'wood' ? 450 : (material === 'grass' ? 380 : 900), now);
    filter.frequency.linearRampToValueAtTime(300, now + 0.2);

    const gain = this.ctx.createGain();
    gain.gain.setValueAtTime(0.45, now);
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.20);

    noise.connect(filter);
    filter.connect(gain);
    gain.connect(this.ctx.destination);

    noise.start(now);
    noise.stop(now + 0.22);
  }

  // 4. Block Place "Thud/Pop" Sound
  playBlockPlace(material = 'stone') {
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const oscGain = this.ctx.createGain();

    osc.type = 'triangle';
    osc.frequency.setValueAtTime(material === 'wood' ? 140 : 220, now);
    osc.frequency.exponentialRampToValueAtTime(70, now + 0.08);

    oscGain.gain.setValueAtTime(0.35, now);
    oscGain.gain.exponentialRampToValueAtTime(0.01, now + 0.08);

    osc.connect(oscGain);
    oscGain.connect(this.ctx.destination);

    osc.start(now);
    osc.stop(now + 0.09);

    // Subtle noise click
    const noise = this.ctx.createBufferSource();
    noise.buffer = this.createNoiseBuffer(0.04);
    const noiseGain = this.ctx.createGain();
    noiseGain.gain.setValueAtTime(0.15, now);
    noiseGain.gain.exponentialRampToValueAtTime(0.01, now + 0.04);
    noise.connect(noiseGain);
    noiseGain.connect(this.ctx.destination);
    noise.start(now);
    noise.stop(now + 0.04);
  }

  // 5. Page Flip / Book Opening Sound
  playPageFlip() {
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    const noise = this.ctx.createBufferSource();
    noise.buffer = this.createNoiseBuffer(0.18);

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.setValueAtTime(1400, now);
    filter.frequency.exponentialRampToValueAtTime(600, now + 0.16);

    const gain = this.ctx.createGain();
    gain.gain.setValueAtTime(0.28, now);
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.17);

    noise.connect(filter);
    filter.connect(gain);
    gain.connect(this.ctx.destination);

    noise.start(now);
    noise.stop(now + 0.18);
  }

  // 6. Classic "Oof" Hurt Grunt
  playHurt() {
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(160, now);
    osc.frequency.exponentialRampToValueAtTime(80, now + 0.14);

    gain.gain.setValueAtTime(0.35, now);
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.14);

    osc.connect(gain);
    gain.connect(this.ctx.destination);

    osc.start(now);
    osc.stop(now + 0.15);
  }
}

export const sounds = new SoundManager();
