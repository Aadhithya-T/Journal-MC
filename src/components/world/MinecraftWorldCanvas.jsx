import React, { useEffect, useRef, useState, useCallback } from "react";
import * as THREE from "three";
import { SteveCharacter } from "./SteveCharacter";
import { FirstPersonHand } from "./FirstPersonHand";
import { ChunkManager } from "./chunk/ChunkManager";
import { WorldHUD } from "./WorldHUD";
import { BookAndQuillModal } from "./BookAndQuillModal";
import { EscapeMenuModal } from "./EscapeMenuModal";

// --- VANILLA MINECRAFT PHYSICS CONSTANTS ---
const WALK_SPEED = 4.317;        // blocks / second
const SPRINT_SPEED = 5.612;      // blocks / second (1.3x walk)
const SNEAK_SPEED = 1.31;        // blocks / second (0.3x walk)
const JUMP_VELOCITY = 8.4;       // blocks / second (0.42 b/tick * 20 TPS)
const GRAVITY = 30.0;            // blocks / second^2
const PLAYER_EYE_HEIGHT = 1.62;  // blocks above feet (standing)
const SNEAK_EYE_HEIGHT = 1.27;   // blocks above feet (sneaking)
const MOUSE_SENSITIVITY = 0.0022;
const REACH_DISTANCE = 5.0;      // max block reach in blocks
const MINING_TIME = 0.38;        // seconds to mine a block

