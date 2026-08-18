import React, { useEffect, useRef, useState, useCallback } from "react";
import * as THREE from "three";
import { SteveCharacter } from "./SteveCharacter";
import { FirstPersonHand } from "./FirstPersonHand";
import { ChunkManager } from "./chunk/ChunkManager";
import { WorldHUD } from "./WorldHUD";
import { BookAndQuillModal } from "./BookAndQuillModal";
import { EscapeMenuModal } from "./EscapeMenuModal";
import { sounds } from "./SoundManager";
import { BLOCK } from "./chunk/TextureAtlas";

// --- VANILLA MINECRAFT CONSTANTS ---
const WALK_SPEED = 4.317;        // blocks / second
const SPRINT_SPEED = 5.612;      // blocks / second (1.3x walk)
const SNEAK_SPEED = 1.31;        // blocks / second (0.3x walk)
const JUMP_VELOCITY = 8.4;       // blocks / second
const GRAVITY = 30.0;            // blocks / second^2
const PLAYER_EYE_HEIGHT = 1.62;  // blocks above feet (standing)
const SNEAK_EYE_HEIGHT = 1.27;   // blocks above feet (sneaking)
const MOUSE_SENSITIVITY = 0.0022;
const REACH_DISTANCE = 5.0;      // max block reach
const MINING_TIME = 0.35;        // seconds to mine

const HOTBAR_BLOCK_MAP = {
  0: null, // Book & Quill
  1: null, // Diamond Pickaxe
  2: BLOCK.DIRT, // Oak Planks / Earth
  3: BLOCK.POPPY, // Poppy
  4: BLOCK.OAK_LOG, // Torch / Oak Wood
  5: BLOCK.COBBLESTONE, // Cobblestone
  6: BLOCK.SAND, // Sand
  7: BLOCK.BIRCH_LOG, // Birch Log
  8: BLOCK.STONE // Stone
};

