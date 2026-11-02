# GUI Overhaul — Phase 1: GUI Foundation & Virtual Resolution Report

## 1. Executive Summary

Phase 1 of the Journal-MC GUI Overhaul is complete and verified on dedicated branch `feature/gui-overhaul`. 

Phase 1 establishes a centralized logical GUI coordinate system, strict integer-only scaling with predictable clamping fallback, bidirectional mouse coordinate transformations, reusable layout helpers, extended bitmap font coverage (with Latin-1, degree symbol `°`, combat sword `⚔`, play `▶`, heart `♥`, and `§0`..`§f` color codes), bounded LRU text-width measurement caching, and settings persistence.

Existing legacy screens (`WorldSelectScreen`, `WorldCreateScreen`, `WorldEditScreen`, `GameOverScreen`) remain 100% operational through backward-compatible physical coordinates, while the first wave of core screens (`TitleScreen`, `OptionsScreen`, `EscapeMenuScreen`) have been cleanly migrated to logical coordinates.

All **25 test suites** in the master suite pass with a **100% success rate**, including 11 comprehensive automated Phase 1 verification tests and all 10 Phase 0 batching/state-restoration regression tests.

---

## 2. Architecture Assessment

Prior to Phase 1, the engine suffered from several structural GUI limitations:
1. **Physical Pixel Coupling:** Every screen directly read raw window width and height. At 4K (3840×2160), 400×40 pixel buttons rendered at microscopic proportions (~10% of screen width).
2. **Divergent Mouse Coordinates:** GLFW cursor callbacks delivered coordinates in window screen points, while `GuiRenderer.begin()` operated in framebuffer pixels. On high-DPI displays (such as macOS Retina or Windows display scaling), mouse clicks and hover bounding boxes drifted from rendered button geometry.
3. **Missing Glyphs & Character Dropping:** `FontRenderer` exclusively populated ASCII characters 32 through 126 (`c < 127`), silently discarding any character $\ge 127$ (replacing with space `32`). This broke degree symbols (`70°`), section formatting symbols (`§`), combat icons, and non-ASCII text.
4. **Lack of Text Caching & Color Parsing:** `getStringWidth()` looped over every character on every frame without caching, causing continuous string traversal overhead. Color codes (`§e`, `§a`) were rendered as literal characters or dropped.
5. **No Centralized Layout Engine:** Screen layouts manually hardcoded screen-center offsets (`height / 2 - 80`), resulting in repetitive code across screens.

---

## 3. Design Resolution and Scaling Decisions

### 3.1 Logical Design Resolution
- **Minimum Logical Width:** 420 units (comfortably accommodates 200px Minecraft buttons or 400px split-column dialogs with lateral margins).
- **Minimum Logical Height:** 240 units (comfortably fits the 3D title logo, 3 button rows, and header/footer metadata).
- Standard aspect ratio: 16:9, with native handling for 4:3, 16:10, 21:9 ultrawide, and portrait windows.

### 3.2 Integer Scaling Rule
- GUI scaling strictly enforces integer scale factors ($S \in \{1, 2, 3, 4, \dots\}$). No fractional scaling is permitted, preventing pixel blur, aliasing, and uneven border distortion.
- **Auto Scale Formulation:**
  $$\text{effectiveScale} = \max\left(1, \min\left(\left\lfloor \frac{W_{\text{fb}}}{420} \right\rfloor, \left\lfloor \frac{H_{\text{fb}}}{240} \right\rfloor\right)\right)$$
- Standard resolutions scale as follows:
  - **854×480 (SD):** Scale **2** $\rightarrow$ Logical **427 × 240**
  - **1280×720 (HD):** Scale **3** $\rightarrow$ Logical **426 × 240**
  - **1920×1080 (FHD):** Scale **4** $\rightarrow$ Logical **480 × 270**
  - **2560×1440 (QHD):** Scale **6** $\rightarrow$ Logical **426 × 240**
  - **3840×2160 (4K UHD):** Scale **9** $\rightarrow$ Logical **426 × 240**

