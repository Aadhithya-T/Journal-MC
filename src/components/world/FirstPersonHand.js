import * as THREE from 'three';

export class FirstPersonHand {
  constructor() {
    this.group = new THREE.Group();
    this.group.name = 'FirstPersonViewModel';

    // Animation States
    this.bobTimer = 0;
    this.swingProgress = 0;
    this.isSwinging = false;
    this.swingDuration = 0.28;

    // Default rest position (matching screenshot bottom-right angle)
    this.restPosition = new THREE.Vector3(0.44, -0.40, -0.58);
    this.restRotation = new THREE.Euler(0.24, -0.42, 0.12);

    this.group.position.copy(this.restPosition);
    this.group.rotation.copy(this.restRotation);

    this.buildBareSteveFist();
  }

  buildBareSteveFist() {
    this.armPivot = new THREE.Group();
    this.armPivot.position.set(0, 0, 0);
    this.group.add(this.armPivot);

    // Procedural 16x16 Pixel Art Skin Texture Canvas
    const canvas = document.createElement('canvas');
    canvas.width = 16;
    canvas.height = 32;
    const ctx = canvas.getContext('2d');
    ctx.imageSmoothingEnabled = false;

    // Skin base tone
    ctx.fillStyle = '#c48a58';
    ctx.fillRect(0, 0, 16, 32);

    // Shaded skin facets
    ctx.fillStyle = '#b37848';
    for (let y = 0; y < 32; y += 4) {
      for (let x = 0; x < 16; x += 4) {
        if ((x + y) % 8 === 0) ctx.fillRect(x, y, 4, 4);
      }
    }

    // Knuckle / Fingernail creases on hand
    ctx.fillStyle = '#9e6438';
    ctx.fillRect(2, 24, 12, 2);
    ctx.fillRect(4, 28, 8, 2);

    // Cyan shirt sleeve cuff
    ctx.fillStyle = '#00a8a8';
    ctx.fillRect(0, 0, 16, 8);
    ctx.fillStyle = '#008585';
    ctx.fillRect(0, 6, 16, 2);

    const skinTex = new THREE.CanvasTexture(canvas);
    skinTex.magFilter = THREE.NearestFilter;
    skinTex.minFilter = THREE.NearestFilter;
    skinTex.colorSpace = THREE.SRGBColorSpace;

    const armMat = new THREE.MeshLambertMaterial({
      map: skinTex,
      side: THREE.FrontSide
    });

    const armGeo = new THREE.BoxGeometry(0.24, 0.58, 0.24);
    const armMesh = new THREE.Mesh(armGeo, armMat);
    armMesh.position.set(0, -0.12, 0);
    this.armPivot.add(armMesh);
  }

  triggerSwing() {
    this.isSwinging = true;
    this.swingProgress = 0;
  }

  update(deltaTime, isMoving, isSprinting, isSneaking) {
    // 1. View Bobbing Physics
    if (isMoving) {
      const bobFreq = isSprinting ? 12.0 : (isSneaking ? 5.0 : 8.5);
      const bobAmpX = isSprinting ? 0.038 : 0.024;
      const bobAmpY = isSprinting ? 0.032 : 0.020;

      this.bobTimer += deltaTime * bobFreq;

      const bobX = Math.sin(this.bobTimer) * bobAmpX;
      const bobY = -Math.abs(Math.cos(this.bobTimer)) * bobAmpY;
      const bobRotZ = Math.sin(this.bobTimer) * 0.04;

      this.group.position.x = this.restPosition.x + bobX;
      this.group.position.y = this.restPosition.y + bobY;
      this.group.rotation.z = this.restRotation.z + bobRotZ;
    } else {
      this.bobTimer += deltaTime * 2.0;
      const idleY = Math.sin(this.bobTimer) * 0.004;
      this.group.position.x = THREE.MathUtils.lerp(this.group.position.x, this.restPosition.x, deltaTime * 8);
      this.group.position.y = THREE.MathUtils.lerp(this.group.position.y, this.restPosition.y + idleY, deltaTime * 8);
      this.group.rotation.z = THREE.MathUtils.lerp(this.group.rotation.z, this.restRotation.z, deltaTime * 8);
    }

    // 2. Minecraft Arm Swing Arc
    if (this.isSwinging) {
      this.swingProgress += deltaTime / this.swingDuration;

      if (this.swingProgress >= 1.0) {
        this.swingProgress = 0;
        this.isSwinging = false;
        this.armPivot.rotation.set(0, 0, 0);
        this.armPivot.position.set(0, 0, 0);
      } else {
        const swingFactor = Math.sin(this.swingProgress * Math.PI);
        this.armPivot.rotation.x = -swingFactor * 1.15;
        this.armPivot.rotation.y = swingFactor * 0.65;
        this.armPivot.rotation.z = -swingFactor * 0.35;
        this.armPivot.position.y = -swingFactor * 0.09;
        this.armPivot.position.z = -swingFactor * 0.14;
      }
    } else {
      this.armPivot.rotation.x = THREE.MathUtils.lerp(this.armPivot.rotation.x, 0, deltaTime * 15);
      this.armPivot.rotation.y = THREE.MathUtils.lerp(this.armPivot.rotation.y, 0, deltaTime * 15);
      this.armPivot.rotation.z = THREE.MathUtils.lerp(this.armPivot.rotation.z, 0, deltaTime * 15);
      this.armPivot.position.set(0, 0, 0);
    }
  }
}
