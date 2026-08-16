import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { MinecraftFrame } from "../components/layout/MinecraftFrame";
import { Button } from "../components/ui/Button";
import { HotbarTags } from "../components/journal/HotbarTags";
import { EntryCard } from "../components/journal/EntryCard";
import { useWorld } from "../hooks/useWorld";
import { useJournalEntries } from "../hooks/useJournalEntries";

export function Journal() {
  const navigate = useNavigate();
  const { currentWorld } = useWorld();
  const { entries, loading, addEntry, deleteEntry } = useJournalEntries(currentWorld?.id);

  const [activeTag, setActiveTag] = useState("all");
  const [isWriting, setIsWriting] = useState(false);
  const [newTitle, setNewTitle] = useState("");
  const [newBody, setNewBody] = useState("");
  const [selectedTag, setSelectedTag] = useState("quest");

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

  // Filter entries based on hotbar tag selection
  const filteredEntries = entries.filter((entry) => {
    if (activeTag === "all") return true;
    return entry.tags && entry.tags.includes(activeTag);
  });

  const handleSaveEntry = async (e) => {
    e.preventDefault();
    if (!newBody.trim()) return;

    await addEntry({
      title: newTitle.trim() || "Day Log",
      body: newBody.trim(),
      tags: [selectedTag]
    });

    setNewTitle("");
    setNewBody("");
    setIsWriting(false);
  };

  return (
    <MinecraftFrame>
      <div
        style={{
          width: "100%",
          maxWidth: "900px",
          display: "flex",
          flexDirection: "column",
          gap: "24px"
        }}
      >
        {/* World Header Card */}
        <div
          className="mc-panel"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: "16px"
          }}
        >
          <div>
            <div style={{ fontSize: "10px", color: "#666", marginBottom: "4px" }}>JOURNAL SHELL</div>
            <h2 className="mc-subhead" style={{ fontSize: "20px", color: "#ffff55" }}>
              📖 {currentWorld.name}
            </h2>
            <div style={{ fontSize: "10px", color: "#555", marginTop: "4px" }}>
              Mode: {currentWorld.hardcore ? "🖤 Hardcore" : "💚 Survival"} | Biome: {currentWorld.biome}
            </div>
          </div>

          <Button variant="green" onClick={() => setIsWriting(!isWriting)}>
            {isWriting ? "✖ Close Quill" : "✍️ Write Entry"}
          </Button>
        </div>

        {/* Create Entry Modal/Panel */}
        {isWriting && (
          <div className="mc-panel" style={{ backgroundColor: "#212121", color: "#fff" }}>
            <h3 style={{ fontSize: "14px", color: "#ffff55", marginBottom: "16px" }}>
              ✍️ WRITE NEW JOURNAL ENTRY
            </h3>
            <form onSubmit={handleSaveEntry} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <input
                type="text"
                className="mc-input"
                placeholder="Entry Title (e.g., Discovered Axolotl Pool)"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
              />

              <textarea
                className="mc-input"
                rows={4}
                placeholder="Record your adventuring notes..."
                value={newBody}
                onChange={(e) => setNewBody(e.target.value)}
                style={{ resize: "vertical" }}
              />

              <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
                <span style={{ fontSize: "11px", color: "#aaa" }}>Tag:</span>
                <select
                  value={selectedTag}
                  onChange={(e) => setSelectedTag(e.target.value)}
                  style={{
                    fontFamily: "'Press Start 2P', monospace",
                    fontSize: "11px",
                    padding: "8px",
                    backgroundColor: "#000",
                    color: "#fff",
                    border: "2px solid #555"
                  }}
                >
                  <option value="quest">Quest</option>
                  <option value="building">Build</option>
                  <option value="mining">Mining</option>
                  <option value="combat">Combat</option>
                  <option value="exploration">Explore</option>
                  <option value="gathering">Gather</option>
                  <option value="lore">Lore</option>
                  <option value="alchemy">Brewing</option>
                </select>

                <Button variant="green" type="submit" style={{ marginLeft: "auto" }}>
                  Save Entry
                </Button>
              </div>
            </form>
          </div>
        )}

        {/* Character Sketch Area & Hotbar Tags Bar */}
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "240px 1fr",
            gap: "20px",
            alignItems: "start"
          }}
        >
          {/* Character Sketch Area */}
          <div
            className="mc-panel-dark"
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: "12px",
              padding: "20px",
              textAlign: "center"
            }}
          >
            <div style={{ fontSize: "11px", color: "#ffaa00" }}>CHARACTER SKETCH</div>
            
            {/* Pixel Steve Rendering SVG Graphic */}
            <div
              style={{
                width: "120px",
                height: "160px",
                backgroundColor: "#5b8731",
                border: "4px solid #000",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                position: "relative",
                backgroundImage: "linear-gradient(to bottom, #7cbd3f 0%, #3e681c 100%)",
                boxShadow: "inset 0 0 10px rgba(0,0,0,0.5)"
              }}
            >
              {/* Pixel Steve Avatar Graphic */}
              <div
                style={{
                  width: "48px",
                  height: "48px",
                  backgroundColor: "#cba383", // skin tone
                  border: "2px solid #22160d",
                  position: "relative"
                }}
              >
                {/* Hair */}
                <div style={{ width: "100%", height: "16px", backgroundColor: "#3a2512" }} />
                {/* Eyes */}
                <div
                  style={{
                    position: "absolute",
                    top: "22px",
                    left: "6px",
                    width: "10px",
                    height: "6px",
                    backgroundColor: "#ffffff"
                  }}
                >
                  <div style={{ width: "5px", height: "6px", backgroundColor: "#3a6fc0" }} />
                </div>
                <div
                  style={{
                    position: "absolute",
                    top: "22px",
                    right: "6px",
                    width: "10px",
                    height: "6px",
                    backgroundColor: "#ffffff"
                  }}
                >
                  <div style={{ width: "5px", height: "6px", backgroundColor: "#3a6fc0" }} />
                </div>
                {/* Nose / Beard */}
                <div
                  style={{
                    position: "absolute",
                    bottom: "6px",
                    left: "14px",
                    width: "20px",
                    height: "8px",
                    backgroundColor: "#4a301a"
                  }}
                />
              </div>

              {/* Cyan Shirt */}
              <div style={{ width: "64px", height: "48px", backgroundColor: "#00a8a8", marginTop: "4px", border: "2px solid #005050" }} />
              {/* Blue Jeans */}
              <div style={{ width: "48px", height: "40px", backgroundColor: "#1e3a8a", border: "2px solid #0f172a" }} />
            </div>

            <div style={{ fontSize: "10px", color: "#ffff55" }}>Steve (Adventurer)</div>
            <div style={{ fontSize: "9px", color: "#aaaaaa" }}>Level 1 Singleplayer</div>
          </div>

          {/* Entries Content & Hotbar */}
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            {/* Hotbar Tags component */}
            <div className="mc-panel-dark" style={{ padding: "16px" }}>
              <HotbarTags activeTag={activeTag} onSelectTag={(tag) => setActiveTag(tag)} />
            </div>

            {/* Entries Display / Empty State */}
            {loading ? (
              <div className="mc-panel" style={{ textAlign: "center", padding: "24px" }}>
                Checking quest log...
              </div>
            ) : filteredEntries.length === 0 ? (
              /* No entries empty state */
              <div
                className="mc-panel"
                style={{
                  textAlign: "center",
                  padding: "40px 24px",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "16px"
                }}
              >
                <div style={{ fontSize: "40px" }}>📜</div>
                <h3 style={{ fontSize: "14px", color: "#222" }}>
                  NO JOURNAL ENTRIES YET
                </h3>
                <p style={{ fontSize: "11px", color: "#555", maxWidth: "400px", lineHeight: "1.5" }}>
                  Your world <strong>"{currentWorld.name}"</strong> currently has no recorded journal entries.
                </p>
                <Button variant="green" onClick={() => setIsWriting(true)}>
                  ✍️ Record First Journal Entry
                </Button>
              </div>
            ) : (
              /* Entries List */
              <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                {filteredEntries.map((entry) => (
                  <EntryCard key={entry.id} entry={entry} onDelete={deleteEntry} />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </MinecraftFrame>
  );
}