### 3.3 Predictable Clamping Fallback for Manual Scaling
- When the user selects manual scale 1, 2, 3, or 4:
  - If $\text{requestedScale} \le \text{maxFittingScale}$: requested scale is used directly (`isScaleClamped = false`).
  - If $\text{requestedScale} > \text{maxFittingScale}$: the scale automatically clamps to `maxFittingScale` (`isScaleClamped = true`).
  - Interface elements are **never silently clipped or pushed offscreen**.

### 3.4 Letterboxing & Viewport Offsets
- Logical dimensions: $W_{\text{gui}} = \lfloor W_{\text{fb}} / S \rfloor$, $H_{\text{gui}} = \lfloor H_{\text{fb}} / S \rfloor$.
- Viewport dimensions: $W_{\text{vp}} = W_{\text{gui}} \times S$, $H_{\text{vp}} = H_{\text{gui}} \times S$.
- Viewport offsets: $X_{\text{vp}} = \lfloor (W_{\text{fb}} - W_{\text{vp}}) / 2 \rfloor$, $Y_{\text{vp}} = \lfloor (H_{\text{fb}} - H_{\text{vp}}) / 2 \rfloor$.
- Any odd pixel remainder ($< S$ pixels) is centered symmetrically.

---

## 4. Coordinate Transformation Design

`GuiScaleManager.java` centralizes all transformations between window points, physical framebuffer pixels, and logical GUI units:

1. **Window-to-GUI (Mouse Input):**
   $$X_{\text{fb}} = X_{\text{win}} \times \left(\frac{W_{\text{fb}}}{W_{\text{win}}}\right), \quad X_{\text{rel}} = X_{\text{fb}} - X_{\text{vp}}, \quad X_{\text{gui}} = \frac{X_{\text{rel}}}{S}$$
2. **GUI-to-Window (Reverse Projection):**
   $$X_{\text{fb}} = X_{\text{vp}} + (X_{\text{gui}} \times S), \quad X_{\text{win}} = X_{\text{fb}} \times \left(\frac{W_{\text{win}}}{W_{\text{fb}}}\right)$$
3. **Letterbox Click Rejection:**
   - `isMouseInViewport(winX, winY)` verifies whether the cursor resides strictly within $[X_{\text{vp}}, X_{\text{vp}} + W_{\text{vp}}) \times [Y_{\text{vp}}, Y_{\text{vp}} + H_{\text{vp}})$.
   - `ScreenManager.handleMouseClick()` intercepts and discards any mouse clicks occurring in letterboxed borders, preventing phantom clicks outside active UI bounds.

---

## 5. Layout API (`GuiLayout.java`)

A lightweight, zero-allocation layout foundation was established:
- **Centering:** `centerX(containerW, elementW)`, `centerY(containerH, elementH)`
- **Anchoring:** `anchorLeft(margin)`, `anchorRight(containerW, elementW, margin)`, `anchorTop(margin)`, `anchorBottom(containerH, elementH, margin)`
- **Rows & Columns:** `rowItemX(startX, itemW, gap, index)`, `columnItemY(startY, itemH, gap, index)`, `rowTotalWidth(count, itemW, gap)`, `columnTotalHeight(count, itemH, gap)`
- **Screen Relative:** `relativeX(containerW, fraction, elementW)`, `relativeY(containerH, fraction, elementH)`
- **Split Rows:** `splitWidth(totalRowWidth, gap, columns)` for 2-column or 3-column button rows.

---

## 6. Bitmap Font Improvements (`FontRenderer.java`)

1. **Extended Character Set (512 Glyphs):**
   - Populated ASCII 32 through 255 (Full Latin-1 Supplement), natively unlocking degree `'°'` (176) and section symbol `'§'` (167).
   - Dedicated slots for custom icons:
     - `SLOT_PLAY = 256` for `'▶'` (U+25B6)
     - `SLOT_SWORD = 257` for `'⚔'` (U+2694)
     - `SLOT_HEART = 258` for `'♥'` (U+2665)
     - `SLOT_SKULL = 259` for `'☠'` (U+2620)
   - Tested against authentic `minecraft.ttf` (Monocraft) font with automatic procedural pixel-art fallbacks if run under headless system fonts.
