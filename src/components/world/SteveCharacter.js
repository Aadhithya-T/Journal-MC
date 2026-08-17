import * as THREE from 'three';

/**
 * Creates procedural canvas textures for Minecraft Steve character parts.
 */
function createSteveTextures() {
  const createTexture = (width, height, drawFn) => {
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
  };

  // Face / Front Head Texture
  const headFrontTexture = createTexture(64, 64, (ctx) => {
    // Skin base
    ctx.fillStyle = '#cba383';
    ctx.fillRect(0, 0, 64, 64);
    // Dark brown hair on top
    ctx.fillStyle = '#3a2512';
    ctx.fillRect(0, 0, 64, 20);
    // Left eye (white + blue)
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(10, 26, 16, 10);
    ctx.fillStyle = '#2b5ea8';
    ctx.fillRect(10, 26, 8, 10);
    // Right eye
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(38, 26, 16, 10);
    ctx.fillStyle = '#2b5ea8';
    ctx.fillRect(46, 26, 8, 10);
    // Nose / beard
    ctx.fillStyle = '#4a301a';
    ctx.fillRect(20, 42, 24, 10);
    ctx.fillStyle = '#946038';
    ctx.fillRect(24, 40, 16, 4);
  });

  // Hair Texture for Top/Sides/Back of Head
  const hairTexture = createTexture(32, 32, (ctx) => {
    ctx.fillStyle = '#3a2512';
    ctx.fillRect(0, 0, 32, 32);
    ctx.fillStyle = '#27170a';
    for (let i = 0; i < 30; i++) {
      ctx.fillRect(Math.floor(Math.random() * 32), Math.floor(Math.random() * 32), 2, 2);
    }
  });

  // Torso / Cyan Shirt Texture
  const shirtTexture = createTexture(32, 32, (ctx) => {
    ctx.fillStyle = '#00a8a8';
    ctx.fillRect(0, 0, 32, 32);
    ctx.fillStyle = '#cba383';
    ctx.fillRect(10, 0, 12, 8);
  });

  // Arm Texture (Top shirt, bottom skin)
  const armTexture = createTexture(32, 32, (ctx) => {
    ctx.fillStyle = '#00a8a8';
    ctx.fillRect(0, 0, 32, 12);
    ctx.fillStyle = '#cba383';
    ctx.fillRect(0, 12, 32, 20);
  });

  // Leg Texture (Blue jeans + dark shoes)
  const legTexture = createTexture(32, 32, (ctx) => {
    ctx.fillStyle = '#1e3a8a';
    ctx.fillRect(0, 0, 32, 24);
    ctx.fillStyle = '#1f2937';
    ctx.fillRect(0, 24, 32, 8);
  });

  return { headFrontTexture, hairTexture, shirtTexture, armTexture, legTexture };
}

export class SteveCharacter {
  constructor() {
    this.group = new THREE.Group();
    this.group.name = 'SteveCharacter';

    const textures = createSteveTextures();

    const headFrontMat = new THREE.MeshLambertMaterial({ map: textures.headFrontTexture });
    const hairMat = new THREE.MeshLambertMaterial({ map: textures.hairTexture });
    const shirtMat = new THREE.MeshLambertMaterial({ map: textures.shirtTexture });
    const armMat = new THREE.MeshLambertMaterial({ map: textures.armTexture });
    const legMat = new THREE.MeshLambertMaterial({ map: textures.legTexture });
    const skinMat = new THREE.MeshLambertMaterial({ color: 0xcba383 });

    // Steve Dimensions (1 unit = 1 block):
    // Total Height = 2.0 blocks
    // Feet: y = 0.0 to 0.75
    // Torso: y = 0.75 to 1.50
    // Head: y = 1.50 to 2.00
    const headGeo = new THREE.BoxGeometry(0.5, 0.5, 0.5);
    const torsoGeo = new THREE.BoxGeometry(0.5, 0.75, 0.25);
    const armGeo = new THREE.BoxGeometry(0.25, 0.75, 0.25);
    const legGeo = new THREE.BoxGeometry(0.24, 0.75, 0.24);

    // --- LEGS (Feet bottom at y = 0.0) ---
    this.leftLegGroup = new THREE.Group();
    this.leftLegGroup.position.set(-0.13, 0.75, 0);
    this.leftLeg = new THREE.Mesh(legGeo, legMat);
    this.leftLeg.position.set(0, -0.375, 0);
    this.leftLeg.castShadow = true;
    this.leftLegGroup.add(this.leftLeg);
    this.group.add(this.leftLegGroup);

    this.rightLegGroup = new THREE.Group();
    this.rightLegGroup.position.set(0.13, 0.75, 0);
    this.rightLeg = new THREE.Mesh(legGeo, legMat);
    this.rightLeg.position.set(0, -0.375, 0);
    this.rightLeg.castShadow = true;
    this.rightLegGroup.add(this.rightLeg);
    this.group.add(this.rightLegGroup);

    // --- TORSO (y = 0.75 to 1.50) ---
    this.torso = new THREE.Mesh(torsoGeo, shirtMat);
    this.torso.position.set(0, 1.125, 0);
    this.torso.castShadow = true;
    this.group.add(this.torso);

    // --- HEAD (y = 1.50 to 2.00) ---
    this.head = new THREE.Mesh(headGeo, [
      hairMat, hairMat, hairMat, skinMat, headFrontMat, hairMat
    ]);
    this.head.position.set(0, 1.75, 0);
    this.head.castShadow = true;
    this.group.add(this.head);

    // --- ARMS (Shoulder pivot at y = 1.50) ---
    this.leftArmGroup = new THREE.Group();
    this.leftArmGroup.position.set(-0.375, 1.50, 0);
    this.leftArm = new THREE.Mesh(armGeo, armMat);
    this.leftArm.position.set(0, -0.375, 0);
    this.leftArm.castShadow = true;
    this.leftArmGroup.add(this.leftArm);
    this.group.add(this.leftArmGroup);

    this.rightArmGroup = new THREE.Group();
    this.rightArmGroup.position.set(0.375, 1.50, 0);
    this.rightArm = new THREE.Mesh(armGeo, armMat);
    this.rightArm.position.set(0, -0.375, 0);
    this.rightArm.castShadow = true;
    this.rightArmGroup.add(this.rightArm);
    this.group.add(this.rightArmGroup);

    // Held Quill
    const quillGeo = new THREE.BoxGeometry(0.08, 0.35, 0.04);
    const quillMat = new THREE.MeshLambertMaterial({ color: 0xffff55 });
    this.quill = new THREE.Mesh(quillGeo, quillMat);
    this.quill.position.set(0, -0.35, 0.15);
    this.quill.rotation.x = Math.PI / 4;
    this.rightArmGroup.add(this.quill);

    this.walkAnimTime = 0;
    this.isMiningAnim = false;
    this.miningTime = 0;
  }

