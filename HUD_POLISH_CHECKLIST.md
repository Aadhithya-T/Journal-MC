# Journal-MC — HUD Polish & Hardcore Feature Roadmap

This document captures the HUD and survival UX feedback organized into three execution stages based on dependency order and visual impact.

---

## Stage 1: Immediate Visual Contrast & Controls (Now) — [COMPLETED]

These items address direct readability issues, controls, and feedback without requiring changes to scaling or core survival mechanics.

- [x] **1. Slot Contrast & Visibility**
  - Lightened the slot cavity fill (`#161620` with 0.70 alpha) so dark blocks (stone, coal, dirt) pop clearly.
  - Added a visible lighter border (`#3c3c4a`) and a 1px recessed inner drop shadow to give each slot clear definition on any background.
- [x] **2. Crosshair Inverted / High-Contrast Blending**
  - Implemented `GuiRenderer.setInvertedBlend(true)` using `glBlendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO)` so the reticle inverts cleanly over bright sky, blinding snow, and deep caverns.
  - Retained the luminous gold focal pip when targeting an interactable block.
- [x] **3. Info Card Clarity & De-clutter**
  - Updated chunk metric label to `"Chunks X/Y"`.
  - Removed the unlabelled top accent notch.
  - Lowered panel background opacity to 0.65 for a lighter, less intrusive presence.
- [x] **4. Heart Container Hurt Flash**
  - Added incandescent white damage flash (`1.0, 1.0, 1.0, 0.95`) across heart containers during `player.hurtTime > 0` for visceral combat feedback.
- [x] **5. Utility Hotkeys (F1 & F2)**
  - **F1:** Toggles HUD visibility (`showHud = !showHud`).
  - **F2:** Captures high-resolution screenshot directly to `screenshots/` folder with an ephemeral on-screen confirmation toast (`ScreenshotHelper`).
- [x] **6. Viewmodel Arm Geometry & Near-Plane Audit**
  - Extended arm base geometry (`yTop = 0.18f`), recalibrated base anchors, and reduced world camera near clipping plane to `0.02f` to eliminate near-plane polygon slicing and the bottom-right tan triangle.
- [x] **7. Window Title Bar Branding**
  - Updated window title in `MCJournalApp` to `"Journal-MC: Native Hardcore Edition"`.

---

## Stage 2: Scaling, Sizing & Survival Mechanics (After Scale Phase)

These items depend on the GUI scale manager and survival state expansions.

- [ ] **1. HUD Scale Setting**
  - Expose `GUI Scale: Auto / 1x / 2x / 3x / 4x` in Video Settings, matching player preference and standard desktop viewing distances.
- [ ] **2. Hotbar Proportions & Item Sizing**
  - Increase item sprite footprint inside the slot from ~68% to ~80% (18–19px inside 22px slot).
  - Brighten tool sprites (axes, pickaxes) or give them a 1px light rim so they don't blend into the dark slot well.
- [ ] **3. Hunger Sprite Redesign & Spacing**
  - Redesign hunger icon with a sharp silhouette: ivory bone end, rich amber meat bulb, and bright highlight.
  - Increase the horizontal spacing between the Health cluster and Hunger cluster to prevent them merging into a single strip.
- [ ] **4. Item Name Switch Popup**
  - Display the selected item name (e.g., `Iron Pickaxe`, `Oak Planks (32)`) above the hotbar for ~2.0 seconds when changing active slots, fading smoothly.
- [ ] **5. Tool Durability Bars**
  - Render colored durability bars (green → yellow → red) underneath damaged tools in hotbar slots.
- [ ] **6. Air / Breath Bubbles**
  - If drowning is enabled, render 10 popping air bubble sprites above the hunger row while submerged.

---

## Stage 3: Hardcore Atmosphere & Immersion (Final Polish)

These features amplify tension and give the interface a unique Journal-MC identity.

- [ ] **1. Low-Health Heartbeat & Pulsing Vignette**
  - At $\le 2$ hearts ($\le 4$ HP), activate a slow rhythmic crimson edge pulsation and heartbeat audio/visual cadence.
- [ ] **2. Journal "Days Survived" Feature**
  - Give "Days Survived" a prominent, thematic placement on the HUD or Pause/Journal overlay.
- [ ] **3. F3 Debug Modes (Cycling)**
  - Cycle F3 through:
    1. `Off`: Completely hidden (immersive default).
    2. `Minimal`: FPS and memory only.
    3. `Full`: Coordinates, Biome, Light, Chunks, and Facing direction.
- [ ] **4. Title Bar Hardcore Indicators**
  - Ensure title bar displays clean release versioning and hardcore status.
