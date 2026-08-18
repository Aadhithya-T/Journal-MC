import React, { useState } from "react";
import { Button } from "../ui/Button";

export function WorldHUD({
  worldName,
  biome = "Plains",
  hardcore = false,
  coords = { x: 0, y: 1, z: 0 },
  nearbyPOI = null,
  selectedSlot = 0,
  onSelectSlot,
  onOpenBookModal,
  onMineBlock,
  toastMessage,
  entries = [],
  onOpenEscapeMenu,
  showDrawer,
  setShowDrawer,
  isSprinting = false,
  isSneaking = false,
  isLocked = false
}) {
  const [drawerTag, setDrawerTag] = useState("all");

  const filteredEntries = entries.filter((e) => {
    if (drawerTag === "all") return true;
    return e.tags && e.tags.includes(drawerTag);
  });

  return (
    <div
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        pointerEvents: "none",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        padding: "12px",
        zIndex: 50,
        fontFamily: "'Press Start 2P', monospace",
        userSelect: "none"
      }}
    >
      {/* --- TOP STATUS BAR --- */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          pointerEvents: "auto",
          gap: "10px",
          flexWrap: "wrap"
        }}
      >
        <div
          className="mc-panel-dark"
          style={{
            padding: "8px 12px",
            display: "flex",
            alignItems: "center",
            gap: "12px",
            boxShadow: "0 4px 12px rgba(0,0,0,0.6)"
          }}
        >
          <div style={{ fontSize: "11px", color: "#ffff55", textShadow: "1px 1px 0 #000" }}>
            🌍 {worldName}
          </div>

          <div style={{ fontSize: "9px", color: "#aaaaaa", textShadow: "1px 1px 0 #000", display: "flex", gap: "8px" }}>
            <span>Biome: <strong style={{ color: "#7cbd3f" }}>{biome}</strong></span>
            <span>{hardcore ? "🖤 Hardcore" : "💚 Survival"}</span>
          </div>

          <div style={{ fontSize: "9px", color: "#55ffff", textShadow: "1px 1px 0 #000" }}>
            XYZ: {coords.x} / {coords.y} / {coords.z}
          </div>

          <div style={{ fontSize: "8px", color: "#dddddd", display: "flex", gap: "6px" }}>
            {isSprinting && <span style={{ color: "#55ff55" }}>⚡ SPRINT</span>}
            {isSneaking && <span style={{ color: "#55ffff" }}>🛡️ SNEAK</span>}
          </div>
        </div>

        {/* Top Right Action Buttons */}
        <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
          <Button
            variant="green"
            onClick={() => setShowDrawer(!showDrawer)}
            style={{ fontSize: "9px", height: "34px", padding: "0 10px" }}
          >
            📚 Journals ({entries.length})
          </Button>

          <Button
            variant="dark"
            onClick={() => onOpenBookModal(null)}
            style={{ fontSize: "9px", height: "34px", padding: "0 10px" }}
          >
            ✍️ Write
          </Button>

          <Button
            variant="gray"
            onClick={onOpenEscapeMenu}
            style={{ fontSize: "9px", height: "34px", padding: "0 10px" }}
            title="Game Menu (Escape)"
          >
            ⚙️ Menu (ESC)
          </Button>
        </div>
      </div>

      {/* --- CENTER MINECRAFT CROSSHAIR --- */}
      {!showDrawer && (
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: "50%",
            transform: "translate(-50%, -50%)",
            pointerEvents: "none",
            zIndex: 60,
            display: "flex",
            alignItems: "center",
            justifyContent: "center"
          }}
        >
          <svg width="24" height="24" viewBox="0 0 24 24" style={{ filter: "drop-shadow(1px 1px 0px rgba(0,0,0,0.8))" }}>
            <rect x="2" y="10" width="20" height="4" fill="#ffffff" opacity="0.9" />
            <rect x="10" y="2" width="4" height="20" fill="#ffffff" opacity="0.9" />
            <rect x="10" y="10" width="4" height="4" fill="#444444" opacity="0.8" />
          </svg>
        </div>
      )}

      {/* Click to Lock Cursor prompt when not locked */}
      {!isLocked && !showDrawer && (
        <div
          style={{
            alignSelf: "center",
            pointerEvents: "none",
            backgroundColor: "rgba(0, 0, 0, 0.7)",
            border: "1px solid #777777",
            color: "#ffffff",
            padding: "6px 14px",
            fontSize: "9px",
            textShadow: "1px 1px 0 #000",
            marginBottom: "8px"
          }}
        >
          🖱️ Click screen to enable Minecraft mouse look (ESC to free cursor)
        </div>
      )}

      {/* --- CENTER FLOATING TOAST NOTIFICATION --- */}
      {toastMessage && (
        <div
          style={{
            alignSelf: "center",
            pointerEvents: "auto",
            backgroundColor: "rgba(0, 0, 0, 0.85)",
            border: "2px solid #55ff55",
            color: "#ffffff",
            padding: "8px 16px",
            fontSize: "10px",
            textShadow: "1px 1px 0 #000",
            boxShadow: "0 0 16px rgba(85,255,85,0.4)"
          }}
        >
          ✨ {toastMessage}
        </div>
      )}

      {/* --- NEARBY POI PROMPT BANNER --- */}
      {nearbyPOI && (
        <div
          style={{
            alignSelf: "center",
            pointerEvents: "auto",
            backgroundColor: "#2a1b08",
            border: "2px solid #ffaa00",
            color: "#ffdd55",
            padding: "10px 18px",
            fontSize: "10px",
            display: "flex",
            alignItems: "center",
            gap: "14px",
            boxShadow: "0 8px 24px rgba(0,0,0,0.8)",
            textShadow: "1px 1px 0 #000"
          }}
        >
          <div>
            <div style={{ fontSize: "11px", color: "#ffff55" }}>{nearbyPOI.name}</div>
            <div style={{ fontSize: "8px", color: "#cccccc", marginTop: "3px" }}>
              {nearbyPOI.subtitle || nearbyPOI.excerpt}
            </div>
          </div>
          <Button
            variant="green"
            onClick={() => onOpenBookModal(nearbyPOI)}
            style={{ fontSize: "9px", padding: "6px 10px" }}
          >
            ✍️ Open Book [E]
          </Button>
        </div>
      )}

      {/* --- AUTHENTIC MINECRAFT BOTTOM HUD (Hearts + Hunger + 9-Slot Hotbar) --- */}
      <div
        style={{
          alignSelf: "center",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: "4px",
          pointerEvents: "auto",
          marginBottom: "6px"
        }}
      >
        {/* Row 1: 10 Hearts (Left) & 10 Drumsticks (Right) */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            width: "360px",
            padding: "0 4px"
          }}
        >
          {/* 10 Red Hearts */}
          <div style={{ display: "flex", gap: "2px" }}>
            {[...Array(10)].map((_, i) => (
              <svg key={i} width="16" height="15" viewBox="0 0 16 15" style={{ filter: "drop-shadow(1px 1px 0 #000)" }}>
                <path
                  d="M2 2 H6 V4 H8 V4 H10 V2 H14 V6 H16 V9 H14 V11 H12 V13 H10 V15 H6 V13 H4 V11 H2 V9 H0 V6 H2 Z"
                  fill="#111111"
                />
                <path
                  d="M3 3 H5 V5 H7 V7 H9 V5 H11 V3 H13 V6 H15 V8 H13 V10 H11 V12 H9 V14 H7 V12 H5 V10 H3 V8 H1 V6 H3 Z"
                  fill="#e52521"
                />
                <rect x="3" y="3" width="2" height="2" fill="#ffffff" />
              </svg>
            ))}
          </div>

          {/* 10 Hunger Drumsticks */}
          <div style={{ display: "flex", gap: "2px" }}>
            {[...Array(10)].map((_, i) => (
              <svg key={i} width="16" height="15" viewBox="0 0 16 15" style={{ filter: "drop-shadow(1px 1px 0 #000)" }}>
                <path
                  d="M6 1 H11 V3 H13 V5 H15 V10 H13 V12 H11 V14 H9 V14 H7 V12 H5 V10 H5 V7 H7 V5 H6 Z"
                  fill="#111111"
                />
                <path
                  d="M7 2 H10 V4 H12 V6 H14 V9 H12 V11 H10 V13 H8 V11 H6 V9 H6 V7 H8 V4 H7 Z"
                  fill="#b5651d"
                />
                <rect x="8" y="3" width="2" height="2" fill="#d28a3f" />
                <rect x="3" y="11" width="3" height="3" fill="#ffffff" stroke="#111" strokeWidth="1" />
              </svg>
            ))}
          </div>
        </div>

        {/* Row 2: Green XP Level Progress Bar */}
        <div
          style={{
            width: "360px",
            height: "5px",
            backgroundColor: "#000000",
            border: "1px solid #333333",
            position: "relative"
          }}
        >
          <div
            style={{
              width: "100%",
              height: "100%",
              backgroundColor: "#55ff55",
              boxShadow: "0 0 4px #55ff55"
            }}
          />
        </div>

        {/* Row 3: Authentic 9-Slot Minecraft Hotbar */}
        <div
          style={{
            display: "flex",
            backgroundColor: "rgba(0, 0, 0, 0.55)",
            border: "2px solid #3c3c3c",
            borderRadius: "2px",
            padding: "2px",
            boxShadow: "0 6px 18px rgba(0,0,0,0.85)"
          }}
        >
          {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((slotIdx) => {
            const isSelected = selectedSlot === slotIdx;
            return (
              <div
                key={slotIdx}
                onClick={() => onSelectSlot && onSelectSlot(slotIdx)}
                style={{
                  width: "38px",
                  height: "38px",
                  backgroundColor: isSelected ? "rgba(255, 255, 255, 0.15)" : "rgba(0, 0, 0, 0.35)",
                  border: isSelected ? "3px solid #ffffff" : "2px solid #222222",
                  boxShadow: isSelected ? "inset 0 0 6px rgba(255,255,255,0.6)" : "inset 0 0 4px rgba(0,0,0,0.8)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "18px",
                  cursor: "pointer",
                  margin: "1px"
                }}
              >
                {slotIdx === 0 && <span title="[1] Book & Quill">📖</span>}
                {slotIdx === 1 && <span title="[2] Diamond Pickaxe">⛏️</span>}
                {slotIdx === 2 && <span title="[3] Oak Planks">🪵</span>}
                {slotIdx === 3 && <span title="[4] Wild Poppy">🌹</span>}
                {slotIdx === 4 && <span title="[5] Torch">🕯️</span>}
                {slotIdx === 5 && <span title="[6] Cobblestone">🪨</span>}
                {slotIdx === 6 && <span title="[7] Sand">🏖️</span>}
                {slotIdx === 7 && <span title="[8] Birch Log">🌲</span>}
                {slotIdx === 8 && <span title="[9] Stone">🧱</span>}
              </div>
            );
          })}
        </div>
      </div>

      {/* --- IN-WORLD JOURNAL INVENTORY DRAWER --- */}
      {showDrawer && (
        <div
          style={{
            position: "fixed",
            right: 0,
            top: 0,
            bottom: 0,
            width: "360px",
            backgroundColor: "#1b1b1b",
            borderLeft: "4px solid #373737",
            boxShadow: "-8px 0 28px rgba(0,0,0,0.85)",
            padding: "16px",
            display: "flex",
            flexDirection: "column",
            gap: "12px",
            zIndex: 9000,
            pointerEvents: "auto",
            color: "#fff",
            overflowY: "auto"
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h3 style={{ fontSize: "11px", color: "#ffff55", margin: 0, textShadow: "1px 1px 0 #000" }}>
              📚 WORLD JOURNALS
            </h3>
            <button
              onClick={() => setShowDrawer(false)}
              style={{
                background: "#aa0000",
                color: "#fff",
                border: "2px solid #000",
                fontSize: "10px",
                padding: "3px 6px",
                cursor: "pointer",
                fontFamily: "inherit"
              }}
            >
              ✕
            </button>
          </div>

          {/* Filter Tags */}
          <div style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
            {["all", "quest", "building", "mining", "exploration", "lore"].map((t) => (
              <button
                key={t}
                onClick={() => setDrawerTag(t)}
                style={{
                  fontFamily: "inherit",
                  fontSize: "8px",
                  padding: "4px 8px",
                  backgroundColor: drawerTag === t ? "#ffff55" : "#333333",
                  color: drawerTag === t ? "#000000" : "#ffffff",
                  border: "1px solid #555555",
                  cursor: "pointer"
                }}
              >
                {t.toUpperCase()}
              </button>
            ))}
          </div>

          {/* Journal Entries List */}
          {filteredEntries.length === 0 ? (
            <div style={{ fontSize: "9px", color: "#aaaaaa", textAlign: "center", marginTop: "20px", lineHeight: "1.6" }}>
              No journals recorded yet.<br />Approach a Lectern or press [E] to record!
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {filteredEntries.map((e) => (
                <div
                  key={e.id}
                  style={{
                    backgroundColor: "#2a2a2a",
                    border: "2px solid #555555",
                    padding: "10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: "6px"
                  }}
                >
                  <div style={{ fontSize: "10px", color: "#ffaa00", textShadow: "1px 1px 0 #000" }}>{e.title}</div>
                  <div style={{ fontSize: "8px", color: "#dddddd", lineHeight: "1.5" }}>{e.body}</div>
                  <div style={{ fontSize: "7px", color: "#888888", display: "flex", justifyContent: "space-between", marginTop: "2px" }}>
                    <span>TAG: {e.tags ? e.tags.join(", ").toUpperCase() : "GENERAL"}</span>
                    <span>{new Date(e.created_at || Date.now()).toLocaleDateString()}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