export function MinecraftWorldCanvas({ world, entries = [], onAddEntry }) {
  const mountRef = useRef(null);

  // HUD & Game States
  const [steveCoords, setSteveCoords] = useState({ x: 0, y: 5, z: 0 });
  const [nearbyPOI, setNearbyPOI] = useState(null);
  const [isBookModalOpen, setIsBookModalOpen] = useState(false);
  const [activePOIForModal, setActivePOIForModal] = useState(null);
  const [toastMessage, setToastMessage] = useState("🎮 Left-Click to mine & interact!");
  const [texturePack, setTexturePack] = useState("faithful64");
  const [isEscapeMenuOpen, setIsEscapeMenuOpen] = useState(false);
  const [showDrawer, setShowDrawer] = useState(false);

  // Movement States
  const [isSprinting, setIsSprinting] = useState(false);
  const [isSneaking, setIsSneaking] = useState(false);
  const [isLocked, setIsLocked] = useState(false);

  // References for values needed inside render loop / event handlers (no re-renders)
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
  const currentMiningTargetRef = useRef(null);
  const isEscapeMenuOpenRef = useRef(false);
  const isBookModalOpenRef = useRef(false);
  const showDrawerRef = useRef(false);
  const steveCoordsRef = useRef({ x: 0, y: 5, z: 0 });
  const targetedBlockRef = useRef(null);
  const nearbyPOIRef = useRef(null);
  const onAddEntryRef = useRef(onAddEntry);
  const worldRef = useRef(world);
  const particleGroupRef = useRef(null);
  const waterAnimTimerRef = useRef(0);
  const posSaveTimerRef = useRef(0);

  // Keep refs in sync with state
  onAddEntryRef.current = onAddEntry;
  worldRef.current = world;
  isEscapeMenuOpenRef.current = isEscapeMenuOpen;
  isBookModalOpenRef.current = isBookModalOpen;
  showDrawerRef.current = showDrawer;

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

  const handleSelectTexturePack = (packId) => {
    setTexturePack(packId);
    if (chunkManagerRef.current) {
      chunkManagerRef.current.switchTexturePack(packId);
      showToast(`🎨 Texture Pack applied: ${packId.toUpperCase()}`);
    }
  };

  const handleOpenBookModal = useCallback((poi = null) => {
    setActivePOIForModal(poi);
    setIsBookModalOpen(true);
    setIsEscapeMenuOpen(false);
    if (document.exitPointerLock) {
      document.exitPointerLock();
    }
  }, []);

  const handleSaveEntry = async (entryData) => {
    await onAddEntry(entryData);
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
  const executeBreakBlock = useCallback((scene, target) => {
    if (!chunkManagerRef.current || !target) return;
    const broken = chunkManagerRef.current.breakBlock(target.blockX, target.blockY, target.blockZ);
    if (broken) {
      spawnBreakParticles(scene, target.blockX, target.blockY, target.blockZ, broken.color);
      showToast(`💥 Broke ${broken.name} at (${target.blockX}, ${target.blockY}, ${target.blockZ})`);
    } else {
      showToast(`🛡️ Bedrock is unbreakable!`);
    }
  }, [showToast, spawnBreakParticles]);

  // Main Three.js Scene Setup (mounts once and stays persistent across UI interactions)
  useEffect(() => {
    const currentMount = mountRef.current;
    if (!currentMount) return;

    // --- THREE.JS SCENE SETUP ---
    const scene = new THREE.Scene();
    const skyColor = new THREE.Color(0x78a7ff);
    scene.background = skyColor;
    // Linear Atmospheric Depth Fog (Softly fades distant terrain into sky)
    const worldFog = new THREE.Fog(0x78a7ff, 22, 105);
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

    // --- 3D FIRST-PERSON VIEWMODEL HAND (Attached to Camera) ---
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

    // --- DIRECTIONAL SUNLIGHT & DEEP SHADOW CONTRAST ---
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

    // --- 100-CHUNK VOXEL PIPELINE (10x10 Chunks = 160x160 blocks) ---
    const chunkManager = new ChunkManager(5, texturePack);
    scene.add(chunkManager.group);
    chunkManagerRef.current = chunkManager;

    // --- STEVE CHARACTER (Invisible in pure 1st Person) ---
    const steve = new SteveCharacter();

    // Retain and Restore Player Position from LocalStorage
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

    // --- TARGETED BLOCK OUTLINE (Vanilla Selection Box) ---
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

    // --- PROGRESSIVE MINING CRACK OVERLAY DECAL ---
    const crackCanvas = document.createElement("canvas");
    crackCanvas.width = 64;
    crackCanvas.height = 64;
    const crackCtx = crackCanvas.getContext("2d");
    const crackTex = new THREE.CanvasTexture(crackCanvas);
    crackTex.magFilter = THREE.NearestFilter;
    const crackMat = new THREE.MeshBasicMaterial({
      map: crackTex,
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

    const updateCrackDecal = (stage) => {
      crackCtx.clearRect(0, 0, 64, 64);
      if (stage <= 0) return;
      crackCtx.fillStyle = "#111111";

      // 6 Progressive Fractures matching Minecraft breaking stages
      if (stage >= 1) {
        crackCtx.fillRect(28, 12, 4, 16);
        crackCtx.fillRect(20, 24, 20, 4);
      }
      if (stage >= 2) {
        crackCtx.fillRect(12, 16, 4, 24);
        crackCtx.fillRect(40, 20, 16, 4);
        crackCtx.fillRect(44, 32, 4, 20);
      }
      if (stage >= 3) {
        crackCtx.fillRect(8, 40, 24, 4);
        crackCtx.fillRect(28, 44, 4, 16);
        crackCtx.fillRect(36, 12, 8, 4);
      }
      if (stage >= 4) {
        crackCtx.fillRect(4, 4, 56, 4);
        crackCtx.fillRect(4, 56, 56, 4);
        crackCtx.fillRect(4, 4, 4, 56);
        crackCtx.fillRect(56, 4, 4, 56);
      }
      if (stage >= 5) {
        crackCtx.fillRect(16, 16, 32, 32);
        crackCtx.clearRect(24, 24, 16, 16);
      }
      crackTex.needsUpdate = true;
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

    // --- 2D PIXELATED CLOUDS (Matching Screenshot Horizon) ---
    const cloudGroup = new THREE.Group();
    const cloudGeo = new THREE.PlaneGeometry(28, 16);
    const cloudMat = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.88, side: THREE.DoubleSide });
    for (let i = 0; i < 35; i++) {
      const cloud = new THREE.Mesh(cloudGeo, cloudMat);
      cloud.rotation.x = Math.PI / 2;
      cloud.position.set((Math.random() - 0.5) * 260, 32 + (i % 2) * 1.5, (Math.random() - 0.5) * 260);
      cloudGroup.add(cloud);
    }
    scene.add(cloudGroup);

    // --- POINTER LOCK CONTROLS WITH SUB-FRAME SMOOTHING ---
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

    // --- MOUSE CLICK INTERACTIONS ---
    const handleMouseDown = (e) => {
      if (document.pointerLockElement !== canvasDom) return;
      if (isEscapeMenuOpenRef.current || isBookModalOpenRef.current) return;

      if (e.button === 0) {
        isLeftMouseDownRef.current = true;
        if (fppHandRef.current) fppHandRef.current.triggerSwing();
        if (steveRef.current) steveRef.current.triggerMining();
      } else if (e.button === 2) {
        const currentPos = steve.group.position;
        const poi = chunkManager.getNearbyPOI(currentPos);
        handleOpenBookModal(poi);
      }
    };

    const handleMouseUp = (e) => {
      if (e.button === 0) {
        isLeftMouseDownRef.current = false;
        miningProgressRef.current = 0;
        crackBox.visible = false;
        updateCrackDecal(0);
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

      // Dynamic Procedural Water Wave Ripple Animation (18 FPS texture cycle)
      waterAnimTimerRef.current += deltaTime;
      if (waterAnimTimerRef.current > 0.055) {
        if (chunkManagerRef.current && chunkManagerRef.current.atlas) {
          chunkManagerRef.current.atlas.updateWaterAnimation(now * 0.001);
        }
        waterAnimTimerRef.current = 0;
      }

      // Sub-frame smooth mouse rotation interpolation (60-144hz fluid)
      cameraYawRef.current = THREE.MathUtils.lerp(cameraYawRef.current, targetYawRef.current, deltaTime * 40);
      cameraPitchRef.current = THREE.MathUtils.lerp(cameraPitchRef.current, targetPitchRef.current, deltaTime * 40);

      // Cloud drift
      cloudGroup.children.forEach((c) => {
        c.position.x += deltaTime * 0.8;
        if (c.position.x > 110) c.position.x = -110;
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

      // Update active block breaking shard particles
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

      // Progressive Mining & Crack Decals while holding left click
      if (isLeftMouseDownRef.current && !isEscapeMenuOpenRef.current && !isBookModalOpenRef.current) {
        const target = targetedBlockRef.current;
        if (target) {
          if (currentMiningTargetRef.current !== `${target.blockX},${target.blockY},${target.blockZ}`) {
            currentMiningTargetRef.current = `${target.blockX},${target.blockY},${target.blockZ}`;
            miningProgressRef.current = 0;
          }

          miningProgressRef.current += deltaTime / MINING_TIME;
          const stage = Math.min(5, Math.floor(miningProgressRef.current * 5) + 1);
          updateCrackDecal(stage);

          crackBox.position.set(target.blockX + 0.5, target.blockY + 0.5, target.blockZ + 0.5);
          crackBox.visible = true;

          // Continuous arm swing
          if (fppHandRef.current && !fppHandRef.current.isSwinging) {
            fppHandRef.current.triggerSwing();
          }

          if (miningProgressRef.current >= 1.0) {
            executeBreakBlock(scene, target);
            miningProgressRef.current = 0;
            crackBox.visible = false;
            updateCrackDecal(0);
          }
        } else {
          crackBox.visible = false;
          updateCrackDecal(0);
        }
      } else {
        crackBox.visible = false;
        updateCrackDecal(0);
      }

      // If game is paused, render scene without physics
      if (isEscapeMenuOpenRef.current || isBookModalOpenRef.current) {
        steve.updateAnimation(deltaTime, false, isGroundedRef.current, isSneakingRef.current);
        fppHand.update(deltaTime, false, false, false);
        renderer.render(scene, camera);
        return;
      }

      // --- PLAYER MOVEMENT RELATIVE TO CAMERA HEADING ---
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

        // 1. Test X Movement with Auto Step-Up & Wall Sliding
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

        // 2. Test Z Movement with Auto Step-Up & Wall Sliding
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

      // --- JUMP, CEILING BONK & VERTICAL GRAVITY PHYSICS ---
      const curPos = steve.group.position;
      const groundY = chunkManager.getGroundHeight(curPos.x, curPos.z, curPos.y);

      if (!isGroundedRef.current) {
        velocityYRef.current -= GRAVITY * deltaTime;
        const nextY = curPos.y + velocityYRef.current * deltaTime;

        // Bonk head on ceiling if solid block is directly above
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

      // Update 3D First-Person Viewmodel Hand Bobbing & Arm Swing
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

      // --- PURE FIRST-PERSON CAMERA POSITIONING ---
      const pitch = cameraPitchRef.current;
      const eyeY = curPos.y + currentEyeHeightRef.current;
      camera.position.set(curPos.x, eyeY, curPos.z);

      const lookDir = new THREE.Vector3(
        Math.sin(yaw) * Math.cos(pitch),
        Math.sin(pitch),
        Math.cos(yaw) * Math.cos(pitch)
      );
      camera.lookAt(camera.position.clone().add(lookDir));

      // Dynamic High-Res Shadow tracking around player
      sunLight.position.set(curPos.x + 40, 95, curPos.z + 40);
      sunLight.target.position.set(curPos.x, curPos.y, curPos.z);

      // --- RAYCAST TARGETED BLOCK (Minecraft Selection Box) ---
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

      // Underwater Fog Effect (When eye is below water level y <= 3.5)
      const currentBlockAtEye = chunkManager.getBlockAt(Math.floor(curPos.x), Math.floor(eyeY), Math.floor(curPos.z));
      if (currentBlockAtEye === 10 || (eyeY <= 3.4 && groundY <= 3.0)) {
        worldFog.color.setHex(0x1d4ed8);
        worldFog.density = 0.04;
      } else {
        worldFog.color.setHex(0xa5d6f7);
        worldFog.density = 0.007;
      }

      // Periodically retain player position in LocalStorage
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

    // Cleanup
    return () => {
      cancelAnimationFrame(animationFrameId);

      // Retain position on unmount
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
  }, [showToast, executeBreakBlock, handleOpenBookModal]);

  return (
    <div style={{ position: "relative", width: "100%", height: "100vh", overflow: "hidden", userSelect: "none" }}>
      {/* 3D WebGL Canvas Container */}
      <div
        ref={mountRef}
        style={{
          width: "100%",
          height: "100%",
          cursor: isLocked ? "none" : "pointer"
        }}
      />

      {/* Minecraft HUD Overlay */}
      <WorldHUD
        worldName={world?.name || "Singleplayer World"}
        biome={world?.biome || "Plains"}
        hardcore={world?.hardcore}
        coords={steveCoords}
        nearbyPOI={nearbyPOI}
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

      {/* Minecraft Escape / Pause Game Menu */}
      <EscapeMenuModal
        isOpen={isEscapeMenuOpen}
        onResume={() => setIsEscapeMenuOpen(false)}
        currentTexturePack={texturePack}
        onSelectTexturePack={handleSelectTexturePack}
        onOpenJournalDrawer={() => setShowDrawer(true)}
      />

      {/* In-World Book & Quill Modal */}
      <BookAndQuillModal
        isOpen={isBookModalOpen}
        onClose={() => setIsBookModalOpen(false)}
        onSaveEntry={handleSaveEntry}
        poi={activePOIForModal}
      />
    </div>
  );
}
