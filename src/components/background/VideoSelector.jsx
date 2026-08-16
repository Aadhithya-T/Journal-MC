import React, { useState } from "react";
import { useBackground } from "../../context/BackgroundContext";

export function VideoSelector({ compact = false }) {
  const { backgrounds, currentBackground, selectBackground, pickRandomBackground } = useBackground();
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="mc-bg-selector-wrapper" style={{ width: compact ? "auto" : "100%" }}>
      {compact ? (
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="mc-button"
            style={{ padding: "6px 12px", fontSize: "14px" }}
            title="Switch Wallpaper"
          >
            {currentBackground.icon} Wallpaper {isOpen ? "▲" : "▼"}
          </button>
          <button
            onClick={pickRandomBackground}
            className="mc-button"
            style={{ padding: "6px 12px", fontSize: "14px" }}
            title="Pick Random Wallpaper"
          >
            🎲
          </button>
        </div>
      ) : (
        <div style={{ display: "flex", gap: "10px", width: "100%" }}>
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="mc-button"
            style={{ flex: 1, height: "46px", fontSize: "16px" }}
          >
            Options: {currentBackground.icon} {isOpen ? "Close ▲" : "Wallpaper ▼"}
          </button>
          <button
            onClick={pickRandomBackground}
            className="mc-button"
            style={{ width: "54px", height: "46px", fontSize: "18px" }}
            title="Pick Random Wallpaper"
          >
            🎲
          </button>
        </div>
      )}

      {isOpen && (
        <div
          className="mc-panel-dark"
          style={{
            position: "absolute",
            bottom: compact ? "auto" : "100%",
            top: compact ? "100%" : "auto",
            left: "50%",
            transform: "translateX(-50%)",
            marginBottom: compact ? "0" : "8px",
            marginTop: compact ? "8px" : "0",
            zIndex: 200,
            width: "320px",
            padding: "16px",
            boxShadow: "0 8px 24px rgba(0,0,0,0.9)",
            border: "3px solid #555",
            borderTopColor: "#888",
            borderLeftColor: "#888",
            borderRightColor: "#222",
            borderBottomColor: "#222"
          }}
        >
          <div
            style={{
              fontSize: "14px",
              color: "#ffff55",
              marginBottom: "12px",
              borderBottom: "2px solid #444",
              paddingBottom: "6px",
              textAlign: "center"
            }}
          >
            Select Live Wallpaper
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
                    padding: "10px 14px",
                    fontSize: "14px",
                    textAlign: "left",
                    color: isActive ? "#ffffa0" : "#e0e0e0",
                    width: "100%"
                  }}
                >
                  <span>
                    {bg.icon} {bg.name}
                  </span>
                  {isActive && <span style={{ color: "#ffff55", fontSize: "12px" }}>Active</span>}
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
                padding: "10px 14px",
                fontSize: "14px",
                marginTop: "4px",
                width: "100%",
                color: "#ffff55"
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
