import React from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { VideoSelector } from "../components/background/VideoSelector";

export function ModeSelect() {
  const navigate = useNavigate();

  return (
    <div className="mc-dirt-background" style={{ minHeight: "100vh", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
      {/* Top Banner / Logo Area */}
      <div className="mc-container" style={{ justifyContent: "center", paddingTop: "60px", paddingBottom: "40px" }}>
        <div style={{ textAlign: "center", marginBottom: "36px" }}>
          <div
            style={{
              fontSize: "14px",
              color: "#ffff55",
              letterSpacing: "4px",
              marginBottom: "8px",
              textShadow: "2px 2px 0 #000"
            }}
          >
            MINECRAFT
          </div>
          <h1 className="mc-title" style={{ fontSize: "38px", lineHeight: "1.2", margin: "0 auto" }}>
            JOURNAL APP
          </h1>
          <div
            style={{
              fontSize: "12px",
              color: "#ffaa00",
              marginTop: "8px",
              textShadow: "2px 2px 0 #000"
            }}
          >
            v1.20.4 - SINGLEPLAYER EDITION
          </div>
        </div>

        {/* Menu Buttons Stack matching Minecraft Main Menu */}
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "10px",
            width: "100%",
            maxWidth: "400px",
            alignItems: "center"
          }}
        >
          <Button
            onClick={() => navigate("/setup")}
            style={{ width: "100%", height: "46px", fontSize: "18px" }}
          >
            Singleplayer
          </Button>

          <Button
            disabled={true}
            tooltip="Coming soon in Multiplayer release!"
            style={{ width: "100%", height: "46px", fontSize: "18px" }}
          >
            Multiplayer
          </Button>

          {/* Bottom Row Buttons (Options / Switch Wallpaper) */}
          <div style={{ display: "flex", gap: "10px", width: "100%", marginTop: "6px" }}>
            <div style={{ flex: 1 }}>
              <VideoSelector />
            </div>
          </div>
        </div>
      </div>

      {/* Footer info note */}
      <div
        style={{
          textAlign: "center",
          padding: "16px",
          fontSize: "12px",
          color: "#cccccc",
          textShadow: "1px 1px 0 #000",
          zIndex: 2
        }}
      >
        Minecraft Journal v1.20.4 • Pair Programming Edition
      </div>
    </div>
  );
}