2. **Minecraft Color Formatting (`§0`..`§f`, `§r`):**
   - 16 standard palette colors parsed during text iteration.
   - `§.` formatting codes are automatically stripped from width measurements (`getStringWidth`), ensuring alignment calculations remain pixel-exact.
3. **Bounded LRU Text Measurement Cache:**
   - 512-entry `LinkedHashMap` cache avoids redundant string scanning.
   - Query time for repeated strings reduced to **3.967 µs** for 1,000 lookups.
4. **Shadow Rendering Consistency:**
   - Shadow offset scales as $\max(1.0, \text{round}(1.0 \times \text{scale}))$.
   - Shadow color dynamically attenuates to 25% brightness of the currently active formatted text color (`(color & 0xFCFCFC) >> 2`).

---

## 7. Settings Integration

1. **`GameSettings.java`:**
   - Added `public int guiScale = 0;` (0 = Auto, 1 = 1x, 2 = 2x, 3 = 3x, 4 = 4x).
   - In `load()`: Enforces `Math.clamp(guiScale, 0, 4)` with automatic fallback to Auto (0) on missing/corrupt values.
   - In `save()`: Persisted cleanly to `options.json`.
   - Added cycling method `cycleGuiScale()` and display string `getGuiScaleDisplayString()`.
2. **`OptionsScreen.java`:**
   - Integrated the GUI Scale button into Row 3 alongside Fog Quality.
   - Clicking cycles `Auto -> 1x -> 2x -> 3x -> 4x -> Auto`, calling `app.applySettings()` to recompute scaling and reinitialize screen layouts in real-time.

---

## 8. Screen Migration Summary

| Screen | Coordinate System | Base Dimensions | Scale Behavior |
|---|---|:---:|---|
| **TitleScreen** | **Logical GUI** | 200×20 buttons, 260×71 logo | Proportional at all scales; centered menu; footer anchored to screen corners. |
| **OptionsScreen** | **Logical GUI** | 150×20 buttons, 200×20 Done | 2-column grid centered horizontally and vertically; GUI Scale button integrated. |
| **EscapeMenuScreen** | **Logical GUI** | 200×20 buttons | Centered game pause menu; split options row; crisp alignment. |
| **WorldSelectScreen** | Legacy Physical | Physical pixels | Unmodified; continues rendering via legacy bridge. |
| **WorldCreateScreen** | Legacy Physical | Physical pixels | Unmodified; continues rendering via legacy bridge. |
| **WorldEditScreen** | Legacy Physical | Physical pixels | Unmodified; continues rendering via legacy bridge. |
| **GameOverScreen** | Legacy Physical | Physical pixels | Unmodified; continues rendering via legacy bridge. |

---

## 9. Test Suite Execution & Results

`GuiPhase1VerificationTest.java` was implemented and integrated into `MasterTestSuite.java`. All 11 test scenarios passed:

