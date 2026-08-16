import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { MinecraftFrame } from "../components/layout/MinecraftFrame";
import { Button } from "../components/ui/Button";
import { Toggle } from "../components/ui/Toggle";
import { useWorld } from "../hooks/useWorld";
import { BIOME_METADATA } from "../lib/biome";

export function WorldSetup() {
  const navigate = useNavigate();
  const { createWorld, loading } = useWorld();

  const [name, setName] = useState("New World");
  const [hardcore, setHardcore] = useState(false);
  const [biome, setBiome] = useState("plains");
  const [validationError, setValidationError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setValidationError("World name cannot be empty!");
      return;
    }

    try {
      await createWorld({ name, hardcore, biome });
      navigate("/journal");
    } catch (err) {
      console.error("World creation failed", err);
    }
  };

  return (
    <MinecraftFrame title="CREATE NEW WORLD">
      <div
        className="mc-panel"
        style={{
          maxWidth: "600px",
          width: "100%",
          display: "flex",
          flexDirection: "column",
          gap: "24px"
        }}
      >
        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
          {/* World Name Input */}
          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <label style={{ fontSize: "12px", color: "#222" }}>World Name</label>
            <input
              type="text"
              className="mc-input"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                setValidationError("");
              }}
              placeholder="e.g. My Survival Journal"
              maxLength={32}
              autoFocus
            />
            {validationError && (
              <span style={{ color: "#aa0000", fontSize: "10px" }}>{validationError}</span>
            )}
          </div>

          {/* Game Mode / Hardcore Toggle */}
          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <label style={{ fontSize: "12px", color: "#222" }}>Game Mode</label>
            <Toggle
              label={hardcore ? "Hardcore Mode (1 Life)" : "Survival Mode"}
              checked={hardcore}
              onChange={(val) => setHardcore(val)}
              description={
                hardcore
                  ? "WARNING: World locks on death! Ultra dark styling and high stakes."
                  : "Standard survival journal with respawn enabled."
              }
            />
          </div>

          {/* Biome Selector */}
          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <label style={{ fontSize: "12px", color: "#222" }}>Starting Biome</label>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))",
                gap: "10px"
              }}
            >
              {Object.entries(BIOME_METADATA).map(([key, data]) => {
                const isSelected = biome === key;
                return (
                  <div
                    key={key}
                    onClick={() => setBiome(key)}
                    style={{
                      border: "3px solid #000",
                      borderColor: isSelected ? "#ffff55" : "#373737",
                      backgroundColor: isSelected ? "#4a4a4a" : "#222222",
                      color: "#ffffff",
                      padding: "10px",
                      cursor: "pointer",
                      display: "flex",
                      flexDirection: "column",
                      gap: "4px"
                    }}
                  >
                    <span style={{ fontSize: "14px" }}>
                      {data.icon} {data.name}
                    </span>
                    <span style={{ fontSize: "9px", color: "#aaaaaa" }}>{data.temperature}</span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Actions */}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              gap: "16px",
              marginTop: "12px"
            }}
          >
            <Button variant="green" type="submit" disabled={loading} style={{ flex: 1 }}>
              {loading ? "Generating..." : "⚡ Create New World"}
            </Button>

            <Button onClick={() => navigate("/")} style={{ flex: 1 }}>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </MinecraftFrame>
  );
}
