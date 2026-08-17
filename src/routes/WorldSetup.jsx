import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { useWorld } from "../hooks/useWorld";

export function WorldSetup() {
  const navigate = useNavigate();
  const { worlds, currentWorld, selectWorld, createWorld, deleteWorld, loading } = useWorld();

  const hasWorld = worlds.length >= 1;
  const [viewMode, setViewMode] = useState("list");
  const [selectedWorldId, setSelectedWorldId] = useState(currentWorld?.id || (worlds[0]?.id ?? null));
  const [searchQuery, setSearchQuery] = useState("");

  // Create World Form State
  const [name, setName] = useState("New World");
  const [hardcore, setHardcore] = useState(true);
  const [validationError, setValidationError] = useState("");

  const filteredWorlds = worlds.filter((w) =>
    w.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handlePlaySelectedWorld = () => {
    if (!selectedWorldId) return;
    selectWorld(selectedWorldId);
    navigate("/journal");
  };

  const handleCreateWorldSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) {
      setValidationError("World name cannot be empty!");
      return;
    }

    try {
      await createWorld({ name: name.trim(), hardcore, biome: "plains" });
      navigate("/journal");
    } catch (err) {
      console.error("World creation failed", err);
    }
  };

  const handleDeleteSelectedWorld = async () => {
    if (!selectedWorldId) return;
    const target = worlds.find((w) => w.id === selectedWorldId);
    if (target && window.confirm(`Are you sure you want to delete "${target.name}"? It will be lost forever!`)) {
      await deleteWorld(selectedWorldId);
      const remaining = worlds.filter((w) => w.id !== selectedWorldId);
      if (remaining.length > 0) {
        setSelectedWorldId(remaining[0].id);
      } else {
        setSelectedWorldId(null);
        setViewMode("create");
      }
    }
  };

  // ==========================================
  // VIEW 1: SELECT WORLD SCREEN (Image 2 Match)
  // ==========================================
  if (viewMode === "list") {
    return (
      <div
        className="mc-dirt-background"
        style={{
          minHeight: "100vh",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          fontFamily: "'Press Start 2P', monospace",
          userSelect: "none"
        }}
      >
        {/* Top Header & Search Bar */}
        <div
          style={{
            textAlign: "center",
            paddingTop: "24px",
            paddingBottom: "12px",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: "10px",
            backgroundColor: "rgba(0, 0, 0, 0.4)",
            borderBottom: "2px solid #333333"
          }}
        >
          <h1
            style={{
              fontSize: "14px",
              color: "#ffffff",
              textShadow: "2px 2px 0 #000000",
              margin: 0,
              letterSpacing: "1px"
            }}
          >
            Select World
          </h1>

          <input
            type="text"
            className="mc-input"
            placeholder="Search..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              maxWidth: "380px",
              width: "90%",
              padding: "6px 10px",
              fontSize: "11px",
              backgroundColor: "#000000",
              color: "#ffffff",
              border: "2px solid #555555",
              textAlign: "center"
            }}
          />
        </div>

        {/* Center Scrollable World List */}
        <div
          style={{
            flex: 1,
            display: "flex",
            justifyContent: "center",
            padding: "16px",
            overflowY: "auto"
          }}
        >
          <div
            style={{
              width: "100%",
              maxWidth: "600px",
              display: "flex",
              flexDirection: "column",
              gap: "8px"
            }}
          >
            {filteredWorlds.length === 0 ? (
              <div
                style={{
                  textAlign: "center",
                  padding: "40px 16px",
                  fontSize: "11px",
                  color: "#aaaaaa",
                  textShadow: "1px 1px 0 #000"
                }}
              >
                No worlds found. Click "Create New World" below to start!
              </div>
            ) : (
              filteredWorlds.map((world) => {
                const isSelected = selectedWorldId === world.id;
                const formattedDate = new Date(world.created_at || Date.now()).toLocaleDateString("en-GB", {
                  day: "2-digit",
                  month: "2-digit",
                  year: "2-digit"
                });
                const formattedTime = new Date(world.created_at || Date.now()).toLocaleTimeString([], {
                  hour: "2-digit",
                  minute: "2-digit"
                });

                return (
                  <div
                    key={world.id}
                    onClick={() => setSelectedWorldId(world.id)}
                    onDoubleClick={handlePlaySelectedWorld}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "14px",
                      padding: "8px 12px",
                      backgroundColor: isSelected ? "rgba(0, 0, 0, 0.8)" : "rgba(0, 0, 0, 0.5)",
                      border: isSelected ? "2px solid #ffffff" : "2px solid rgba(0, 0, 0, 0.4)",
                      outline: isSelected ? "1px solid #ffffff" : "none",
                      cursor: "pointer",
                      transition: "all 0.1s"
                    }}
                  >
                    {/* World Thumbnail / Grass Block Icon */}
                    <div
                      style={{
                        width: "48px",
                        height: "48px",
                        backgroundColor: "#5c8e32",
                        border: "2px solid #000000",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: "24px",
                        flexShrink: 0,
                        backgroundImage: "linear-gradient(to bottom, #7ebd42 0%, #3c641d 100%)"
                      }}
                    >
                      🌱
                    </div>

                    {/* World Info */}
                    <div style={{ display: "flex", flexDirection: "column", gap: "4px", overflow: "hidden" }}>
                      <div
                        style={{
                          fontSize: "12px",
                          color: "#ffffff",
                          textShadow: "1px 1px 0 #000",
                          fontWeight: "bold"
                        }}
                      >
                        {world.name}
                      </div>
                      <div style={{ fontSize: "9px", color: "#888888", textShadow: "1px 1px 0 #000" }}>
                        {world.name} ({formattedDate}, {formattedTime})
                      </div>
                      <div style={{ fontSize: "9px", color: "#aaaaaa", textShadow: "1px 1px 0 #000" }}>
                        {world.hardcore ? "Hardcore Mode" : "Survival Mode"}, Version: 1.20.4
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Bottom Minecraft Toolbar matching Image 2 */}
        <div
          style={{
            padding: "16px",
            backgroundColor: "rgba(0, 0, 0, 0.6)",
            borderTop: "2px solid #333333",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: "10px"
          }}
        >
          {/* Row 1 */}
          <div style={{ display: "flex", gap: "10px", width: "100%", maxWidth: "560px" }}>
            <Button
              onClick={handlePlaySelectedWorld}
              disabled={!selectedWorldId}
              style={{ flex: 1, height: "42px", fontSize: "11px" }}
            >
              Play Selected World
            </Button>

            {/* Disabled once a singleplayer world exists */}
            <Button
              disabled={hasWorld}
              onClick={() => {
                if (!hasWorld) {
                  setName("New World");
                  setViewMode("create");
                }
              }}
              tooltip={hasWorld ? "Only 1 world allowed! Delete existing world to create a new one." : undefined}
              style={{
                flex: 1,
                height: "42px",
                fontSize: "11px",
                opacity: hasWorld ? 0.5 : 1,
                cursor: hasWorld ? "not-allowed" : "pointer"
              }}
            >
              Create New World
            </Button>
          </div>

          {/* Row 2 */}
          <div style={{ display: "flex", gap: "8px", width: "100%", maxWidth: "560px" }}>
            <Button
              disabled={!selectedWorldId}
              onClick={() => {
                const target = worlds.find((w) => w.id === selectedWorldId);
                const newTitle = prompt("Rename World:", target?.name);
                if (newTitle && newTitle.trim()) {
                  // rename
                }
              }}
              style={{ flex: 1, height: "38px", fontSize: "10px" }}
            >
              Edit
            </Button>

            <Button
              disabled={!selectedWorldId}
              onClick={handleDeleteSelectedWorld}
              style={{ flex: 1, height: "38px", fontSize: "10px" }}
            >
              Delete
            </Button>

            <Button
              disabled={!selectedWorldId}
              onClick={handlePlaySelectedWorld}
              style={{ flex: 1, height: "38px", fontSize: "10px" }}
            >
              Re-Create
            </Button>

            <Button
              onClick={() => navigate("/")}
              style={{ flex: 1, height: "38px", fontSize: "10px" }}
            >
              Cancel
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // ==========================================
  // VIEW 2: CREATE NEW WORLD SCREEN
  // ==========================================
  return (
    <div
      className="mc-dirt-background"
      style={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        alignItems: "center",
        padding: "24px 16px",
        fontFamily: "'Press Start 2P', monospace",
        userSelect: "none"
      }}
    >
      {/* Title */}
      <div style={{ textAlign: "center", marginTop: "16px" }}>
        <h1
          style={{
            fontSize: "14px",
            color: "#ffffff",
            textShadow: "2px 2px 0 #000000",
            margin: 0,
            letterSpacing: "1px"
          }}
        >
          Create New World
        </h1>
      </div>

      {/* Center Setup Panel */}
      <div
        style={{
          width: "100%",
          maxWidth: "460px",
          display: "flex",
          flexDirection: "column",
          gap: "20px"
        }}
      >
        <form onSubmit={handleCreateWorldSubmit} style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
          {/* World Name Field */}
          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <label style={{ fontSize: "10px", color: "#aaaaaa", textShadow: "1px 1px 0 #000" }}>
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
              style={{
                fontSize: "12px",
                padding: "10px 12px",
                backgroundColor: "#000000",
                color: "#ffffff",
                border: "2px solid #555555"
              }}
            />
            {validationError && (
              <span style={{ color: "#ff5555", fontSize: "9px" }}>{validationError}</span>
            )}
          </div>

          {/* Game Mode Selector Card */}
          <div
            className="mc-panel-dark"
            style={{
              padding: "14px",
              display: "flex",
              flexDirection: "column",
              gap: "8px",
              backgroundColor: "#1b1b1b"
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <div style={{ fontSize: "11px", color: hardcore ? "#ff5555" : "#55ff55" }}>
                Game Mode: {hardcore ? "🖤 Hardcore" : "💚 Survival"}
              </div>
              <button
                type="button"
                onClick={() => setHardcore(!hardcore)}
                style={{
                  fontFamily: "inherit",
                  fontSize: "9px",
                  padding: "6px 10px",
                  backgroundColor: "#333333",
                  color: "#ffffff",
                  border: "2px solid #555555",
                  cursor: "pointer"
                }}
              >
                Toggle Mode
              </button>
            </div>
            <div style={{ fontSize: "9px", color: "#aaaaaa", lineHeight: "1.4" }}>
              {hardcore
                ? "Locked at hardest difficulty and single life only."
                : "Standard survival mode with exploration and crafting."}
            </div>
          </div>

          {/* Bottom Action Buttons */}
          <div style={{ display: "flex", gap: "12px", marginTop: "12px" }}>
            <Button
              type="submit"
              disabled={loading}
              style={{ flex: 1, height: "44px", fontSize: "11px" }}
            >
              {loading ? "Generating..." : "Create New World"}
            </Button>

            <Button
              type="button"
              onClick={() => {
                if (worlds.length > 0) {
                  setViewMode("list");
                } else {
                  navigate("/");
                }
              }}
              style={{ flex: 1, height: "44px", fontSize: "11px" }}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>

      <div style={{ height: "20px" }} />
    </div>
  );
}
