import * as THREE from 'three';

export class FirstPersonHand {
  constructor() {
    this.group = new THREE.Group();
    this.group.name = 'FirstPersonViewModel';

    // Animation States
    this.bobTimer = 0;
    this.swingProgress = 0;
    this.isSwinging = false;
    this.swingDuration = 0.24;
    this.currentSlot = 0;

    // Default rest position (matching first-person Minecraft view)
    this.restPosition = new THREE.Vector3(0.42, -0.38, -0.55);
    this.restRotation = new THREE.Euler(0.22, -0.38, 0.10);

    this.group.position.copy(this.restPosition);
    this.group.rotation.copy(this.restRotation);

    this.heldItemContainer = new THREE.Group();
    this.group.add(this.heldItemContainer);

    this.buildArmModel();
    this.setHeldItemSlot(0);
  }

  buildArmModel() {
    this.armPivot = new THREE.Group();
    this.heldItemContainer.add(this.armPivot);

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

    const armGeo = new THREE.BoxGeometry(0.22, 0.54, 0.22);
    this.armMesh = new THREE.Mesh(armGeo, armMat);
    this.armMesh.position.set(0, -0.12, 0);
    this.armMesh.castShadow = true;
    this.armPivot.add(this.armMesh);

    // Slot for holding 3D item attached to the hand
    this.itemAttachment = new THREE.Group();
    this.itemAttachment.position.set(0, -0.32, -0.05);
    this.armPivot.add(this.itemAttachment);
  }

  setHeldItemSlot(slotIndex) {
    this.currentSlot = slotIndex;
    while (this.itemAttachment.children.length > 0) {
      this.itemAttachment.remove(this.itemAttachment.children[0]);
    }

    const woodMat = new THREE.MeshLambertMaterial({ color: 0x855025 });
    const diamondMat = new THREE.MeshLambertMaterial({ color: 0x55ffff });
    const leatherMat = new THREE.MeshLambertMaterial({ color: 0x5c3218 });
    const paperMat = new THREE.MeshLambertMaterial({ color: 0xe8dcbe });
    const featherMat = new THREE.MeshLambertMaterial({ color: 0xf5f5f5 });
    const torchGlowMat = new THREE.MeshBasicMaterial({ color: 0xffaa00 });

    if (slotIndex === 0) {
      // 📖 3D Book & Quill
      const book = new THREE.Group();
      const cover = new THREE.Mesh(new THREE.BoxGeometry(0.24, 0.32, 0.06), leatherMat);
      const pages = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.30, 0.04), paperMat);
      pages.position.set(0.01, 0, 0.01);
      const quill = new THREE.Mesh(new THREE.BoxGeometry(0.03, 0.24, 0.03), featherMat);
      quill.position.set(0.10, 0.12, 0.04);
      quill.rotation.z = -0.35;
      book.add(cover, pages, quill);
      book.rotation.set(0.4, 0.3, -0.2);
      this.itemAttachment.add(book);
    } else if (slotIndex === 1) {
      // ⛏️ 3D Diamond Pickaxe
      const pickaxe = new THREE.Group();
      const stick = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.55, 0.05), woodMat);
      const head = new THREE.Mesh(new THREE.BoxGeometry(0.38, 0.08, 0.08), diamondMat);
      head.position.set(0, 0.26, 0);
      const leftTip = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.10, 0.08), diamondMat);
      leftTip.position.set(-0.16, 0.20, 0);
      const rightTip = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.10, 0.08), diamondMat);
      rightTip.position.set(0.16, 0.20, 0);
      pickaxe.add(stick, head, leftTip, rightTip);
      pickaxe.position.set(0, 0.12, -0.15);
      pickaxe.rotation.set(-0.2, 0.2, -0.4);
      this.itemAttachment.add(pickaxe);
    } else if (slotIndex === 2) {
      // 🪵 3D Oak Planks Block
      const blockMat = new THREE.MeshLambertMaterial({ color: 0x966838 });
      const block = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.22, 0.22), blockMat);
      block.position.set(0, 0.05, -0.12);
      block.rotation.set(0.3, 0.4, 0);
      this.itemAttachment.add(block);
    } else if (slotIndex === 3) {
      // 🌹 3D Wild Poppy Flower
      const flower = new THREE.Group();
      const stem = new THREE.Mesh(new THREE.BoxGeometry(0.03, 0.28, 0.03), new THREE.MeshLambertMaterial({ color: 0x3d7428 }));
      const petal = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.12, 0.14), new THREE.MeshLambertMaterial({ color: 0xdd2222 }));
      petal.position.set(0, 0.14, 0);
      flower.add(stem, petal);
      flower.position.set(0, 0.08, -0.10);
      flower.rotation.set(0.2, 0.1, -0.2);
      this.itemAttachment.add(flower);
    } else if (slotIndex === 4) {
      // 🕯️ 3D Torch with Glowing Ember
      const torch = new THREE.Group();
      const stick = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.38, 0.05), woodMat);
      const head = new THREE.Mesh(new THREE.BoxGeometry(0.07, 0.09, 0.07), torchGlowMat);
      head.position.set(0, 0.18, 0);
      torch.add(stick, head);
      torch.position.set(0, 0.10, -0.12);
      torch.rotation.set(0.3, 0.1, -0.1);
      this.itemAttachment.add(torch);
    } else if (slotIndex === 5) {
      // 🪨 Cobblestone Block
      const cobbleMat = new THREE.MeshLambertMaterial({ color: 0x5a5a5a });
      const block = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.22, 0.22), cobbleMat);
      block.position.set(0, 0.05, -0.12);
      block.rotation.set(0.3, 0.4, 0);
      this.itemAttachment.add(block);
    } else if (slotIndex === 6) {
      // 🏖️ Sand Block
      const sandMat = new THREE.MeshLambertMaterial({ color: 0xdbd3a0 });
      const block = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.22, 0.22), sandMat);
      block.position.set(0, 0.05, -0.12);
      block.rotation.set(0.3, 0.4, 0);
      this.itemAttachment.add(block);
    } else if (slotIndex === 7) {
      // 🌲 Birch Wood Log
      const birchMat = new THREE.MeshLambertMaterial({ color: 0xeaeaea });
      const block = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.22, 0.22), birchMat);
      block.position.set(0, 0.05, -0.12);
      block.rotation.set(0.3, 0.4, 0);
      this.itemAttachment.add(block);
    }
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
