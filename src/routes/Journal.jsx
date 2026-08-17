import React from "react";
import { useNavigate } from "react-router-dom";
import { MinecraftFrame } from "../components/layout/MinecraftFrame";
import { Button } from "../components/ui/Button";
import { useWorld } from "../hooks/useWorld";
import { useJournalEntries } from "../hooks/useJournalEntries";
import { MinecraftWorldCanvas } from "../components/world/MinecraftWorldCanvas";

export function Journal() {
  const navigate = useNavigate();
  const { currentWorld } = useWorld();
  const { entries, addEntry } = useJournalEntries(currentWorld?.id);

  if (!currentWorld) {
    return (
      <MinecraftFrame title="NO WORLD LOADED">
        <div className="mc-panel" style={{ textAlign: "center", maxWidth: "500px", padding: "32px" }}>
          <p style={{ marginBottom: "20px", fontSize: "14px" }}>
            No active singleplayer world found. Please create or select a world first.
          </p>
          <Button variant="green" onClick={() => navigate("/setup")}>
            🎮 Create New World
          </Button>
        </div>
      </MinecraftFrame>
    );
  }

  // 3D Minecraft Interactive World (Pure 3D View)
  return (
    <MinecraftWorldCanvas
      world={currentWorld}
      entries={entries}
      onAddEntry={addEntry}
    />
  );
}