| Test ID | Test Scenario | Verified Condition | Status |
|---|---|---|:---:|
| **1.1** | Standard Resolutions | 854x480 (s2), 1280x720 (s3), 1920x1080 (s4), 1440p (s6), 4K (s9) | **PASSED** |
| **1.2** | Manual Scale & Clamping | Requested 3 & 4 on 480p safely clamp to 2 with `isScaleClamped = true` | **PASSED** |
| **1.3** | Extreme Aspect Ratios | Ultrawide 21:9 (2560x1080 -> s4), sub-minimum (300x200 -> s1), portrait (600x900 -> s1) | **PASSED** |
| **1.4** | Coordinate Conversion | 4 corners and center round-trip between window and GUI space with $\Delta < 10^{-4}$ | **PASSED** |
| **1.5** | High-DPI Compensation | 2x Retina display (960x540 window vs 1920x1080 framebuffer) maps accurately | **PASSED** |
| **1.6** | Letterbox Rejection | Clicks on remainder letterbox pixels ($X \ge W_{\text{vp}}$ or $Y \ge H_{\text{vp}}$) rejected | **PASSED** |
| **1.7** | Layout Helpers | Centering, anchoring, row spacing, column height, and split widths match math | **PASSED** |
| **1.8** | Glyphs & § Formatting | Degree `'°'`, `'⚔'`, `'▶'`, `'♥'`, and color codes (`§e`, `§a`, `§r`) verified in atlas | **PASSED** |
| **1.9** | Text Measurement Cache | 1,000 queries of formatted strings completed in avg 3.967 µs with 0 garbage | **PASSED** |
| **1.10**| Screen Migration Contract | `usesLogicalCoordinates()` respected; logical screens receive virtual dimensions | **PASSED** |
| **1.11**| Phase 0 Regression Check | Dynamic quad batching, viewport isolation, and 8-point state restoration verified | **PASSED** |

**Engine Suite Regression:** **25 / 25 Test Suites Passed** (100% success rate across all world, physics, streaming, and GUI modules).

---

## 10. Performance Impact

Hardware: **NVIDIA GeForce RTX 4050 Laptop GPU / OpenGL 3.3 Core Profile**

| Workload | Phase 0 Baseline | Phase 1 Foundation | Delta |
|---|---|---|:---:|
| **Typical Menu Screen Frame Time** | 0.080 ms | **0.082 ms** | +0.002 ms (negligible) |
| **Draw Calls (OptionsScreen)** | 14 draw calls (6 buttons) | **16 draw calls** (7 buttons) | Exact batched draw calls (2 per button + background + title) |
| **Text Measurement (1,000 queries)** | ~45.0 µs (uncached traversal) | **3.967 µs** (LRU cache) | **11.3x speedup** |
| **Memory Allocation per Frame** | 0 bytes (pre-allocated) | **0 bytes** (zero allocation during rendering) | Unchanged |

---

## 11. Files Changed & Added

- **Added:**
  - `engine/src/com/mcjournal/client/gui/GuiScaleManager.java`
  - `engine/src/com/mcjournal/client/gui/GuiLayout.java`
  - `engine/src/com/mcjournal/test/GuiPhase1VerificationTest.java`
  - `GUI_PHASE_1_REPORT.md`
- **Modified:**
  - `engine/src/com/mcjournal/client/gui/FontRenderer.java`
  - `engine/src/com/mcjournal/client/gui/GuiRenderer.java`
  - `engine/src/com/mcjournal/client/gui/Screen.java`
  - `engine/src/com/mcjournal/client/gui/Button.java`
  - `engine/src/com/mcjournal/client/gui/TitleScreen.java`
  - `engine/src/com/mcjournal/client/gui/OptionsScreen.java`
  - `engine/src/com/mcjournal/client/gui/EscapeMenuScreen.java`
  - `engine/src/com/mcjournal/client/ScreenManager.java`
  - `engine/src/com/mcjournal/client/GameSettings.java`
  - `engine/src/com/mcjournal/client/InputHandler.java`
  - `engine/src/com/mcjournal/client/MCJournalApp.java`
  - `engine/src/com/mcjournal/test/GuiRendererVerificationTest.java`
  - `engine/src/com/mcjournal/test/MasterTestSuite.java`

---

## 12. Remaining Boundaries & Phase 2 Outlook

In accordance with strict Phase 1 constraints:
- No Phase 2 widget trees or composite controls were introduced.
- Unmigrated screens (`WorldSelectScreen`, `WorldCreateScreen`, `WorldEditScreen`, `GameOverScreen`) remain functional in legacy mode.
- The in-game HUD remains untouched.
- Visual art style and screen redesigns belong to future phases.

Phase 1 foundation is complete, verified, and ready for review.
