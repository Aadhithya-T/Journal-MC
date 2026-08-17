import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../ui/Button";
import { TEXTURE_PACKS } from "./texturePacks";

export function EscapeMenuModal({
  isOpen,
  onResume,
  currentTexturePack,
  onSelectTexturePack,
  onOpenJournalDrawer
}) {
  const navigate = useNavigate();
  const [showPacksSubmenu, setShowPacksSubmenu] = useState(false);

  if (!isOpen) return null;

  const handleQuitToTitle = () => {
    navigate("/");
  };

  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0, 0, 0, 0.72)",
        backdropFilter: "blur(6px)",
        zIndex: 9999,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: "'Press Start 2P', monospace",
        userSelect: "none"
      }}
      onClick={onResume}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          width: "100%",
          maxWidth: "420px",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: "14px",
          textAlign: "center"
        }}
      >
        {/* Title */}
        <h2
          style={{
            fontSize: "15px",
            color: "#ffffff",
            textShadow: "2px 2px 0 #000000",
            marginBottom: "12px",
            letterSpacing: "1px"
          }}
        >
          {showPacksSubmenu ? "TEXTURE PACKS" : "GAME MENU"}
        </h2>

        {!showPacksSubmenu ? (
          /* Main Pause Menu */
          <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: "10px" }}>
            <Button
              onClick={onResume}
              style={{ width: "100%", height: "44px", fontSize: "11px" }}
            >
              Back to Game
            </Button>

            <Button
              onClick={() => setShowPacksSubmenu(true)}
              style={{ width: "100%", height: "44px", fontSize: "11px" }}
            >
              🎨 Texture Packs...
            </Button>

            <Button
              onClick={() => {
                onResume();
                onOpenJournalDrawer();
              }}
              style={{ width: "100%", height: "44px", fontSize: "11px" }}
            >
              📚 View Journal Log
            </Button>

            <div style={{ height: "12px" }} />

            <Button
              variant="dark"
              onClick={handleQuitToTitle}
              style={{ width: "100%", height: "44px", fontSize: "11px", borderColor: "#772222" }}
            >
              Save &amp; Quit to Title
            </Button>
          </div>
        ) : (
          /* Texture Packs Submenu */
          <div
            className="mc-panel-dark"
            style={{
              width: "100%",
              padding: "16px",
              display: "flex",
              flexDirection: "column",
              gap: "10px"
            }}
          >
            <div style={{ fontSize: "9px", color: "#aaaaaa", marginBottom: "4px" }}>
              Select active world texture pack:
            </div>

            {Object.values(TEXTURE_PACKS).map((pack) => {
              const isSelected = currentTexturePack === pack.id;
              return (
                <button
                  key={pack.id}
                  onClick={() => onSelectTexturePack(pack.id)}
                  style={{
                    fontFamily: "inherit",
                    fontSize: "10px",
                    textAlign: "left",
                    padding: "10px 12px",
                    backgroundColor: isSelected ? "#2e4a1f" : "#222222",
                    border: isSelected ? "2px solid #55ff55" : "2px solid #444444",
                    color: isSelected ? "#55ff55" : "#ffffff",
                    cursor: "pointer",
                    display: "flex",
                    flexDirection: "column",
                    gap: "4px"
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <span style={{ fontWeight: "bold" }}>{pack.name}</span>
                    {isSelected && <span style={{ color: "#55ff55" }}>✓ ACTIVE</span>}
                  </div>
                  <div style={{ fontSize: "8px", color: "#888888" }}>
                    {pack.description}
                  </div>
                </button>
              );
            })}

            <Button
              onClick={() => setShowPacksSubmenu(false)}
              style={{ width: "100%", height: "40px", fontSize: "10px", marginTop: "8px" }}
            >
              Done
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
