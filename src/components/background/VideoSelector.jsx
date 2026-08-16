import React, { useState } from "react";
import { useBackground } from "../../context/BackgroundContext";

export function VideoSelector({ compact = false }) {
  const { backgrounds, currentBackground, selectBackground, pickRandomBackground } = useBackground();
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="mc-bg-selector-wrapper">
      <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
        <span
          style={{
            fontSize: "10px",
            color: "#ffff55",
            textShadow: "1px 1px 0 #000",
            display: "flex",
            alignItems: "center",
            gap: "4px"
          }}
        >
          🖼️ Wallpaper: <strong>{currentBackground.icon} {currentBackground.name}</strong>
        </span>

        <button
          onClick={() => setIsOpen(!isOpen)}
          className="mc-button"
          style={{ padding: "4px 8px", fontSize: "10px", backgroundColor: "#3a3a3a" }}
          title="Change Live Video Background"
        >
          🎬 Switch {isOpen ? "▲" : "▼"}
        </button>

        <button
          onClick={pickRandomBackground}
          className="mc-button"
          style={{ padding: "4px 8px", fontSize: "10px", backgroundColor: "#2e7d32" }}
          title="Pick Random Wallpaper"
        >
          🎲 Random
        </button>
      </div>

      {isOpen && (
        <div
          className="mc-panel-dark"
          style={{
            position: "absolute",
            top: "100%",
            right: "0",
            marginTop: "8px",
            zIndex: 100,
            width: "280px",
            padding: "12px",
            borderRadius: "0px",
            boxShadow: "0 8px 16px rgba(0,0,0,0.8)"
          }}
        >
          <div
            style={{
              fontSize: "11px",
              color: "#ffaa00",
              marginBottom: "8px",
              borderBottom: "2px solid #444",
              paddingBottom: "4px"
            }}
          >
            SELECT LIVE BACKGROUND:
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            {backgrounds.map((bg) => {
              const isActive = bg.id === currentBackground.id;
              return (
                <button
                  key={bg.id}
                  onClick={() => {
                    selectBackground(bg.id);
                    setIsOpen(false);
                  }}
                  className="mc-button"
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "8px 10px",
                    fontSize: "10px",
                    textAlign: "left",
                    backgroundColor: isActive ? "#2e7d32" : "#4a4a4a",
                    borderTopColor: isActive ? "#4caf50" : "#666",
                    width: "100%"
                  }}
                >
                  <span>
                    {bg.icon} {bg.name}
                  </span>
                  {isActive && <span style={{ color: "#ffff55" }}>✔ Active</span>}
                </button>
              );
            })}

            <button
              onClick={() => {
                pickRandomBackground();
                setIsOpen(false);
              }}
              className="mc-button"
              style={{
                padding: "8px 10px",
                fontSize: "10px",
                backgroundColor: "#1b5e20",
                marginTop: "4px",
                width: "100%"
              }}
            >
              🎲 Roll Random Wallpaper
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
