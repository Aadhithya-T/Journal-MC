import React, { useState } from "react";
import { Button } from "../ui/Button";

export function WorldHUD({
  worldName,
  biome = "Plains",
  hardcore = false,
  coords = { x: 0, y: 1, z: 0 },
  nearbyPOI = null,
  onOpenBookModal,
  onMineBlock,
  toastMessage,
  entries = [],
  onOpenEscapeMenu,
  showDrawer,
  setShowDrawer
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
      {/* --- TOP HUD BAR --- */}
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
        {/* Subtle World Status & Coordinates Badge */}
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

      {/* --- CENTER FLOATING TOAST NOTIFICATION --- */}
      {toastMessage && (
        <div
          style={{
            alignSelf: "center",
            pointerEvents: "auto",
            backgroundColor: "rgba(0, 0, 0, 0.82)",
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
              {nearbyPOI.description}
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

      {/* --- BOTTOM ACTION BUTTONS (NO GUIDELINE BAR) --- */}
      <div
        style={{
          display: "flex",
          justifyContent: "flex-end",
          alignItems: "center",
          pointerEvents: "auto",
          gap: "8px"
        }}
      >
        <Button
          variant="green"
          onClick={() => onOpenBookModal(null)}
          style={{ fontSize: "9px", height: "36px", padding: "0 12px" }}
        >
          ✍️ Write Journal
        </Button>

        <Button
          variant="gray"
          onClick={onMineBlock}
          style={{ fontSize: "9px", height: "36px", padding: "0 12px" }}
        >
          ⛏️ Mine Block
        </Button>
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