export function MinecraftWorldCanvas({ world, entries = [], onAddEntry }) {
  const mountRef = useRef(null);

  // HUD & Game States
  const [steveCoords, setSteveCoords] = useState({ x: 0, y: 5, z: 0 });
  const [nearbyPOI, setNearbyPOI] = useState(null);
  const [isBookModalOpen, setIsBookModalOpen] = useState(false);
  const [activePOIForModal, setActivePOIForModal] = useState(null);
  const [toastMessage, setToastMessage] = useState("🎮 Left-Click: Mine | Right-Click: Place/Interact | 1-9: Hotbar");
  const [texturePack, setTexturePack] = useState("faithful64");
  const [isEscapeMenuOpen, setIsEscapeMenuOpen] = useState(false);
  const [showDrawer, setShowDrawer] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState(0);

  // Movement States
  const [isSprinting, setIsSprinting] = useState(false);
  const [isSneaking, setIsSneaking] = useState(false);
  const [isLocked, setIsLocked] = useState(false);

  // References for render loop
  const steveRef = useRef(null);
  const fppHandRef = useRef(null);
  const chunkManagerRef = useRef(null);
  const keysRef = useRef({});
  const lastToastTimeoutRef = useRef(null);
  const lastWPressTimeRef = useRef(0);
  const isSprintingRef = useRef(false);
  const isSneakingRef = useRef(false);
  const isLeftMouseDownRef = useRef(false);
  const miningProgressRef = useRef(0);
  const miningHitSoundTimerRef = useRef(0);
  const footstepTimerRef = useRef(0);
  const currentMiningTargetRef = useRef(null);
  const isEscapeMenuOpenRef = useRef(false);
  const isBookModalOpenRef = useRef(false);
  const showDrawerRef = useRef(false);
  const steveCoordsRef = useRef({ x: 0, y: 5, z: 0 });
  const targetedBlockRef = useRef(null);
  const selectedSlotRef = useRef(0);
  const onAddEntryRef = useRef(onAddEntry);
  const worldRef = useRef(world);
  const particleGroupRef = useRef(null);
  const waterAnimTimerRef = useRef(0);
  const posSaveTimerRef = useRef(0);

  // Sync refs
  onAddEntryRef.current = onAddEntry;
  worldRef.current = world;
  isEscapeMenuOpenRef.current = isEscapeMenuOpen;
  isBookModalOpenRef.current = isBookModalOpen;
  showDrawerRef.current = showDrawer;
  selectedSlotRef.current = selectedSlot;

  // Camera Orientation Refs
  const targetYawRef = useRef(0);
  const targetPitchRef = useRef(0);
  const cameraYawRef = useRef(0);
  const cameraPitchRef = useRef(0);
  const currentFovRef = useRef(70);
  const currentEyeHeightRef = useRef(PLAYER_EYE_HEIGHT);

  // Physics Velocity State Refs
  const velocityXRef = useRef(0);
  const velocityZRef = useRef(0);
  const velocityYRef = useRef(0);
  const isGroundedRef = useRef(true);

  const showToast = useCallback((msg) => {
    setToastMessage(msg);
    if (lastToastTimeoutRef.current) clearTimeout(lastToastTimeoutRef.current);
    lastToastTimeoutRef.current = setTimeout(() => {
      setToastMessage("");
    }, 3500);
  }, []);

  const handleSelectSlot = useCallback((slotIdx) => {
    const clamped = Math.max(0, Math.min(8, slotIdx));
    setSelectedSlot(clamped);
    selectedSlotRef.current = clamped;
    if (fppHandRef.current) {
      fppHandRef.current.setHeldItemSlot(clamped);
    }
  }, []);

  const handleSelectTexturePack = (packId) => {
    setTexturePack(packId);
    if (chunkManagerRef.current) {
      chunkManagerRef.current.switchTexturePack(packId);
      showToast(`🎨 Texture Pack: ${packId.toUpperCase()}`);
    }
  };

  const handleOpenBookModal = useCallback((poi = null) => {
    sounds.playPageFlip();
    setActivePOIForModal(poi);
    setIsBookModalOpen(true);
    setIsEscapeMenuOpen(false);
    if (document.exitPointerLock) {
      document.exitPointerLock();
    }
  }, []);

  const handleSaveEntry = async (entryData) => {
    await onAddEntry(entryData);
    sounds.playPageFlip();
    showToast(`✍️ Recorded: "${entryData.title}"`);
    if (fppHandRef.current) fppHandRef.current.triggerSwing();
    if (steveRef.current) steveRef.current.triggerMining();
  };

  // Block Break Particle Burst
  const spawnBreakParticles = useCallback((scene, bx, by, bz, colorHex) => {
    const pCount = 18;
    const geo = new THREE.BoxGeometry(0.12, 0.12, 0.12);
    const mat = new THREE.MeshBasicMaterial({ color: colorHex || 0x888888 });

    const particles = [];
    for (let i = 0; i < pCount; i++) {
      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.set(
        bx + 0.5 + (Math.random() - 0.5) * 0.6,
        by + 0.5 + (Math.random() - 0.5) * 0.6,
        bz + 0.5 + (Math.random() - 0.5) * 0.6
      );
      const velocity = new THREE.Vector3(
        (Math.random() - 0.5) * 4.0,
        Math.random() * 3.5 + 1.0,
        (Math.random() - 0.5) * 4.0
      );
      scene.add(mesh);
      particles.push({ mesh, velocity, life: 0.6 });
    }

    if (particleGroupRef.current) {
      particleGroupRef.current.push(...particles);
    }
  }, []);

  // Block Break Execution
  const executeBreakBlock = useCallback(async (scene, target) => {
    if (!chunkManagerRef.current || !target) return;
    const broken = await chunkManagerRef.current.breakBlock(target.blockX, target.blockY, target.blockZ);
    if (broken) {
      sounds.playBlockBreak('stone');
      spawnBreakParticles(scene, target.blockX, target.blockY, target.blockZ, broken.color);
      showToast(`💥 Broke ${broken.name}`);
    } else {
      showToast(`🛡️ Bedrock is unbreakable!`);
    }
  }, [showToast, spawnBreakParticles]);

  // Block Placement Execution
  const executePlaceBlock = useCallback((target, slotIndex) => {
    if (!chunkManagerRef.current || !target || !target.normal) return;
    const blockType = HOTBAR_BLOCK_MAP[slotIndex];
    if (blockType === null || blockType === undefined) {
      // Slot 0 opens book & quill
      if (slotIndex === 0) {
        handleOpenBookModal(nearbyPOIRef.current);
      }
      return;
    }

    const placeX = target.blockX + target.normal[0];
    const placeY = target.blockY + target.normal[1];
    const placeZ = target.blockZ + target.normal[2];

    // Check player AABB intersection to prevent placing inside player
    if (steveRef.current) {
      const p = steveRef.current.group.position;
      const minPX = p.x - 0.28, maxPX = p.x + 0.28;
      const minPZ = p.z - 0.28, maxPZ = p.z + 0.28;
      const minPY = p.y, maxPY = p.y + 1.8;

      if (
        placeX + 1 > minPX && placeX < maxPX &&
        placeZ + 1 > minPZ && placeZ < maxPZ &&
        placeY + 1 > minPY && placeY < maxPY
      ) {
        return; // Intersects player
      }
    }

    const placed = chunkManagerRef.current.setBlockAt(placeX, placeY, placeZ, blockType);
    if (placed) {
      sounds.playBlockPlace('stone');
      if (fppHandRef.current) fppHandRef.current.triggerSwing();
      showToast(`🧱 Placed Block at (${placeX}, ${placeY}, ${placeZ})`);
    }
  }, [handleOpenBookModal, showToast]);

  // Main Three.js Scene Setup
  useEffect(() => {
    const currentMount = mountRef.current;
    if (!currentMount) return;

    // --- THREE.JS SCENE SETUP ---
    const scene = new THREE.Scene();
    const skyColor = new THREE.Color(0x78a7ff);
    scene.background = skyColor;
    const worldFog = new THREE.Fog(0x78a7ff, 25, 120);
    scene.fog = worldFog;

    const activeParticles = [];
    particleGroupRef.current = activeParticles;

    const camera = new THREE.PerspectiveCamera(
      70,
      currentMount.clientWidth / currentMount.clientHeight,
      0.05,
      350
    );

    const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: "high-performance" });
    renderer.setSize(currentMount.clientWidth, currentMount.clientHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    currentMount.appendChild(renderer.domElement);

    // --- 3D FIRST-PERSON VIEWMODEL HAND ---
    const fppHand = new FirstPersonHand();
    camera.add(fppHand.group);
    scene.add(camera);
    fppHandRef.current = fppHand;

    // --- ATMOSPHERIC SKY DOME ---
    const skyGeo = new THREE.SphereGeometry(260, 32, 15);
    const skyCanvas = document.createElement("canvas");
    skyCanvas.width = 16;
    skyCanvas.height = 256;
    const skyCtx = skyCanvas.getContext("2d");
    const skyGrad = skyCtx.createLinearGradient(0, 0, 0, 256);
    skyGrad.addColorStop(0.0, "#3b72cf");
    skyGrad.addColorStop(0.55, "#78a7ff");
    skyGrad.addColorStop(0.85, "#b5d4ff");
    skyGrad.addColorStop(1.0, "#ffffff");
    skyCtx.fillStyle = skyGrad;
    skyCtx.fillRect(0, 0, 16, 256);
    const skyTex = new THREE.CanvasTexture(skyCanvas);
    skyTex.colorSpace = THREE.SRGBColorSpace;
    const skyMat = new THREE.MeshBasicMaterial({ map: skyTex, side: THREE.BackSide, depthWrite: false });
    const skyMesh = new THREE.Mesh(skyGeo, skyMat);
    scene.add(skyMesh);

    // --- MINECRAFT SQUARE SUN ---
    const sunGroup = new THREE.Group();
    const sunGeo = new THREE.PlaneGeometry(24, 24);
    const sunMat = new THREE.MeshBasicMaterial({ color: 0xffffff, side: THREE.DoubleSide, transparent: true, opacity: 0.95 });
    const sunMesh = new THREE.Mesh(sunGeo, sunMat);
    sunMesh.position.set(90, 140, 90);
    sunMesh.lookAt(0, 0, 0);
    sunGroup.add(sunMesh);

    const glowGeo = new THREE.PlaneGeometry(42, 42);
    const glowMat = new THREE.MeshBasicMaterial({ color: 0xffe6aa, transparent: true, opacity: 0.35, side: THREE.DoubleSide });
    const glowMesh = new THREE.Mesh(glowGeo, glowMat);
    glowMesh.position.set(89.5, 139.5, 89.5);
    glowMesh.lookAt(0, 0, 0);
    sunGroup.add(glowMesh);
    scene.add(sunGroup);

    // --- DIRECTIONAL SUNLIGHT ---
    const hemiLight = new THREE.HemisphereLight(0x90c2ff, 0x3a4820, 0.45);
    scene.add(hemiLight);

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.35);
    scene.add(ambientLight);

    const sunLight = new THREE.DirectionalLight(0xfffae8, 1.75);
    sunLight.position.set(45, 90, 40);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    sunLight.shadow.camera.near = 0.5;
    sunLight.shadow.camera.far = 180;
    sunLight.shadow.camera.left = -38;
    sunLight.shadow.camera.right = 38;
    sunLight.shadow.camera.top = 38;
    sunLight.shadow.camera.bottom = -38;
    sunLight.shadow.bias = -0.0003;
    scene.add(sunLight);
    scene.add(sunLight.target);

    // --- 100-CHUNK VOXEL PIPELINE ---
    const chunkManager = new ChunkManager(5, texturePack);
    scene.add(chunkManager.group);
    chunkManagerRef.current = chunkManager;

    // --- STEVE CHARACTER ---
    const steve = new SteveCharacter();

    const saveKey = `mc_player_pos_${world?.id || 'default'}`;
    let spawnX = 6, spawnZ = -10, spawnY = null, spawnYaw = 0, spawnPitch = 0;
    const saved = localStorage.getItem(saveKey);
    if (saved) {
      try {
        const p = JSON.parse(saved);
        if (typeof p.x === 'number' && typeof p.z === 'number') {
          spawnX = p.x;
          spawnZ = p.z;
          if (typeof p.y === 'number') spawnY = p.y;
          if (typeof p.yaw === 'number') spawnYaw = p.yaw;
          if (typeof p.pitch === 'number') spawnPitch = p.pitch;
        }
      } catch (_) {}
    }

    const groundY = chunkManager.getGroundHeight(spawnX, spawnZ);
    const startY = spawnY !== null && spawnY >= groundY - 0.2 ? spawnY : groundY;
    steve.setPosition(spawnX, startY, spawnZ);
    steve.setVisible(false);
    scene.add(steve.group);
    steveRef.current = steve;

    targetYawRef.current = spawnYaw;
    cameraYawRef.current = spawnYaw;
    targetPitchRef.current = spawnPitch;
    cameraPitchRef.current = spawnPitch;

    // --- TARGETED BLOCK OUTLINE ---
    const outlineGeo = new THREE.EdgesGeometry(new THREE.BoxGeometry(1.004, 1.004, 1.004));
    const outlineMat = new THREE.LineBasicMaterial({
      color: 0x000000,
      linewidth: 2,
      transparent: true,
      opacity: 0.45
    });
    const blockOutlineBox = new THREE.LineSegments(outlineGeo, outlineMat);
    blockOutlineBox.visible = false;
    scene.add(blockOutlineBox);

    // --- 10-STAGE MINING CRACK OVERLAY DECALS ---
    const crackMat = new THREE.MeshBasicMaterial({
      transparent: true,
      opacity: 0.85,
      depthTest: true,
      polygonOffset: true,
      polygonOffsetFactor: -1,
      polygonOffsetUnits: -1
    });
    const crackBox = new THREE.Mesh(new THREE.BoxGeometry(1.008, 1.008, 1.008), crackMat);
    crackBox.visible = false;
    scene.add(crackBox);

    // Load authentic 10 breaking stage textures
    const crackTextures = [];
    const loader = new THREE.TextureLoader();
    for (let i = 0; i <= 9; i++) {
      const tex = loader.load(`./texturepacks/faithful64x/destroy_stage_${i}.png`);
      tex.magFilter = THREE.NearestFilter;
      tex.minFilter = THREE.NearestFilter;
      crackTextures.push(tex);
    }

    const updateCrackDecal = (stageIndex) => {
      if (stageIndex < 0 || stageIndex > 9 || !crackTextures[stageIndex]) {
        crackBox.visible = false;
        return;
      }
      crackMat.map = crackTextures[stageIndex];
      crackMat.needsUpdate = true;
      crackBox.visible = true;
    };

    // --- ATMOSPHERIC PARTICLES ---
    const particleCount = 160;
    const particleGeo = new THREE.BufferGeometry();
    const particlePositions = new Float32Array(particleCount * 3);
    for (let i = 0; i < particleCount * 3; i += 3) {
      particlePositions[i] = (Math.random() - 0.5) * 120;
      particlePositions[i + 1] = Math.random() * 16 + 0.5;
      particlePositions[i + 2] = (Math.random() - 0.5) * 120;
    }
    particleGeo.setAttribute("position", new THREE.BufferAttribute(particlePositions, 3));
    const particleMat = new THREE.PointsMaterial({ color: 0xfff3c4, size: 0.15, transparent: true, opacity: 0.6 });
    const particles = new THREE.Points(particleGeo, particleMat);
    scene.add(particles);

    // --- CONTINUOUS MINECRAFT CLOUDS (At Y=128) ---
    const cloudGroup = new THREE.Group();
    const cloudGeo = new THREE.PlaneGeometry(36, 24);
    const cloudMat = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.82, side: THREE.DoubleSide });
    for (let i = 0; i < 40; i++) {
      const cloud = new THREE.Mesh(cloudGeo, cloudMat);
      cloud.rotation.x = Math.PI / 2;
      cloud.position.set((Math.random() - 0.5) * 280, 36 + (i % 2) * 1.2, (Math.random() - 0.5) * 280);
      cloudGroup.add(cloud);
    }
    scene.add(cloudGroup);

    // --- POINTER LOCK CONTROLS ---
    const canvasDom = renderer.domElement;

    const handlePointerLockChange = () => {
      const isCurrentlyLocked = document.pointerLockElement === canvasDom;
      setIsLocked(isCurrentlyLocked);
    };

    document.addEventListener("pointerlockchange", handlePointerLockChange);

    const handleCanvasClick = () => {
      if (isEscapeMenuOpenRef.current || isBookModalOpenRef.current || showDrawerRef.current) return;
      if (document.pointerLockElement !== canvasDom) {
        canvasDom.requestPointerLock();
      }
    };

    canvasDom.addEventListener("click", handleCanvasClick);

    const handleMouseMove = (e) => {
      if (document.pointerLockElement !== canvasDom) return;
      if (isEscapeMenuOpenRef.current || isBookModalOpenRef.current) return;

      const movementX = e.movementX || 0;
      const movementY = e.movementY || 0;

      targetYawRef.current -= movementX * MOUSE_SENSITIVITY;
      targetPitchRef.current -= movementY * MOUSE_SENSITIVITY;

      const maxPitch = Math.PI / 2 - 0.05;
      targetPitchRef.current = THREE.MathUtils.clamp(targetPitchRef.current, -maxPitch, maxPitch);
    };

    document.addEventListener("mousemove", handleMouseMove);

    // --- MOUSE WHEEL FOR HOTBAR ---
    const handleWheel = (e) => {
      if (document.pointerLockElement !== canvasDom) return;
      const delta = Math.sign(e.deltaY);
      const nextSlot = (selectedSlotRef.current + delta + 9) % 9;
      handleSelectSlot(nextSlot);
    };
    window.addEventListener("wheel", handleWheel);

    // --- MOUSE CLICK INTERACTIONS (Mine & Place) ---
    const handleMouseDown = (e) => {
      if (document.pointerLockElement !== canvasDom) return;
      if (isEscapeMenuOpenRef.current || isBookModalOpenRef.current) return;

      if (e.button === 0) {
        // Left Click: Mine Block
        isLeftMouseDownRef.current = true;
        if (fppHandRef.current) fppHandRef.current.triggerSwing();
        if (steveRef.current) steveRef.current.triggerMining();
      } else if (e.button === 2) {
        // Right Click: Place Block or Interact with Lectern
        const target = targetedBlockRef.current;
        if (target) {
          executePlaceBlock(target, selectedSlotRef.current);
        } else {
          const currentPos = steve.group.position;
          const poi = chunkManager.getNearbyPOI(currentPos);
          if (poi) handleOpenBookModal(poi);
        }
      }
    };

    const handleMouseUp = (e) => {
      if (e.button === 0) {
        isLeftMouseDownRef.current = false;
        miningProgressRef.current = 0;
        crackBox.visible = false;
        updateCrackDecal(-1);
      }
    };

    const handleContextMenu = (e) => {
      if (document.pointerLockElement === canvasDom) {
        e.preventDefault();
      }
    };

    window.addEventListener("mousedown", handleMouseDown);
    window.addEventListener("mouseup", handleMouseUp);
    window.addEventListener("contextmenu", handleContextMenu);

    // --- KEYBOARD CONTROLS LISTENERS ---
    const handleKeyDown = (e) => {
      const key = e.key.toLowerCase();
      keysRef.current[key] = true;

      // Escape key to toggle Game Menu / release lock
      if (e.key === "Escape") {
        if (isBookModalOpenRef.current) {
          setIsBookModalOpen(false);
        } else if (showDrawerRef.current) {
          setShowDrawer(false);
        } else {
          setIsEscapeMenuOpen((prev) => !prev);
          if (document.exitPointerLock) {
            document.exitPointerLock();
          }
        }
        return;
      }

      if (isEscapeMenuOpenRef.current || isBookModalOpenRef.current) return;

      // Hotbar Slot Keys 1-9
      if (e.code.startsWith("Digit")) {
        const num = parseInt(e.code.replace("Digit", ""), 10);
        if (num >= 1 && num <= 9) {
          handleSelectSlot(num - 1);
        }
      } else if (!isNaN(parseInt(key, 10)) && parseInt(key, 10) >= 1 && parseInt(key, 10) <= 9) {
        handleSelectSlot(parseInt(key, 10) - 1);
      }

      // Sneaking on Left Shift
      if (e.key === "Shift" || e.code === "ShiftLeft") {
        isSneakingRef.current = true;
        setIsSneaking(true);
        isSprintingRef.current = false;
        setIsSprinting(false);
      }

      // Sprinting on Left Control
      if ((e.key === "Control" || e.code === "ControlLeft") && !isSneakingRef.current) {
        isSprintingRef.current = true;
        setIsSprinting(true);
      }

      // Double-Tap W to Sprint
      if (key === "w" && !isSneakingRef.current) {
        const now = performance.now();
        if (now - lastWPressTimeRef.current < 280) {
          isSprintingRef.current = true;
          setIsSprinting(true);
        }
        lastWPressTimeRef.current = now;
      }

      // Jump on Spacebar
      if (e.code === "Space") {
        if (isGroundedRef.current) {
          sounds.playFootstep('stone');
          velocityYRef.current = JUMP_VELOCITY;
          isGroundedRef.current = false;

          if (isSprintingRef.current) {
            const yaw = cameraYawRef.current;
            velocityXRef.current += Math.sin(yaw) * 1.8;
            velocityZRef.current += Math.cos(yaw) * 1.8;
          }
        }
      }

      // Interact on E key
      if (key === "e") {
        const currentPos = steve.group.position;
        const poi = chunkManager.getNearbyPOI(currentPos);
        handleOpenBookModal(poi);
      }
    };

    const handleKeyUp = (e) => {
      const key = e.key.toLowerCase();
      keysRef.current[key] = false;

      if (e.key === "Shift" || e.code === "ShiftLeft") {
        isSneakingRef.current = false;
        setIsSneaking(false);
      }

      if (e.key === "Control" || e.code === "ControlLeft") {
        isSprintingRef.current = false;
        setIsSprinting(false);
      }

      if (key === "w" && !keysRef.current["control"]) {
        isSprintingRef.current = false;
        setIsSprinting(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);

    const handleResize = () => {
      if (!currentMount) return;
      camera.aspect = currentMount.clientWidth / currentMount.clientHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(currentMount.clientWidth, currentMount.clientHeight);
    };
    window.addEventListener("resize", handleResize);

    let lastTime = performance.now();

    // --- ANIMATION & PHYSICS RENDER LOOP ---
    let animationFrameId;

    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);
      const now = performance.now();
      const deltaTime = Math.min((now - lastTime) / 1000, 0.1);
      lastTime = now;

      // Water Ripple Animation
      waterAnimTimerRef.current += deltaTime;
      if (waterAnimTimerRef.current > 0.055) {
        if (chunkManagerRef.current && chunkManagerRef.current.atlas) {
          chunkManagerRef.current.atlas.updateWaterAnimation(now * 0.001);
        }
        waterAnimTimerRef.current = 0;
      }

      // Sub-frame smooth mouse look
      cameraYawRef.current = THREE.MathUtils.lerp(cameraYawRef.current, targetYawRef.current, deltaTime * 40);
      cameraPitchRef.current = THREE.MathUtils.lerp(cameraPitchRef.current, targetPitchRef.current, deltaTime * 40);

      // Cloud drift
      cloudGroup.children.forEach((c) => {
        c.position.x += deltaTime * 0.8;
        if (c.position.x > 140) c.position.x = -140;
      });

      // Floating dust particles
      const pPositions = particleGeo.attributes.position.array;
      for (let i = 0; i < particleCount * 3; i += 3) {
        pPositions[i + 1] -= deltaTime * 0.2;
        pPositions[i] += Math.sin(now * 0.001 + i) * 0.01;
        if (pPositions[i + 1] < 0) {
          pPositions[i + 1] = 16;
        }
      }
      particleGeo.attributes.position.needsUpdate = true;

      // Particle decay
      for (let i = activeParticles.length - 1; i >= 0; i--) {
        const p = activeParticles[i];
        p.life -= deltaTime;
        p.velocity.y -= 18.0 * deltaTime;
        p.mesh.position.addScaledVector(p.velocity, deltaTime);
        p.mesh.rotation.x += deltaTime * 6;
        p.mesh.rotation.y += deltaTime * 6;

        if (p.life <= 0) {
          scene.remove(p.mesh);
          p.mesh.geometry.dispose();
          activeParticles.splice(i, 1);
        }
      }

      // Progressive Mining & 10-Stage Decals
      if (isLeftMouseDownRef.current && !isEscapeMenuOpenRef.current && !isBookModalOpenRef.current) {
        const target = targetedBlockRef.current;
        if (target) {
          if (currentMiningTargetRef.current !== `${target.blockX},${target.blockY},${target.blockZ}`) {
            currentMiningTargetRef.current = `${target.blockX},${target.blockY},${target.blockZ}`;
            miningProgressRef.current = 0;
          }

          miningProgressRef.current += deltaTime / MINING_TIME;
          const stageIndex = Math.min(9, Math.floor(miningProgressRef.current * 10));
          updateCrackDecal(stageIndex);

          crackBox.position.set(target.blockX + 0.5, target.blockY + 0.5, target.blockZ + 0.5);

          // Play mining hit tick sound periodically
          miningHitSoundTimerRef.current += deltaTime;
          if (miningHitSoundTimerRef.current > 0.18) {
            sounds.playMiningHit('stone');
            miningHitSoundTimerRef.current = 0;
          }

          if (fppHandRef.current && !fppHandRef.current.isSwinging) {
            fppHandRef.current.triggerSwing();
          }

          if (miningProgressRef.current >= 1.0) {
            executeBreakBlock(scene, target);
            miningProgressRef.current = 0;
            crackBox.visible = false;
            updateCrackDecal(-1);
          }
        } else {
          crackBox.visible = false;
          updateCrackDecal(-1);
        }
      } else {
        crackBox.visible = false;
        updateCrackDecal(-1);
      }

      // If game is paused, render scene without physics
      if (isEscapeMenuOpenRef.current || isBookModalOpenRef.current) {
        steve.updateAnimation(deltaTime, false, isGroundedRef.current, isSneakingRef.current);
        fppHand.update(deltaTime, false, false, false);
        renderer.render(scene, camera);
        return;
      }

      // --- PLAYER MOVEMENT ---
      const keys = keysRef.current;
      let forward = 0;
      let strafe = 0;

      if (keys["w"] || keys["arrowup"]) forward += 1;
      if (keys["s"] || keys["arrowdown"]) forward -= 1;
      if (keys["a"] || keys["arrowleft"]) strafe -= 1;
      if (keys["d"] || keys["arrowright"]) strafe += 1;

      let isMoving = false;
      let targetSpeed = WALK_SPEED;
      if (isSneakingRef.current) {
        targetSpeed = SNEAK_SPEED;
      } else if (isSprintingRef.current) {
        targetSpeed = SPRINT_SPEED;
      }

      // Dynamic FOV
      const targetFov = isSprintingRef.current && (forward !== 0 || strafe !== 0) ? 79 : 70;
      currentFovRef.current = THREE.MathUtils.lerp(currentFovRef.current, targetFov, deltaTime * 8);
      camera.fov = currentFovRef.current;
      camera.updateProjectionMatrix();

      // Dynamic Eye Height
      const targetEyeHeight = isSneakingRef.current ? SNEAK_EYE_HEIGHT : PLAYER_EYE_HEIGHT;
      currentEyeHeightRef.current = THREE.MathUtils.lerp(currentEyeHeightRef.current, targetEyeHeight, deltaTime * 10);

      const yaw = cameraYawRef.current;

      if (forward !== 0 || strafe !== 0) {
        isMoving = true;
        const norm = Math.sqrt(forward * forward + strafe * strafe);
        const normF = forward / norm;
        const normS = strafe / norm;

        const wishDirX = Math.sin(yaw) * normF + Math.cos(yaw) * normS;
        const wishDirZ = Math.cos(yaw) * normF - Math.sin(yaw) * normS;

        const accel = isGroundedRef.current ? 18.0 : 6.0;
        velocityXRef.current = THREE.MathUtils.lerp(velocityXRef.current, wishDirX * targetSpeed, deltaTime * accel);
        velocityZRef.current = THREE.MathUtils.lerp(velocityZRef.current, wishDirZ * targetSpeed, deltaTime * accel);

        const targetRotation = Math.atan2(wishDirX, wishDirZ);
        steve.setRotation(targetRotation);

        // Footstep Audio while moving on ground
        if (isGroundedRef.current) {
          const stepInterval = isSprintingRef.current ? 0.28 : (isSneakingRef.current ? 0.55 : 0.38);
          footstepTimerRef.current += deltaTime;
          if (footstepTimerRef.current > stepInterval) {
            sounds.playFootstep('grass');
            footstepTimerRef.current = 0;
          }
        }
      } else {
        const friction = isGroundedRef.current ? 14.0 : 3.0;
        velocityXRef.current = THREE.MathUtils.lerp(velocityXRef.current, 0, deltaTime * friction);
        velocityZRef.current = THREE.MathUtils.lerp(velocityZRef.current, 0, deltaTime * friction);
      }

      const moveStepX = velocityXRef.current * deltaTime;
      const moveStepZ = velocityZRef.current * deltaTime;

      if (Math.abs(moveStepX) > 0.0005 || Math.abs(moveStepZ) > 0.0005) {
        let curX = steve.group.position.x;
        let curY = steve.group.position.y;
        let curZ = steve.group.position.z;

        // 1. Test X Movement
        if (Math.abs(moveStepX) > 0.0005) {
          const nextX = THREE.MathUtils.clamp(curX + moveStepX, -78, 78);
          const targetGroundX = chunkManager.getGroundHeight(nextX, curZ, curY);
          const hitWallX = chunkManager.isCollidingWithSolid(nextX, curY, curZ, 0.28, 1.8);
          const canStepUpX = isGroundedRef.current && (targetGroundX <= curY + 0.55) && !chunkManager.isCollidingWithSolid(nextX, targetGroundX, curZ, 0.28, 1.8);
          const isDangerousDropX = isSneakingRef.current && isGroundedRef.current && (targetGroundX < curY - 0.6);

          if (!isDangerousDropX) {
            if (!hitWallX) {
              curX = nextX;
              if (isGroundedRef.current && targetGroundX <= curY + 0.05 && targetGroundX >= curY - 0.5) {
                curY = targetGroundX;
              }
            } else if (canStepUpX) {
              curX = nextX;
              curY = targetGroundX;
            }
          }
        }

        // 2. Test Z Movement
        if (Math.abs(moveStepZ) > 0.0005) {
          const nextZ = THREE.MathUtils.clamp(curZ + moveStepZ, -78, 78);
          const targetGroundZ = chunkManager.getGroundHeight(curX, nextZ, curY);
          const hitWallZ = chunkManager.isCollidingWithSolid(curX, curY, nextZ, 0.28, 1.8);
          const canStepUpZ = isGroundedRef.current && (targetGroundZ <= curY + 0.55) && !chunkManager.isCollidingWithSolid(curX, targetGroundZ, nextZ, 0.28, 1.8);
          const isDangerousDropZ = isSneakingRef.current && isGroundedRef.current && (targetGroundZ < curY - 0.6);

          if (!isDangerousDropZ) {
            if (!hitWallZ) {
              curZ = nextZ;
              if (isGroundedRef.current && targetGroundZ <= curY + 0.05 && targetGroundZ >= curY - 0.5) {
                curY = targetGroundZ;
              }
            } else if (canStepUpZ) {
              curZ = nextZ;
              curY = targetGroundZ;
            }
          }
        }

        steve.group.position.x = curX;
        steve.group.position.y = curY;
        steve.group.position.z = curZ;
      }

      // --- JUMP, CEILING BONK & VERTICAL GRAVITY ---
      const curPos = steve.group.position;
      const groundY = chunkManager.getGroundHeight(curPos.x, curPos.z, curPos.y);

      if (!isGroundedRef.current) {
        velocityYRef.current -= GRAVITY * deltaTime;
        const nextY = curPos.y + velocityYRef.current * deltaTime;

        if (velocityYRef.current > 0) {
          const hitCeiling = chunkManager.isCollidingWithSolid(curPos.x, nextY, curPos.z, 0.28, 1.8);
          if (hitCeiling) {
            velocityYRef.current = 0;
          } else {
            curPos.y = nextY;
          }
        } else {
          curPos.y = nextY;
        }

        if (curPos.y <= groundY) {
          // Check fall damage
          if (velocityYRef.current < -14.0) {
            sounds.playHurt();
          }
          curPos.y = groundY;
          velocityYRef.current = 0;
          isGroundedRef.current = true;
        }
      } else {
        if (curPos.y > groundY + 0.05) {
          isGroundedRef.current = false;
        } else if (curPos.y < groundY) {
          curPos.y = groundY;
        }
      }

      steve.updateAnimation(deltaTime, isMoving, isGroundedRef.current, isSneakingRef.current);
      fppHand.update(deltaTime, isMoving, isSprintingRef.current, isSneakingRef.current);

      const roundedCoords = {
        x: Math.round(curPos.x),
        y: Math.round(curPos.y),
        z: Math.round(curPos.z)
      };
      steveCoordsRef.current = roundedCoords;
      setSteveCoords(roundedCoords);

      const poiNearby = chunkManager.getNearbyPOI(curPos);
      nearbyPOIRef.current = poiNearby;
      setNearbyPOI(poiNearby);

      // --- CAMERA POSITIONING ---
      const pitch = cameraPitchRef.current;
      const eyeY = curPos.y + currentEyeHeightRef.current;
      camera.position.set(curPos.x, eyeY, curPos.z);

      const lookDir = new THREE.Vector3(
        Math.sin(yaw) * Math.cos(pitch),
        Math.sin(pitch),
        Math.cos(yaw) * Math.cos(pitch)
      );
      camera.lookAt(camera.position.clone().add(lookDir));

      sunLight.position.set(curPos.x + 40, 95, curPos.z + 40);
      sunLight.target.position.set(curPos.x, curPos.y, curPos.z);

      // --- RAYCAST TARGETED BLOCK ---
      const targeted = chunkManager.raycastBlock(camera.position, lookDir, REACH_DISTANCE);
      targetedBlockRef.current = targeted;

      if (targeted) {
        blockOutlineBox.position.set(
          targeted.blockX + 0.5,
          targeted.blockY + 0.5,
          targeted.blockZ + 0.5
        );
        blockOutlineBox.visible = true;
      } else {
        blockOutlineBox.visible = false;
      }

      // Underwater Fog Effect
      const currentBlockAtEye = chunkManager.getBlockAt(Math.floor(curPos.x), Math.floor(eyeY), Math.floor(curPos.z));
      if (currentBlockAtEye === 10 || (eyeY <= 3.4 && groundY <= 3.0)) {
        worldFog.color.setHex(0x1d4ed8);
        worldFog.density = 0.04;
      } else {
        worldFog.color.setHex(0xa5d6f7);
        worldFog.density = 0.007;
      }

      // Retain player position in LocalStorage
      posSaveTimerRef.current += deltaTime;
      if (posSaveTimerRef.current > 0.8) {
        posSaveTimerRef.current = 0;
        const saveKey = `mc_player_pos_${worldRef.current?.id || 'default'}`;
        try {
          localStorage.setItem(saveKey, JSON.stringify({
            x: curPos.x,
            y: curPos.y,
            z: curPos.z,
            yaw: cameraYawRef.current,
            pitch: cameraPitchRef.current
          }));
        } catch (_) {}
      }

      skyMesh.position.copy(camera.position);
      renderer.render(scene, camera);
    };

    animate();

    return () => {
      cancelAnimationFrame(animationFrameId);

      const saveKey = `mc_player_pos_${worldRef.current?.id || 'default'}`;
      if (steveRef.current) {
        const curPos = steveRef.current.group.position;
        try {
          localStorage.setItem(saveKey, JSON.stringify({
            x: curPos.x,
            y: curPos.y,
            z: curPos.z,
            yaw: cameraYawRef.current,
            pitch: cameraPitchRef.current
          }));
        } catch (_) {}
      }

      document.removeEventListener("pointerlockchange", handlePointerLockChange);
      document.removeEventListener("mousemove", handleMouseMove);
      canvasDom.removeEventListener("click", handleCanvasClick);
      window.removeEventListener("wheel", handleWheel);
      window.removeEventListener("mousedown", handleMouseDown);
      window.removeEventListener("mouseup", handleMouseUp);
      window.removeEventListener("contextmenu", handleContextMenu);
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
      window.removeEventListener("resize", handleResize);
      if (currentMount && renderer.domElement) {
        currentMount.removeChild(renderer.domElement);
      }
      renderer.dispose();
    };
  }, [showToast, executeBreakBlock, executePlaceBlock, handleOpenBookModal, handleSelectSlot]);

  return (
    <div style={{ position: "relative", width: "100%", height: "100vh", overflow: "hidden", userSelect: "none" }}>
      <div
        ref={mountRef}
        style={{
          width: "100%",
          height: "100%",
          cursor: isLocked ? "none" : "pointer"
        }}
      />

      <WorldHUD
        worldName={world?.name || "Singleplayer World"}
        biome={world?.biome || "Plains"}
        hardcore={world?.hardcore}
        coords={steveCoords}
        nearbyPOI={nearbyPOI}
        selectedSlot={selectedSlot}
        onSelectSlot={handleSelectSlot}
        onOpenBookModal={handleOpenBookModal}
        onMineBlock={() => executeBreakBlock(null, targetedBlockRef.current)}
        toastMessage={toastMessage}
        entries={entries}
        onOpenEscapeMenu={() => setIsEscapeMenuOpen(true)}
        showDrawer={showDrawer}
        setShowDrawer={setShowDrawer}
        isSprinting={isSprinting}
        isSneaking={isSneaking}
        isLocked={isLocked}
      />

      <EscapeMenuModal
        isOpen={isEscapeMenuOpen}
        onResume={() => setIsEscapeMenuOpen(false)}
        currentTexturePack={texturePack}
        onSelectTexturePack={handleSelectTexturePack}
        onOpenJournalDrawer={() => setShowDrawer(true)}
      />

      <BookAndQuillModal
        isOpen={isBookModalOpen}
        onClose={() => setIsBookModalOpen(false)}
        onSaveEntry={handleSaveEntry}
        poi={activePOIForModal}
      />
    </div>
  );
}