  setPosition(x, y, z) {
    this.group.position.set(x, y, z);
  }

  setRotation(yRad) {
    this.group.rotation.y = yRad;
  }

  setVisible(visible) {
    this.group.visible = visible;
  }

  triggerMining() {
    this.isMiningAnim = true;
    this.miningTime = 0;
  }

  updateAnimation(deltaTime, isMoving, isGrounded = true, isSneaking = false) {
    // Crouch pose when sneaking
    if (isSneaking) {
      this.torso.position.y = 1.0;
      this.head.position.y = 1.6;
      this.leftArmGroup.position.y = 1.35;
      this.rightArmGroup.position.y = 1.35;
      this.torso.rotation.x = 0.25;
      this.head.rotation.x = -0.15;
    } else {
      this.torso.position.y = 1.125;
      this.head.position.y = 1.75;
      this.leftArmGroup.position.y = 1.50;
      this.rightArmGroup.position.y = 1.50;
      this.torso.rotation.x = 0;
      this.head.rotation.x = 0;
    }

    if (!isGrounded) {
      this.leftLegGroup.rotation.x = THREE.MathUtils.lerp(this.leftLegGroup.rotation.x, -0.4, 0.2);
      this.rightLegGroup.rotation.x = THREE.MathUtils.lerp(this.rightLegGroup.rotation.x, 0.4, 0.2);
      this.leftArmGroup.rotation.x = THREE.MathUtils.lerp(this.leftArmGroup.rotation.x, -0.5, 0.2);
      if (!this.isMiningAnim) {
        this.rightArmGroup.rotation.x = THREE.MathUtils.lerp(this.rightArmGroup.rotation.x, -0.5, 0.2);
      }
    } else if (isMoving) {
      const animSpeed = isSneaking ? 5 : 8;
      this.walkAnimTime += deltaTime * animSpeed;
      const angle = Math.sin(this.walkAnimTime) * (isSneaking ? 0.35 : 0.6);

      this.leftLegGroup.rotation.x = angle;
      this.rightLegGroup.rotation.x = -angle;
      this.leftArmGroup.rotation.x = -angle * 0.8;

      if (!this.isMiningAnim) {
        this.rightArmGroup.rotation.x = angle * 0.8;
      }
    } else {
      this.walkAnimTime = 0;
      this.leftLegGroup.rotation.x *= 0.8;
      this.rightLegGroup.rotation.x *= 0.8;
      this.leftArmGroup.rotation.x *= 0.8;

      if (!this.isMiningAnim) {
        this.rightArmGroup.rotation.x *= 0.8;
      }
    }

    if (this.isMiningAnim) {
      this.miningTime += deltaTime * 14;
      this.rightArmGroup.rotation.x = -Math.abs(Math.sin(this.miningTime)) * 1.5 - 0.3;
      if (this.miningTime > Math.PI * 2) {
        this.isMiningAnim = false;
        this.rightArmGroup.rotation.x = 0;
      }
    }
  }
}
