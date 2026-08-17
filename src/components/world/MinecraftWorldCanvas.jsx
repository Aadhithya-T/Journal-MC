import React, { useEffect, useRef, useState, useCallback } from "react";
import * as THREE from "three";
import { SteveCharacter } from "./SteveCharacter";
import { VoxelTerrain } from "./VoxelTerrain";
import { WorldHUD } from "./WorldHUD";
import { BookAndQuillModal } from "./BookAndQuillModal";
import { EscapeMenuModal } from "./EscapeMenuModal";

export function MinecraftWorldCanvas({ world, entries = [], onAddEntry }) {
  const mountRef = useRef(null);

  // HUD & Game States
  const [steveCoords, setSteveCoords] = useState({ x: 0, y: 1, z: 0 });
  const [nearbyPOI, setNearbyPOI] = useState(null);
  const [isBookModalOpen, setIsBookModalOpen] = useState(false);
  const [activePOIForModal, setActivePOIForModal] = useState(null);
  const [toastMessage, setToastMessage] = useState("🎮 Steve spawned into the world!");
  const [texturePack, setTexturePack] = useState("faithful64");
  const [isEscapeMenuOpen, setIsEscapeMenuOpen] = useState(false);
  const [showDrawer, setShowDrawer] = useState(false);

  // Three.js and Interaction References
  const steveRef = useRef(null);
  const terrainRef = useRef(null);
  const keysRef = useRef({});
  const lastToastTimeoutRef = useRef(null);

  // Camera Orbit / Free POV State Refs
  const cameraYawRef = useRef(0);
  const cameraPitchRef = useRef(0.35);
  const cameraDistanceRef = useRef(5.5);
  const isDraggingRef = useRef(false);
  const previousPointerRef = useRef({ x: 0, y: 0 });

  // Physics State Refs
  const velocityYRef = useRef(0);
  const isGroundedRef = useRef(true);

  const showToast = useCallback((msg) => {
    setToastMessage(msg);
    if (lastToastTimeoutRef.current) clearTimeout(lastToastTimeoutRef.current);
    lastToastTimeoutRef.current = setTimeout(() => {
      setToastMessage("");
    }, 4000);
  }, []);

  const handleSelectTexturePack = (packId) => {
    setTexturePack(packId);
    if (terrainRef.current) {
      terrainRef.current.switchTexturePack(packId);
      showToast(`🎨 Texture Pack applied: ${packId.toUpperCase()}`);
    }
  };

  const handleOpenBookModal = (poi = null) => {
    setActivePOIForModal(poi);
    setIsBookModalOpen(true);
    setIsEscapeMenuOpen(false);
  };

  const handleSaveEntry = async (entryData) => {
    await onAddEntry(entryData);
    showToast(`✍️ Recorded: "${entryData.title}"`);
    if (steveRef.current) {
      steveRef.current.triggerMining();
    }
  };

  const handleMineBlock = () => {
    if (!steveRef.current) return;
    steveRef.current.triggerMining();

    if (nearbyPOI) {
      handleOpenBookModal(nearbyPOI);
    } else {
      const autoTitles = [
        "Steve Mined Oak Wood",
        "Discovered Rolling Plains",
        "Found Flowing Spring Water",
        "Gathered Wild Flowers",
        "Surveyed Distant Mountain Ridge"
      ];
      const randomTitle = autoTitles[Math.floor(Math.random() * autoTitles.length)];
      onAddEntry({
        title: randomTitle,
        body: `Steve interacted with the terrain at coordinates (${steveCoords.x}, ${steveCoords.y}, ${steveCoords.z}) in ${world?.name || 'World'}.`,
        tags: ["exploration"]
      });
      showToast(`⛏️ Action logged: "${randomTitle}"`);
    }
  };

  useEffect(() => {
    const currentMount = mountRef.current;
    if (!currentMount) return;

    // --- THREE.JS SCENE SETUP ---
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x7ec0ee); // Minecraft Sky Blue
    scene.fog = new THREE.FogExp2(0x7ec0ee, 0.015);

    const camera = new THREE.PerspectiveCamera(
      65,
      currentMount.clientWidth / currentMount.clientHeight,
      0.1,
      200
    );

    const renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(currentMount.clientWidth, currentMount.clientHeight);
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFShadowMap;
    currentMount.appendChild(renderer.domElement);

    // --- LIGHTS ---
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.85);
    scene.add(ambientLight);

    const sunLight = new THREE.DirectionalLight(0xfffaed, 1.25);
    sunLight.position.set(30, 45, 30);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    sunLight.shadow.camera.near = 0.5;
    sunLight.shadow.camera.far = 120;
    sunLight.shadow.camera.left = -40;
    sunLight.shadow.camera.right = 40;
    sunLight.shadow.camera.top = 40;
    sunLight.shadow.camera.bottom = -40;
    scene.add(sunLight);

    // --- MULTI-CHUNK VOXEL TERRAIN ---
    const terrain = new VoxelTerrain(80, texturePack);
    scene.add(terrain.group);
    terrainRef.current = terrain;

    // --- STEVE CHARACTER ---
    const steve = new SteveCharacter();
    const startY = terrain.getGroundHeight(0, 0);
    steve.setPosition(0, startY, 0);
    scene.add(steve.group);
    steveRef.current = steve;

    // --- CLOUDS ---
    const cloudGroup = new THREE.Group();
    const cloudGeo = new THREE.BoxGeometry(10, 1.2, 6);
    const cloudMat = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.85 });
    for (let i = 0; i < 24; i++) {
      const cloud = new THREE.Mesh(cloudGeo, cloudMat);
      cloud.position.set((Math.random() - 0.5) * 120, 18 + Math.random() * 4, (Math.random() - 0.5) * 120);
      cloudGroup.add(cloud);
    }
    scene.add(cloudGroup);

    // --- KEYBOARD CONTROLS LISTENERS ---
    const handleKeyDown = (e) => {
      const key = e.key.toLowerCase();
      keysRef.current[key] = true;

      // Escape key to toggle Game Menu / close modals
      if (e.key === "Escape") {
        if (isBookModalOpen) {
          setIsBookModalOpen(false);
        } else if (showDrawer) {
          setShowDrawer(false);
        } else {
          setIsEscapeMenuOpen((prev) => !prev);
        }
        return;
      }

      // If menus or modals are open, disable game movement inputs
      if (isEscapeMenuOpen || isBookModalOpen) return;

      // Jump on Spacebar
      if (e.code === 'Space') {
        if (isGroundedRef.current) {
          velocityYRef.current = 8.5;
          isGroundedRef.current = false;
        }
      }

      // Interact on E key
      if (key === 'e') {
        const currentPos = steve.group.position;
        const poi = terrain.getNearbyPOI(currentPos);
        handleOpenBookModal(poi);
      }
    };

    const handleKeyUp = (e) => {
      keysRef.current[e.key.toLowerCase()] = false;
    };

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);

    // --- MOUSE / POINTER FREE POV ROTATION LISTENERS ---
    const handlePointerDown = (e) => {
      if (isEscapeMenuOpen || isBookModalOpen) return;
      isDraggingRef.current = true;
      previousPointerRef.current = { x: e.clientX, y: e.clientY };
    };

    const handlePointerMove = (e) => {
      if (!isDraggingRef.current || isEscapeMenuOpen || isBookModalOpen) return;
      const deltaX = e.clientX - previousPointerRef.current.x;
      const deltaY = e.clientY - previousPointerRef.current.y;
      previousPointerRef.current = { x: e.clientX, y: e.clientY };

      cameraYawRef.current -= deltaX * 0.006;
      cameraPitchRef.current += deltaY * 0.005;
      cameraPitchRef.current = THREE.MathUtils.clamp(cameraPitchRef.current, -0.4, 1.25);
    };

    const handlePointerUp = () => {
      isDraggingRef.current = false;
    };

    const handleWheel = (e) => {
      if (isEscapeMenuOpen || isBookModalOpen) return;
      e.preventDefault();
      cameraDistanceRef.current += e.deltaY * 0.005;
      cameraDistanceRef.current = THREE.MathUtils.clamp(cameraDistanceRef.current, 2.0, 15.0);
    };

    const canvasDom = renderer.domElement;
    canvasDom.addEventListener("pointerdown", handlePointerDown);
    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp);
    canvasDom.addEventListener("wheel", handleWheel, { passive: false });

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

      // Cloud slow drift
      cloudGroup.children.forEach((c) => {
        c.position.x += deltaTime * 0.8;
        if (c.position.x > 60) c.position.x = -60;
      });

      // If game is paused by menu, only render scene
      if (isEscapeMenuOpen || isBookModalOpen) {
        steve.updateAnimation(deltaTime, false, isGroundedRef.current);
        renderer.render(scene, camera);
        return;
      }

      // --- PLAYER MOVEMENT RELATIVE TO CAMERA POV ---
      const keys = keysRef.current;
      let forward = 0;
      let strafe = 0;

      if (keys['w'] || keys['arrowup']) forward += 1;
      if (keys['s'] || keys['arrowdown']) forward -= 1;
      if (keys['a'] || keys['arrowleft']) strafe -= 1;
      if (keys['d'] || keys['arrowright']) strafe += 1;

      let isMoving = false;
      const speed = 5.2 * deltaTime;

      if (forward !== 0 || strafe !== 0) {
        isMoving = true;
        const norm = Math.sqrt(forward * forward + strafe * strafe);
        const normF = forward / norm;
        const normS = strafe / norm;

        const yaw = cameraYawRef.current;
        const moveX = Math.sin(yaw) * normF + Math.cos(yaw) * normS;
        const moveZ = Math.cos(yaw) * normF - Math.sin(yaw) * normS;

        const nextX = THREE.MathUtils.clamp(steve.group.position.x + moveX * speed, -38, 38);
        const nextZ = THREE.MathUtils.clamp(steve.group.position.z + moveZ * speed, -38, 38);

        const currentY = steve.group.position.y;
        const targetGroundY = terrain.getGroundHeight(nextX, nextZ);

        const hitSolidObstacle = terrain.isCollidingWithSolid(nextX, currentY, nextZ, 0.35, 1.8);
        const isHigherBlock = targetGroundY > currentY + 0.15;

        if (!hitSolidObstacle) {
          if (!isHigherBlock) {
            steve.group.position.x = nextX;
            steve.group.position.z = nextZ;

            if (isGroundedRef.current && targetGroundY <= currentY && targetGroundY >= currentY - 0.5) {
              steve.group.position.y = targetGroundY;
            }
          } else {
            if (!isGroundedRef.current && currentY >= targetGroundY - 0.05) {
              steve.group.position.x = nextX;
              steve.group.position.z = nextZ;
            }
          }
        }

        const targetRotation = Math.atan2(moveX, moveZ);
        steve.setRotation(targetRotation);
      }

      // --- JUMP & VERTICAL GRAVITY PHYSICS ---
      const curPos = steve.group.position;
      const groundY = terrain.getGroundHeight(curPos.x, curPos.z);

      if (!isGroundedRef.current) {
        velocityYRef.current -= 22.0 * deltaTime;
        curPos.y += velocityYRef.current * deltaTime;

        if (curPos.y <= groundY) {
          curPos.y = groundY;
          velocityYRef.current = 0;
          isGroundedRef.current = true;
        }
      } else {
        if (curPos.y < groundY) {
          curPos.y = groundY;
        } else if (curPos.y > groundY + 0.1) {
          isGroundedRef.current = false;
        }
      }

      steve.updateAnimation(deltaTime, isMoving, isGroundedRef.current);

      setSteveCoords({
        x: Math.round(curPos.x),
        y: Math.round(curPos.y),
        z: Math.round(curPos.z)
      });

      const poiNearby = terrain.getNearbyPOI(curPos);
      setNearbyPOI(poiNearby);

      // --- FREE POV CAMERA ORBIT AROUND STEVE ---
      const yaw = cameraYawRef.current;
      const pitch = cameraPitchRef.current;
      const dist = cameraDistanceRef.current;

      const camX = curPos.x - Math.sin(yaw) * Math.cos(pitch) * dist;
      const camY = curPos.y + 1.2 + Math.sin(pitch) * dist;
      const camZ = curPos.z - Math.cos(yaw) * Math.cos(pitch) * dist;

      camera.position.set(camX, Math.max(camY, groundY + 0.3), camZ);
      camera.lookAt(curPos.x, curPos.y + 1.1, curPos.z);

      renderer.render(scene, camera);
    };

    animate();

    // Cleanup
    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
      canvasDom.removeEventListener("pointerdown", handlePointerDown);
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
      canvasDom.removeEventListener("wheel", handleWheel);
      window.removeEventListener("resize", handleResize);
      if (currentMount && renderer.domElement) {
        currentMount.removeChild(renderer.domElement);
      }
      renderer.dispose();
    };
  }, [showToast, texturePack, isEscapeMenuOpen, isBookModalOpen, showDrawer]);

  return (
    <div style={{ position: "relative", width: "100%", height: "100vh", overflow: "hidden", userSelect: "none" }}>
      {/* 3D WebGL Canvas Container */}
      <div
        ref={mountRef}
        style={{
          width: "100%",
          height: "100%",
          cursor: isDraggingRef.current ? "grabbing" : "grab"
        }}
      />

      {/* Minecraft HUD Overlay (Subtle Pixel Aesthetics) */}
      <WorldHUD
        worldName={world?.name || "Singleplayer World"}
        biome={world?.biome || "Plains"}
        hardcore={world?.hardcore}
        coords={steveCoords}
        nearbyPOI={nearbyPOI}
        onOpenBookModal={handleOpenBookModal}
        onMineBlock={handleMineBlock}
        toastMessage={toastMessage}
        entries={entries}
        onOpenEscapeMenu={() => setIsEscapeMenuOpen(true)}
        showDrawer={showDrawer}
        setShowDrawer={setShowDrawer}
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
