import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { MinecraftFrame } from "../components/layout/MinecraftFrame";
import { Button } from "../components/ui/Button";
import { useWorld } from "../hooks/useWorld";

export function WorldSetup() {
  const navigate = useNavigate();
  const { createWorld, loading } = useWorld();

  const [name, setName] = useState("New World");
  const [validationError, setValidationError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setValidationError("World name cannot be empty!");
      return;
    }

    try {
      // Hardcoded Hardcore mode & default plains biome
      await createWorld({ name, hardcore: true, biome: "plains" });
      navigate("/journal");
    } catch (err) {
      console.error("World creation failed", err);
    }
  };

  return (
    <MinecraftFrame title="Create New World">
      <div
        className="mc-panel"
        style={{
          maxWidth: "520px",
          width: "100%",
          display: "flex",
          flexDirection: "column",
          gap: "24px",
          padding: "32px 28px"
        }}
      >
        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
          {/* World Name Input */}
          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <label style={{ fontSize: "16px", color: "#373737", textShadow: "1px 1px 0 #ffffff" }}>
              World Name
            </label>
            <input
              type="text"
              className="mc-input"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                setValidationError("");
              }}
              placeholder="Enter world name..."
              maxLength={32}
              autoFocus
              style={{ fontSize: "18px", padding: "12px 14px" }}
            />
            {validationError && (
              <span style={{ color: "#aa0000", fontSize: "13px" }}>{validationError}</span>
            )}
          </div>

          {/* Hardcore Mode Status Card (Fixed to Hardcore) */}
          <div
            style={{
              backgroundColor: "#1b1b1b",
              border: "2px solid #000000",
              borderTopColor: "#373737",
              borderLeftColor: "#373737",
              borderRightColor: "#555555",
              borderBottomColor: "#555555",
              padding: "16px",
              display: "flex",
              flexDirection: "column",
              gap: "6px"
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <span style={{ fontSize: "20px" }}>🖤</span>
              <span style={{ fontSize: "17px", color: "#ff5555", textShadow: "2px 2px 0 #2a0000" }}>
                Game Mode: Hardcore
              </span>
            </div>
            <div style={{ fontSize: "13px", color: "#aaaaaa", lineHeight: "1.4", marginLeft: "28px" }}>
              Same as Survival mode, locked at hardest difficulty and one life only.
            </div>
          </div>

          {/* Actions: Create New World & Cancel */}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              gap: "14px",
              marginTop: "8px"
            }}
          >
            <Button
              type="submit"
              disabled={loading}
              style={{ flex: 1, height: "46px", fontSize: "17px" }}
            >
              {loading ? "Generating..." : "Create New World"}
            </Button>

            <Button
              type="button"
              onClick={() => navigate("/")}
              style={{ flex: 1, height: "46px", fontSize: "17px" }}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </MinecraftFrame>
  );
}
