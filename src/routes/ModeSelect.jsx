import React from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { VideoSelector } from "../components/background/VideoSelector";

export function ModeSelect() {
  const navigate = useNavigate();

  return (
    <div className="mc-dirt-background">
      <div className="mc-container" style={{ justifyContent: "center" }}>
        <div
          className="mc-panel"
          style={{
            maxWidth: "540px",
            width: "100%",
            textAlign: "center",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: "28px",
            padding: "40px 24px"
          }}
        >
          {/* Logo Title */}
          <div>
            <div
              style={{
                fontSize: "12px",
                color: "#ffff55",
                letterSpacing: "4px",
                marginBottom: "8px",
                textShadow: "2px 2px 0 #000"
              }}
            >
              MINECRAFT
            </div>
            <h1 className="mc-title" style={{ fontSize: "28px", lineHeight: "1.3" }}>
              JOURNAL APP
            </h1>
            <div
              style={{
                fontSize: "10px",
                color: "#ffaa00",
                marginTop: "6px",
                textShadow: "1px 1px 0 #000"
              }}
            >
              v1.20.4 - SINGLEPLAYER EDITION
            </div>
          </div>

          {/* Background Wallpaper Switcher Bar */}
          <div
            style={{
              backgroundColor: "rgba(0, 0, 0, 0.6)",
              padding: "10px 14px",
              border: "2px solid #555",
              borderRadius: "4px",
              width: "100%",
              maxWidth: "420px"
            }}
          >
            <VideoSelector />
          </div>

          {/* Menu Options */}
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "16px",
              width: "100%",
              maxWidth: "360px"
            }}
          >
            <Button
              variant="green"
              onClick={() => navigate("/setup")}
              style={{ width: "100%", padding: "16px" }}
            >
              🎮 Singleplayer
            </Button>

            <Button
              disabled={true}
              tooltip="Coming soon in Multi-player release!"
              style={{ width: "100%", padding: "16px" }}
            >
              🌐 Multiplayer
            </Button>
          </div>

          <div
            style={{
              fontSize: "10px",
              color: "#666666",
              marginTop: "12px",
              borderTop: "2px solid #aaa",
              paddingTop: "12px",
              width: "100%"
            }}
          >
            Select Singleplayer to launch world setup & quest log.
          </div>
        </div>
      </div>
    </div>
  );
}
