import React from "react";
import { useNavigate } from "react-router-dom";
import { MinecraftFrame } from "../components/layout/MinecraftFrame";
import { Button } from "../components/ui/Button";
import { useWorld } from "../hooks/useWorld";
import { getBiome, getBiomeDetails } from "../lib/biome";

export function Profile() {
  const navigate = useNavigate();
  const { currentWorld } = useWorld();

  if (!currentWorld) {
    return (
      <MinecraftFrame title="PROFILE">
        <div className="mc-panel" style={{ textAlign: "center", maxWidth: "500px", padding: "32px" }}>
          <p style={{ marginBottom: "20px", fontSize: "14px" }}>
            No active singleplayer world loaded.
          </p>
          <Button variant="green" onClick={() => navigate("/setup")}>
            🎮 Setup World
          </Button>
        </div>
      </MinecraftFrame>
    );
  }

  // Get raw biome field via biome.js stub
  const rawBiome = getBiome(currentWorld);
  // Get full biome metadata for display
  const biomeDetails = getBiomeDetails(rawBiome);

  // 3x9 Inventory Grid slots array
  const inventorySlots = Array.from({ length: 27 }, (_, i) => i + 1);

  return (
    <MinecraftFrame title="PLAYER PROFILE">
      <div
        style={{
          width: "100%",
          maxWidth: "880px",
          display: "flex",
          flexDirection: "column",
          gap: "24px"
        }}
      >
        {/* Top Profile Summary Panel */}
        <div
          className="mc-panel"
          style={{
            display: "grid",
            gridTemplateColumns: "220px 1fr",
            gap: "24px",
            alignItems: "center"
          }}
        >
          {/* Steve Render Box */}
          <div
            style={{
              backgroundColor: "#181818",
              border: "4px solid #000",
              padding: "16px",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: "12px",
              boxShadow: "inset 0 0 10px #000"
            }}
          >
            <div style={{ fontSize: "11px", color: "#ffff55" }}>CHARACTER</div>

            {/* Steve Avatar Graphic */}
            <div
              style={{
                width: "64px",
                height: "64px",
                backgroundColor: "#cba383",
                border: "3px solid #000",
                position: "relative"
              }}
            >
              <div style={{ width: "100%", height: "20px", backgroundColor: "#3a2512" }} />
              <div
                style={{
                  position: "absolute",
                  top: "30px",
                  left: "8px",
                  width: "14px",
                  height: "8px",
                  backgroundColor: "#ffffff"
                }}
              >
                <div style={{ width: "7px", height: "8px", backgroundColor: "#3a6fc0" }} />
              </div>
              <div
                style={{
                  position: "absolute",
                  top: "30px",
                  right: "8px",
                  width: "14px",
                  height: "8px",
                  backgroundColor: "#ffffff"
                }}
              >
                <div style={{ width: "7px", height: "8px", backgroundColor: "#3a6fc0" }} />
              </div>
              <div
                style={{
                  position: "absolute",
                  bottom: "8px",
                  left: "18px",
                  width: "28px",
                  height: "10px",
                  backgroundColor: "#4a301a"
                }}
              />
            </div>

            <span style={{ fontSize: "12px", color: "#fff" }}>Steve</span>
            <span style={{ fontSize: "9px", color: "#aaaaaa" }}>Skin: Default Classic</span>
          </div>

          {/* Biome Overview Card */}
          <div
            style={{
              background: biomeDetails.bgGradient,
              border: "3px solid #000",
              padding: "20px",
              color: "#ffffff",
              display: "flex",
              flexDirection: "column",
              gap: "10px",
              boxShadow: "0 4px 12px rgba(0,0,0,0.4)"
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontSize: "10px", letterSpacing: "2px", color: "#ffff55" }}>
                WORLD BIOME STUB
              </span>
              <span style={{ fontSize: "20px" }}>{biomeDetails.icon}</span>
            </div>

            <h2 style={{ fontSize: "22px", fontFamily: "'Press Start 2P', monospace", margin: "4px 0" }}>
              {biomeDetails.name}
            </h2>

            <p style={{ fontSize: "11px", lineHeight: "1.4", color: "#e0e0e0" }}>
              {biomeDetails.description}
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "8px", marginTop: "8px", fontSize: "10px" }}>
              <div>
                <span style={{ color: "#ffff55" }}>Temp: </span>
                {biomeDetails.temperature}
              </div>
              <div>
                <span style={{ color: "#ffff55" }}>Primary Block: </span>
                {biomeDetails.primaryBlock}
              </div>
            </div>
          </div>
        </div>

        {/* Inventory Placeholder Grid Section */}
        <div className="mc-panel-dark" style={{ padding: "24px" }}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "16px"
            }}
          >
            <span style={{ fontSize: "12px", color: "#ffff55" }}>INVENTORY & EQUIPMENT (PLACEHOLDER)</span>
            <span style={{ fontSize: "10px", color: "#aaaaaa" }}>3x9 Storage Slots</span>
          </div>

          <div style={{ display: "flex", gap: "24px", flexWrap: "wrap", alignItems: "center", justifyContent: "center" }}>
            {/* Armor Slots */}
            <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
              <div className="mc-slot" title="Helmet Slot">🪖</div>
              <div className="mc-slot" title="Chestplate Slot">👕</div>
              <div className="mc-slot" title="Leggings Slot">👖</div>
              <div className="mc-slot" title="Boots Slot">🥾</div>
            </div>

            {/* Main 3x9 Inventory Grid */}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(9, 44px)",
                gap: "4px",
                backgroundColor: "#8b8b8b",
                padding: "8px",
                border: "3px solid #373737"
              }}
            >
              {/* Preset Sample Items in inventory */}
              <div className="mc-slot" title="Diamond Sword">🗡️</div>
              <div className="mc-slot" title="Oak Wood x64">🪵</div>
              <div className="mc-slot" title="Book and Quill">📜</div>
              <div className="mc-slot" title="Golden Apple">🍎</div>
              <div className="mc-slot" title="Crafting Table">📦</div>
              <div className="mc-slot" title="Torch x32">🕯️</div>
              {inventorySlots.slice(6).map((slotNum) => (
                <div key={slotNum} className="mc-slot" />
              ))}
            </div>
          </div>
        </div>
      </div>
    </MinecraftFrame>
  );
}
